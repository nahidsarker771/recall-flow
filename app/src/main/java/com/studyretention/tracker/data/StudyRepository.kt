package com.studyretention.tracker.data

import androidx.room.withTransaction
import com.studyretention.tracker.logic.ReviewState
import com.studyretention.tracker.logic.SpacedRepetitionScheduler
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray
import org.json.JSONObject

fun Topic.toReviewState(): ReviewState = ReviewState(
    easeFactor = easeFactor,
    intervalDays = intervalDays,
    repetitions = repetitions,
    masteryLevel = masteryLevel,
)

class StudyRepository(
    private val database: RecallFlowDatabase,
    private val preferences: AppPreferences,
) {
    private val topicDao = database.topicDao()
    private val sessionDao = database.reviewSessionDao()
    private val eventDao = database.scheduledEventDao()
    private val scheduler = SpacedRepetitionScheduler()

    val topics: Flow<List<Topic>> = topicDao.observeAll()
    val sessions: Flow<List<ReviewSession>> = sessionDao.observeAll()
    val events: Flow<List<ScheduledEvent>> = eventDao.observeAll()

    fun observeTopic(id: Long): Flow<Topic?> = topicDao.observeById(id)

    fun observeSessionsForTopic(id: Long): Flow<List<ReviewSession>> =
        sessionDao.observeForTopic(id)

    suspend fun addTopic(name: String, notes: String, category: String): Long {
        val now = System.currentTimeMillis()
        val topic = Topic(
            name = name.trim(),
            notes = notes.trim(),
            category = category.trim(),
            createdAt = now,
            nextReviewAt = scheduler.firstReviewAt(now),
        )
        return database.withTransaction {
            val id = topicDao.insert(topic)
            if (preferences.settings.value.autoSchedule) {
                eventDao.insert(ScheduledEvent(topicId = id, scheduledTime = topic.nextReviewAt))
            }
            id
        }
    }

    suspend fun updateTopic(topic: Topic) {
        topicDao.update(topic)
    }

    suspend fun deleteTopic(topic: Topic) {
        topicDao.delete(topic)
    }

    suspend fun saveReview(topicId: Long, rating: Int, durationSeconds: Int) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            val topic = topicDao.getById(topicId) ?: return@withTransaction
            val result = scheduler.schedule(
                state = topic.toReviewState(),
                rating = rating,
                nowMillis = now,
                algorithm = preferences.settings.value.algorithm,
            )
            val duration = durationSeconds.coerceIn(0, 24 * 60 * 60)

            sessionDao.insert(
                ReviewSession(
                    topicId = topicId,
                    timestamp = now,
                    recallRating = rating.coerceIn(0, 5),
                    durationSeconds = duration,
                ),
            )
            topicDao.update(
                topic.copy(
                    lastReviewedAt = now,
                    nextReviewAt = result.nextReviewAt,
                    easeFactor = result.easeFactor,
                    intervalDays = result.intervalDays,
                    repetitions = result.repetitions,
                    masteryLevel = result.masteryLevel,
                    totalTimeStudied = topic.totalTimeStudied + duration,
                ),
            )
            eventDao.completePendingForTopic(topicId)
            if (preferences.settings.value.autoSchedule) {
                eventDao.insert(
                    ScheduledEvent(topicId = topicId, scheduledTime = result.nextReviewAt),
                )
            }
        }
    }

    suspend fun snoozeTopic(topicId: Long, durationMillis: Long) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            val topic = topicDao.getById(topicId) ?: return@withTransaction
            val next = maxOf(topic.nextReviewAt, now) + durationMillis
            topicDao.update(topic.copy(nextReviewAt = next))
            eventDao.movePendingForTopic(topicId, next)
        }
    }

    suspend fun overrideInterval(topicId: Long, days: Int) {
        val now = System.currentTimeMillis()
        database.withTransaction {
            val topic = topicDao.getById(topicId) ?: return@withTransaction
            val result = scheduler.overrideInterval(topic.toReviewState(), days, now)
            topicDao.update(
                topic.copy(
                    intervalDays = result.intervalDays,
                    nextReviewAt = result.nextReviewAt,
                    masteryLevel = result.masteryLevel,
                ),
            )
            val moved = eventDao.pendingCountForTopic(topicId) > 0
            if (moved) {
                eventDao.movePendingForTopic(topicId, result.nextReviewAt)
            } else if (preferences.settings.value.autoSchedule) {
                eventDao.insert(
                    ScheduledEvent(topicId = topicId, scheduledTime = result.nextReviewAt),
                )
            }
        }
    }

    suspend fun addEvent(topicId: Long, scheduledTime: Long) {
        eventDao.insert(ScheduledEvent(topicId = topicId, scheduledTime = scheduledTime))
    }

    suspend fun moveEvent(eventId: Long, newTime: Long) {
        database.withTransaction {
            val event = eventDao.getById(eventId) ?: return@withTransaction
            eventDao.moveEvent(eventId, newTime)
            if (!event.isCompleted) {
                topicDao.setNextReviewAt(event.topicId, newTime)
            }
        }
    }

    suspend fun setEventCompleted(eventId: Long, completed: Boolean) {
        eventDao.setCompleted(eventId, completed)
    }

    suspend fun deleteEvent(eventId: Long) {
        eventDao.deleteById(eventId)
    }

    /** Fills the calendar for every topic that has no pending review event. */
    suspend fun ensureAutoScheduleEvents() {
        val topics = topicDao.getAll()
        database.withTransaction {
            topics.forEach { topic ->
                if (eventDao.pendingCountForTopic(topic.id) == 0) {
                    eventDao.insert(
                        ScheduledEvent(topicId = topic.id, scheduledTime = topic.nextReviewAt),
                    )
                }
            }
        }
    }

    suspend fun nextUpcoming(now: Long): Topic? = topicDao.nextUpcoming(now)

    suspend fun earliestTopic(): Topic? = topicDao.earliest()

    // ---- JSON export / import (offline backup, no network) ----

    suspend fun exportJson(): String {
        val topics = topicDao.getAll()
        val sessions = sessionDao.getAll()
        val events = eventDao.getAll()

        val root = JSONObject()
        root.put("app", "RecallFlow")
        root.put("schema", 1)
        root.put("exportedAt", System.currentTimeMillis())

        root.put("topics", JSONArray().apply {
            topics.forEach { topic ->
                put(JSONObject().apply {
                    put("id", topic.id)
                    put("name", topic.name)
                    put("notes", topic.notes)
                    put("category", topic.category)
                    put("createdAt", topic.createdAt)
                    put("lastReviewedAt", topic.lastReviewedAt ?: JSONObject.NULL)
                    put("nextReviewAt", topic.nextReviewAt)
                    put("easeFactor", topic.easeFactor)
                    put("intervalDays", topic.intervalDays)
                    put("repetitions", topic.repetitions)
                    put("masteryLevel", topic.masteryLevel)
                    put("totalTimeStudied", topic.totalTimeStudied)
                })
            }
        })

        root.put("sessions", JSONArray().apply {
            sessions.forEach { session ->
                put(JSONObject().apply {
                    put("id", session.id)
                    put("topicId", session.topicId)
                    put("timestamp", session.timestamp)
                    put("recallRating", session.recallRating)
                    put("durationSeconds", session.durationSeconds)
                })
            }
        })

        root.put("events", JSONArray().apply {
            events.forEach { event ->
                put(JSONObject().apply {
                    put("id", event.id)
                    put("topicId", event.topicId)
                    put("scheduledTime", event.scheduledTime)
                    put("isCompleted", event.isCompleted)
                })
            }
        })

        return root.toString(2)
    }

    /**
     * Replaces the local database with the supplied backup. Returns false when the
     * JSON is unreadable or contains no topics. Sessions/events pointing at missing
     * topics are skipped so foreign keys stay valid.
     */
    suspend fun importJson(json: String): Boolean {
        return runCatching {
            val root = JSONObject(json)
            val topicArray = root.optJSONArray("topics") ?: return false

            val importedTopics = (0 until topicArray.length()).mapNotNull { index ->
                val o = topicArray.optJSONObject(index) ?: return@mapNotNull null
                val name = o.optString("name").trim()
                if (name.isEmpty()) return@mapNotNull null
                Topic(
                    id = o.optLong("id"),
                    name = name,
                    notes = o.optString("notes"),
                    category = o.optString("category"),
                    createdAt = o.optLong("createdAt", System.currentTimeMillis()),
                    lastReviewedAt = if (o.isNull("lastReviewedAt")) null else o.optLong("lastReviewedAt"),
                    nextReviewAt = o.optLong("nextReviewAt", System.currentTimeMillis()),
                    easeFactor = o.optDouble("easeFactor", 2.5),
                    intervalDays = o.optInt("intervalDays", 0),
                    repetitions = o.optInt("repetitions", 0),
                    masteryLevel = o.optInt("masteryLevel", 0),
                    totalTimeStudied = o.optLong("totalTimeStudied", 0L),
                )
            }
            if (importedTopics.isEmpty()) return false

            val topicIds = importedTopics.map { it.id }.toSet()
            val sessionArray = root.optJSONArray("sessions")
            val importedSessions = if (sessionArray == null) emptyList() else
                (0 until sessionArray.length()).mapNotNull { index ->
                    val o = sessionArray.optJSONObject(index) ?: return@mapNotNull null
                    val topicId = o.optLong("topicId")
                    if (topicId !in topicIds) return@mapNotNull null
                    ReviewSession(
                        id = o.optLong("id"),
                        topicId = topicId,
                        timestamp = o.optLong("timestamp"),
                        recallRating = o.optInt("recallRating", 3).coerceIn(0, 5),
                        durationSeconds = o.optInt("durationSeconds", 0),
                    )
                }

            val eventArray = root.optJSONArray("events")
            val importedEvents = if (eventArray == null) emptyList() else
                (0 until eventArray.length()).mapNotNull { index ->
                    val o = eventArray.optJSONObject(index) ?: return@mapNotNull null
                    val topicId = o.optLong("topicId")
                    if (topicId !in topicIds) return@mapNotNull null
                    ScheduledEvent(
                        id = o.optLong("id"),
                        topicId = topicId,
                        scheduledTime = o.optLong("scheduledTime"),
                        isCompleted = o.optBoolean("isCompleted", false),
                    )
                }

            database.withTransaction {
                eventDao.deleteAll()
                sessionDao.deleteAll()
                topicDao.deleteAll()
                importedTopics.forEach { topicDao.insert(it) }
                importedSessions.forEach { sessionDao.insert(it) }
                importedEvents.forEach { eventDao.insert(it) }
            }
            true
        }.getOrDefault(false)
    }
}

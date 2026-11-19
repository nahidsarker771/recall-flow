package com.studyretention.tracker.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TopicDao {

    @Query("SELECT * FROM topics ORDER BY nextReviewAt ASC")
    fun observeAll(): Flow<List<Topic>>

    @Query("SELECT * FROM topics WHERE id = :id")
    fun observeById(id: Long): Flow<Topic?>

    @Query("SELECT * FROM topics WHERE id = :id")
    suspend fun getById(id: Long): Topic?

    @Query("SELECT * FROM topics ORDER BY name COLLATE NOCASE ASC")
    suspend fun getAll(): List<Topic>

    @Query("SELECT * FROM topics WHERE nextReviewAt > :now ORDER BY nextReviewAt ASC LIMIT 1")
    suspend fun nextUpcoming(now: Long): Topic?

    @Query("UPDATE topics SET nextReviewAt = :time WHERE id = :topicId")
    suspend fun setNextReviewAt(topicId: Long, time: Long)

    @Query("SELECT * FROM topics ORDER BY nextReviewAt ASC LIMIT 1")
    suspend fun earliest(): Topic?

    @Insert
    suspend fun insert(topic: Topic): Long

    @Update
    suspend fun update(topic: Topic)

    @Delete
    suspend fun delete(topic: Topic)

    @Query("DELETE FROM topics")
    suspend fun deleteAll()
}

@Dao
interface ReviewSessionDao {

    @Query("SELECT * FROM review_sessions ORDER BY timestamp DESC")
    fun observeAll(): Flow<List<ReviewSession>>

    @Query("SELECT * FROM review_sessions WHERE topicId = :topicId ORDER BY timestamp DESC")
    fun observeForTopic(topicId: Long): Flow<List<ReviewSession>>

    @Query("SELECT * FROM review_sessions ORDER BY timestamp ASC")
    suspend fun getAll(): List<ReviewSession>

    @Insert
    suspend fun insert(session: ReviewSession): Long

    @Query("DELETE FROM review_sessions")
    suspend fun deleteAll()
}

@Dao
interface ScheduledEventDao {

    @Query("SELECT * FROM scheduled_events ORDER BY scheduledTime ASC")
    fun observeAll(): Flow<List<ScheduledEvent>>

    @Query("SELECT * FROM scheduled_events ORDER BY scheduledTime ASC")
    suspend fun getAll(): List<ScheduledEvent>

    @Query("SELECT * FROM scheduled_events WHERE id = :id")
    suspend fun getById(id: Long): ScheduledEvent?

    @Query("SELECT COUNT(*) FROM scheduled_events WHERE topicId = :topicId AND isCompleted = 0")
    suspend fun pendingCountForTopic(topicId: Long): Int

    @Insert
    suspend fun insert(event: ScheduledEvent): Long

    @Update
    suspend fun update(event: ScheduledEvent)

    @Query("UPDATE scheduled_events SET scheduledTime = :time WHERE topicId = :topicId AND isCompleted = 0")
    suspend fun movePendingForTopic(topicId: Long, time: Long)

    @Query("UPDATE scheduled_events SET scheduledTime = :time WHERE id = :id")
    suspend fun moveEvent(id: Long, time: Long)

    @Query("UPDATE scheduled_events SET isCompleted = :completed WHERE id = :id")
    suspend fun setCompleted(id: Long, completed: Boolean)

    @Query("UPDATE scheduled_events SET isCompleted = 1 WHERE topicId = :topicId AND isCompleted = 0")
    suspend fun completePendingForTopic(topicId: Long)

    @Query("DELETE FROM scheduled_events WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM scheduled_events")
    suspend fun deleteAll()
}

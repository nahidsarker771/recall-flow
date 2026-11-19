package com.studyretention.tracker.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "topics")
data class Topic(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val notes: String = "",
    val category: String = "",
    val createdAt: Long = System.currentTimeMillis(),
    val lastReviewedAt: Long? = null,
    val nextReviewAt: Long,
    val easeFactor: Double = 2.5,
    val intervalDays: Int = 0,
    val repetitions: Int = 0,
    val masteryLevel: Int = 0,
    val totalTimeStudied: Long = 0L,
)

@Entity(
    tableName = "review_sessions",
    foreignKeys = [
        ForeignKey(
            entity = Topic::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("topicId"), Index("timestamp")],
)
data class ReviewSession(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val topicId: Long,
    val timestamp: Long,
    val recallRating: Int,
    val durationSeconds: Int,
)

@Entity(
    tableName = "scheduled_events",
    foreignKeys = [
        ForeignKey(
            entity = Topic::class,
            parentColumns = ["id"],
            childColumns = ["topicId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("topicId"), Index("scheduledTime")],
)
data class ScheduledEvent(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val topicId: Long,
    val scheduledTime: Long,
    val isCompleted: Boolean = false,
)

package com.studyretention.tracker.data

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [Topic::class, ReviewSession::class, ScheduledEvent::class],
    version = 1,
    exportSchema = false,
)
abstract class RecallFlowDatabase : RoomDatabase() {
    abstract fun topicDao(): TopicDao
    abstract fun reviewSessionDao(): ReviewSessionDao
    abstract fun scheduledEventDao(): ScheduledEventDao
}

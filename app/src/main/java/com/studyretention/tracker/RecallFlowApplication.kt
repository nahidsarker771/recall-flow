package com.studyretention.tracker

import android.app.Application
import com.studyretention.tracker.data.AppContainer
import com.studyretention.tracker.notifications.DueCheckWorker
import com.studyretention.tracker.notifications.NotificationHelper
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class RecallFlowApplication : Application() {

    lateinit var container: AppContainer
        private set

    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
        NotificationHelper.createChannel(this)
        DueCheckWorker.enqueue(this)
        appScope.launch { container.alarmScheduler.sync() }
    }
}

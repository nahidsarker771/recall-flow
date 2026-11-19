package com.studyretention.tracker.notifications

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.studyretention.tracker.RecallFlowApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return
        val app = context.applicationContext as? RecallFlowApplication ?: return
        DueCheckWorker.enqueue(context)
        val pendingResult = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.IO).launch {
            try {
                app.container.alarmScheduler.sync()
            } finally {
                pendingResult.finish()
            }
        }
    }
}

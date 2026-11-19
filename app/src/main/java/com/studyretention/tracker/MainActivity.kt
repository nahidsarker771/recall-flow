package com.studyretention.tracker

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyretention.tracker.data.ThemeMode
import com.studyretention.tracker.notifications.NotificationHelper
import com.studyretention.tracker.ui.RecallFlowApp
import com.studyretention.tracker.ui.theme.RecallFlowTheme

class MainActivity : ComponentActivity() {

    private val reviewTopicId = mutableStateOf<Long?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        reviewTopicId.value = reviewTopicFrom(intent)

        val container = (application as RecallFlowApplication).container

        setContent {
            val settings by container.preferences.settings.collectAsStateWithLifecycle()
            val darkTheme = when (settings.themeMode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            RecallFlowTheme(
                darkTheme = darkTheme,
                dynamicColor = settings.dynamicColor,
            ) {
                val permissionLauncher = rememberLauncherForActivityResult(
                    ActivityResultContracts.RequestPermission(),
                ) { }
                var askedForNotifications by rememberSaveable { mutableStateOf(false) }

                LaunchedEffect(settings.notificationsEnabled) {
                    if (!askedForNotifications &&
                        container.onboarding.isCompleted &&
                        settings.notificationsEnabled &&
                        Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                        ContextCompat.checkSelfPermission(
                            this@MainActivity,
                            Manifest.permission.POST_NOTIFICATIONS,
                        ) != PackageManager.PERMISSION_GRANTED
                    ) {
                        askedForNotifications = true
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                }

                RecallFlowApp(
                    initialReviewTopicId = reviewTopicId.value,
                    onDeepLinkConsumed = { reviewTopicId.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        reviewTopicId.value = reviewTopicFrom(intent)
    }

    private fun reviewTopicFrom(intent: Intent?): Long? {
        intent ?: return null
        val extra = intent.getLongExtra(NotificationHelper.EXTRA_TOPIC_ID, -1L)
        if (extra > 0L) return extra
        val data = intent.data ?: return null
        if (data.scheme == "recallflow" && data.host == "review") {
            return data.lastPathSegment?.toLongOrNull()
        }
        return null
    }
}

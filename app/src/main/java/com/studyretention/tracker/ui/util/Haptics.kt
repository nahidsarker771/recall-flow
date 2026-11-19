package com.studyretention.tracker.ui.util

import android.os.Build
import android.view.HapticFeedbackConstants
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalView
import com.studyretention.tracker.data.HapticsMode

/**
 * Two levels so important actions feel firm while navigation stays quiet.
 * "Strong" mode uses CONFIRM for actions and a light clock tick for taps;
 * "Light" mode uses the clock tick everywhere.
 */
class Haptics(
    private val view: View,
    private val mode: HapticsMode,
) {
    fun soft() {
        if (mode == HapticsMode.OFF) return
        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
    }

    fun strong() {
        if (mode == HapticsMode.OFF) return
        if (mode == HapticsMode.LIGHT) {
            view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
            return
        }
        view.performHapticFeedback(
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                HapticFeedbackConstants.CONFIRM
            } else {
                HapticFeedbackConstants.LONG_PRESS
            },
        )
    }
}

@Composable
fun rememberHaptics(mode: HapticsMode): Haptics {
    val view = LocalView.current
    return remember(view, mode) { Haptics(view, mode) }
}

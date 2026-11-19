package com.studyretention.tracker.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import com.studyretention.tracker.data.FocusPalette

/**
 * App theme. Uses Material You dynamic color on Android 12+ when available,
 * otherwise one of the calm focus palettes. Dark mode follows the system.
 */
@Composable
fun RecallFlowTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = true,
    palette: FocusPalette = FocusPalette.SAGE,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = remember(context, darkTheme, dynamicColor, palette) {
        when {
            dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ->
                if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)

            else -> focusColorScheme(palette, darkTheme)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = RecallFlowTypography,
        shapes = RecallFlowShapes,
        content = content,
    )
}

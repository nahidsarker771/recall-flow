package com.studyretention.tracker.data

import android.content.Context
import com.studyretention.tracker.logic.Algorithm
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

enum class ThemeMode(val label: String) {
    SYSTEM("System"),
    LIGHT("Light"),
    DARK("Dark"),
}

enum class FocusPalette(val label: String, val description: String) {
    SAGE("Sage", "Calm green — easy to stay with"),
    OCEAN("Ocean", "Muted blue-teal"),
    DUSK("Dusk", "Soft gray-violet"),
    SAND("Sand", "Warm neutral"),
    INDIGO("Indigo", "Original indigo"),
}

enum class HapticsMode(val label: String, val description: String) {
    STRONG("Strong", "Firm taps for add, save, stop and milestones"),
    LIGHT("Light", "Soft ticks everywhere"),
    OFF("Off", "No vibration"),
}

data class AppSettings(
    val dailyLimit: Int = 0,
    val notificationsEnabled: Boolean = true,
    val reminderHour: Int = 9,
    val reminderMinute: Int = 0,
    val autoSchedule: Boolean = true,
    val algorithm: Algorithm = Algorithm.GUIDED,
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val dynamicColor: Boolean = true,
    val hapticsMode: HapticsMode = HapticsMode.STRONG,
    val focusPalette: FocusPalette = FocusPalette.SAGE,
)

/**
 * Typed settings backed by SharedPreferences. Exposes a StateFlow so the theme
 * and every screen react immediately when an option changes.
 */
class AppPreferences(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("recallflow_settings", Context.MODE_PRIVATE)

    private val _settings = MutableStateFlow(read())
    val settings: StateFlow<AppSettings> = _settings.asStateFlow()

    fun update(transform: (AppSettings) -> AppSettings) {
        val updated = transform(_settings.value)
        write(updated)
        _settings.value = updated
    }

    private fun read(): AppSettings = AppSettings(
        dailyLimit = prefs.getInt(KEY_DAILY_LIMIT, 0),
        notificationsEnabled = prefs.getBoolean(KEY_NOTIFICATIONS, true),
        reminderHour = prefs.getInt(KEY_REMINDER_HOUR, 9),
        reminderMinute = prefs.getInt(KEY_REMINDER_MINUTE, 0),
        autoSchedule = prefs.getBoolean(KEY_AUTO_SCHEDULE, true),
        algorithm = prefs.getString(KEY_ALGORITHM, null)
            ?.let { name -> runCatching { Algorithm.valueOf(name) }.getOrNull() }
            ?: Algorithm.GUIDED,
        themeMode = prefs.getString(KEY_THEME, null)
            ?.let { name -> runCatching { ThemeMode.valueOf(name) }.getOrNull() }
            ?: ThemeMode.SYSTEM,
        dynamicColor = prefs.getBoolean(KEY_DYNAMIC_COLOR, true),
        hapticsMode = prefs.getString(KEY_HAPTICS, null)
            ?.let { name -> runCatching { HapticsMode.valueOf(name) }.getOrNull() }
            ?: HapticsMode.STRONG,
        focusPalette = prefs.getString(KEY_PALETTE, null)
            ?.let { name -> runCatching { FocusPalette.valueOf(name) }.getOrNull() }
            ?: FocusPalette.SAGE,
    )

    private fun write(settings: AppSettings) {
        prefs.edit()
            .putInt(KEY_DAILY_LIMIT, settings.dailyLimit)
            .putBoolean(KEY_NOTIFICATIONS, settings.notificationsEnabled)
            .putInt(KEY_REMINDER_HOUR, settings.reminderHour)
            .putInt(KEY_REMINDER_MINUTE, settings.reminderMinute)
            .putBoolean(KEY_AUTO_SCHEDULE, settings.autoSchedule)
            .putString(KEY_ALGORITHM, settings.algorithm.name)
            .putString(KEY_THEME, settings.themeMode.name)
            .putBoolean(KEY_DYNAMIC_COLOR, settings.dynamicColor)
            .putString(KEY_HAPTICS, settings.hapticsMode.name)
            .putString(KEY_PALETTE, settings.focusPalette.name)
            .apply()
    }

    private companion object {
        const val KEY_DAILY_LIMIT = "daily_limit"
        const val KEY_NOTIFICATIONS = "notifications_enabled"
        const val KEY_REMINDER_HOUR = "reminder_hour"
        const val KEY_REMINDER_MINUTE = "reminder_minute"
        const val KEY_AUTO_SCHEDULE = "auto_schedule"
        const val KEY_ALGORITHM = "algorithm"
        const val KEY_THEME = "theme_mode"
        const val KEY_DYNAMIC_COLOR = "dynamic_color"
        const val KEY_HAPTICS = "haptics_mode"
        const val KEY_PALETTE = "focus_palette"
    }
}

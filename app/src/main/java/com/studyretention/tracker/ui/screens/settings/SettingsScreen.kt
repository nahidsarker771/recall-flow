package com.studyretention.tracker.ui.screens.settings

import android.Manifest
import android.app.AlarmManager
import android.app.TimePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyretention.tracker.BuildConfig
import com.studyretention.tracker.data.FocusPalette
import com.studyretention.tracker.data.HapticsMode
import com.studyretention.tracker.data.ThemeMode
import com.studyretention.tracker.logic.Algorithm
import com.studyretention.tracker.ui.components.ActionRow
import com.studyretention.tracker.ui.components.ScreenHeader
import com.studyretention.tracker.ui.theme.focusPrimaryColor
import com.studyretention.tracker.ui.theme.Spacing
import com.studyretention.tracker.ui.util.Haptics
import com.studyretention.tracker.ui.util.formatTime
import com.studyretention.tracker.viewmodel.RecallFlowViewModel
import java.time.LocalDate

@Composable
fun SettingsScreen(
    vm: RecallFlowViewModel,
    onBack: () -> Unit,
    onOpenHowItWorks: () -> Unit,
) {
    val context = LocalContext.current
    val settings by vm.settings.collectAsStateWithLifecycle()

    var showAlgorithmDialog by rememberSaveable { mutableStateOf(false) }
    var showThemeDialog by rememberSaveable { mutableStateOf(false) }
    var showHapticsDialog by rememberSaveable { mutableStateOf(false) }
    var showPaletteDialog by rememberSaveable { mutableStateOf(false) }
    var showLimitDialog by rememberSaveable { mutableStateOf(false) }

    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("application/json"),
    ) { uri ->
        if (uri != null) {
            vm.exportData(context, uri) { ok ->
                Toast.makeText(
                    context,
                    if (ok) "Backup saved" else "Could not save backup",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.OpenDocument(),
    ) { uri ->
        if (uri != null) {
            vm.importData(context, uri) { ok ->
                Toast.makeText(
                    context,
                    if (ok) "Backup restored" else "That file could not be imported",
                    Toast.LENGTH_SHORT,
                ).show()
            }
        }
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission(),
    ) { granted ->
        vm.setNotificationsEnabled(granted)
        if (!granted) {
            Toast.makeText(context, "Notifications stay off without permission", Toast.LENGTH_SHORT).show()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screen, vertical = Spacing.lg),
    ) {
        IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
            Icon(
                imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(Spacing.sm))
        ScreenHeader(
            title = "Settings",
            subtitle = "Tune the rhythm of your reviews.",
        )
        Spacer(Modifier.height(Spacing.xl))

        SettingsLabel("Study")
        SettingsCard {
            ActionRow(
                title = "Scheduling algorithm",
                subtitle = settings.algorithm.title,
                onClick = { showAlgorithmDialog = true },
            )
            Divider()
            ActionRow(
                title = "Daily review limit",
                subtitle = if (settings.dailyLimit == 0) {
                    "No limit — show every due topic"
                } else {
                    "Up to ${settings.dailyLimit} due topics per day"
                },
                onClick = { showLimitDialog = true },
            )
            Divider()
            ActionRow(
                title = "Auto-schedule reviews",
                subtitle = "Place every next review on the calendar",
                trailing = {
                    Switch(
                        checked = settings.autoSchedule,
                        onCheckedChange = vm::setAutoSchedule,
                    )
                },
            )
        }

        Spacer(Modifier.height(Spacing.xl))
        SettingsLabel("Reminders")
        SettingsCard {
            ActionRow(
                title = "Review reminders",
                subtitle = "One gentle reminder when topics are due",
                trailing = {
                    Switch(
                        checked = settings.notificationsEnabled,
                        onCheckedChange = { enabled ->
                            if (!enabled) {
                                vm.setNotificationsEnabled(false)
                            } else {
                                val granted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                                    ContextCompat.checkSelfPermission(
                                        context,
                                        Manifest.permission.POST_NOTIFICATIONS,
                                    ) == PackageManager.PERMISSION_GRANTED
                                if (granted) {
                                    vm.setNotificationsEnabled(true)
                                } else {
                                    permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                                }
                            }
                        },
                    )
                },
            )
            Divider()
            ActionRow(
                title = "Reminder time",
                subtitle = formatTime(settings.reminderHour, settings.reminderMinute),
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, hour, minute -> vm.setReminderTime(hour, minute) },
                        settings.reminderHour,
                        settings.reminderMinute,
                        false,
                    ).show()
                },
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Divider()
                val alarmManager = context.getSystemService(AlarmManager::class.java)
                val exact = alarmManager?.canScheduleExactAlarms() == true
                ActionRow(
                    title = "Exact alarm timing",
                    subtitle = if (exact) {
                        "On — reminders arrive to the minute"
                    } else {
                        "Off — reminders may be a few minutes late. Tap to allow."
                    },
                    onClick = {
                        runCatching {
                            context.startActivity(
                                Intent(
                                    Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM,
                                    Uri.parse("package:${context.packageName}"),
                                ),
                            )
                        }
                    },
                )
            }
        }

        Spacer(Modifier.height(Spacing.xl))
        SettingsLabel("Appearance")
        SettingsCard {
            ActionRow(
                title = "Theme",
                subtitle = when (settings.themeMode) {
                    ThemeMode.SYSTEM -> "Follow system"
                    ThemeMode.LIGHT -> "Light"
                    ThemeMode.DARK -> "Dark"
                },
                onClick = { showThemeDialog = true },
            )
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                Divider()
                ActionRow(
                    title = "Dynamic color",
                    subtitle = "Use wallpaper colors on Android 12+",
                    trailing = {
                        Switch(
                            checked = settings.dynamicColor,
                            onCheckedChange = vm::setDynamicColor,
                        )
                    },
                )
            }
            Divider()
            ActionRow(
                title = "Focus palette",
                subtitle = settings.focusPalette.description,
                onClick = { showPaletteDialog = true },
            )
            Divider()
            ActionRow(
                title = "Haptics",
                subtitle = settings.hapticsMode.description,
                onClick = { showHapticsDialog = true },
            )
        }

        Spacer(Modifier.height(Spacing.xl))
        SettingsLabel("Data")
        SettingsCard {
            ActionRow(
                title = "Export backup",
                subtitle = "Save topics and history as JSON",
                onClick = {
                    val name = "recallflow-backup-${LocalDate.now()}.json"
                    runCatching { exportLauncher.launch(name) }
                },
            )
            Divider()
            ActionRow(
                title = "Import backup",
                subtitle = "Replaces everything on this device",
                onClick = {
                    runCatching { importLauncher.launch(arrayOf("application/json", "text/plain")) }
                },
            )
            Divider()
            ActionRow(
                title = "Storage",
                subtitle = "Room database · on this device only",
            )
        }

        Spacer(Modifier.height(Spacing.xl))
        SettingsLabel("About")
        SettingsCard {
            ActionRow(
                title = "How it works",
                subtitle = "A 60-second refresher of spaced repetition",
                onClick = onOpenHowItWorks,
            )
            Divider()
            ActionRow(title = "Version", subtitle = BuildConfig.VERSION_NAME)
            Divider()
            ActionRow(title = "Offline by design", subtitle = "No account, no ads, no tracking")
        }

        Spacer(Modifier.height(Spacing.xl))
        Text(
            text = "RecallFlow keeps everything on this phone. Export a backup before changing devices.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.xxl))
    }

    if (showAlgorithmDialog) {
        AlgorithmDialog(
            selected = settings.algorithm,
            onSelect = {
                vm.setAlgorithm(it)
                showAlgorithmDialog = false
            },
            onDismiss = { showAlgorithmDialog = false },
        )
    }

    if (showThemeDialog) {
        ThemeDialog(
            selected = settings.themeMode,
            onSelect = {
                vm.setThemeMode(it)
                showThemeDialog = false
            },
            onDismiss = { showThemeDialog = false },
        )
    }

    if (showHapticsDialog) {
        HapticsDialog(
            selected = settings.hapticsMode,
            onSelect = {
                vm.setHapticsMode(it)
                showHapticsDialog = false
            },
            onDismiss = { showHapticsDialog = false },
        )
    }

    if (showPaletteDialog) {
        val dark = when (settings.themeMode) {
            ThemeMode.SYSTEM -> isSystemInDarkTheme()
            ThemeMode.LIGHT -> false
            ThemeMode.DARK -> true
        }
        FocusPaletteDialog(
            selected = settings.focusPalette,
            dark = dark,
            onSelect = {
                vm.setFocusPalette(it)
                showPaletteDialog = false
            },
            onDismiss = { showPaletteDialog = false },
        )
    }

    if (showLimitDialog) {
        LimitDialog(
            current = settings.dailyLimit,
            onConfirm = {
                vm.setDailyLimit(it)
                showLimitDialog = false
            },
            onDismiss = { showLimitDialog = false },
        )
    }
}

private val Algorithm.title: String get() = label

private val Algorithm.description: String
    get() = when (this) {
        Algorithm.GUIDED -> "Gentle ladder: 1, 7, 16, 35 days, then ease-based."
        Algorithm.SM2 -> "Textbook SM-2: 1 day, 6 days, then ease-based."
    }

@Composable
private fun SettingsLabel(text: String) {
    Text(
        text = text.uppercase(),
        style = MaterialTheme.typography.labelSmall,
        color = MaterialTheme.colorScheme.primary,
    )
    Spacer(Modifier.height(Spacing.sm))
}

@Composable
private fun SettingsCard(content: @Composable () -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column { content() }
    }
}

@Composable
private fun Divider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = Spacing.lg),
        color = MaterialTheme.colorScheme.outlineVariant,
    )
}

@Composable
private fun AlgorithmDialog(
    selected: Algorithm,
    onSelect: (Algorithm) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Scheduling algorithm") },
        text = {
            Column {
                Algorithm.entries.forEach { algorithm ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(algorithm) }
                            .padding(vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = algorithm == selected,
                            onClick = { onSelect(algorithm) },
                        )
                        Column {
                            Text(algorithm.title, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = algorithm.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun FocusPaletteDialog(
    selected: FocusPalette,
    dark: Boolean,
    onSelect: (FocusPalette) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Focus palette") },
        text = {
            Column {
                FocusPalette.entries.forEach { palette ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(palette) }
                            .padding(vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = palette == selected,
                            onClick = null,
                        )
                        Box(
                            modifier = Modifier
                                .padding(start = Spacing.sm)
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(focusPrimaryColor(palette, dark)),
                        )
                        Column(modifier = Modifier.padding(start = Spacing.sm)) {
                            Text(palette.label, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = palette.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun HapticsDialog(
    selected: HapticsMode,
    onSelect: (HapticsMode) -> Unit,
    onDismiss: () -> Unit,
) {
    val view = LocalView.current
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Haptics") },
        text = {
            Column {
                HapticsMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                Haptics(view, mode).strong()
                                onSelect(mode)
                            }
                            .padding(vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(
                            selected = mode == selected,
                            onClick = null,
                        )
                        Column(modifier = Modifier.padding(start = Spacing.sm)) {
                            Text(mode.label, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = mode.description,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun ThemeDialog(
    selected: ThemeMode,
    onSelect: (ThemeMode) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Theme") },
        text = {
            Column {
                ThemeMode.entries.forEach { mode ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(mode) }
                            .padding(vertical = Spacing.xs),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = mode == selected, onClick = { onSelect(mode) })
                        Text(mode.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        },
    )
}

@Composable
private fun LimitDialog(
    current: Int,
    onConfirm: (Int) -> Unit,
    onDismiss: () -> Unit,
) {
    var value by rememberSaveable { mutableStateOf(if (current == 0) "0" else current.toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Daily review limit") },
        text = {
            Column {
                Text(
                    text = "Caps how many due topics Today shows. 0 means no limit.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.md))
                OutlinedTextField(
                    value = value,
                    onValueChange = { input -> value = input.filter { it.isDigit() }.take(3) },
                    label = { Text("Topics per day") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                Spacer(Modifier.height(Spacing.md))
                Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    listOf(0, 10, 20, 30).forEach { preset ->
                        OutlinedButton(
                            onClick = { value = preset.toString() },
                            shape = MaterialTheme.shapes.small,
                        ) {
                            Text(if (preset == 0) "None" else preset.toString())
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(onClick = { onConfirm(value.toIntOrNull() ?: 0) }) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

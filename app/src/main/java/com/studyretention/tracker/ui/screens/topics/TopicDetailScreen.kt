package com.studyretention.tracker.ui.screens.topics

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyretention.tracker.data.Topic
import com.studyretention.tracker.ui.components.EmptyState
import com.studyretention.tracker.ui.components.MasteryBar
import com.studyretention.tracker.ui.components.SectionTitle
import com.studyretention.tracker.ui.components.TagChip
import com.studyretention.tracker.ui.components.TopicEditorDialog
import com.studyretention.tracker.ui.theme.Spacing
import com.studyretention.tracker.ui.util.durationLabel
import com.studyretention.tracker.ui.util.formatDayLong
import com.studyretention.tracker.ui.util.formatTime
import com.studyretention.tracker.ui.util.ratingLabel
import com.studyretention.tracker.ui.util.relativeDueLabel
import com.studyretention.tracker.viewmodel.RecallFlowViewModel
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TopicDetailScreen(
    vm: RecallFlowViewModel,
    topicId: Long,
    onBack: () -> Unit,
    onReview: (Long) -> Unit,
) {
    val topics by vm.topics.collectAsStateWithLifecycle()
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val topic = topics.firstOrNull { it.id == topicId }

    var showDeleteDialog by rememberSaveable { mutableStateOf(false) }
    var showIntervalDialog by rememberSaveable { mutableStateOf(false) }
    var showEditDialog by rememberSaveable { mutableStateOf(false) }

    if (topic == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.screen),
        ) {
            IconButton(onClick = onBack) {
                Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back")
            }
            EmptyState(
                icon = Icons.Outlined.PlayArrow,
                title = "Topic not found",
                message = "It may have been deleted. Go back to your topic library.",
            )
            Spacer(Modifier.height(Spacing.md))
            Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) { Text("Back to topics") }
        }
        return
    }

    val topicSessions = remember(sessions, topicId) {
        sessions.filter { it.topicId == topicId }.sortedByDescending { it.timestamp }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screen, vertical = Spacing.lg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(onClick = onBack, modifier = Modifier.size(40.dp)) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = "Back",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = "Topic",
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            IconButton(onClick = { showEditDialog = true }) {
                Icon(
                    imageVector = Icons.Outlined.Edit,
                    contentDescription = "Edit topic",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = topic.name,
            style = MaterialTheme.typography.headlineLarge,
            color = MaterialTheme.colorScheme.onBackground,
        )
        if (topic.category.isNotBlank()) {
            Spacer(Modifier.height(Spacing.sm))
            TagChip(text = topic.category)
        }

        Spacer(Modifier.height(Spacing.lg))
        if (topic.notes.isNotBlank()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Text(
                    text = topic.notes,
                    modifier = Modifier.padding(Spacing.lg),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
            Spacer(Modifier.height(Spacing.lg))
        }

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(modifier = Modifier.padding(Spacing.lg)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "Mastery",
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = "${topic.masteryLevel}%",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
                Spacer(Modifier.height(Spacing.sm))
                MasteryBar(level = topic.masteryLevel)
                Spacer(Modifier.height(Spacing.lg))
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                Spacer(Modifier.height(Spacing.md))

                StatRow("Next review", relativeDueLabel(topic.nextReviewAt))
                StatRow("Interval", if (topic.intervalDays == 1) "1 day" else "${topic.intervalDays} days")
                StatRow("Ease factor", String.format(Locale.US, "%.2f", topic.easeFactor))
                StatRow("Repetitions", topic.repetitions.toString())
                StatRow("Time studied", durationLabel(topic.totalTimeStudied))
            }
        }

        Spacer(Modifier.height(Spacing.lg))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Button(
                onClick = { onReview(topic.id) },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.large,
            ) {
                Icon(Icons.Outlined.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(Modifier.size(Spacing.xs))
                Text("Review now")
            }
            OutlinedButton(
                onClick = { showIntervalDialog = true },
                modifier = Modifier.weight(1f),
                shape = MaterialTheme.shapes.large,
            ) {
                Text("Adjust interval")
            }
        }

        Spacer(Modifier.height(Spacing.xl))
        SectionTitle(title = "History")
        Spacer(Modifier.height(Spacing.sm))

        if (topicSessions.isEmpty()) {
            Text(
                text = "No reviews yet. Your first rating starts the history.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            topicSessions.take(30).forEach { session ->
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = Spacing.sm),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Row(
                        modifier = Modifier.padding(Spacing.md),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RatingBadge(rating = session.recallRating)
                        Spacer(Modifier.size(Spacing.md))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = ratingLabel(session.recallRating),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                            Text(
                                text = "${formatDayLong(Instant.ofEpochMilli(session.timestamp).atZone(ZoneId.systemDefault()).toLocalDate())} · ${formatTime(session.timestamp)}",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        Text(
                            text = durationLabel(session.durationSeconds.toLong()),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(Spacing.xl))
        TextButton(
            onClick = { showDeleteDialog = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.size(Spacing.xs))
            Text("Delete topic", color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(Spacing.xxl))
    }

    if (showEditDialog) {
        TopicEditorDialog(
            title = "Edit topic",
            initial = topic,
            onDismiss = { showEditDialog = false },
            onConfirm = { name, notes, category ->
                vm.updateTopic(topic.copy(name = name, notes = notes, category = category))
                showEditDialog = false
            },
        )
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Delete \"${topic.name}\"?") },
            text = { Text("Its review history and calendar events will be removed too. This can't be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteDialog = false
                        vm.deleteTopic(topic)
                        onBack()
                    },
                ) {
                    Text("Delete", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) { Text("Cancel") }
            },
        )
    }

    if (showIntervalDialog) {
        IntervalDialog(
            topic = topic,
            onDismiss = { showIntervalDialog = false },
            onConfirm = { days ->
                vm.overrideInterval(topic.id, days)
                showIntervalDialog = false
            },
        )
    }
}

@Composable
private fun StatRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = Spacing.xs),
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun RatingBadge(rating: Int) {
    val container = when {
        rating >= 4 -> MaterialTheme.colorScheme.tertiaryContainer
        rating == 3 -> MaterialTheme.colorScheme.secondaryContainer
        else -> MaterialTheme.colorScheme.errorContainer
    }
    val content = when {
        rating >= 4 -> MaterialTheme.colorScheme.onTertiaryContainer
        rating == 3 -> MaterialTheme.colorScheme.onSecondaryContainer
        else -> MaterialTheme.colorScheme.onErrorContainer
    }
    Surface(shape = CircleShape, color = container) {
        Text(
            text = rating.toString(),
            modifier = Modifier.padding(horizontal = Spacing.sm, vertical = Spacing.xs),
            style = MaterialTheme.typography.labelLarge,
            color = content,
        )
    }
}

@Composable
private fun IntervalDialog(
    topic: Topic,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit,
) {
    var value by rememberSaveable { mutableStateOf(topic.intervalDays.coerceAtLeast(1).toString()) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Adjust interval") },
        text = {
            Column {
                Text(
                    text = "Set how many days until the next review. Ease and repetition count stay the same.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(Spacing.md))
                OutlinedTextField(
                    value = value,
                    onValueChange = { input -> value = input.filter { it.isDigit() }.take(3) },
                    label = { Text("Days") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onConfirm(value.toIntOrNull() ?: 1) },
                enabled = (value.toIntOrNull() ?: 0) > 0,
            ) {
                Text("Apply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        },
    )
}

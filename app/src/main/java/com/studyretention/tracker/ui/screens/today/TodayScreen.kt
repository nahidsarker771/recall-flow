package com.studyretention.tracker.ui.screens.today

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyretention.tracker.data.Topic
import com.studyretention.tracker.logic.StatsCalculator
import com.studyretention.tracker.ui.components.EmptyState
import com.studyretention.tracker.ui.components.MasteryBar
import com.studyretention.tracker.ui.components.ProgressRing
import com.studyretention.tracker.ui.components.ScreenHeader
import com.studyretention.tracker.ui.components.SectionTitle
import com.studyretention.tracker.ui.components.StreakChip
import com.studyretention.tracker.ui.components.SwipeActionRow
import com.studyretention.tracker.ui.components.TagChip
import com.studyretention.tracker.ui.theme.Spacing
import com.studyretention.tracker.ui.util.relativeDueLabel
import com.studyretention.tracker.viewmodel.RecallFlowViewModel
import kotlinx.coroutines.delay
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

@Composable
fun TodayScreen(
    vm: RecallFlowViewModel,
    onOpenSettings: () -> Unit,
    onOpenTopics: () -> Unit,
    onOpenTopic: (Long) -> Unit,
    onReview: (Long) -> Unit,
) {
    val topics by vm.topics.collectAsStateWithLifecycle()
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val settings by vm.settings.collectAsStateWithLifecycle()

    var nowMs by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(60_000)
            nowMs = System.currentTimeMillis()
        }
    }

    val due = remember(topics, nowMs, settings.dailyLimit) {
        val allDue = topics.filter { it.nextReviewAt <= nowMs }.sortedBy { it.nextReviewAt }
        if (settings.dailyLimit > 0) allDue.take(settings.dailyLimit) else allDue
    }
    val completedToday = remember(sessions) {
        StatsCalculator.reviewsToday(sessions, LocalDate.now())
    }
    val streak = remember(sessions) { StatsCalculator.currentStreak(sessions) }
    val totalForToday = completedToday + due.size
    val progress = if (totalForToday == 0) 0f else completedToday.toFloat() / totalForToday

    val heroTopic = due.firstOrNull() ?: topics
        .filter { it.nextReviewAt > nowMs }
        .minByOrNull { it.nextReviewAt }

    val dateLabel = remember {
        LocalDateTime.now().format(DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault()))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screen, vertical = Spacing.lg),
    ) {
        ScreenHeader(
            eyebrow = dateLabel,
            title = greetingFor(LocalDateTime.now().hour),
            subtitle = when {
                due.isNotEmpty() -> "${due.size} ${if (due.size == 1) "topic" else "topics"} due now"
                topics.isEmpty() -> "A calm place to keep what you learn."
                else -> "All caught up — nice work."
            },
            trailing = {
                IconButton(onClick = onOpenSettings) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
        )

        Spacer(Modifier.height(Spacing.lg))

        if (heroTopic == null) {
            EmptyState(
                icon = Icons.Outlined.MenuBook,
                title = "No topics yet",
                message = "Add what you're learning and RecallFlow will schedule the first review about 26 hours later.",
            )
            Spacer(Modifier.height(Spacing.md))
            Button(
                onClick = onOpenTopics,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
            ) {
                Text("Add your first topic")
            }
            Spacer(Modifier.height(Spacing.xl))
            return@Column
        }

        val isDue = heroTopic.nextReviewAt <= nowMs
        HeroCard(
            topic = heroTopic,
            isDue = isDue,
            nowMs = nowMs,
            onStart = { onReview(heroTopic.id) },
        )

        Spacer(Modifier.height(Spacing.lg))

        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Row(
                modifier = Modifier.padding(Spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                ProgressRing(progress = progress)
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Today's reviews",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Spacer(Modifier.height(Spacing.xxs))
                    Text(
                        text = "$completedToday of $totalForToday complete",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(Modifier.height(Spacing.sm))
                    StreakChip(days = streak)
                }
            }
        }

        if (due.isNotEmpty()) {
            Spacer(Modifier.height(Spacing.xl))
            SectionTitle(title = "Due now")
            Spacer(Modifier.height(Spacing.sm))
            due.forEach { topic ->
                SwipeActionRow(
                    onSwipeRight = { onReview(topic.id) },
                    onSwipeLeft = { vm.snoozeTopic(topic.id) },
                    rightLabel = "Review",
                    leftLabel = "Snooze 1h",
                ) {
                    DueTopicRow(
                        topic = topic,
                        nowMs = nowMs,
                        onClick = { onOpenTopic(topic.id) },
                    )
                }
                Spacer(Modifier.height(Spacing.sm))
            }
        } else if (completedToday > 0) {
            Spacer(Modifier.height(Spacing.xl))
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.extraLarge,
                color = MaterialTheme.colorScheme.surfaceContainerLow,
            ) {
                Row(
                    modifier = Modifier.padding(Spacing.lg),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(Spacing.md),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                    Column {
                        Text(
                            text = "All done for today",
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Come back tomorrow — consistency beats cramming.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }

        if (due.isEmpty() && heroTopic.nextReviewAt > nowMs) {
            Spacer(Modifier.height(Spacing.xl))
            Text(
                text = "Next up: ${heroTopic.name} — ${relativeDueLabel(heroTopic.nextReviewAt, nowMs)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Spacer(Modifier.height(Spacing.xxl))
    }
}

@Composable
private fun HeroCard(
    topic: Topic,
    isDue: Boolean,
    nowMs: Long,
    onStart: () -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primary,
    ) {
        Column(modifier = Modifier.padding(Spacing.xl)) {
            Text(
                text = if (isDue) "NEXT UP" else "UPCOMING",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.72f),
            )
            Spacer(Modifier.height(Spacing.sm))
            Text(
                text = topic.name,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.onPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = relativeDueLabel(topic.nextReviewAt, nowMs),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.82f),
            )
            if (topic.category.isNotBlank()) {
                Spacer(Modifier.height(Spacing.sm))
                TagChip(text = topic.category)
            }
            Spacer(Modifier.height(Spacing.lg))
            Button(
                onClick = onStart,
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.large,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.onPrimary,
                    contentColor = MaterialTheme.colorScheme.primary,
                ),
            ) {
                Text(
                    text = if (isDue) "Start review" else "Review early",
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                )
            }
        }
    }
}

@Composable
private fun DueTopicRow(
    topic: Topic,
    nowMs: Long,
    onClick: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = topic.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = relativeDueLabel(topic.nextReviewAt, nowMs),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            Spacer(Modifier.height(Spacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Mastery ${topic.masteryLevel}%",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(0.dp))
            }
            Spacer(Modifier.height(Spacing.xs))
            MasteryBar(level = topic.masteryLevel)
        }
    }
}

private fun greetingFor(hour: Int): String = when {
    hour < 5 -> "Good night"
    hour < 12 -> "Good morning"
    hour < 17 -> "Good afternoon"
    hour < 21 -> "Good evening"
    else -> "Good night"
}

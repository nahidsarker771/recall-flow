package com.studyretention.tracker.ui.screens.review

import android.os.SystemClock
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyretention.tracker.data.Topic
import com.studyretention.tracker.data.toReviewState
import com.studyretention.tracker.ui.components.TagChip
import com.studyretention.tracker.ui.theme.Spacing
import com.studyretention.tracker.ui.util.formatDayShort
import com.studyretention.tracker.ui.util.intervalLabel
import com.studyretention.tracker.ui.util.ratingLabel
import com.studyretention.tracker.ui.util.stopwatchLabel
import com.studyretention.tracker.ui.util.stopwatchTenthsLabel
import com.studyretention.tracker.viewmodel.RecallFlowViewModel
import kotlinx.coroutines.delay

private val ratingHints = listOf(
    "None",
    "Wrong",
    "Hard",
    "Shaky",
    "Good",
    "Great",
)

private enum class ReviewPhase { Running, Rating }

/**
 * Two clean phases: first a bare stopwatch with a single Stop button, then the
 * rating card once the timer is stopped. Nothing can be rated while it runs.
 */
@Composable
fun ReviewScreen(
    vm: RecallFlowViewModel,
    topicId: Long,
    onDone: () -> Unit,
) {
    val topics by vm.topics.collectAsStateWithLifecycle()
    val topic = topics.firstOrNull { it.id == topicId }

    var phase by rememberSaveable { mutableStateOf(ReviewPhase.Running) }
    var startedAt by rememberSaveable { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    var elapsedSeconds by rememberSaveable { mutableLongStateOf(0L) }
    var rating by rememberSaveable { mutableIntStateOf(-1) }

    if (topic == null) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(Spacing.screen),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(Spacing.xxl))
            Text("Topic not found", style = MaterialTheme.typography.titleLarge)
            Spacer(Modifier.height(Spacing.md))
            Button(onClick = onDone) { Text("Go back") }
        }
        return
    }

    when (phase) {
        ReviewPhase.Running -> RunningReview(
            topic = topic,
            startedAt = startedAt,
            onStop = { seconds ->
                elapsedSeconds = seconds
                phase = ReviewPhase.Rating
            },
            onClose = onDone,
        )

        ReviewPhase.Rating -> RatingReview(
            vm = vm,
            topic = topic,
            rating = rating,
            elapsedSeconds = elapsedSeconds,
            onRating = { rating = it },
            onClose = onDone,
            onSaved = onDone,
        )
    }
}

@Composable
private fun RunningReview(
    topic: Topic,
    startedAt: Long,
    onStop: (Long) -> Unit,
    onClose: () -> Unit,
) {
    var elapsedMillis by remember {
        mutableLongStateOf((SystemClock.elapsedRealtime() - startedAt).coerceAtLeast(0L))
    }

    LaunchedEffect(startedAt) {
        while (true) {
            elapsedMillis = (SystemClock.elapsedRealtime() - startedAt).coerceAtLeast(0L)
            delay(100)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = Spacing.screen, vertical = Spacing.lg),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "REVIEWING",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = topic.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Outlined.Close, contentDescription = "Close review")
            }
        }

        Spacer(Modifier.weight(1f))
        Text(
            text = stopwatchTenthsLabel(elapsedMillis),
            style = MaterialTheme.typography.displayLarge.copy(
                fontFamily = FontFamily.Monospace,
                fontSize = 52.sp,
                letterSpacing = 1.sp,
            ),
            fontWeight = FontWeight.Light,
            color = MaterialTheme.colorScheme.onBackground,
        )
        Spacer(Modifier.height(Spacing.xl))
        Button(
            onClick = {
                val seconds = (SystemClock.elapsedRealtime() - startedAt).coerceAtLeast(0L) / 1000
                onStop(seconds)
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Icon(
                imageVector = Icons.Outlined.Stop,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
            )
            Spacer(Modifier.width(Spacing.sm))
            Text("Stop")
        }
        Spacer(Modifier.weight(1f))
    }
}

@Composable
private fun RatingReview(
    vm: RecallFlowViewModel,
    topic: Topic,
    rating: Int,
    elapsedSeconds: Long,
    onRating: (Int) -> Unit,
    onClose: () -> Unit,
    onSaved: () -> Unit,
) {
    val settings by vm.settings.collectAsStateWithLifecycle()
    val preview = remember(topic, rating, settings.algorithm) {
        if (rating < 0) {
            null
        } else {
            vm.scheduler.schedule(
                state = topic.toReviewState(),
                rating = rating,
                nowMillis = System.currentTimeMillis(),
                algorithm = settings.algorithm,
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screen, vertical = Spacing.lg),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "RECALL CHECK",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = "How did it go?",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onClose) {
                Icon(Icons.Outlined.Close, contentDescription = "Close review")
            }
        }

        Spacer(Modifier.height(Spacing.xl))
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
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(modifier = Modifier.padding(Spacing.lg)) {
                Text(
                    text = "PROMPT",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                )
                Spacer(Modifier.height(Spacing.sm))
                Text(
                    text = topic.notes.ifBlank {
                        "Explain \"${topic.name}\" out loud from memory, then rate how it went."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Spacer(Modifier.height(Spacing.lg))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainer,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Icon(
                    imageVector = Icons.Outlined.Timer,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(18.dp),
                )
                Text(
                    text = "Time on this card",
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = stopwatchLabel(elapsedSeconds),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }

        Spacer(Modifier.height(Spacing.xl))
        Text(
            text = "How well did you recall it?",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Spacer(Modifier.height(Spacing.md))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(Spacing.xs),
        ) {
            (0..5).forEach { value ->
                RatingButton(
                    value = value,
                    selected = rating == value,
                    onClick = { onRating(value) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        Spacer(Modifier.height(Spacing.sm))
        Text(
            text = if (rating < 0) {
                "Pick 0 (blackout) to 5 (perfect)."
            } else {
                "${ratingLabel(rating)} · ${ratingHints[rating]}"
            },
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )

        Spacer(Modifier.height(Spacing.lg))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Row(
                modifier = Modifier.padding(Spacing.lg),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Next review",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = preview?.let { intervalLabel(it.intervalDays) } ?: "Choose a rating",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                if (preview != null) {
                    Text(
                        text = formatDayShort(preview.nextReviewAt),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.xl))
        Button(
            onClick = {
                if (rating >= 0) {
                    vm.saveReview(topic.id, rating, elapsedSeconds.toInt()) { onSaved() }
                }
            },
            enabled = rating >= 0,
            modifier = Modifier
                .fillMaxWidth()
                .height(52.dp),
            shape = MaterialTheme.shapes.large,
        ) {
            Text("Save review")
        }
        Spacer(Modifier.height(Spacing.md))
        Text(
            text = "Your rating decides the next interval. A low rating brings the topic back sooner.",
            modifier = Modifier.fillMaxWidth(),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(Spacing.xxl))
    }
}

@Composable
private fun RatingButton(
    value: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = when {
        !selected -> MaterialTheme.colorScheme.surfaceContainerHigh
        value >= 4 -> MaterialTheme.colorScheme.tertiaryContainer
        value == 3 -> MaterialTheme.colorScheme.primaryContainer
        else -> MaterialTheme.colorScheme.errorContainer
    }
    val content = when {
        !selected -> MaterialTheme.colorScheme.onSurface
        value >= 4 -> MaterialTheme.colorScheme.onTertiaryContainer
        value == 3 -> MaterialTheme.colorScheme.onPrimaryContainer
        else -> MaterialTheme.colorScheme.onErrorContainer
    }
    Box(
        modifier = modifier
            .height(56.dp)
            .clip(MaterialTheme.shapes.medium)
            .background(container)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = value.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = content,
            )
            Text(
                text = ratingHints[value],
                style = MaterialTheme.typography.labelSmall,
                color = content.copy(alpha = 0.8f),
            )
        }
    }
}

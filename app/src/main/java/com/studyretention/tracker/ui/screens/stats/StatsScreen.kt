package com.studyretention.tracker.ui.screens.stats

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BarChart
import androidx.compose.material.icons.outlined.LocalFireDepartment
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyretention.tracker.logic.DayRetention
import com.studyretention.tracker.logic.StatsCalculator
import com.studyretention.tracker.ui.components.EmptyState
import com.studyretention.tracker.ui.components.MetricCard
import com.studyretention.tracker.ui.components.ScreenHeader
import com.studyretention.tracker.ui.components.SectionTitle
import com.studyretention.tracker.ui.theme.Spacing
import com.studyretention.tracker.ui.util.durationLabel
import com.studyretention.tracker.ui.util.formatDateShort
import com.studyretention.tracker.viewmodel.RecallFlowViewModel
import java.time.LocalDate
import java.time.temporal.ChronoUnit

@Composable
fun StatsScreen(vm: RecallFlowViewModel) {
    val topics by vm.topics.collectAsStateWithLifecycle()
    val sessions by vm.sessions.collectAsStateWithLifecycle()

    val stats = remember(sessions, topics) { StatsCalculator.stats(sessions, topics) }
    val today = LocalDate.now()

    val weekly = remember(sessions) {
        (6 downTo 0).map { offset ->
            val date = LocalDate.now().minusDays(offset.toLong())
            date to sessions.count { StatsCalculator.dateOf(it.timestamp) == date }
        }
    }

    val ratingCounts = remember(sessions) {
        (0..5).map { rating -> rating to sessions.count { it.recallRating == rating } }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = Spacing.screen, vertical = Spacing.lg),
    ) {
        ScreenHeader(
            title = "Stats",
            subtitle = "Your memory, measured. All on this device.",
        )
        Spacer(Modifier.height(Spacing.xl))

        if (sessions.isEmpty()) {
            EmptyState(
                icon = Icons.Outlined.BarChart,
                title = "No insights yet",
                message = "Complete your first recall check and the patterns appear here automatically.",
            )
            return@Column
        }

        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            MetricCard(
                label = "Total reviews",
                value = stats.totalReviews.toString(),
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Avg retention",
                value = "${stats.averageRetentionPercent}%",
                modifier = Modifier.weight(1f),
            )
        }
        Spacer(Modifier.height(Spacing.md))
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            MetricCard(
                label = "Time studied",
                value = durationLabel(stats.totalStudySeconds),
                modifier = Modifier.weight(1f),
            )
            MetricCard(
                label = "Reviews today",
                value = stats.reviewsToday.toString(),
                modifier = Modifier.weight(1f),
            )
        }

        Spacer(Modifier.height(Spacing.lg))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = Spacing.lg, vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.lg),
            ) {
                Icon(
                    imageVector = Icons.Outlined.LocalFireDepartment,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Current streak",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = if (stats.currentStreak == 1) "1 day" else "${stats.currentStreak} days",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "Longest",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = if (stats.longestStreak == 1) "1 day" else "${stats.longestStreak} days",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
            }
        }

        Spacer(Modifier.height(Spacing.xl))
        SectionTitle(title = "Retention")
        Text(
            text = "Average recall on review days, last 30 days",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.md))
        RetentionCard(stats.retentionByDay)

        Spacer(Modifier.height(Spacing.xl))
        SectionTitle(title = "This week")
        Text(
            text = "How many reviews you completed each day",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.md))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(modifier = Modifier.padding(Spacing.lg)) {
                WeeklyBars(data = weekly, today = today)
            }
        }

        Spacer(Modifier.height(Spacing.xl))
        SectionTitle(title = "Recall quality")
        Text(
            text = "Every rating you have given",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(Modifier.height(Spacing.md))
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Column(modifier = Modifier.padding(Spacing.lg)) {
                val max = ratingCounts.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
                ratingCounts.reversed().forEachIndexed { index, (rating, count) ->
                    RatingBar(
                        rating = rating,
                        count = count,
                        fraction = count.toFloat() / max,
                    )
                    if (index != ratingCounts.lastIndex) Spacer(Modifier.height(Spacing.sm))
                }
            }
        }

        Spacer(Modifier.height(Spacing.xxl))
        Text(
            text = "Retention is the average of your 0–5 recall ratings. Streaks count days with at least one review.",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun RetentionCard(days: List<DayRetention>) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            if (days.isEmpty()) {
                Text(
                    text = "No reviews in the last 30 days.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                RetentionLine(values = days.map { it.percent.toFloat() })
                Spacer(Modifier.height(Spacing.sm))
                Row(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = formatDateShort(days.first().date),
                        modifier = Modifier.weight(1f),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        text = formatDateShort(days.last().date),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun RetentionLine(values: List<Float>) {
    val line = MaterialTheme.colorScheme.primary
    val grid = MaterialTheme.colorScheme.outlineVariant
    val fillTop = line.copy(alpha = 0.22f)
    val fillBottom = line.copy(alpha = 0f)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(150.dp),
    ) {
        val top = 6.dp.toPx()
        val bottom = size.height - 6.dp.toPx()
        val chartHeight = bottom - top

        for (row in 0..3) {
            val y = top + chartHeight * row / 3f
            drawLine(
                color = grid.copy(alpha = 0.5f),
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1f,
            )
        }

        if (values.isEmpty()) return@Canvas

        val stepX = if (values.size == 1) 0f else size.width / (values.size - 1)
        val points = values.mapIndexed { index, value ->
            Offset(
                x = if (values.size == 1) size.width / 2f else index * stepX,
                y = bottom - (value.coerceIn(0f, 100f) / 100f) * chartHeight,
            )
        }

        val path = Path().apply {
            moveTo(points.first().x, points.first().y)
            for (index in 1 until points.size) {
                val previous = points[index - 1]
                val current = points[index]
                val midX = (previous.x + current.x) / 2f
                cubicTo(midX, previous.y, midX, current.y, current.x, current.y)
            }
        }

        if (points.size > 1) {
            val fillPath = Path().apply {
                addPath(path)
                lineTo(points.last().x, bottom)
                lineTo(points.first().x, bottom)
                close()
            }
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(colorStops = arrayOf(0f to fillTop, 1f to fillBottom)),
            )
        }

        drawPath(
            path = path,
            color = line,
            style = Stroke(width = 2.5.dp.toPx(), cap = StrokeCap.Round),
        )

        points.forEach { point ->
            drawCircle(color = line, radius = 3.dp.toPx(), center = point)
        }
    }
}

@Composable
private fun WeeklyBars(data: List<Pair<LocalDate, Int>>, today: LocalDate) {
    val max = data.maxOfOrNull { it.second }?.coerceAtLeast(1) ?: 1
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(140.dp),
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.Bottom,
    ) {
        data.forEach { (date, count) ->
            val isToday = date == today
            Column(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
                verticalArrangement = Arrangement.Bottom,
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = count.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Spacer(Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .width(18.dp)
                        .height((88.dp * (count.toFloat() / max)).coerceAtLeast(4.dp))
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (isToday) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                            },
                        ),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = date.dayOfWeek.name.take(1),
                    style = MaterialTheme.typography.labelSmall,
                    color = if (isToday) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun RatingBar(rating: Int, count: Int, fraction: Float) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = rating.toString(),
            modifier = Modifier.width(20.dp),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(10.dp)
                .clip(RoundedCornerShape(5.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest),
        ) {
            if (count > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fraction.coerceIn(0.04f, 1f))
                        .clip(RoundedCornerShape(5.dp))
                        .background(
                            when {
                                rating >= 4 -> MaterialTheme.colorScheme.tertiary
                                rating == 3 -> MaterialTheme.colorScheme.primary
                                else -> MaterialTheme.colorScheme.error
                            },
                        ),
                )
            }
        }
        Spacer(Modifier.width(Spacing.sm))
        Text(
            text = count.toString(),
            modifier = Modifier.width(32.dp),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.End,
        )
    }
}

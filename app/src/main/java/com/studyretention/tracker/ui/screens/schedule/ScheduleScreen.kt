package com.studyretention.tracker.ui.screens.schedule

import android.app.DatePickerDialog
import android.app.TimePickerDialog
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.ChevronLeft
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.UnfoldLess
import androidx.compose.material.icons.outlined.UnfoldMore
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyretention.tracker.data.ReviewSession
import com.studyretention.tracker.data.ScheduledEvent
import com.studyretention.tracker.data.Topic
import com.studyretention.tracker.ui.components.ScreenHeader
import com.studyretention.tracker.ui.theme.Spacing
import com.studyretention.tracker.ui.util.durationLabel
import com.studyretention.tracker.ui.util.formatDateShort
import com.studyretention.tracker.ui.util.formatMonthYear
import com.studyretention.tracker.ui.util.formatTime
import com.studyretention.tracker.ui.util.ratingLabel
import com.studyretention.tracker.ui.util.toEpochMillis
import com.studyretention.tracker.ui.util.toLocalDate
import com.studyretention.tracker.ui.util.toLocalDateTime
import com.studyretention.tracker.viewmodel.RecallFlowViewModel
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.LocalTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.temporal.ChronoUnit
import java.util.Locale

private const val INITIAL_PAGE = 1200
private const val PAGE_COUNT = 2401
private val CELL_HEIGHT = 48.dp

private val HourFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("h:mm", Locale.getDefault())
private val AmPmFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("a", Locale.getDefault())
private val WeekdayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE", Locale.getDefault())
private val MonthDayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d", Locale.getDefault())
private val FullDayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EEEE, MMM d", Locale.getDefault())

/**
 * Apple-Calendar-style schedule: a swipeable month grid on top, the selected
 * day's timeline underneath, and a floating + that adds a review on the go.
 * Completed reviews show up in the same timeline as planned ones.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ScheduleScreen(vm: RecallFlowViewModel, onReview: (Long) -> Unit = {}) {
    val topics by vm.topics.collectAsStateWithLifecycle()
    val sessions by vm.sessions.collectAsStateWithLifecycle()
    val events by vm.events.collectAsStateWithLifecycle()

    val today = LocalDate.now()
    var selectedEpochDay by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    val selectedDate = LocalDate.ofEpochDay(selectedEpochDay)

    val baseMonth = remember { YearMonth.now() }
    val pagerState = rememberPagerState(initialPage = INITIAL_PAGE, pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()

    var expanded by rememberSaveable { mutableStateOf(true) }
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var detailEventId by rememberSaveable { mutableStateOf<Long?>(null) }

    val topicsById = remember(topics) { topics.associateBy { it.id } }
    val eventsByDay = remember(events) { events.groupBy { it.scheduledTime.toLocalDate() } }
    val sessionsByDay = remember(sessions) { sessions.groupBy { it.timestamp.toLocalDate() } }

    val visibleMonth = baseMonth.plusMonths((pagerState.currentPage - INITIAL_PAGE).toLong())
    val monthPlanned = remember(events, visibleMonth) {
        events.count { !it.isCompleted && YearMonth.from(it.scheduledTime.toLocalDate()) == visibleMonth }
    }
    val monthReviewed = remember(sessions, visibleMonth) {
        sessions.count { YearMonth.from(it.timestamp.toLocalDate()) == visibleMonth }
    }

    val entries = remember(eventsByDay, sessionsByDay, selectedDate, topicsById) {
        val dayEvents = eventsByDay[selectedDate].orEmpty()
        val daySessions = sessionsByDay[selectedDate].orEmpty()
        val reviewedTopicIds = daySessions.map { it.topicId }.toSet()
        buildList {
            dayEvents.forEach { event ->
                val topic = topicsById[event.topicId]
                when {
                    !event.isCompleted ->
                        add(AgendaEntry.Planned(event, event.scheduledTime, topic))
                    event.topicId !in reviewedTopicIds ->
                        add(AgendaEntry.Done(event, event.scheduledTime, topic))
                }
            }
            daySessions.forEach { session ->
                add(AgendaEntry.Reviewed(session, session.timestamp, topicsById[session.topicId]))
            }
        }.sortedBy { it.time }
    }
    val plannedToday = entries.count { it is AgendaEntry.Planned }
    val doneToday = entries.count { it is AgendaEntry.Reviewed || it is AgendaEntry.Done }

    fun pageFor(month: YearMonth): Int =
        (INITIAL_PAGE + ChronoUnit.MONTHS.between(baseMonth, month)).toInt()
            .coerceIn(0, PAGE_COUNT - 1)

    fun jumpTo(date: LocalDate) {
        selectedEpochDay = date.toEpochDay()
        val target = pageFor(YearMonth.from(date))
        if (target != pagerState.currentPage) {
            scope.launch { pagerState.animateScrollToPage(target) }
        }
    }

    LaunchedEffect(pagerState.currentPage) {
        val month = baseMonth.plusMonths((pagerState.currentPage - INITIAL_PAGE).toLong())
        if (YearMonth.from(selectedDate) != month) {
            selectedEpochDay = month
                .atDay(selectedDate.dayOfMonth.coerceAtMost(month.lengthOfMonth()))
                .toEpochDay()
        }
    }

    val gridHeight by animateDpAsState(
        targetValue = if (expanded) CELL_HEIGHT * 6 else CELL_HEIGHT,
        label = "calendarHeight",
    )

    val detailEvent = events.firstOrNull { it.id == detailEventId }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = Spacing.screen,
                end = Spacing.screen,
                top = Spacing.lg,
                bottom = 104.dp,
            ),
        ) {
            item {
                ScreenHeader(
                    title = "Calendar",
                    subtitle = "Every review and study session, in one glance.",
                )
                Spacer(Modifier.height(Spacing.lg))
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    TextButton(onClick = { jumpTo(today) }) { Text("Today") }
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = {
                            scope.launch {
                                if (pagerState.currentPage > 0) {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                }
                            }
                        },
                    ) {
                        Icon(Icons.Outlined.ChevronLeft, contentDescription = "Previous month")
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                if (pagerState.currentPage < PAGE_COUNT - 1) {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                }
                            }
                        },
                    ) {
                        Icon(Icons.Outlined.ChevronRight, contentDescription = "Next month")
                    }
                }
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = formatMonthYear(visibleMonth),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onBackground,
                        )
                        Text(
                            text = "$monthPlanned planned · $monthReviewed reviewed this month",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    IconButton(onClick = { expanded = !expanded }) {
                        Icon(
                            imageVector = if (expanded) Icons.Outlined.UnfoldLess else Icons.Outlined.UnfoldMore,
                            contentDescription = if (expanded) "Collapse to week" else "Expand to month",
                        )
                    }
                }
            }

            item { WeekdayHeader() }

            item {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(gridHeight),
                ) { page ->
                    val month = baseMonth.plusMonths((page - INITIAL_PAGE).toLong())
                    MonthGrid(
                        month = month,
                        selectedDate = selectedDate,
                        today = today,
                        eventsByDay = eventsByDay,
                        sessionsByDay = sessionsByDay,
                        weekOnly = !expanded,
                        onSelect = { jumpTo(it) },
                    )
                }
                Spacer(Modifier.height(Spacing.lg))
            }

            item {
                Column {
                    Text(
                        text = dayTitle(selectedDate, today),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = if (entries.isEmpty()) {
                            "No activity yet"
                        } else {
                            "$plannedToday planned · $doneToday done"
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.height(Spacing.sm))
            }

            if (entries.isEmpty()) {
                item {
                    Surface(
                        modifier = Modifier.fillMaxWidth(),
                        shape = MaterialTheme.shapes.large,
                        color = MaterialTheme.colorScheme.surfaceContainerLow,
                    ) {
                        Column(
                            modifier = Modifier.padding(Spacing.lg),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Text(
                                text = if (topics.isEmpty()) {
                                    "Add a topic first — its review will land here."
                                } else {
                                    "Nothing on this day. Tap + to add a review."
                                },
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                    }
                    Spacer(Modifier.height(Spacing.lg))
                }
            }

            items(
                items = entries,
                key = {
                    when (it) {
                        is AgendaEntry.Planned -> "e-${it.event.id}"
                        is AgendaEntry.Done -> "d-${it.event.id}"
                        is AgendaEntry.Reviewed -> "s-${it.session.id}"
                    }
                },
            ) { entry ->
                AgendaRow(
                    entry = entry,
                    onClick = {
                        when (entry) {
                            is AgendaEntry.Planned -> detailEventId = entry.event.id
                            is AgendaEntry.Done -> detailEventId = entry.event.id
                            is AgendaEntry.Reviewed -> Unit
                        }
                    },
                )
                Spacer(Modifier.height(Spacing.sm))
            }

            item {
                Spacer(Modifier.height(Spacing.lg))
                Text(
                    text = "Swipe months · tap a day · tap an event to move it or start the review. " +
                        "The + button adds a review from anywhere in the month.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        FloatingActionButton(
            onClick = { showAddSheet = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(end = Spacing.screen, bottom = Spacing.lg),
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(Icons.Outlined.Add, contentDescription = "Add review")
        }
    }

    if (showAddSheet) {
        QuickAddSheet(
            topics = topics,
            initialDate = selectedDate,
            onDismiss = { showAddSheet = false },
            onConfirm = { topicId, whenMillis ->
                vm.addEvent(topicId, whenMillis)
                showAddSheet = false
            },
        )
    }

    if (detailEvent != null) {
        val event = detailEvent
        EventDetailSheet(
            event = event,
            topic = topicsById[event.topicId],
            onDismiss = { detailEventId = null },
            onMove = { newTime ->
                vm.moveEvent(event.id, newTime)
                detailEventId = null
            },
            onDelete = {
                vm.deleteEvent(event.id)
                detailEventId = null
            },
            onReview = { topicId ->
                detailEventId = null
                onReview(topicId)
            },
        )
    }
}

private sealed interface AgendaEntry {
    val time: Long
    val topic: Topic?

    data class Planned(
        val event: ScheduledEvent,
        override val time: Long,
        override val topic: Topic?,
    ) : AgendaEntry

    data class Done(
        val event: ScheduledEvent,
        override val time: Long,
        override val topic: Topic?,
    ) : AgendaEntry

    data class Reviewed(
        val session: ReviewSession,
        override val time: Long,
        override val topic: Topic?,
    ) : AgendaEntry
}

@Composable
private fun WeekdayHeader() {
    Row(modifier = Modifier.fillMaxWidth()) {
        listOf("S", "M", "T", "W", "T", "F", "S").forEach { label ->
            Text(
                text = label,
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

@Composable
private fun MonthGrid(
    month: YearMonth,
    selectedDate: LocalDate,
    today: LocalDate,
    eventsByDay: Map<LocalDate, List<ScheduledEvent>>,
    sessionsByDay: Map<LocalDate, List<ReviewSession>>,
    weekOnly: Boolean,
    onSelect: (LocalDate) -> Unit,
) {
    val leading = month.atDay(1).dayOfWeek.value % 7
    val first = month.atDay(1).minusDays(leading.toLong())
    val selectedWeek = ((selectedDate.dayOfMonth - 1 + leading) / 7).coerceIn(0, 5)
    val rows = if (weekOnly) listOf(selectedWeek) else (0 until 6).toList()

    Column(modifier = Modifier.fillMaxWidth()) {
        rows.forEach { week ->
            Row(modifier = Modifier.fillMaxWidth()) {
                repeat(7) { weekday ->
                    val date = first.plusDays((week * 7 + weekday).toLong())
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(CELL_HEIGHT),
                        contentAlignment = Alignment.TopCenter,
                    ) {
                        DayCell(
                            date = date,
                            inMonth = YearMonth.from(date) == month,
                            selected = date == selectedDate,
                            today = date == today,
                            events = eventsByDay[date].orEmpty(),
                            sessionCount = sessionsByDay[date]?.size ?: 0,
                            onClick = { onSelect(date) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayCell(
    date: LocalDate,
    inMonth: Boolean,
    selected: Boolean,
    today: Boolean,
    events: List<ScheduledEvent>,
    sessionCount: Int,
    onClick: () -> Unit,
) {
    val pending = events.count { !it.isCompleted }
    val done = events.count { it.isCompleted } + sessionCount
    val dots = buildList {
        repeat(pending.coerceAtMost(3)) { add(MaterialTheme.colorScheme.primary) }
        repeat(done.coerceAtMost(3 - size)) { add(MaterialTheme.colorScheme.tertiary) }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
            .padding(top = 3.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(32.dp)
                .then(
                    when {
                        selected -> Modifier.background(MaterialTheme.colorScheme.primary, CircleShape)
                        today -> Modifier.border(1.5.dp, MaterialTheme.colorScheme.primary, CircleShape)
                        else -> Modifier
                    },
                ),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = date.dayOfMonth.toString(),
                style = MaterialTheme.typography.bodySmall,
                fontWeight = if (selected || today) FontWeight.SemiBold else FontWeight.Normal,
                color = when {
                    selected -> MaterialTheme.colorScheme.onPrimary
                    !inMonth -> MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.45f)
                    today -> MaterialTheme.colorScheme.primary
                    else -> MaterialTheme.colorScheme.onSurface
                },
            )
        }
        Spacer(Modifier.height(3.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
            dots.forEach { color ->
                Box(
                    modifier = Modifier
                        .size(4.dp)
                        .clip(CircleShape)
                        .background(if (inMonth) color else color.copy(alpha = 0.35f)),
                )
            }
        }
    }
}

@Composable
private fun AgendaRow(entry: AgendaEntry, onClick: () -> Unit) {
    val accent = when (entry) {
        is AgendaEntry.Planned -> MaterialTheme.colorScheme.primary
        is AgendaEntry.Reviewed -> MaterialTheme.colorScheme.tertiary
        is AgendaEntry.Done -> MaterialTheme.colorScheme.outline
    }
    val subtitle = when (entry) {
        is AgendaEntry.Planned ->
            entry.topic?.category?.takeIf { it.isNotBlank() } ?: "Scheduled review"
        is AgendaEntry.Done -> "Completed"
        is AgendaEntry.Reviewed -> "Reviewed · " +
            "${durationLabel(entry.session.durationSeconds.toLong())} · " +
            ratingLabel(entry.session.recallRating)
    }
    val local = entry.time.toLocalDateTime()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(
                if (entry is AgendaEntry.Reviewed) Modifier else Modifier.clickable(onClick = onClick),
            ),
    ) {
        Column(
            modifier = Modifier
                .width(62.dp)
                .padding(top = Spacing.xs),
        ) {
            Text(
                text = local.format(HourFormat),
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.SemiBold,
                color = if (entry is AgendaEntry.Planned) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Text(
                text = local.format(AmPmFormat),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            modifier = Modifier.weight(1f),
            shape = RoundedCornerShape(14.dp),
            color = MaterialTheme.colorScheme.surfaceContainerLow,
        ) {
            Row(
                modifier = Modifier.padding(horizontal = Spacing.md, vertical = Spacing.md),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Box(
                    modifier = Modifier
                        .width(4.dp)
                        .height(38.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(accent),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = entry.topic?.name ?: "Deleted topic",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                when (entry) {
                    is AgendaEntry.Planned -> Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = "Start review",
                        tint = MaterialTheme.colorScheme.primary,
                    )
                    is AgendaEntry.Done -> Icon(
                        imageVector = Icons.Outlined.Check,
                        contentDescription = "Completed",
                        tint = MaterialTheme.colorScheme.tertiary,
                    )
                    is AgendaEntry.Reviewed -> Text(
                        text = "${entry.session.recallRating}/5",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddSheet(
    topics: List<Topic>,
    initialDate: LocalDate,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long) -> Unit,
) {
    val context = LocalContext.current
    val today = LocalDate.now()
    var topicId by rememberSaveable { mutableLongStateOf(topics.firstOrNull()?.id ?: 0L) }
    var dateEpoch by rememberSaveable { mutableLongStateOf(initialDate.toEpochDay()) }
    var minuteOfDay by rememberSaveable { mutableIntStateOf(nextHourMinute()) }
    val date = LocalDate.ofEpochDay(dateEpoch)
    val time = LocalTime.of(minuteOfDay / 60, minuteOfDay % 60)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xl)
                .padding(bottom = Spacing.xxl),
        ) {
            Text(
                text = "New review",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Spacer(Modifier.height(Spacing.xs))
            Text(
                text = "Add it now, keep moving.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Spacer(Modifier.height(Spacing.lg))

            Text(
                text = "DATE",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(Spacing.xs))
            LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                item {
                    FilterChip(
                        selected = date == today,
                        onClick = { dateEpoch = today.toEpochDay() },
                        label = { Text("Today") },
                    )
                }
                item {
                    FilterChip(
                        selected = date == today.plusDays(1),
                        onClick = { dateEpoch = today.plusDays(1).toEpochDay() },
                        label = { Text("Tomorrow") },
                    )
                }
                item {
                    AssistChip(
                        onClick = {
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    dateEpoch = LocalDate.of(year, month + 1, day).toEpochDay()
                                },
                                date.year,
                                date.monthValue - 1,
                                date.dayOfMonth,
                            ).show()
                        },
                        label = { Text(formatDateShort(date)) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Outlined.CalendarMonth,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                        },
                    )
                }
            }

            Spacer(Modifier.height(Spacing.md))
            Text(
                text = "TIME",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(Spacing.xs))
            OutlinedButton(
                onClick = {
                    TimePickerDialog(
                        context,
                        { _, hour, minute -> minuteOfDay = hour * 60 + minute },
                        time.hour,
                        time.minute,
                        false,
                    ).show()
                },
            ) {
                Icon(
                    imageVector = Icons.Outlined.AccessTime,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(Modifier.width(Spacing.sm))
                Text(formatTime(time.hour, time.minute))
            }

            Spacer(Modifier.height(Spacing.md))
            Text(
                text = "TOPIC",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(Spacing.xs))
            if (topics.isEmpty()) {
                Text(
                    text = "Add a topic from the Topics tab first.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    items(topics, key = { it.id }) { topic ->
                        FilterChip(
                            selected = topicId == topic.id,
                            onClick = { topicId = topic.id },
                            label = {
                                Text(
                                    text = topic.name,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                )
                            },
                        )
                    }
                }
            }

            Spacer(Modifier.height(Spacing.xl))
            Button(
                onClick = { onConfirm(topicId, date.toEpochMillis(time)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                enabled = topics.isNotEmpty() && topicId > 0,
            ) {
                Text("Add to calendar")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EventDetailSheet(
    event: ScheduledEvent,
    topic: Topic?,
    onDismiss: () -> Unit,
    onMove: (Long) -> Unit,
    onDelete: () -> Unit,
    onReview: (Long) -> Unit,
) {
    val local = event.scheduledTime.toLocalDateTime()
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(onDismissRequest = onDismiss, sheetState = sheetState) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = Spacing.xl)
                .padding(bottom = Spacing.xxl),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            ) {
                Box(
                    modifier = Modifier
                        .width(6.dp)
                        .height(44.dp)
                        .clip(RoundedCornerShape(3.dp))
                        .background(
                            if (event.isCompleted) {
                                MaterialTheme.colorScheme.outline
                            } else {
                                MaterialTheme.colorScheme.primary
                            },
                        ),
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = topic?.name ?: "Deleted topic",
                        style = MaterialTheme.typography.headlineSmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${local.format(FullDayFormat)} · ${formatTime(event.scheduledTime)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(Spacing.lg))
            if (topic != null && !event.isCompleted) {
                Button(
                    onClick = { onReview(topic.id) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                ) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(Spacing.sm))
                    Text("Start review")
                }
                Spacer(Modifier.height(Spacing.lg))
            }

            Text(
                text = "MOVE TO",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary,
            )
            Spacer(Modifier.height(Spacing.xs))
            DetailAction("In 1 hour") {
                onMove(System.currentTimeMillis() + 3_600_000L)
            }
            DetailAction("Tomorrow, same time") {
                onMove(
                    local.plusDays(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                )
            }
            DetailAction("Next week, same time") {
                onMove(
                    local.plusWeeks(1).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                )
            }
            DetailAction("In 3 days") {
                onMove(
                    local.plusDays(3).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli(),
                )
            }

            Spacer(Modifier.height(Spacing.sm))
            DetailAction("Remove from calendar", destructive = true, onClick = onDelete)
        }
    }
}

@Composable
private fun DetailAction(
    label: String,
    destructive: Boolean = false,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.small)
            .clickable(onClick = onClick)
            .padding(vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        if (destructive) {
            Icon(
                imageVector = Icons.Outlined.DeleteOutline,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(16.dp),
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = if (destructive) {
                MaterialTheme.colorScheme.error
            } else {
                MaterialTheme.colorScheme.onSurface
            },
        )
    }
}

private fun dayTitle(date: LocalDate, today: LocalDate): String {
    val prefix = when (date) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(WeekdayFormat)
    }
    return "$prefix, ${date.format(MonthDayFormat)}"
}

private fun nextHourMinute(): Int {
    val next = LocalTime.now().plusHours(1).withMinute(0).withSecond(0).withNano(0)
    return next.hour * 60 + next.minute
}

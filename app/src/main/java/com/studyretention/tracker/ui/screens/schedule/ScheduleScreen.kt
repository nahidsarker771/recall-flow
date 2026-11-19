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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.material3.HorizontalDivider
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
import com.studyretention.tracker.ui.util.Haptics
import com.studyretention.tracker.ui.util.durationLabel
import com.studyretention.tracker.ui.util.formatDateShort
import com.studyretention.tracker.ui.util.formatMonthYear
import com.studyretention.tracker.ui.util.formatTime
import com.studyretention.tracker.ui.util.ratingLabel
import com.studyretention.tracker.ui.util.rememberHaptics
import com.studyretention.tracker.ui.util.relativeDueLabel
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
private const val DAY_SECTION_INDEX = 5
private const val TIMELINE_START_INDEX = 8
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
    val settings by vm.settings.collectAsStateWithLifecycle()
    val haptics = rememberHaptics(settings.hapticsMode)

    val today = LocalDate.now()
    var selectedEpochDay by rememberSaveable { mutableLongStateOf(today.toEpochDay()) }
    val selectedDate = LocalDate.ofEpochDay(selectedEpochDay)

    val baseMonth = remember { YearMonth.now() }
    val pagerState = rememberPagerState(initialPage = INITIAL_PAGE, pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()

    var expanded by rememberSaveable { mutableStateOf(true) }
    var showAddSheet by rememberSaveable { mutableStateOf(false) }
    var detailEventId by rememberSaveable { mutableStateOf<Long?>(null) }
    var addSheetDateEpoch by rememberSaveable { mutableLongStateOf(selectedEpochDay) }
    var addSheetHour by rememberSaveable { mutableIntStateOf(-1) }
    val listState = rememberLazyListState()
    val currentHour = LocalTime.now().hour

    val topicsById = remember(topics) { topics.associateBy { it.id } }
    val eventsByDay = remember(events) { events.groupBy { it.scheduledTime.toLocalDate() } }
    val sessionsByDay = remember(sessions) { sessions.groupBy { it.timestamp.toLocalDate() } }

    val visibleMonth = baseMonth.plusMonths((pagerState.currentPage - INITIAL_PAGE).toLong())
    val plannedByMonth = remember(events) {
        events.filter { !it.isCompleted }
            .groupingBy { YearMonth.from(it.scheduledTime.toLocalDate()) }
            .eachCount()
    }
    val reviewedByMonth = remember(sessions) {
        sessions.groupingBy { YearMonth.from(it.timestamp.toLocalDate()) }.eachCount()
    }
    val monthPlanned = plannedByMonth[visibleMonth] ?: 0
    val monthReviewed = reviewedByMonth[visibleMonth] ?: 0

    val entries = remember(eventsByDay, sessionsByDay, selectedDate, topicsById) {
        buildDayEntries(selectedDate, eventsByDay, sessionsByDay, topicsById)
    }
    val nextDate = selectedDate.plusDays(1)
    val nextEntries = remember(eventsByDay, sessionsByDay, nextDate, topicsById) {
        buildDayEntries(nextDate, eventsByDay, sessionsByDay, topicsById)
    }
    val plannedToday = entries.count { it is AgendaEntry.Planned }
    val doneToday = entries.count { it is AgendaEntry.Reviewed || it is AgendaEntry.Done }
    val entriesByHour = remember(entries) {
        entries.groupBy { it.time.toLocalDateTime().hour }
    }
    val nextEntriesByHour = remember(nextEntries) {
        nextEntries.groupBy { it.time.toLocalDateTime().hour }
    }
    val nextPending = remember(events) {
        val pending = events.filter { !it.isCompleted }
        val now = System.currentTimeMillis()
        pending.filter { it.scheduledTime >= now }.minByOrNull { it.scheduledTime }
            ?: pending.minByOrNull { it.scheduledTime }
    }

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

    LaunchedEffect(pagerState.settledPage) {
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

    val detailEvent = remember(events, detailEventId) {
        detailEventId?.let { id -> events.firstOrNull { it.id == id } }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        LazyColumn(
            state = listState,
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
                    TextButton(
                        onClick = {
                            haptics.soft()
                            jumpTo(today)
                        },
                    ) { Text("Today") }
                    Spacer(Modifier.weight(1f))
                    IconButton(
                        onClick = {
                            haptics.soft()
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
                            haptics.soft()
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
                    IconButton(
                        onClick = {
                            haptics.soft()
                            expanded = !expanded
                        },
                    ) {
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
                        onSelect = { date ->
                            haptics.soft()
                            jumpTo(date)
                            scope.launch { listState.animateScrollToItem(DAY_SECTION_INDEX) }
                        },
                    )
                }
                Spacer(Modifier.height(Spacing.lg))
            }

            item {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Two-day timeline",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                        Text(
                            text = "Tap an empty hour to add a review. " +
                                "$plannedToday planned · $doneToday done",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    if (selectedDate == today) {
                        TextButton(
                            onClick = {
                                haptics.soft()
                                scope.launch {
                                    listState.animateScrollToItem(
                                        TIMELINE_START_INDEX + currentHour,
                                    )
                                }
                            },
                        ) {
                            Text("Now")
                        }
                    }
                }
                Spacer(Modifier.height(Spacing.sm))
            }

            item {
                NextUpCard(
                    next = nextPending,
                    topic = nextPending?.let { topicsById[it.topicId] },
                    onStart = { topicId ->
                        haptics.strong()
                        onReview(topicId)
                    },
                )
                Spacer(Modifier.height(Spacing.md))
            }

            item {
                TwoDayHeader(
                    leftDate = selectedDate,
                    rightDate = nextDate,
                    today = today,
                    leftPlanned = plannedToday,
                    rightPlanned = nextEntries.count { it is AgendaEntry.Planned },
                )
                Spacer(Modifier.height(Spacing.xs))
            }

            items(24, key = { "hour-$it" }) { hour ->
                TwoDayHourRow(
                    hour = hour,
                    leftDate = selectedDate,
                    rightDate = nextDate,
                    leftEntries = entriesByHour[hour].orEmpty(),
                    rightEntries = nextEntriesByHour[hour].orEmpty(),
                    leftIsToday = selectedDate == today,
                    rightIsToday = nextDate == today,
                    currentHour = currentHour,
                    haptics = haptics,
                    onEntryClick = { entry ->
                        haptics.soft()
                        when (entry) {
                            is AgendaEntry.Planned -> detailEventId = entry.event.id
                            is AgendaEntry.Done -> detailEventId = entry.event.id
                            is AgendaEntry.Reviewed -> Unit
                        }
                    },
                    onAdd = { date, addHour ->
                        addSheetDateEpoch = date.toEpochDay()
                        addSheetHour = addHour
                        showAddSheet = true
                    },
                )
            }

            item {
                Spacer(Modifier.height(Spacing.lg))
                Text(
                    text = "Swipe months · tap a day for its timeline · tap an event to move it " +
                        "or start the review. The + button adds a review from anywhere in the month.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        FloatingActionButton(
            onClick = {
                haptics.soft()
                addSheetDateEpoch = selectedEpochDay
                addSheetHour = -1
                showAddSheet = true
            },
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
            initialDate = LocalDate.ofEpochDay(addSheetDateEpoch),
            initialHour = addSheetHour.takeIf { it >= 0 },
            haptics = haptics,
            onDismiss = { showAddSheet = false },
            onConfirm = { topicId, whenMillis ->
                haptics.strong()
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
                haptics.soft()
                vm.moveEvent(event.id, newTime)
                detailEventId = null
            },
            onDelete = {
                haptics.strong()
                vm.deleteEvent(event.id)
                detailEventId = null
            },
            onReview = { topicId ->
                haptics.strong()
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

private fun buildDayEntries(
    date: LocalDate,
    eventsByDay: Map<LocalDate, List<ScheduledEvent>>,
    sessionsByDay: Map<LocalDate, List<ReviewSession>>,
    topicsById: Map<Long, Topic>,
): List<AgendaEntry> {
    val dayEvents = eventsByDay[date].orEmpty()
    val daySessions = sessionsByDay[date].orEmpty()
    val reviewedTopicIds = daySessions.map { it.topicId }.toSet()
    return buildList {
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
                            inMonth = date.year == month.year && date.monthValue == month.monthValue,
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
private fun NextUpCard(
    next: ScheduledEvent?,
    topic: Topic?,
    onStart: (Long) -> Unit,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(Spacing.lg),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "NEXT UP",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Spacer(Modifier.height(Spacing.xs))
                if (next == null) {
                    Text(
                        text = "Nothing scheduled. Tap + to plan a review.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                } else {
                    Text(
                        text = topic?.name ?: "Deleted topic",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                    Text(
                        text = "${dayPrefix(next.scheduledTime)} · " +
                            "${formatTime(next.scheduledTime)} · " +
                            nextUpLabel(next.scheduledTime),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f),
                    )
                }
            }
            if (next != null && topic != null && !next.isCompleted) {
                Button(onClick = { onStart(topic.id) }) {
                    Icon(
                        imageVector = Icons.Outlined.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                    )
                    Spacer(Modifier.width(Spacing.xs))
                    Text("Start")
                }
            }
        }
    }
}

@Composable
private fun TwoDayHeader(
    leftDate: LocalDate,
    rightDate: LocalDate,
    today: LocalDate,
    leftPlanned: Int,
    rightPlanned: Int,
) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Spacer(Modifier.width(58.dp))
        DayColumnHeader(
            date = leftDate,
            today = today,
            planned = leftPlanned,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(Spacing.sm))
        DayColumnHeader(
            date = rightDate,
            today = today,
            planned = rightPlanned,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DayColumnHeader(
    date: LocalDate,
    today: LocalDate,
    planned: Int,
    modifier: Modifier = Modifier,
) {
    val label = when (date) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(WeekdayFormat)
    }
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = "${date.format(MonthDayFormat)} · $planned planned",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun TwoDayHourRow(
    hour: Int,
    leftDate: LocalDate,
    rightDate: LocalDate,
    leftEntries: List<AgendaEntry>,
    rightEntries: List<AgendaEntry>,
    leftIsToday: Boolean,
    rightIsToday: Boolean,
    currentHour: Int,
    haptics: Haptics,
    onEntryClick: (AgendaEntry) -> Unit,
    onAdd: (LocalDate, Int) -> Unit,
) {
    val leftIsNow = leftIsToday && hour == currentHour
    val rightIsNow = rightIsToday && hour == currentHour
    val labelColor = if (leftIsNow || rightIsNow) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 56.dp),
    ) {
        Column(
            modifier = Modifier
                .width(58.dp)
                .padding(top = 2.dp, end = Spacing.sm),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = LocalTime.of(hour, 0).format(HourFormat),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = if (leftIsNow || rightIsNow) FontWeight.Bold else FontWeight.Medium,
                color = labelColor,
            )
            Text(
                text = LocalTime.of(hour, 0).format(AmPmFormat),
                style = MaterialTheme.typography.labelSmall,
                color = labelColor.copy(alpha = 0.7f),
            )
        }
        DayHourCell(
            date = leftDate,
            hour = hour,
            entries = leftEntries,
            isNow = leftIsNow,
            haptics = haptics,
            onEntryClick = onEntryClick,
            onAdd = onAdd,
            modifier = Modifier.weight(1f),
        )
        Spacer(Modifier.width(Spacing.sm))
        DayHourCell(
            date = rightDate,
            hour = hour,
            entries = rightEntries,
            isNow = rightIsNow,
            haptics = haptics,
            onEntryClick = onEntryClick,
            onAdd = onAdd,
            modifier = Modifier.weight(1f),
        )
    }
}

@Composable
private fun DayHourCell(
    date: LocalDate,
    hour: Int,
    entries: List<AgendaEntry>,
    isNow: Boolean,
    haptics: Haptics,
    onEntryClick: (AgendaEntry) -> Unit,
    onAdd: (LocalDate, Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.heightIn(min = 56.dp)) {
        HorizontalDivider(
            modifier = Modifier.padding(top = 6.dp),
            thickness = if (isNow) 1.5.dp else 1.dp,
            color = if (isNow) {
                MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            },
        )
        if (entries.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .clickable {
                        haptics.soft()
                        onAdd(date, hour)
                    },
                contentAlignment = Alignment.CenterEnd,
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Add review at this hour",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.22f),
                    modifier = Modifier
                        .padding(end = Spacing.sm)
                        .size(16.dp),
                )
            }
        } else {
            Spacer(Modifier.height(Spacing.xs))
            entries.forEach { entry ->
                TimelineEventCard(entry = entry, onClick = { onEntryClick(entry) })
                Spacer(Modifier.height(Spacing.xs))
            }
        }
    }
}

@Composable
private fun TimelineEventCard(entry: AgendaEntry, onClick: () -> Unit) {
    val accent = when (entry) {
        is AgendaEntry.Planned -> MaterialTheme.colorScheme.primary
        is AgendaEntry.Reviewed -> MaterialTheme.colorScheme.tertiary
        is AgendaEntry.Done -> MaterialTheme.colorScheme.outline
    }
    val container = when (entry) {
        is AgendaEntry.Planned -> MaterialTheme.colorScheme.primaryContainer
        is AgendaEntry.Reviewed -> MaterialTheme.colorScheme.tertiaryContainer
        is AgendaEntry.Done -> MaterialTheme.colorScheme.surfaceContainerHigh
    }
    val content = when (entry) {
        is AgendaEntry.Planned -> MaterialTheme.colorScheme.onPrimaryContainer
        is AgendaEntry.Reviewed -> MaterialTheme.colorScheme.onTertiaryContainer
        is AgendaEntry.Done -> MaterialTheme.colorScheme.onSurface
    }
    val subtitle = when (entry) {
        is AgendaEntry.Planned ->
            entry.topic?.category?.takeIf { it.isNotBlank() } ?: "Planned review"
        is AgendaEntry.Done -> "Completed"
        is AgendaEntry.Reviewed -> "Reviewed · " +
            "${durationLabel(entry.session.durationSeconds.toLong())} · " +
            ratingLabel(entry.session.recallRating)
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(container)
            .then(
                if (entry is AgendaEntry.Reviewed) Modifier else Modifier.clickable(onClick = onClick),
            )
            .padding(horizontal = Spacing.md, vertical = Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        Box(
            modifier = Modifier
                .width(3.dp)
                .height(32.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(accent),
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = entry.topic?.name ?: "Deleted topic",
                style = MaterialTheme.typography.titleSmall,
                color = content,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = "${formatTime(entry.time)} · $subtitle",
                style = MaterialTheme.typography.bodySmall,
                color = content.copy(alpha = 0.75f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
        when (entry) {
            is AgendaEntry.Planned -> Icon(
                imageVector = Icons.Outlined.PlayArrow,
                contentDescription = "Start review",
                tint = content,
            )
            is AgendaEntry.Done -> Icon(
                imageVector = Icons.Outlined.Check,
                contentDescription = "Completed",
                tint = content,
            )
            is AgendaEntry.Reviewed -> Text(
                text = "${entry.session.recallRating}/5",
                style = MaterialTheme.typography.labelMedium,
                color = content,
            )
        }
    }
}

private fun dayPrefix(epochMillis: Long): String {
    val date = epochMillis.toLocalDate()
    val today = LocalDate.now()
    return when (date) {
        today -> "Today"
        today.plusDays(1) -> "Tomorrow"
        today.minusDays(1) -> "Yesterday"
        else -> date.format(MonthDayFormat)
    }
}

private fun nextUpLabel(
    epochMillis: Long,
    nowMillis: Long = System.currentTimeMillis(),
): String {
    if (epochMillis <= nowMillis) return "overdue"
    val minutes = (epochMillis - nowMillis) / 60_000L
    return when {
        minutes < 1L -> "now"
        minutes < 60L -> "in ${minutes}m"
        minutes < 24L * 60L -> "in ${minutes / 60}h ${minutes % 60}m"
        else -> relativeDueLabel(epochMillis, nowMillis).lowercase(Locale.getDefault())
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun QuickAddSheet(
    topics: List<Topic>,
    initialDate: LocalDate,
    initialHour: Int?,
    haptics: Haptics,
    onDismiss: () -> Unit,
    onConfirm: (Long, Long) -> Unit,
) {
    val context = LocalContext.current
    val today = LocalDate.now()
    var topicId by rememberSaveable { mutableLongStateOf(topics.firstOrNull()?.id ?: 0L) }
    var dateEpoch by rememberSaveable { mutableLongStateOf(initialDate.toEpochDay()) }
    var minuteOfDay by rememberSaveable {
        mutableIntStateOf(initialHour?.coerceIn(0, 23)?.times(60) ?: nextHourMinute())
    }
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
                        onClick = {
                            haptics.soft()
                            dateEpoch = today.toEpochDay()
                        },
                        label = { Text("Today") },
                    )
                }
                item {
                    FilterChip(
                        selected = date == today.plusDays(1),
                        onClick = {
                            haptics.soft()
                            dateEpoch = today.plusDays(1).toEpochDay()
                        },
                        label = { Text("Tomorrow") },
                    )
                }
                item {
                    AssistChip(
                        onClick = {
                            DatePickerDialog(
                                context,
                                { _, year, month, day ->
                                    haptics.soft()
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
                        { _, hour, minute ->
                            haptics.soft()
                            minuteOfDay = hour * 60 + minute
                        },
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
                            onClick = {
                                haptics.soft()
                                topicId = topic.id
                            },
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

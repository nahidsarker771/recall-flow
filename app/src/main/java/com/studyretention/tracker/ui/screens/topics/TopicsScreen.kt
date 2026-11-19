package com.studyretention.tracker.ui.screens.topics

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.studyretention.tracker.data.Topic
import com.studyretention.tracker.ui.components.EmptyState
import com.studyretention.tracker.ui.components.MasteryBar
import com.studyretention.tracker.ui.components.ScreenHeader
import com.studyretention.tracker.ui.components.TagChip
import com.studyretention.tracker.ui.components.TopicEditorDialog
import com.studyretention.tracker.ui.theme.Spacing
import com.studyretention.tracker.ui.util.relativeDueLabel
import com.studyretention.tracker.viewmodel.RecallFlowViewModel

@Composable
fun TopicsScreen(
    vm: RecallFlowViewModel,
    onOpenTopic: (Long) -> Unit,
) {
    val topics by vm.topics.collectAsStateWithLifecycle()
    var query by rememberSaveable { mutableStateOf("") }
    var showAddDialog by rememberSaveable { mutableStateOf(false) }

    val filtered = remember(topics, query) {
        if (query.isBlank()) {
            topics
        } else {
            val needle = query.trim().lowercase()
            topics.filter { topic ->
                topic.name.lowercase().contains(needle) ||
                    topic.category.lowercase().contains(needle) ||
                    topic.notes.lowercase().contains(needle)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize()) {
            Column(modifier = Modifier.padding(horizontal = Spacing.screen, vertical = Spacing.lg)) {
                ScreenHeader(
                    title = "Topics",
                    subtitle = "${topics.size} ${if (topics.size == 1) "topic" else "topics"} in your library.",
                )
                Spacer(Modifier.height(Spacing.lg))
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    modifier = Modifier.fillMaxWidth(),
                    placeholder = { Text("Search topics, tags, notes") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Outlined.Search,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    },
                    trailingIcon = {
                        if (query.isNotEmpty()) {
                            IconButton(onClick = { query = "" }) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = "Clear search",
                                )
                            }
                        }
                    },
                    singleLine = true,
                    shape = MaterialTheme.shapes.large,
                    colors = OutlinedTextFieldDefaults.colors(
                        unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerLow,
                        unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant,
                    ),
                )
            }

            when {
                topics.isEmpty() -> Column(modifier = Modifier.padding(horizontal = Spacing.screen)) {
                    EmptyState(
                        icon = Icons.Outlined.MenuBook,
                        title = "No topics yet",
                        message = "Tap the + button to add what you're learning. Its first review is scheduled automatically.",
                    )
                }

                filtered.isEmpty() -> Column(modifier = Modifier.padding(horizontal = Spacing.screen)) {
                    EmptyState(
                        icon = Icons.Outlined.Search,
                        title = "No matches",
                        message = "Nothing matches \"$query\". Try a different word or clear the search.",
                    )
                }

                else -> LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = Spacing.screen,
                        end = Spacing.screen,
                        top = Spacing.sm,
                        bottom = 96.dp,
                    ),
                    verticalArrangement = Arrangement.spacedBy(Spacing.sm),
                ) {
                    items(filtered, key = { it.id }) { topic ->
                        TopicListItem(
                            topic = topic,
                            onClick = { onOpenTopic(topic.id) },
                        )
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(Spacing.screen),
            shape = CircleShape,
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary,
        ) {
            Icon(imageVector = Icons.Filled.Add, contentDescription = "Add topic")
        }
    }

    if (showAddDialog) {
        TopicEditorDialog(
            title = "New topic",
            onDismiss = { showAddDialog = false },
            onConfirm = { name, notes, category ->
                vm.addTopic(name, notes, category)
                showAddDialog = false
            },
        )
    }
}

@Composable
private fun TopicListItem(topic: Topic, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(modifier = Modifier.padding(Spacing.lg)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = topic.name,
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.size(Spacing.sm))
                Text(
                    text = "${topic.masteryLevel}%",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            if (topic.category.isNotBlank()) {
                Spacer(Modifier.height(Spacing.sm))
                TagChip(text = topic.category)
            }
            Spacer(Modifier.height(Spacing.md))
            MasteryBar(level = topic.masteryLevel)
            Spacer(Modifier.height(Spacing.sm))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = relativeDueLabel(topic.nextReviewAt),
                    modifier = Modifier.weight(1f),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (topic.nextReviewAt <= System.currentTimeMillis()) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
                Text(
                    text = "Mastery",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

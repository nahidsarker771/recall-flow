package com.studyretention.tracker.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.studyretention.tracker.ui.navigation.BottomTab
import com.studyretention.tracker.ui.navigation.BottomTabs
import com.studyretention.tracker.ui.navigation.Routes
import com.studyretention.tracker.ui.screens.onboarding.OnboardingScreen
import com.studyretention.tracker.ui.screens.review.ReviewScreen
import com.studyretention.tracker.ui.screens.schedule.ScheduleScreen
import com.studyretention.tracker.ui.screens.settings.SettingsScreen
import com.studyretention.tracker.ui.screens.stats.StatsScreen
import com.studyretention.tracker.ui.screens.today.TodayScreen
import com.studyretention.tracker.ui.screens.topics.TopicDetailScreen
import com.studyretention.tracker.ui.screens.topics.TopicsScreen
import com.studyretention.tracker.viewmodel.RecallFlowViewModel

/**
 * Single-activity app shell: NavHost plus the four-tab bottom navigation.
 * The bar hides itself on secondary routes (topic detail, review, settings).
 */
@Composable
fun RecallFlowApp(
    initialReviewTopicId: Long? = null,
    onDeepLinkConsumed: () -> Unit = {},
) {
    val vm: RecallFlowViewModel = viewModel()
    val navController = rememberNavController()
    val startDestination = androidx.compose.runtime.remember {
        if (vm.onboardingCompleted()) Routes.TODAY else Routes.ONBOARDING
    }

    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val activeTab = BottomTabs.firstOrNull { tab ->
        currentDestination?.hierarchy?.any { it.route == tab.route } == true
    }

    val rootRoutes = remember { BottomTabs.map { it.route }.toSet() }
    val tabHistory = remember { mutableStateListOf<String>() }

    val openTab: (BottomTab) -> Unit = { tab ->
        val current = currentDestination?.route
        if (tab.route != current) {
            if (current != null && current in rootRoutes && tabHistory.lastOrNull() != current) {
                tabHistory.add(current)
                if (tabHistory.size > 8) tabHistory.removeAt(0)
            }
            navController.navigateToTab(tab)
        }
    }

    val currentRoute = currentDestination?.route
    BackHandler(
        enabled = currentRoute != null &&
            currentRoute in rootRoutes &&
            (tabHistory.isNotEmpty() || currentRoute != Routes.TODAY),
    ) {
        val previous = tabHistory.removeLastOrNull()
            ?: Routes.TODAY.takeIf { currentRoute != Routes.TODAY }
        if (previous != null) navController.navigateToRoot(previous)
    }

    LaunchedEffect(initialReviewTopicId) {
        val topicId = initialReviewTopicId
        if (topicId != null && topicId > 0) {
            navController.navigate(Routes.review(topicId)) { launchSingleTop = true }
            onDeepLinkConsumed()
        }
    }

    val closeReview: () -> Unit = {
        if (!navController.popBackStack()) {
            navController.navigate(Routes.TODAY) {
                popUpTo(navController.graph.findStartDestination().id) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        bottomBar = {
            if (activeTab != null) {
                RecallFlowBottomBar(
                    activeTab = activeTab,
                    onSelect = { tab -> openTab(tab) },
                )
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onFinish = {
                        vm.completeOnboarding()
                        if (!navController.popBackStack()) {
                            navController.navigate(Routes.TODAY) {
                                popUpTo(Routes.ONBOARDING) { inclusive = true }
                            }
                        }
                    },
                )
            }
            composable(Routes.TODAY) {
                TodayScreen(
                    vm = vm,
                    onOpenSettings = { navController.navigate(Routes.SETTINGS) },
                    onOpenTopics = {
                        val topicsTab = BottomTabs.first { it.route == Routes.TOPICS }
                        openTab(topicsTab)
                    },
                    onOpenTopic = { navController.navigate(Routes.topicDetail(it)) },
                    onReview = { navController.navigate(Routes.review(it)) },
                )
            }
            composable(Routes.TOPICS) {
                TopicsScreen(
                    vm = vm,
                    onOpenTopic = { navController.navigate(Routes.topicDetail(it)) },
                )
            }
            composable(Routes.SCHEDULE) {
                ScheduleScreen(
                    vm = vm,
                    onReview = { navController.navigate(Routes.review(it)) },
                )
            }
            composable(Routes.STATS) { StatsScreen(vm) }
            composable(Routes.SETTINGS) {
                SettingsScreen(
                    vm = vm,
                    onBack = { navController.popBackStack() },
                    onOpenHowItWorks = {
                        navController.navigate(Routes.ONBOARDING) { launchSingleTop = true }
                    },
                )
            }
            composable(
                route = Routes.TOPIC_DETAIL,
                arguments = listOf(navArgument(Routes.ARG_TOPIC_ID) { type = NavType.LongType }),
            ) { entry ->
                val topicId = entry.arguments?.getLong(Routes.ARG_TOPIC_ID) ?: 0L
                TopicDetailScreen(
                    vm = vm,
                    topicId = topicId,
                    onBack = { navController.popBackStack() },
                    onReview = { navController.navigate(Routes.review(it)) },
                )
            }
            composable(
                route = Routes.REVIEW,
                arguments = listOf(navArgument(Routes.ARG_TOPIC_ID) { type = NavType.LongType }),
            ) { entry ->
                val topicId = entry.arguments?.getLong(Routes.ARG_TOPIC_ID) ?: 0L
                ReviewScreen(
                    vm = vm,
                    topicId = topicId,
                    onDone = closeReview,
                )
            }
        }
    }
}

@Composable
private fun RecallFlowBottomBar(
    activeTab: BottomTab,
    onSelect: (BottomTab) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface),
    ) {
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        NavigationBar(
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 0.dp,
        ) {
            BottomTabs.forEach { tab ->
                val selected = tab == activeTab
                NavigationBarItem(
                    selected = selected,
                    onClick = { onSelect(tab) },
                    icon = {
                        Icon(
                            imageVector = if (selected) tab.selectedIcon else tab.unselectedIcon,
                            contentDescription = tab.label,
                        )
                    },
                    label = {
                        Text(
                            text = tab.label,
                            style = MaterialTheme.typography.labelMedium,
                        )
                    },
                    alwaysShowLabel = true,
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = MaterialTheme.colorScheme.primary,
                        selectedTextColor = MaterialTheme.colorScheme.primary,
                        indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                        unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                    ),
                )
            }
        }
    }
}

private fun NavHostController.navigateToTab(tab: BottomTab) = navigateToRoot(tab.route)

private fun NavHostController.navigateToRoot(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

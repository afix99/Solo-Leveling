package com.ascend.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Equalizer
import androidx.compose.material.icons.filled.MoreHoriz
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.TaskAlt
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.ascend.app.data.repo.AscendRepository
import com.ascend.app.domain.Leveling
import com.ascend.app.ui.components.DockItem
import com.ascend.app.ui.components.SystemDock
import com.ascend.app.ui.screens.achievements.AchievementsScreen
import com.ascend.app.ui.screens.habits.HabitsScreen
import com.ascend.app.ui.screens.help.HowItWorksScreen
import com.ascend.app.ui.screens.liestruths.LieTruthScreen
import com.ascend.app.ui.screens.onboarding.OnboardingScreen
import com.ascend.app.ui.screens.profile.ProfileScreen
import com.ascend.app.ui.screens.shop.ShopScreen
import com.ascend.app.ui.screens.stats.StatsScreen
import com.ascend.app.ui.screens.today.TodayScreen
import com.ascend.app.ui.screens.weeklyreview.WeeklyReviewScreen
import com.ascend.app.ui.theme.AscendColors

private sealed class Dest(val route: String, val label: String, val icon: ImageVector) {
    data object Today : Dest("today", "Today", Icons.Default.TaskAlt)
    data object Stats : Dest("stats", "Stats", Icons.Default.Equalizer)
    data object Habits : Dest("habits", "Habits", Icons.Default.Checklist)
    data object Shop : Dest("shop", "Shop", Icons.Default.Storefront)
    data object Profile : Dest("profile", "More", Icons.Default.MoreHoriz)
}

private val bottomTabs = listOf(Dest.Today, Dest.Stats, Dest.Habits, Dest.Shop, Dest.Profile)
private const val ROUTE_LIES_TRUTHS = "lies_truths"
private const val ROUTE_HOW_IT_WORKS = "how_it_works"
private const val ROUTE_WEEKLY_REVIEW = "weekly_review"
private const val ROUTE_ACHIEVEMENTS = "achievements"

@Composable
fun AscendApp(repository: AscendRepository) {
    val hunterProfile by repository.observeHunterProfile().collectAsStateWithLifecycle(initialValue = null)
    // Onboarding writes onboardingComplete=true at the very end; remembering
    // that locally avoids a one-frame flash back to onboarding while the DB
    // write is still propagating through the Flow.
    var onboardedThisSession by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(AscendColors.Background)
            .statusBarsPadding(),
        contentAlignment = Alignment.Center,
    ) {
        val profile = hunterProfile
        when {
            // First launch, before the profile row is written. Showing the
            // wordmark rather than nothing, so this never reads as a dead app.
            profile == null -> Text(
                "ASCEND",
                color = AscendColors.AccentBlue,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                letterSpacing = 4.sp,
            )
            profile.onboardingComplete || onboardedThisSession -> MainScaffold(repository)
            else -> OnboardingScreen(repository = repository, onComplete = { onboardedThisSession = true })
        }
    }
}

@Composable
private fun MainScaffold(repository: AscendRepository) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val selectedRoute = bottomTabs.firstOrNull { dest ->
        currentDestination?.hierarchy?.any { it.route == dest.route } == true
    }?.route

    // Laid out as a Column rather than a Scaffold so the dock reserves its own
    // space — content can never end up hidden underneath it. statusBarsPadding
    // keeps the header clear of the clock and battery icons.
    Column(modifier = Modifier.fillMaxSize().background(AscendColors.Background)) {
        Box(modifier = Modifier.weight(1f).fillMaxSize()) {
            NavHost(navController = navController, startDestination = Dest.Today.route) {
                composable(Dest.Today.route) {
                    TodayScreen(repository = repository, onOpenHabits = { navController.navigate(Dest.Habits.route) })
                }
                composable(Dest.Stats.route) { StatsScreen(repository = repository) }
                composable(Dest.Habits.route) { HabitsScreen(repository = repository) }
                composable(Dest.Shop.route) { ShopScreen(repository = repository) }
                composable(Dest.Profile.route) {
                    ProfileScreen(
                        repository = repository,
                        onOpenLiesTruths = { navController.navigate(ROUTE_LIES_TRUTHS) },
                        onOpenHowItWorks = { navController.navigate(ROUTE_HOW_IT_WORKS) },
                        onOpenWeeklyReview = { navController.navigate(ROUTE_WEEKLY_REVIEW) },
                        onOpenAchievements = { navController.navigate(ROUTE_ACHIEVEMENTS) },
                    )
                }
                composable(ROUTE_WEEKLY_REVIEW) { WeeklyReviewScreen(repository = repository) }
                composable(ROUTE_LIES_TRUTHS) { LieTruthScreen(repository = repository) }
                composable(ROUTE_HOW_IT_WORKS) { HowItWorksScreen() }
                composable(ROUTE_ACHIEVEMENTS) {
                    val totalXp by repository.observeTotalXp().collectAsStateWithLifecycle(initialValue = 0)
                    AchievementsScreen(
                        repository = repository,
                        currentRank = Leveling.rankForTotalXp(totalXp),
                    )
                }
            }
        }

        SystemDock(
            items = bottomTabs.map { DockItem(route = it.route, label = it.label, icon = it.icon) },
            selectedRoute = selectedRoute,
            onSelect = { route ->
                navController.navigate(route) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            },
        )
    }
}

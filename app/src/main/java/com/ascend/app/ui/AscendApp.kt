package com.ascend.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
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
import com.ascend.app.ui.screens.habits.HabitsScreen
import com.ascend.app.ui.screens.help.HowItWorksScreen
import com.ascend.app.ui.screens.liestruths.LieTruthScreen
import com.ascend.app.ui.screens.onboarding.OnboardingScreen
import com.ascend.app.ui.screens.profile.ProfileScreen
import com.ascend.app.ui.screens.stats.StatsScreen
import com.ascend.app.ui.screens.today.TodayScreen
import com.ascend.app.ui.screens.weeklyreview.WeeklyReviewScreen
import com.ascend.app.ui.theme.AscendColors

private sealed class Dest(val route: String, val label: String, val icon: ImageVector) {
    data object Today : Dest("today", "Today", Icons.Default.Home)
    data object Stats : Dest("stats", "Stats", Icons.Default.Bolt)
    data object Habits : Dest("habits", "Habits", Icons.Default.List)
    data object Review : Dest("weekly_review", "Review", Icons.Default.Insights)
    data object Profile : Dest("profile", "More", Icons.Default.Person)
}

private val bottomTabs = listOf(Dest.Today, Dest.Stats, Dest.Habits, Dest.Review, Dest.Profile)
private const val ROUTE_LIES_TRUTHS = "lies_truths"
private const val ROUTE_HOW_IT_WORKS = "how_it_works"

@Composable
fun AscendApp(repository: AscendRepository) {
    val hunterProfile by repository.observeHunterProfile().collectAsStateWithLifecycle(initialValue = null)
    // Onboarding writes onboardingComplete=true at the very end; remembering
    // that locally avoids a one-frame flash back to onboarding while the DB
    // write is still propagating through the Flow.
    var onboardedThisSession by remember { mutableStateOf(false) }

    Box(
        modifier = Modifier.fillMaxSize().background(AscendColors.Background),
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

    Scaffold(
        containerColor = AscendColors.Background,
        bottomBar = {
            val backStackEntry by navController.currentBackStackEntryAsState()
            val currentDestination = backStackEntry?.destination
            NavigationBar(containerColor = AscendColors.Surface) {
                bottomTabs.forEach { dest ->
                    val selected = currentDestination?.hierarchy?.any { it.route == dest.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(dest.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(dest.icon, contentDescription = dest.label) },
                        label = { Text(dest.label) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = AscendColors.AccentBlue,
                            selectedTextColor = AscendColors.AccentBlue,
                            unselectedIconColor = AscendColors.TextTertiary,
                            unselectedTextColor = AscendColors.TextTertiary,
                            indicatorColor = AscendColors.SurfaceElevated2,
                        ),
                    )
                }
            }
        },
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AscendColors.Background)
                .padding(bottom = padding.calculateBottomPadding()),
        ) {
            NavHost(navController = navController, startDestination = Dest.Today.route) {
                composable(Dest.Today.route) {
                    TodayScreen(repository = repository, onOpenHabits = { navController.navigate(Dest.Habits.route) })
                }
                composable(Dest.Stats.route) { StatsScreen(repository = repository) }
                composable(Dest.Habits.route) { HabitsScreen(repository = repository) }
                composable(Dest.Review.route) { WeeklyReviewScreen(repository = repository) }
                composable(Dest.Profile.route) {
                    ProfileScreen(
                        repository = repository,
                        onOpenLiesTruths = { navController.navigate(ROUTE_LIES_TRUTHS) },
                        onOpenHowItWorks = { navController.navigate(ROUTE_HOW_IT_WORKS) },
                    )
                }
                composable(ROUTE_LIES_TRUTHS) { LieTruthScreen(repository = repository) }
                composable(ROUTE_HOW_IT_WORKS) { HowItWorksScreen() }
            }
        }
    }
}

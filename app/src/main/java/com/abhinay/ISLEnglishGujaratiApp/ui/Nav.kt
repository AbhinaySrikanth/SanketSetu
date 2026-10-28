package com.abhinay.ISLEnglishGujaratiApp.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.abhinay.ISLEnglishGujaratiApp.ui.screens.*

/**
 * Route map
 *  Level 0  onboarding (first run only; removed from the back stack when finished)
 *  Level 1  home
 *  Level 2  learn | practice | communicate | settings   (bottom bar tabs, each keeps its own stack)
 *  Level 3+ category/{c}, sign/{id}, phrases/{c}, display/{id}, review, quiz/{key}, recognize, about
 * Back: child -> parent; tab root -> Home; Home -> exit.
 */
object Routes {
    const val ONBOARDING = "onboarding"
    const val HOME = "home"
    const val LEARN = "learn"
    const val PRACTICE = "practice"
    const val COMMUNICATE = "communicate"
    const val SETTINGS = "settings"
    const val CATEGORY = "category/{c}"
    const val SIGN = "sign/{id}"
    const val PHRASES = "phrases/{c}"
    const val DISPLAY = "display/{id}"
    const val REVIEW = "review"
    const val QUIZ = "quiz/{key}"
    const val RECOGNIZE = "recognize"
    const val ABOUT = "about"

    fun category(c: String) = "category/$c"
    fun sign(id: String) = "sign/$id"
    fun phrases(c: String) = "phrases/$c"
    fun display(id: String) = "display/$id"
    fun quiz(key: String) = "quiz/$key"
}

private class Tab(val route: String, val label: String, val icon: ImageVector)

private val tabs = listOf(
    Tab(Routes.HOME, "Home", Icons.Default.Home),
    Tab(Routes.LEARN, "Learn", Icons.AutoMirrored.Filled.List),
    Tab(Routes.PRACTICE, "Practice", Icons.Default.Check),
    Tab(Routes.COMMUNICATE, "Communicate", Icons.Default.Face),
    Tab(Routes.SETTINGS, "Settings", Icons.Default.Settings),
)

/** Which tab a route belongs to, or null when the route is a focused flow without the bottom bar. */
private fun tabOf(route: String?): String? = when (route) {
    Routes.HOME, Routes.LEARN, Routes.PRACTICE, Routes.COMMUNICATE, Routes.SETTINGS -> route
    Routes.CATEGORY -> Routes.LEARN
    Routes.PHRASES -> Routes.COMMUNICATE
    else -> null
}

private fun NavController.goToTab(route: String) {
    navigate(route) {
        popUpTo(graph.findStartDestination().id) { saveState = true }
        launchSingleTop = true
        restoreState = true
    }
}

/** Ignores a tap when the screen is already leaving, so fast double taps cannot stack screens. */
private fun NavController.go(route: String) {
    if (currentBackStackEntry?.lifecycle?.currentState == androidx.lifecycle.Lifecycle.State.RESUMED) navigate(route) { launchSingleTop = true }
}

private fun NavController.up() {
    if (currentBackStackEntry?.lifecycle?.currentState == androidx.lifecycle.Lifecycle.State.RESUMED) popBackStack()
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SanketSetuNav(vm: AppViewModel) {
    val nav = rememberNavController()
    val state by vm.state.collectAsStateWithLifecycle()
    val startOnboarding = remember { !state.onboarded }
    val entry by nav.currentBackStackEntryAsState()
    val route = entry?.destination?.route
    val currentTab = tabOf(route)
    val keyboardOpen = WindowInsets.isImeVisible

    // Back from a non-home tab root returns to Home rather than leaving the app.
    BackHandler(enabled = currentTab != null && route == currentTab && route != Routes.HOME) { nav.goToTab(Routes.HOME) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0),
        bottomBar = {
            if (currentTab != null && !keyboardOpen) NavigationBar {
                tabs.forEach { t ->
                    NavigationBarItem(
                        selected = currentTab == t.route,
                        onClick = {
                            // Reselecting the current tab goes back to that tab's root.
                            if (currentTab == t.route) nav.popBackStack(t.route, inclusive = false) else nav.goToTab(t.route)
                        },
                        icon = { Icon(t.icon, contentDescription = null) },
                        label = { Text(t.label, maxLines = 1) },
                    )
                }
            }
        },
    ) { pad ->
        val showBar = currentTab != null && !keyboardOpen
        CompositionLocalProvider(LocalBarShown provides showBar) { NavHost(
            nav,
            startDestination = if (startOnboarding) Routes.ONBOARDING else Routes.HOME,
            modifier = Modifier.padding(if (showBar) pad else PaddingValues(0.dp)),
        ) {
            composable(Routes.ONBOARDING) {
                OnboardingScreen { gu ->
                    vm.finishOnboarding(gu)
                    nav.navigate(Routes.HOME) { popUpTo(Routes.ONBOARDING) { inclusive = true } }
                }
            }
            composable(Routes.HOME) {
                HomeScreen(vm, onLesson = { nav.go(Routes.category(it)) }, onSign = { nav.go(Routes.sign(it)) }, onReview = { vm.startReview(); nav.go(Routes.REVIEW) })
            }
            composable(Routes.LEARN) { LearnScreen(vm, { nav.go(Routes.category(it)) }, { nav.go(Routes.sign(it)) }) }
            composable(Routes.CATEGORY) { CategoryScreen(vm, it.arg("c"), { nav.up() }, { id -> nav.go(Routes.sign(id)) }) }
            composable(Routes.SIGN) { SignDetailScreen(vm, it.arg("id"), { nav.up() }, { id -> nav.go(Routes.sign(id)) }) }
            composable(Routes.PRACTICE) {
                PracticeScreen(vm, onReview = { vm.startReview(); nav.go(Routes.REVIEW) }, onWeak = { vm.startWeakReview(); nav.go(Routes.REVIEW) }, onQuiz = { vm.startQuiz(it); nav.go(Routes.quiz(it)) })
            }
            composable(Routes.REVIEW) { ReviewScreen(vm, { nav.up() }, { id -> nav.go(Routes.sign(id)) }) }
            composable(Routes.QUIZ) { QuizScreen(vm, { nav.up() }, { id -> nav.go(Routes.sign(id)) }) }
            composable(Routes.COMMUNICATE) { CommunicateScreen(vm, { nav.go(Routes.phrases(it)) }, { nav.go(Routes.display(it)) }, { nav.go(Routes.RECOGNIZE) }) }
            composable(Routes.PHRASES) { PhraseListScreen(vm, it.arg("c"), { nav.up() }, { id -> nav.go(Routes.display(id)) }) }
            composable(Routes.DISPLAY) { PhraseDisplayScreen(vm, it.arg("id")) { nav.up() } }
            composable(Routes.RECOGNIZE) { RecognizeScreen(vm, { nav.up() }, { id -> nav.go(Routes.sign(id)) }) }
            composable(Routes.SETTINGS) { SettingsScreen(vm) { nav.go(Routes.ABOUT) } }
            composable(Routes.ABOUT) { AboutScreen { nav.up() } }
        } }
    }
}

private fun androidx.navigation.NavBackStackEntry.arg(name: String) = arguments?.getString(name).orEmpty()

package com.factory.aquacoach

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
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
import com.factory.aquacoach.data.billing.PremiumManager
import com.factory.aquacoach.ui.AquaCoachViewModelFactory
import com.factory.aquacoach.ui.history.HistoryScreen
import com.factory.aquacoach.ui.history.HistoryViewModel
import com.factory.aquacoach.ui.home.HomeScreen
import com.factory.aquacoach.ui.home.HomeViewModel
import com.factory.aquacoach.ui.paywall.PaywallScreen
import com.factory.aquacoach.ui.paywall.PaywallViewModel
import com.factory.aquacoach.ui.settings.SettingsScreen
import com.factory.aquacoach.ui.settings.SettingsViewModel
import com.factory.aquacoach.ui.theme.AquaCoachTheme
import kotlinx.coroutines.flow.first

private sealed class AquaCoachDestination(val route: String, val label: String) {
    data object Home : AquaCoachDestination("home", "Home")
    data object History : AquaCoachDestination("history", "History")
    data object Settings : AquaCoachDestination("settings", "Settings")
}

private const val PAYWALL_ROUTE = "paywall/{trigger}"
private fun paywallRoute(trigger: String) = "paywall/$trigger"

private val bottomNavDestinations = listOf(
    AquaCoachDestination.Home,
    AquaCoachDestination.History,
    AquaCoachDestination.Settings
)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val app = application as AquaCoachApp
        val viewModelFactory = AquaCoachViewModelFactory(app)

        setContent {
            AquaCoachTheme {
                AquaCoachApp(viewModelFactory = viewModelFactory, premiumManager = app.premiumManager)
            }
        }
    }
}

@Composable
private fun AquaCoachApp(
    viewModelFactory: AquaCoachViewModelFactory,
    premiumManager: PremiumManager
) {
    val navController = rememberNavController()
    var startDestination by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        val hasSeenOnboardingPaywall = premiumManager.hasSeenOnboardingPaywall.first()
        startDestination = if (hasSeenOnboardingPaywall) {
            AquaCoachDestination.Home.route
        } else {
            paywallRoute("onboarding")
        }
    }

    val currentStartDestination = startDestination
    if (currentStartDestination == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val currentBackStackEntry by navController.currentBackStackEntryAsState()
    val showBottomBar = bottomNavDestinations.any { it.route == currentBackStackEntry?.destination?.route }

    Scaffold(
        bottomBar = { if (showBottomBar) AquaCoachBottomBar(navController) }
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = currentStartDestination,
            modifier = Modifier.padding(if (showBottomBar) innerPadding else PaddingValues(0.dp))
        ) {
            composable(AquaCoachDestination.Home.route) {
                val viewModel: HomeViewModel = viewModel(factory = viewModelFactory)
                HomeScreen(
                    viewModel = viewModel,
                    onUpgradeRequired = { navController.navigate(paywallRoute("feature")) }
                )
            }
            composable(AquaCoachDestination.History.route) {
                val viewModel: HistoryViewModel = viewModel(factory = viewModelFactory)
                HistoryScreen(
                    viewModel = viewModel,
                    onUpgradeRequired = { navController.navigate(paywallRoute("feature")) }
                )
            }
            composable(AquaCoachDestination.Settings.route) {
                val viewModel: SettingsViewModel = viewModel(factory = viewModelFactory)
                SettingsScreen(
                    viewModel = viewModel,
                    onUpgradeRequired = { navController.navigate(paywallRoute("settings")) }
                )
            }
            composable(
                route = PAYWALL_ROUTE,
                arguments = listOf(navArgument("trigger") { type = NavType.StringType })
            ) { entry ->
                val trigger = entry.arguments?.getString("trigger") ?: "feature"
                val viewModel: PaywallViewModel = viewModel(factory = viewModelFactory)
                PaywallScreen(
                    viewModel = viewModel,
                    trigger = trigger,
                    onClose = {
                        if (navController.previousBackStackEntry == null) {
                            navController.navigate(AquaCoachDestination.Home.route) {
                                popUpTo(PAYWALL_ROUTE) { inclusive = true }
                            }
                        } else {
                            navController.popBackStack()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun AquaCoachBottomBar(navController: NavHostController) {
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination
    val haptics = LocalHapticFeedback.current

    NavigationBar {
        bottomNavDestinations.forEach { destination ->
            val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
            NavigationBarItem(
                selected = selected,
                onClick = {
                    if (!selected) {
                        haptics.performHapticFeedback(HapticFeedbackType.LongPress)
                    }
                    navController.navigate(destination.route) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = {
                    Icon(
                        imageVector = when (destination) {
                            AquaCoachDestination.Home -> Icons.Filled.Home
                            AquaCoachDestination.History -> Icons.AutoMirrored.Filled.ShowChart
                            AquaCoachDestination.Settings -> Icons.Filled.Settings
                        },
                        contentDescription = destination.label
                    )
                },
                label = { Text(destination.label) }
            )
        }
    }
}

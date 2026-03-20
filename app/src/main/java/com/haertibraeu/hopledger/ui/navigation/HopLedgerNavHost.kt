package com.haertibraeu.hopledger.ui.navigation

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.haertibraeu.hopledger.data.repository.SyncStatus
import com.haertibraeu.hopledger.ui.AppViewModel
import com.haertibraeu.hopledger.ui.accounting.AccountingScreen
import com.haertibraeu.hopledger.ui.inventory.InventoryScreen
import com.haertibraeu.hopledger.ui.settings.SettingsScreen

enum class Screen(val route: String, val label: String, val icon: ImageVector) {
    Inventory("inventory", "Inventar", Icons.Default.Inventory2),
    Accounting("accounting", "Finanzen", Icons.Default.Payments),
    Settings("settings", "Einstellungen", Icons.Default.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HopLedgerNavHost(appViewModel: AppViewModel = hiltViewModel()) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    val syncStatus by appViewModel.syncStatus.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🍺 HopLedger") },
                actions = { SyncIndicator(syncStatus) },
            )
        },
        bottomBar = {
            NavigationBar {
                Screen.entries.forEach { screen ->
                    NavigationBarItem(
                        icon = { Icon(screen.icon, contentDescription = screen.label) },
                        label = { Text(screen.label) },
                        selected = currentDestination?.hierarchy?.any { it.route == screen.route } == true,
                        onClick = {
                            navController.navigate(screen.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Screen.Inventory.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Screen.Inventory.route) { InventoryScreen() }
            composable(Screen.Accounting.route) { AccountingScreen() }
            composable(Screen.Settings.route) { SettingsScreen() }
        }
    }
}

@Composable
private fun SyncIndicator(status: SyncStatus) {
    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f, targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
        label = "rotation",
    )

    when (status) {
        is SyncStatus.Idle -> {} // nothing
        is SyncStatus.Syncing -> Icon(
            Icons.Default.Sync, contentDescription = "Synchronisiere…",
            modifier = Modifier.padding(end = 12.dp).size(20.dp).rotate(rotation),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )
        is SyncStatus.Success -> Icon(
            Icons.Default.CloudDone, contentDescription = "Synchronisiert",
            modifier = Modifier.padding(end = 12.dp).size(20.dp),
            tint = Color(0xFF4CAF50),
        )
        is SyncStatus.Error -> Icon(
            Icons.Default.CloudOff, contentDescription = "Fehler: ${status.message}",
            modifier = Modifier.padding(end = 12.dp).size(20.dp),
            tint = MaterialTheme.colorScheme.error,
        )
    }
}

package com.haertibraeu.hopledger.ui.navigation

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sync
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.haertibraeu.hopledger.data.repository.SyncStatus
import com.haertibraeu.hopledger.ui.AppViewModel
import com.haertibraeu.hopledger.ui.accounting.AccountingScreen
import com.haertibraeu.hopledger.ui.accounting.AccountingViewModel
import com.haertibraeu.hopledger.ui.inventory.InventoryScreen
import com.haertibraeu.hopledger.ui.inventory.InventoryViewModel
import com.haertibraeu.hopledger.ui.settings.SettingsScreen
import com.haertibraeu.hopledger.ui.settings.SettingsViewModel

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

    // Hoist ViewModels so we can call refresh() from the sync indicator tap
    val inventoryViewModel: InventoryViewModel = hiltViewModel()
    val accountingViewModel: AccountingViewModel = hiltViewModel()
    val settingsViewModel: SettingsViewModel = hiltViewModel()

    val currentScreen = Screen.entries.firstOrNull {
        currentDestination?.hierarchy?.any { d -> d.route == it.route } == true
    }
    val onRefresh: () -> Unit = {
        when (currentScreen) {
            Screen.Inventory -> inventoryViewModel.refresh()
            Screen.Accounting -> accountingViewModel.refresh()
            Screen.Settings -> settingsViewModel.refreshAll()
            null -> {}
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("🍺 HopLedger") },
                actions = { SyncIndicator(syncStatus, onRefresh) },
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
            composable(Screen.Inventory.route) { InventoryScreen(inventoryViewModel) }
            composable(Screen.Accounting.route) { AccountingScreen(accountingViewModel) }
            composable(Screen.Settings.route) { SettingsScreen(settingsViewModel) }
        }
    }
}

@Composable
private fun SyncIndicator(status: SyncStatus, onRefresh: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "spin")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = -360f,
        animationSpec = infiniteRepeatable(tween(1000, easing = LinearEasing)),
        label = "rotation",
    )

    when (status) {
        is SyncStatus.Idle -> {}

        is SyncStatus.Syncing -> Icon(
            Icons.Default.Sync,
            contentDescription = "Synchronisiere…",
            modifier = Modifier.padding(end = 12.dp).size(20.dp).rotate(rotation),
            tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
        )

        is SyncStatus.Success -> IconButton(onClick = onRefresh) {
            Icon(
                Icons.Default.CloudDone,
                contentDescription = "Synchronisiert – tippen zum Aktualisieren",
                modifier = Modifier.size(20.dp),
                tint = Color(0xFF527455),
            )
        }

        is SyncStatus.Error -> IconButton(onClick = onRefresh) {
            Icon(
                Icons.Default.CloudOff,
                contentDescription = "Fehler – tippen zum Aktualisieren",
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.error,
            )
        }
    }
}

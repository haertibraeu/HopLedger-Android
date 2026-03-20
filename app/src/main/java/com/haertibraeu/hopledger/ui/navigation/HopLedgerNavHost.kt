package com.haertibraeu.hopledger.ui.navigation

import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.haertibraeu.hopledger.ui.inventory.InventoryScreen
import com.haertibraeu.hopledger.ui.accounting.AccountingScreen
import com.haertibraeu.hopledger.ui.settings.SettingsScreen

enum class Screen(val route: String, val label: String, val icon: ImageVector) {
    Inventory("inventory", "Inventar", Icons.Default.Inventory2),
    Accounting("accounting", "Abrechnung", Icons.Default.AccountBalance),
    Settings("settings", "Einstellungen", Icons.Default.Settings),
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HopLedgerNavHost() {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination

    Scaffold(
        topBar = {
            TopAppBar(title = { Text("🍺 HopLedger") })
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

package com.example.alris.lower_authority

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun HigherAuthorityBottomNavigationBar(navController: NavHostController) {
    // Only include permanent tabs like Settings, Reports, etc.
    val items = listOf(
        BottomNavItem("settings", "Settings", Icons.Default.Settings)
        // Add more main tabs here if needed, e.g., "reports"
    )

    NavigationBar {
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    // Navigate to the tab route
                    navController.navigate(item.route) {
                        // Avoid multiple copies in back stack
                        launchSingleTop = true
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}

data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)

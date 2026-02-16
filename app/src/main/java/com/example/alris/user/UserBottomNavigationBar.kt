package com.example.alris.user

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun UserBottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem("map", "Map", Icons.Default.Map),
        BottomNavItem("camera", "Report", Icons.Default.CameraAlt),
        BottomNavItem("my_reports", "My Reports", Icons.Default.ListAlt),
        BottomNavItem("notifications", "Alerts", Icons.Default.Notifications),
        BottomNavItem("settings", "Settings", Icons.Default.Settings)
    )

    val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route

    NavigationBar {
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = {
                    navController.navigate(item.route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}

data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)

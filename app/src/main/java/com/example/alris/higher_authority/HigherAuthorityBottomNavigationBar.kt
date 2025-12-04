package com.example.alris.higher_authority


import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState

@Composable
fun HigherAuthorityBottomNavigationBar(navController: NavHostController) {
    val items = listOf(
        BottomNavItem("map", "Map", Icons.Default.Map),
        BottomNavItem("department issues", "Department Issues", Icons.Default.List),
        BottomNavItem("settings", "Settings", Icons.Default.Settings)
    )

    NavigationBar {
        val currentRoute = navController.currentBackStackEntryAsState().value?.destination?.route
        items.forEach { item ->
            NavigationBarItem(
                selected = currentRoute == item.route,
                onClick = { navController.navigate(item.route) },
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) }
            )
        }
    }
}

data class BottomNavItem(val route: String, val label: String, val icon: ImageVector)

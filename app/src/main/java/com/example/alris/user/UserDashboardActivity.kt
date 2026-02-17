package com.example.alris.user

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.alris.SettingsScreen
import com.example.alris.ui.theme.AlrisTheme

class UserDashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AlrisTheme {
                val navController = rememberNavController()
                Scaffold(
                    bottomBar = {
                        UserBottomNavigationBar(navController)
                    }
                ) { padding ->
                    Box(modifier = Modifier.Companion.padding(padding)) {
                        NavHost(navController, startDestination = "map") {
                            composable("map") { MapScreen() }
                            composable("camera") { MultiPhotoCameraScreen() }
                            composable("my_reports") { MyReportsScreen() }
                            composable("notifications") { NotificationsScreen() }
                            composable("profile") { UserProfileScreen(onBack = { navController.popBackStack() }) }
                            composable("settings") { SettingsScreen() }
                        }
                    }
                }
            }
        }
    }
}
package com.example.alris.lower_authority


import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.alris.SettingsScreen
import com.example.alris.data.ApiClient
import com.example.alris.data.UpdateAuthorityProfileRequest
import com.example.alris.ui.theme.AlrisTheme
import com.example.alris.user.MapScreen
import com.example.alris.authority.AuthorityMapScreen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LowerAuthorityDashboardActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AlrisTheme {
                val navController = rememberNavController()
                Scaffold(
                    bottomBar = {
                        LowerAuthorityBottomNavigationBar(navController)
                    }
                ) { padding ->
                    Box(modifier = Modifier.padding(padding)) {
                        val initialSetup = intent.getBooleanExtra("initialSetup", false)
                        val startDest = if (initialSetup) "profile" else "map"

                        NavHost(navController = navController, startDestination = startDest) {
                            composable("map") { 
                                val context = LocalContext.current
                                AuthorityMapScreen() 
                            }
                            composable("issues") {
                                LowerAuthorityIssuesListScreen(
                                    onIssueClick = { issueId ->
                                        navController.navigate("issue_details/$issueId")
                                    }
                                )
                            }
                            composable("issue_details/{issueId}") { backStackEntry ->
                                val issueId = backStackEntry.arguments?.getString("issueId") ?: ""
                                IssueDetailsScreen(
                                    issueId = issueId,
                                    onBack = { navController.popBackStack() },
                                    isLowerAuthority = true
                                )
                            }
                            composable("settings") { SettingsScreen() }
                            composable("profile") {
                                LowerAuthorityProfileScreen()

                            }
                        }
                    }
                }
            }
        }
    }
}


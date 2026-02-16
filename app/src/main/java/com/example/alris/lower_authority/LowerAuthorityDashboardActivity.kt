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
                            composable("map") { MapScreen() }
                            composable("settings") { SettingsScreen() }
                            composable("profile") {
                                val context = LocalContext.current
                                val api = remember { ApiClient.createUserApi(context) }
                                var loading by remember { mutableStateOf(false) }
                                val nav = navController

                                AuthorityProfileSetupScreen(
                                    isLoading = loading,
                                    onSubmit = { name, phone, lat, lon, pass ->
                                        loading = true

                                        CoroutineScope(Dispatchers.IO).launch {
                                            try {
                                                val body = UpdateAuthorityProfileRequest(
                                                    name,
                                                    phone,
                                                    lat,
                                                    lon,
                                                    pass
                                                )
                                                val response = api.updateAuthorityProfile(body)

                                                withContext(Dispatchers.Main) {
                                                    loading = false
                                                    if (response.isSuccessful && response.body()?.success == true) {
                                                        nav.navigate("map") {
                                                            popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                                            launchSingleTop = true
                                                        }
                                                    } else {
                                                        Log.e("PROFILE_UPDATE", "Failed: ${response.body()?.error ?: response.message()}")
                                                    }
                                                }
                                            } catch (e: Exception) {
                                                Log.e("PROFILE_UPDATE", "Failed: ${e.message}")
                                                withContext(Dispatchers.Main) { loading = false }
                                            }
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}


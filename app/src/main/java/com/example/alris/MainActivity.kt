package com.example.alris

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.alris.ui.screens.LoginScreen
import com.example.alris.ui.screens.RegisterScreen
import com.example.alris.ui.theme.AlrisTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AlrisTheme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "login") {
                    composable("login") {
                        LoginScreen(
                            onLoginResult = { 
                                // LoginScreen handles activity navigation
                                finish() 
                            },
                            onNavigateToRegister = {
                                navController.navigate("register")
                            }
                        )
                    }
                    composable("register") {
                        RegisterScreen(
                            onRegisterSuccess = {
                                Toast.makeText(applicationContext, "Registration successful!", Toast.LENGTH_LONG).show()
                                navController.popBackStack()
                            },
                            onNavigateToLogin = {
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}

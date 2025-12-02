package com.example.alris.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.alris.data.LoginRequest
import com.example.alris.data.TokenManager
import com.example.alris.user.DashboardActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.jvm.java

@Composable
fun LoginScreen(onLoginResult: (String) -> Unit) {
    val context = LocalContext.current

    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("user") }
    var isLoading by remember { mutableStateOf(false) }

    Column(modifier = Modifier.padding(16.dp)) {
        Text("Select Role", style = MaterialTheme.typography.titleMedium)

        Row {
            listOf("user", "authority", "higher_authority").forEach { r ->
                Button(
                    onClick = { role = r },
                    modifier = Modifier.padding(4.dp)
                ) {
                    Text(r)
                }
            }
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        )

        Button(
            onClick = {
                isLoading = true
                val request = LoginRequest(email, password)
                val tokenManager = TokenManager(context)
                val api = ApiClient.createUserApi(context)

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val response = api.loginUser(request)
                        if (response.isSuccessful) {
                            val loginResponse = response.body()

                            loginResponse?.accessToken?.let { token ->
                                tokenManager.saveAccessToken(token)
                                println("Access Token Saved: $token")
                            }
                            loginResponse?.refreshToken?.let { token ->
                                tokenManager.saveRefreshToken(token)
                                println("Refresh Token Saved: $token")
                            }

                            withContext(Dispatchers.Main) {
                                Toast.makeText(context, "Login successful!", Toast.LENGTH_LONG).show()
                                onLoginResult(loginResponse?.accessToken ?: "")
                                val intent = Intent(context, DashboardActivity::class.java)
                                context.startActivity(intent)
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                val errorMsg = response.errorBody()?.string() ?: "Unknown error"
                                Toast.makeText(context, "Error: $errorMsg", Toast.LENGTH_LONG).show()
                                println("Login error response: $errorMsg")
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Exception: ${e.message}", Toast.LENGTH_LONG).show()
                            println("Login exception: ${e.message}")
                        }
                    } finally {
                        withContext(Dispatchers.Main) { isLoading = false }
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            Text(if (isLoading) "Logging in..." else "Login")
        }

        Button(
            onClick = {
                val intent = Intent(context, RegisterActivity::class.java)
                context.startActivity(intent)
            },
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
        ) {
            Text("Register")
        }
    }
}

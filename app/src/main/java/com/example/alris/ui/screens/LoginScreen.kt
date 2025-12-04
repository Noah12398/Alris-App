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
// ⭐ ADD THESE IMPORTS
import com.example.alris.user.UserDashboardActivity
import com.example.alris.authority.AuthorityDashboardActivity
import com.example.alris.higher_authority.HigherAuthorityDashboardActivity
// ⭐
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@Composable
fun LoginScreen(onLoginResult: (String) -> Unit) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var role by remember { mutableStateOf("user") }
    var isLoading by remember { mutableStateOf(false) }

    val api = ApiClient.createUserApi(context)
    val tokenManager = TokenManager(context)

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
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        )

        Button(
            onClick = {
                isLoading = true
                val request = LoginRequest(email, password)

                CoroutineScope(Dispatchers.IO).launch {
                    try {
                        val response = when (role) {
                            "user" -> api.loginUser(request)
                            "authority", "higher_authority" -> api.loginAuthority(request)
                            else -> api.loginUser(request)
                        }

                        if (response.isSuccessful) {
                            val loginResponse = response.body()

                            loginResponse?.accessToken?.let { tokenManager.saveAccessToken(it) }
                            loginResponse?.refreshToken?.let { tokenManager.saveRefreshToken(it) }

                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "${role.uppercase()} Login Successful!",
                                    Toast.LENGTH_LONG
                                ).show()

                                onLoginResult(loginResponse?.accessToken ?: "")

                                val nextScreen = when (role) {
                                    "user" -> UserDashboardActivity::class.java
                                    "authority" -> AuthorityDashboardActivity::class.java
                                    "higher_authority" -> HigherAuthorityDashboardActivity::class.java
                                    else -> UserDashboardActivity::class.java
                                }

                                context.startActivity(Intent(context, nextScreen))
                            }
                        } else {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(
                                    context,
                                    "Login failed: ${response.errorBody()?.string()}",
                                    Toast.LENGTH_LONG
                                ).show()
                            }
                        }
                    } catch (e: Exception) {
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                        }
                    } finally {
                        withContext(Dispatchers.Main) {
                            isLoading = false
                        }
                    }
                }
            },
            enabled = !isLoading,
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Text(if (isLoading) "Logging in..." else "Login")
        }

        // Show Register ONLY for "user" role
        if (role == "user") {
            Button(
                onClick = {
                    context.startActivity(Intent(context, RegisterActivity::class.java))
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
            ) {
                Text("Register")
            }
        }
    }
}
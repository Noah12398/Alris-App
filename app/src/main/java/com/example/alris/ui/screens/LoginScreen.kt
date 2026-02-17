package com.example.alris.ui.screens

import android.content.Intent
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alris.data.ApiClient
import com.example.alris.data.LoginRequest
import com.example.alris.data.TokenManager
import com.example.alris.user.UserDashboardActivity
import com.example.alris.higher_authority.HigherAuthorityDashboardActivity
import com.example.alris.lower_authority.LowerAuthorityDashboardActivity
import kotlinx.coroutines.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LoginScreen(
    onLoginResult: (String) -> Unit,
    onNavigateToRegister: () -> Unit
) {
    val context = LocalContext.current
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var role by remember { mutableStateOf("user") }
    var isLoading by remember { mutableStateOf(false) }

    val api = ApiClient.createUserApi(context)
    val tokenManager = TokenManager(context)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        MaterialTheme.colorScheme.primaryContainer,
                        MaterialTheme.colorScheme.background
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {

            // Logo / Branding
            Surface(
                modifier = Modifier.size(80.dp).padding(bottom = 16.dp),
                shape = RoundedCornerShape(20.dp),
                color = MaterialTheme.colorScheme.primary,
                shadowElevation = 8.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "A",
                        fontSize = 40.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }

            Text(
                text = "ALRIS",
                style = MaterialTheme.typography.displayMedium,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            
            Text(
                text = "Civic Intelligence Platform",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 4.dp, bottom = 48.dp)
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                shape = RoundedCornerShape(24.dp)
            ) {
                Column(Modifier.padding(24.dp)) {
                    Text(
                        text = "Sign In",
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 24.dp)
                    )

                    // Role Selection
                    SingleChoiceSegmentedButtonRow(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp)
                    ) {
                        val roles = listOf(
                            "user" to "User",
                            "authority" to "Auth",
                            "higher_authority" to "Admin"
                        )
                        roles.forEachIndexed { index, (value, label) ->
                            SegmentedButton(
                                selected = role == value,
                                onClick = { role = value },
                                shape = SegmentedButtonDefaults.itemShape(index = index, count = roles.size)
                            ) {
                                Text(label)
                            }
                        }
                    }

                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        leadingIcon = { Icon(Icons.Default.Email, contentDescription = null) },
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                    )

                    OutlinedTextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) },
                        trailingIcon = {
                            IconButton(onClick = { passwordVisible = !passwordVisible }) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                                    contentDescription = null
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.fillMaxWidth().padding(bottom = 32.dp)
                    )

                    Button(
                        enabled = !isLoading && email.isNotBlank() && password.isNotBlank(),
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

                                    if (!response.isSuccessful) {
                                        withContext(Dispatchers.Main) {
                                            Toast.makeText(context, "Login failed: ${response.message()}", Toast.LENGTH_LONG).show()
                                        }
                                        return@launch
                                    }

                                    val apiResponse = response.body()!!
                                    val loginData = apiResponse.data!!
                                    tokenManager.saveAccessToken(loginData.accessToken)
                                    tokenManager.saveRefreshToken(loginData.refreshToken)

                                    val backendUser = loginData.user
                                    val backendRole = backendUser?.role
                                    val isInitialized = backendUser?.isInitialized ?: true

                                    val mappedRole = when (backendRole) {
                                        "higher" -> "higher_authority"
                                        "authority" -> "authority"
                                        else -> "user"
                                    }
                                    tokenManager.saveUserRole(mappedRole)

                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Welcome back!", Toast.LENGTH_SHORT).show()
                                        onLoginResult(loginData.accessToken)

                                        when (mappedRole) {
                                            "authority" -> {
                                                if (!isInitialized) {
                                                    val intent = Intent(context, LowerAuthorityDashboardActivity::class.java)
                                                    intent.putExtra("initialSetup", true)
                                                    context.startActivity(intent)
                                                } else {
                                                    context.startActivity(Intent(context, LowerAuthorityDashboardActivity::class.java))
                                                }
                                            }
                                            "higher_authority" -> {
                                                context.startActivity(Intent(context, HigherAuthorityDashboardActivity::class.java))
                                            }
                                            else -> {
                                                context.startActivity(Intent(context, UserDashboardActivity::class.java))
                                            }
                                        }
                                    }

                                } catch (e: Exception) {
                                    withContext(Dispatchers.Main) {
                                        Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                    }
                                } finally {
                                    withContext(Dispatchers.Main) { isLoading = false }
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        if (isLoading)
                            CircularProgressIndicator(modifier = Modifier.size(24.dp), color = MaterialTheme.colorScheme.onPrimary)
                        else
                            Text("Sign In", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    if (role == "user") {
                        Spacer(Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Don't have an account?", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            TextButton(onClick = onNavigateToRegister) {
                                Text("Sign Up", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

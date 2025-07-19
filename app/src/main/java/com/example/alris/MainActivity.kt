package com.example.alris

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AdminPanelSettings
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.lifecycleScope
import com.example.alris.admin.AdminActivity
import com.example.alris.authority.AuthorityDashboardActivity
import com.example.alris.authority.PendingApprovalActivity
import com.example.alris.ui.theme.AlrisTheme
import com.example.alris.user.DashboardActivity
import io.github.jan.supabase.auth.Auth
import io.github.jan.supabase.createSupabaseClient
import kotlinx.coroutines.launch
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    private var loginRole: String = "user" // or any safe default

    // Add timeout to OkHttpClient
    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()

    val supabase = createSupabaseClient(
        supabaseUrl = Constants.SUPABASE_URL,
        supabaseKey = Constants.SUPABASE_ANON_KEY
    ) {
        install(Auth)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        Log.d(TAG, "MainActivity started")
        Log.d(TAG, "BASE_URL: ${Constants.BASE_URL}")
        Log.d(TAG, "SUPABASE_URL: ${Constants.SUPABASE_URL}")
        Log.d(TAG, "REDIRECT_URI: ${Constants.REDIRECT_URI}")

        handleDeepLink(intent) // 👈 Handle redirect back from Google

        setContent {
            AlrisTheme {
                GoogleSignInTabbedScreen(
                    onSignInClicked = { signInWithSupabase() },
                    onRoleSelected = { role ->
                        loginRole = role
                        Log.d(TAG, "Role selected: $role")
                    }
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        Log.d(TAG, "onNewIntent called")
        handleDeepLink(intent)
    }

    private fun handleDeepLink(intent: Intent?) {
        Log.d(TAG, "handleDeepLink called")
        val uri = intent?.data
        Log.d(TAG, "Deep link URI: $uri")

        if (uri == null) {
            Log.d(TAG, "No URI found in intent")
            return
        }

        val fragment = uri.fragment
        Log.d(TAG, "URI fragment: $fragment")

        if (fragment == null) {
            Log.d(TAG, "No fragment found in URI")
            return
        }

        val accessToken = fragment
            .split("&")
            .firstOrNull { it.startsWith("access_token=") }
            ?.substringAfter("=")

        Log.d(TAG, "Access token found: ${accessToken != null}")

        if (!accessToken.isNullOrBlank()) {
            Log.d(TAG, "Sending access token to server...")
            sendAccessTokenToServer(accessToken)
        } else {
            Log.e(TAG, "Access token not found in redirect.")
            showToast("Access token not found in redirect.")
        }
    }

    private fun signInWithSupabase() {
        Log.d(TAG, "signInWithSupabase called for role: $loginRole")
        val authUrl = "${Constants.SUPABASE_URL}/auth/v1/authorize?provider=google&redirect_to=${Constants.REDIRECT_URI}"
        Log.d(TAG, "Auth URL: $authUrl")

        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(authUrl))
        startActivity(intent)
    }

    private fun sendAccessTokenToServer(token: String) {
        Log.d(TAG, "Preparing to send token to server...")
        Log.d(TAG, "Server URL: ${Constants.BASE_URL}/verifyToken")
        Log.d(TAG, "Role: $loginRole")

        val json = """{"accessToken":"$token", "role":"$loginRole"}"""
        Log.d(TAG, "Request JSON: $json")

        val requestBody = json.toRequestBody("application/json".toMediaTypeOrNull())

        val request = Request.Builder()
            .url("${Constants.BASE_URL}/verifyToken")
            .post(requestBody)
            .addHeader("Content-Type", "application/json")
            .build()

        Log.d(TAG, "Making HTTP request...")

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "HTTP request failed", e)
                Log.e(TAG, "Error message: ${e.message}")
                Log.e(TAG, "Error cause: ${e.cause}")

                runOnUiThread {
                    showToast("Server connection failed: ${e.message}")
                }
            }

            override fun onResponse(call: Call, response: Response) {
                Log.d(TAG, "HTTP response received")
                Log.d(TAG, "Response code: ${response.code}")
                Log.d(TAG, "Response message: ${response.message}")

                runOnUiThread {
                    try {
                        val body = response.body?.string()
                        Log.d(TAG, "Response body: $body")

                        if (response.isSuccessful && body != null) {
                            val jsonResponse = JSONObject(body)
                            val status = jsonResponse.optString("status", "pending")
                            Log.d(TAG, "Status from server: $status")
                            navigateByRole(status)
                        } else {
                            val errorMsg = "Login failed: ${response.code} - ${response.message}"
                            Log.e(TAG, errorMsg)
                            Log.e(TAG, "Error response body: $body")
                            showToast(errorMsg)
                        }
                    } catch (e: Exception) {
                        Log.e(TAG, "Error parsing response", e)
                        showToast("Error parsing server response: ${e.message}")
                    }
                }
            }
        })
    }

    private fun navigateByRole(status: String) {
        Log.d(TAG, "Navigating by role: $loginRole, status: $status")

        when {
            loginRole == "user" -> {
                Log.d(TAG, "Navigating to DashboardActivity")
                startActivity(Intent(this, DashboardActivity::class.java))
                finish()
            }
            loginRole == "admin" && status == "approved" -> {
                Log.d(TAG, "Navigating to AdminActivity")
                startActivity(Intent(this, AdminActivity::class.java))
                finish()
            }
            loginRole == "admin" && status == "denied" -> {
                Log.w(TAG, "Admin access denied")
                showToast("Admin access denied.")
            }
            loginRole == "authority" && status == "pending" -> {
                Log.d(TAG, "Navigating to PendingApprovalActivity")
                startActivity(Intent(this, PendingApprovalActivity::class.java))
                finish()
            }
            loginRole == "authority" && status == "approved" -> {
                Log.d(TAG, "Navigating to AuthorityDashboardActivity")
                startActivity(Intent(this, AuthorityDashboardActivity::class.java))
                finish()
            }
            else -> {
                Log.w(TAG, "Access not granted for role: $loginRole, status: $status")
                showToast("Access not granted.")
            }
        }
    }

    private fun showToast(message: String) {
        Log.d(TAG, "Showing toast: $message")
        Toast.makeText(this, message, Toast.LENGTH_LONG).show()
    }
}

@Composable
fun GoogleSignInTabbedScreen(
    onSignInClicked: () -> Unit,
    onRoleSelected: (String) -> Unit
) {
    val tabs = listOf(
        TabData("User", Icons.Default.AccountCircle, "Access your dashboard"),
        TabData("Authority", Icons.Default.Security, "Authority portal access"),
        TabData("Admin", Icons.Default.AdminPanelSettings, "Administrative controls")
    )
    var selectedTabIndex by remember { mutableStateOf(0) }

    onRoleSelected(tabs[selectedTabIndex].title.lowercase())

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF667eea),
                        Color(0xFF764ba2),
                        Color(0xFF6B73FF)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            Text(
                text = "Welcome to Alris",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Choose your role to continue",
                fontSize = 16.sp,
                color = Color.White.copy(alpha = 0.8f),
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 8.dp)
            )

            Spacer(modifier = Modifier.height(48.dp))

            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(20.dp, RoundedCornerShape(24.dp)),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(
                    containerColor = Color.White.copy(alpha = 0.95f)
                )
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.Gray.copy(alpha = 0.1f))
                            .padding(4.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        tabs.forEachIndexed { index, tab ->
                            MainTabItem(
                                tab = tab,
                                isSelected = selectedTabIndex == index,
                                onClick = {
                                    selectedTabIndex = index
                                    onRoleSelected(tabs[index].title.lowercase())
                                },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(32.dp))

                    AnimatedContent(
                        targetState = selectedTabIndex,
                        transitionSpec = {
                            slideInHorizontally { it } + fadeIn() togetherWith
                                    slideOutHorizontally { -it } + fadeOut()
                        },
                        modifier = Modifier.fillMaxWidth()
                    ) { index ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = tabs[index].icon,
                                contentDescription = null,
                                tint = Color(0xFF6B73FF),
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = tabs[index].title,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2D3748)
                            )
                            Text(
                                text = tabs[index].description,
                                fontSize = 14.sp,
                                color = Color(0xFF718096),
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(40.dp))

                    Button(
                        onClick = onSignInClicked,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .shadow(8.dp, RoundedCornerShape(16.dp)),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF6B73FF)
                        )
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(24.dp)
                                    .background(Color.White, RoundedCornerShape(12.dp)),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "G",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF6B73FF)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Continue with Google",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "Secure authentication powered by Google",
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun MainTabItem(
    tab: TabData,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val backgroundColor = if (isSelected) Color(0xFF6B73FF) else Color.Transparent
    val textColor = if (isSelected) Color.White else Color(0xFF718096)

    Card(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 4.dp else 0.dp
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = tab.icon,
                contentDescription = null,
                tint = textColor,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = tab.title,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                color = textColor,
                textAlign = TextAlign.Center
            )
        }
    }
}

data class TabData(
    val title: String,
    val icon: ImageVector,
    val description: String
)
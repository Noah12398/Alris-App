package com.example.alris.authority

import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.alris.data.ApiClient
import com.example.alris.data.AuthorityProfile
import com.example.alris.data.UpdateAuthorityProfileRequest
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AuthorityProfileScreen(navController: NavController) {
    val context = LocalContext.current
    val api = remember { ApiClient.createUserApi(context) }
    val scope = rememberCoroutineScope()

    var profile by remember { mutableStateOf<AuthorityProfile?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    // Edit State
    var isEditing by remember { mutableStateOf(false) }
    var editName by remember { mutableStateOf("") }
    var editPhone by remember { mutableStateOf("") }
    var editLat by remember { mutableStateOf("") }
    var editLon by remember { mutableStateOf("") }
    var editPassword by remember { mutableStateOf("") }

    LaunchedEffect(Unit) {
        try {
            val response = api.getAuthorityProfile()
            if (response.isSuccessful && response.body()?.success == true) {
                profile = response.body()?.data
                profile?.let {
                    editName = it.name ?: ""
                    editPhone = it.phone ?: ""
                    editLat = it.latitude?.toString() ?: ""
                    editLon = it.longitude?.toString() ?: ""
                }
            } else {
                errorMsg = response.body()?.error ?: "Failed to load profile"
            }
        } catch (e: Exception) {
            errorMsg = "Error: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile") },
                actions = {
                    if (profile != null && !isLoading) {
                        IconButton(onClick = { isEditing = !isEditing }) {
                            Icon(if (isEditing) Icons.Default.Close else Icons.Default.Edit, contentDescription = "Edit")
                        }
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            } else if (errorMsg != null) {
                Text(
                    text = errorMsg!!,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                profile?.let { data ->
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Header Card
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = data.name ?: "Authority",
                                    style = MaterialTheme.typography.headlineMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = data.role?.uppercase() ?: "AUTHORITY",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(text = data.email, style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = "Dept: ${data.department}",
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }

                        if (isEditing) {
                            // Edit Form
                            Text("Update Details", style = MaterialTheme.typography.titleMedium)
                            
                            OutlinedTextField(
                                value = editName,
                                onValueChange = { editName = it },
                                label = { Text("Name") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true
                            )
                            OutlinedTextField(
                                value = editPhone,
                                onValueChange = { editPhone = it },
                                label = { Text("Phone") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
                            )
                            
                            if (data.role == "authority") {
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    OutlinedTextField(
                                        value = editLat,
                                        onValueChange = { editLat = it },
                                        label = { Text("Latitude") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                    OutlinedTextField(
                                        value = editLon,
                                        onValueChange = { editLon = it },
                                        label = { Text("Longitude") },
                                        modifier = Modifier.weight(1f),
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                                    )
                                }
                            }

                            Divider()
                            Text("Change Password (Optional)", style = MaterialTheme.typography.labelLarge)
                            OutlinedTextField(
                                value = editPassword,
                                onValueChange = { editPassword = it },
                                label = { Text("New Password") },
                                modifier = Modifier.fillMaxWidth(),
                                singleLine = true,
                                visualTransformation = PasswordVisualTransformation(),
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password)
                            )

                            Button(
                                onClick = {
                                    scope.launch {
                                        isLoading = true
                                        try {
                                            val lat = editLat.toDoubleOrNull()
                                            val lon = editLon.toDoubleOrNull()
                                            
                                            val req = UpdateAuthorityProfileRequest(
                                                name = editName.ifBlank { null },
                                                phone = editPhone.ifBlank { null },
                                                latitude = lat,
                                                longitude = lon,
                                                newPassword = editPassword.ifBlank { null }
                                            )
                                            
                                            val res = api.updateAuthorityProfile(req)
                                            if (res.isSuccessful && res.body()?.success == true) {
                                                Toast.makeText(context, "Profile Updated", Toast.LENGTH_SHORT).show()
                                                // Refresh profile data locally
                                                val updated = res.body()?.data
                                                profile = profile?.copy(
                                                    name = updated?.name ?: profile?.name,
                                                    phone = editPhone, // Response might not have phone? Check model.
                                                    latitude = updated?.latitude ?: profile?.latitude,
                                                    longitude = updated?.longitude ?: profile?.longitude
                                                )
                                                isEditing = false
                                                editPassword = ""
                                            } else {
                                                Toast.makeText(context, res.body()?.error ?: "Update failed", Toast.LENGTH_LONG).show()
                                            }
                                        } catch (e: Exception) {
                                            Toast.makeText(context, "Error: ${e.message}", Toast.LENGTH_LONG).show()
                                        } finally {
                                            isLoading = false
                                        }
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Save Changes")
                            }

                        } else {
                            // View Details
                            ProfileDetailItem(Icons.Default.Phone, "Phone", data.phone ?: "Not set")
                            if (data.role == "authority") {
                                ProfileDetailItem(Icons.Default.LocationOn, "Location", "${data.latitude ?: 0.0}, ${data.longitude ?: 0.0}")
                            }
                            ProfileDetailItem(Icons.Default.CalendarToday, "Joined", data.createdAt?.substringBefore("T") ?: "Unknown")
                            
                            Spacer(modifier = Modifier.weight(1f))
                            
                            OutlinedButton(
                                onClick = {
                                    scope.launch {
                                        // Logout logic if needed, or just navigate back
                                        try {
                                            api.logout(emptyMap()) // Optional
                                        } catch(_:Exception){}
                                        // Clear token/prefs logic here usually
                                        // For now just finish/navigate
                                        Toast.makeText(context, "Logged out", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error)
                            ) {
                                Icon(Icons.Default.ExitToApp, contentDescription = null)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("Logout")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileDetailItem(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(text = value, style = MaterialTheme.typography.bodyLarge)
        }
    }
}

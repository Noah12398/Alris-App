package com.example.alris.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.alris.data.ApiClient
import com.example.alris.data.UserApi
import com.example.alris.data.UserProfile
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserProfileScreen(onBack: () -> Unit) {
    val context = LocalContext.current
    val api = remember { ApiClient.createUserApi(context) }
    val viewModel: UserProfileViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return UserProfileViewModel(api) as T
        }
    })

    val profileState = viewModel.profileState

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Profile") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues).fillMaxSize()) {
            when {
                profileState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                profileState.error != null -> {
                    Text(
                        text = "Error: ${profileState.error}",
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                profileState.profile != null -> {
                    ProfileContent(profileState.profile!!)
                }
            }
        }
    }
}

@Composable
fun ProfileContent(profile: UserProfile) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Avatar
        Box(
            modifier = Modifier
                .size(100.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primaryContainer),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = null,
                modifier = Modifier.size(60.dp),
                tint = MaterialTheme.colorScheme.onPrimaryContainer
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = profile.name,
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )
        
        Text(
            text = profile.email,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Stats Cards
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            InfoCard(
                label = "Trust Score",
                value = String.format("%.1f", profile.trustScore ?: 0.0),
                modifier = Modifier.weight(1f)
            )
            InfoCard(
                label = "Reports",
                value = (profile.totalReports ?: 0).toString(),
                modifier = Modifier.weight(1f)
            )
            InfoCard(
                label = "Upvotes",
                value = (profile.totalUpvotes ?: 0).toString(),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Details Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                DetailRow("Phone", profile.phone ?: "N/A")
                DetailRow("Member Since", profile.createdAt?.take(10) ?: "N/A")
                DetailRow("Flagged ID", if (profile.isFlagged == true) "Yes" else "No")
            }
        }
    }
}

@Composable
fun InfoCard(label: String, value: String, modifier: Modifier = Modifier) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(value, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.bodySmall)
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontWeight = FontWeight.SemiBold)
    }
}

class UserProfileViewModel(private val api: UserApi) : ViewModel() {
    var profileState by mutableStateOf(ProfileState())
        private set

    init {
        fetchProfile()
    }

    private fun fetchProfile() {
        viewModelScope.launch {
            profileState = profileState.copy(isLoading = true)
            try {
                val response = api.getUserProfile()
                if (response.isSuccessful && response.body()?.success == true) {
                    profileState = profileState.copy(
                        isLoading = false,
                        profile = response.body()?.data,
                        error = null
                    )
                } else {
                    profileState = profileState.copy(
                        isLoading = false,
                        error = "Failed to load profile: ${response.message()}"
                    )
                }
            } catch (e: Exception) {
                profileState = profileState.copy(
                    isLoading = false,
                    error = e.localizedMessage ?: "Unknown error"
                )
            }
        }
    }
}

data class ProfileState(
    val isLoading: Boolean = false,
    val profile: UserProfile? = null,
    val error: String? = null
)

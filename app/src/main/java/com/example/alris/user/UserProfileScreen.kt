package com.example.alris.user

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.alris.data.ApiClient
import com.example.alris.data.UserApi
import com.example.alris.data.UserProfile
import com.example.alris.data.NotificationItem
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.alris.ui.components.ThemeSelector

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
    val notifications = viewModel.notifications
    val notificationsLoading = viewModel.notificationsLoading
    val scope = rememberCoroutineScope()

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
        Box(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.1f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            when {
                profileState.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                profileState.error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = "Error: ${profileState.error}",
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
                profileState.profile != null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Profile Section
                        item {
                            ProfileContent(profileState.profile!!)
                        }
                        
                        // Alerts/Notifications Section
                        item {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .shadow(4.dp, RoundedCornerShape(16.dp)),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(
                                                Icons.Default.Notifications,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(24.dp)
                                            )
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Text(
                                                text = "Alerts",
                                                style = MaterialTheme.typography.titleMedium,
                                                fontWeight = FontWeight.Bold,
                                                color = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                        
                                        val unreadCount = notifications.count { !it.isRead }
                                        if (unreadCount > 0) {
                                            TextButton(
                                                onClick = {
                                                    scope.launch {
                                                        try {
                                                            val res = api.markAllAsRead()
                                                            if (res.isSuccessful) {
                                                                viewModel.markAllNotificationsRead()
                                                            }
                                                        } catch (_: Exception) { }
                                                    }
                                                }
                                            ) {
                                                Text("Mark All Read", style = MaterialTheme.typography.labelSmall)
                                            }
                                        }
                                    }
                                    
                                    val unreadCount = notifications.count { !it.isRead }
                                    Text(
                                        text = if (unreadCount > 0) "$unreadCount unread" else "All caught up",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(start = 36.dp, top = 4.dp, bottom = 12.dp)
                                    )
                                    
                                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                                }
                                
                                when {
                                    notificationsLoading -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            CircularProgressIndicator(modifier = Modifier.size(32.dp))
                                        }
                                    }
                                    notifications.isEmpty() -> {
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(32.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    Icons.Default.NotificationsNone,
                                                    contentDescription = null,
                                                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                                                    modifier = Modifier.size(48.dp)
                                                )
                                                Spacer(modifier = Modifier.height(12.dp))
                                                Text(
                                                    text = "No alerts yet",
                                                    style = MaterialTheme.typography.bodyMedium,
                                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                    else -> {
                                        Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
                                            notifications.take(5).forEach { notification ->
                                                NotificationCard(
                                                    notification = notification,
                                                    onMarkAsRead = {
                                                        scope.launch {
                                                            try {
                                                                val res = api.markAsRead(notification.id)
                                                                if (res.isSuccessful) {
                                                                    viewModel.markNotificationRead(notification.id)
                                                                }
                                                            } catch (e: Exception) {
                                                                // Handle error
                                                            }
                                                        }
                                                    }
                                                )
                                                if (notification != notifications.take(5).last()) {
                                                    Spacer(modifier = Modifier.height(8.dp))
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                        
                        // Appearance Section
                        item {
                            ThemeSelector()
                        }
                        
                        // Bottom Spacer
                        item {
                            Spacer(modifier = Modifier.height(32.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileContent(profile: UserProfile) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Avatar
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Person,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = profile.name,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = profile.email,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(16.dp))

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

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(12.dp))

            // Details
            DetailRow("Phone", profile.phone ?: "N/A")
            DetailRow("Member Since", profile.createdAt?.take(10) ?: "N/A")
            DetailRow("Flagged", if (profile.isFlagged == true) "Yes" else "No")
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
            Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
        Text(value, fontWeight = FontWeight.Medium, fontSize = 14.sp)
    }
}

class UserProfileViewModel(private val api: UserApi) : ViewModel() {
    var profileState by mutableStateOf(ProfileState())
        private set
    
    var notifications by mutableStateOf<List<NotificationItem>>(emptyList())
        private set
    var notificationsLoading by mutableStateOf(true)
        private set

    init {
        fetchProfile()
        fetchNotifications()
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
    
    private fun fetchNotifications() {
        viewModelScope.launch {
            try {
                val response = api.getMyNotifications()
                if (response.isSuccessful && response.body()?.success == true) {
                    notifications = response.body()?.data?.notifications ?: emptyList()
                }
            } catch (e: Exception) {
                // Handle error silently
            } finally {
                notificationsLoading = false
            }
        }
    }
    
    fun markNotificationRead(id: String) {
        notifications = notifications.map {
            if (it.id == id) it.copy(isRead = true) else it
        }
    }
    
    fun markAllNotificationsRead() {
        notifications = notifications.map { it.copy(isRead = true) }
    }
}

data class ProfileState(
    val isLoading: Boolean = false,
    val profile: UserProfile? = null,
    val error: String? = null
)

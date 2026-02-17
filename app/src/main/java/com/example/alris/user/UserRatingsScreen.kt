package com.example.alris.user

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.alris.data.ApiClient
import com.example.alris.data.UserApi
import com.example.alris.data.UserRating
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserRatingsScreen(
    userId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val api = remember { ApiClient.createUserApi(context) }
    val viewModel: UserRatingsViewModel = viewModel(factory = object : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return UserRatingsViewModel(api, userId) as T
        }
    })

    val state = viewModel.state

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("My Ratings") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.error != null -> {
                    Text(
                        text = state.error,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                state.ratings.isEmpty() -> {
                    Text(
                        text = "No ratings found",
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                else -> {
                    LazyColumn(
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                             // Header Summary
                             Card(
                                 modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                                 colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
                             ) {
                                 Column(
                                     modifier = Modifier.padding(16.dp),
                                     horizontalAlignment = Alignment.CenterHorizontally
                                 ) {
                                     Text(
                                         text = String.format("%.1f", state.averageRating),
                                         style = MaterialTheme.typography.displayMedium,
                                         fontWeight = FontWeight.Bold
                                     )
                                     Row(verticalAlignment = Alignment.CenterVertically) {
                                        repeat(5) { index ->
                                            Icon(
                                                imageVector = Icons.Default.Star,
                                                contentDescription = null,
                                                tint = if (index < (state.averageRating + 0.5).toInt()) Color(0xFFFFC107) else Color.Gray.copy(alpha = 0.5f),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                     }
                                     Spacer(modifier = Modifier.height(4.dp))
                                     Text(
                                         text = "Based on ${state.totalRatings} reviews",
                                         style = MaterialTheme.typography.bodyMedium
                                     )
                                 }
                             }
                        }
                        
                        items(state.ratings) { rating ->
                            RatingItemCard(rating)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun RatingItemCard(rating: UserRating) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Stars
                Row {
                    repeat(5) { index ->
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = null,
                            tint = if (index < rating.rating) Color(0xFFFFC107) else Color.Gray.copy(alpha = 0.3f),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Text(
                    text = try { rating.createdAt?.substringBefore("T") ?: "" } catch(e:Exception){""},
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            if (!rating.comment.isNullOrBlank()) {
                Text(
                    text = rating.comment,
                    style = MaterialTheme.typography.bodyMedium
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            
            Text(
                text = "Rated by ${rating.ratedByRole.replace("_", " ").uppercase()}",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

class UserRatingsViewModel(private val api: UserApi, private val userId: String) : ViewModel() {
    var state by mutableStateOf(RatingsState())
        private set

    init {
        loadRatings()
    }

    private fun loadRatings() {
        viewModelScope.launch {
            state = state.copy(isLoading = true)
            try {
                val response = api.getUserRatings(userId)
                if (response.isSuccessful && response.body()?.success == true) {
                    val data = response.body()?.data
                    val list = data?.ratings ?: emptyList()
                    val total = data?.totalRatings ?: list.size
                    // Calculate average if not provided
                    val avg = if (list.isNotEmpty()) list.map { it.rating }.average() else 0.0
                    
                    state = state.copy(
                        isLoading = false,
                        ratings = list,
                        totalRatings = total,
                        averageRating = avg,
                        error = null
                    )
                } else {
                    state = state.copy(isLoading = false, error = response.body()?.error ?: "Failed to load ratings")
                }
            } catch (e: Exception) {
                state = state.copy(isLoading = false, error = e.message ?: "Unknown error")
            }
        }
    }
}

data class RatingsState(
    val isLoading: Boolean = false,
    val ratings: List<UserRating> = emptyList(),
    val totalRatings: Int = 0,
    val averageRating: Double = 0.0,
    val error: String? = null
)

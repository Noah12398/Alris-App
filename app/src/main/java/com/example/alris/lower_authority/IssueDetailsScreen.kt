package com.example.alris.lower_authority

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.alris.data.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IssueDetailsScreen(
    issueId: String,
    onBack: () -> Unit,
    isLowerAuthority: Boolean = true
) {
    val context = LocalContext.current
    val api = remember { ApiClient.createUserApi(context) }
    
    val viewModel: IssueDetailsViewModel = viewModel(
        key = issueId,
        factory = object : ViewModelProvider.Factory {
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                @Suppress("UNCHECKED_CAST")
                return IssueDetailsViewModel(api, issueId) as T
            }
        }
    )
    
    val issue = viewModel.issue
    val isLoading = viewModel.isLoading
    val errorMsg = viewModel.errorMsg
    
    LaunchedEffect(issueId) {
        viewModel.loadIssueDetails()
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Issue Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.15f),
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            when {
                isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = MaterialTheme.colorScheme.primary,
                                strokeWidth = 4.dp
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                "Loading details...",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                errorMsg != null -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Card(
                            modifier = Modifier.padding(32.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.errorContainer
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(
                                    Icons.Default.ErrorOutline,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(48.dp)
                                )
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = errorMsg,
                                    color = MaterialTheme.colorScheme.onErrorContainer,
                                    style = MaterialTheme.typography.bodyMedium
                                )
                            }
                        }
                    }
                }
                issue != null -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Main Issue Info
                        item {
                            IssueInfoCard(issue = issue)
                        }
                        
                        // Reports Section
                        if (issue.reports.isNotEmpty()) {
                            item {
                                Text(
                                    "Reports (${issue.reports.size})",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    modifier = Modifier.padding(vertical = 8.dp)
                                )
                            }
                            
                            items(issue.reports) { report ->
                                ReportCard(report = report)
                            }
                        }
                        
                        // Status Update Section (for authorities)
                        if (isLowerAuthority) {
                            item {
                                StatusUpdateCard(
                                    currentStatus = issue.status,
                                    onStatusUpdate = { newStatus ->
                                        viewModel.updateStatus(newStatus)
                                    }
                                )
                            }
                        }
                        
                        item { Spacer(modifier = Modifier.height(16.dp)) }
                    }
                }
            }
        }
    }
}

@Composable
private fun IssueInfoCard(issue: IssueDetail) {
    val statusColor = when (issue.status.lowercase()) {
        "submitted" -> Color(0xFFFF9800)
        "in_progress" -> Color(0xFF2196F3)
        "resolved" -> Color(0xFF4CAF50)
        "rejected" -> Color(0xFFF44336)
        else -> Color(0xFF9E9E9E)
    }

    val statusLabel = when (issue.status.lowercase()) {
        "submitted" -> "Pending"
        "in_progress" -> "In Progress"
        "resolved" -> "Resolved"
        "rejected" -> "Rejected"
        else -> issue.status
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            // Category
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Category,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = issue.category ?: "Other",
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Status Badge
            Box(
                modifier = Modifier
                    .background(statusColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 12.dp, vertical = 6.dp)
            ) {
                Text(
                    text = statusLabel,
                    color = statusColor,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
            Spacer(modifier = Modifier.height(16.dp))
            
            // Description
            if (!issue.description.isNullOrBlank()) {
                InfoRow(icon = Icons.Default.Description, label = "Description", value = issue.description)
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Location
            if (issue.latitude != null && issue.longitude != null) {
                InfoRow(
                    icon = Icons.Default.LocationOn,
                    label = "Location",
                    value = "${issue.latitude}, ${issue.longitude}"
                )
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Department
            issue.department?.let {
                InfoRow(icon = Icons.Default.Business, label = "Department", value = it)
                Spacer(modifier = Modifier.height(12.dp))
            }
            
            // Stats
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatChip(
                    icon = Icons.Default.ThumbUp,
                    label = "${issue.upvote_count ?: 0} Upvotes",
                    color = MaterialTheme.colorScheme.secondary
                )
                StatChip(
                    icon = Icons.Default.Report,
                    label = "${issue.report_count ?: 0} Reports",
                    color = MaterialTheme.colorScheme.tertiary
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Timestamps
            issue.created_at?.let {
                Text(
                    "Created: $it",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            issue.resolved_at?.let {
                Text(
                    "Resolved: $it",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun InfoRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.Top) {
        Icon(
            icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Column {
            Text(
                text = label,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun StatChip(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, color: Color) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = label,
                fontSize = 12.sp,
                color = color,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun ReportCard(report: ReportSummary) {
    val context = LocalContext.current
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(4.dp, RoundedCornerShape(12.dp)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = report.description ?: "No description",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface
            )
            
            report.created_at?.let {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Reported: $it",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            if (report.uploads.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "Attachments (${report.uploads.size})",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                
                report.uploads.take(3).forEach { upload ->
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(upload.url)
                            .crossfade(true)
                            .build(),
                        contentDescription = "Report Image",
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(180.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }
    }
}

@Composable
private fun StatusUpdateCard(currentStatus: String, onStatusUpdate: (String) -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(6.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text(
                "Update Status",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(16.dp))
            
            val statuses = listOf(
                "in_progress" to "In Progress" to Color(0xFF2196F3),
                "resolved" to "Resolved" to Color(0xFF4CAF50),
                "rejected" to "Rejected" to Color(0xFFF44336)
            )
            
            statuses.forEach { (statusPair, color) ->
                val (statusValue, statusLabel) = statusPair
                val isCurrentStatus = currentStatus.lowercase() == statusValue
                
                Button(
                    onClick = { if (!isCurrentStatus) onStatusUpdate(statusValue) },
                    enabled = !isCurrentStatus,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isCurrentStatus) color.copy(alpha = 0.3f) else color,
                        disabledContainerColor = color.copy(alpha = 0.3f)
                    )
                ) {
                    Icon(
                        when (statusValue) {
                            "in_progress" -> Icons.Default.PlayArrow
                            "resolved" -> Icons.Default.CheckCircle
                            "rejected" -> Icons.Default.Cancel
                            else -> Icons.Default.Circle
                        },
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isCurrentStatus) "$statusLabel (Current)" else statusLabel,
                        fontWeight = FontWeight.Medium
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
            }
        }
    }
}

class IssueDetailsViewModel(
    private val api: UserApi,
    private val issueId: String
) : ViewModel() {
    var issue by mutableStateOf<IssueDetail?>(null)
        private set
    var isLoading by mutableStateOf(true)
        private set
    var errorMsg by mutableStateOf<String?>(null)
        private set
    
    fun loadIssueDetails() {
        viewModelScope.launch {
            try {
                isLoading = true
                errorMsg = null
                
                val response = api.getIssueById(issueId)
                
                if (response.isSuccessful) {
                    issue = response.body()?.data?.issue
                } else {
                    errorMsg = "Error: ${response.code()} ${response.message()}"
                }
            } catch (e: Exception) {
                errorMsg = "Exception: ${e.message}"
            } finally {
                isLoading = false
            }
        }
    }
    
    fun updateStatus(newStatus: String) {
        viewModelScope.launch {
            try {
                val response = api.updateIssueStatus(StatusUpdate(issueId, newStatus))
                
                if (response.isSuccessful) {
                    // Reload to get updated data
                    loadIssueDetails()
                } else {
                    errorMsg = "Failed to update: ${response.code()}"
                }
            } catch (e: Exception) {
                errorMsg = "Failed to update: ${e.message}"
            }
        }
    }
}

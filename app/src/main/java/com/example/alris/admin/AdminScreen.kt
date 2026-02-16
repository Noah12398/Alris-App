package com.example.alris.admin
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.alris.Constants.logoutAndGoToLogin
import com.example.alris.data.*
import com.example.alris.ui.theme.AlrisTheme
import com.google.firebase.ktx.Firebase
import com.google.firebase.firestore.ktx.firestore

class AdminActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AlrisTheme {
                AdminScreen()
            }
        }
    }
}
@Composable
fun AdminScreen() {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(AdminTab.Requests) }

    Scaffold(
        bottomBar = {
            BottomNavigationBar(
                selectedTab = selectedTab,
                onTabSelected = { selectedTab = it }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { logoutAndGoToLogin(context) },
                containerColor = MaterialTheme.colorScheme.error,
                contentColor = MaterialTheme.colorScheme.onError
            ) {
                Icon(Icons.Default.ExitToApp, contentDescription = "Logout")
            }
        },
        floatingActionButtonPosition = FabPosition.End
    ) { innerPadding ->
        Box(modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)) {
            when (selectedTab) {
                AdminTab.Requests -> RequestsScreen()
                AdminTab.AuditLogs -> AuditLogsScreen()
                AdminTab.Analytics -> AnalyticsScreen()
            }
        }
    }
}

enum class AdminTab(val title: String) {
    Requests("Requests"),
    AuditLogs("Audit Logs"),
    Analytics("Analytics")
}
@Composable
fun BottomNavigationBar(
    selectedTab: AdminTab,
    onTabSelected: (AdminTab) -> Unit
) {
    val icons = mapOf(
        AdminTab.Requests to Icons.Default.Inbox,
        AdminTab.AuditLogs to Icons.Default.History,
        AdminTab.Analytics to Icons.Default.Analytics
    )
    NavigationBar {
        AdminTab.values().forEach { tab ->
            NavigationBarItem(
                selected = selectedTab == tab,
                onClick = { onTabSelected(tab) },
                label = { Text(tab.title, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                icon = { Icon(icons[tab] ?: Icons.Default.Info, contentDescription = tab.title) }
            )
        }
    }
}
@Composable
fun RequestsScreen() {
    val db = Firebase.firestore
    val pendingRequests = remember { mutableStateListOf<ApprovalRequest>() }

    LaunchedEffect(Unit) {
        db.collection("approval_requests")
            .whereEqualTo("status", "pending")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    val data = document.toObject(ApprovalRequest::class.java).copy(
                        requestId = document.id
                    )
                    pendingRequests.add(data)
                }
            }
    }

    LazyColumn {
        items(pendingRequests) { request ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Email: ${request.email}")
                Text("Role: ${request.role}")
                Spacer(modifier = Modifier.height(8.dp))

                Button(onClick = {
                    approveUser(request.uid, request.requestId)
                    pendingRequests.remove(request)
                }) {
                    Text("Approve")
                }

                Divider(modifier = Modifier.padding(vertical = 8.dp))
            }
        }
    }
}
@Composable
fun AuditLogsScreen() {
    val context = LocalContext.current
    val api = remember { ApiClient.createUserApi(context) }
    var logs by remember { mutableStateOf<List<AuditLogItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    var total by remember { mutableStateOf(0) }
    var hasMore by remember { mutableStateOf(false) }
    var offset by remember { mutableStateOf(0) }
    val limit = 50

    LaunchedEffect(Unit) {
        try {
            val response = api.getAuditLogs(limit = limit, offset = 0)
            if (response.isSuccessful && response.body()?.success == true) {
                val data = response.body()!!.data!!
                logs = data.logs
                total = data.total
                hasMore = data.hasMore
                offset = data.logs.size
            } else {
                errorMsg = "Failed to load audit logs: ${response.code()}"
            }
        } catch (e: Exception) {
            errorMsg = "Error: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        // Header
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
                .shadow(4.dp, RoundedCornerShape(16.dp)),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            shape = RoundedCornerShape(16.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.Default.History,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.width(16.dp))
                Column {
                    Text(
                        text = "Audit Logs",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = "$total total entries",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        when {
            isLoading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
            }
            errorMsg != null -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(errorMsg!!, color = MaterialTheme.colorScheme.error)
                }
            }
            logs.isEmpty() -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No audit logs found", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> {
                LazyColumn(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(logs, key = { it.id }) { log ->
                        AuditLogCard(log)
                    }
                    if (hasMore) {
                        item {
                            TextButton(
                                onClick = { /* Load more could be implemented */ },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("$total total logs")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun AuditLogCard(log: AuditLogItem) {
    val actionColor = when {
        log.action.contains("deleted") -> Color(0xFFF44336)
        log.action.contains("created") -> Color(0xFF4CAF50)
        log.action.contains("updated") -> Color(0xFF2196F3)
        log.action.contains("flagged") -> Color(0xFFFF9800)
        log.action.contains("rated") -> Color(0xFF9C27B0)
        log.action.contains("upvoted") || log.action.contains("downvoted") -> Color(0xFF00BCD4)
        else -> Color.Gray
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            // Color indicator
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(48.dp)
                    .background(actionColor, RoundedCornerShape(2.dp))
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = log.action.replace("_", " ").replaceFirstChar { it.uppercase() },
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = actionColor
                    )
                    Text(
                        text = log.actorRole.replaceFirstChar { it.uppercase() },
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${log.entityType}: ${log.entityId.take(8)}...",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = try { log.createdAt.substringBefore("T") } catch (e: Exception) { log.createdAt },
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun AnalyticsScreen() {
    val context = LocalContext.current
    val api = remember { ApiClient.createUserApi(context) }
    var stats by remember { mutableStateOf<AdminStatsResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val response = api.getAdminStats()
            if (response.isSuccessful && response.body()?.success == true) {
                stats = response.body()!!.data
            } else {
                errorMsg = "Failed to load stats: ${response.code()}"
            }
        } catch (e: Exception) {
            errorMsg = "Error: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    when {
        isLoading -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
            }
        }
        errorMsg != null -> {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(errorMsg!!, color = MaterialTheme.colorScheme.error)
            }
        }
        stats != null -> {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header card
                item {
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .shadow(8.dp, RoundedCornerShape(20.dp)),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    Brush.horizontalGradient(
                                        colors = listOf(
                                            MaterialTheme.colorScheme.primary,
                                            MaterialTheme.colorScheme.tertiary
                                        )
                                    )
                                )
                                .padding(24.dp)
                        ) {
                            Column {
                                Text(
                                    "Dashboard Statistics",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onPrimary
                                )
                                Text(
                                    "System-wide metrics and insights",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                                )
                            }
                        }
                    }
                }

                // Stats grid
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AdminStatCard(
                            modifier = Modifier.weight(1f),
                            title = "Users",
                            value = stats!!.totalUsers.toString(),
                            icon = Icons.Default.People,
                            color = Color(0xFF2196F3)
                        )
                        AdminStatCard(
                            modifier = Modifier.weight(1f),
                            title = "Reports",
                            value = stats!!.totalReports.toString(),
                            icon = Icons.Default.Description,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AdminStatCard(
                            modifier = Modifier.weight(1f),
                            title = "Issues",
                            value = stats!!.totalIssues.toString(),
                            icon = Icons.Default.Warning,
                            color = Color(0xFFFF9800)
                        )
                        AdminStatCard(
                            modifier = Modifier.weight(1f),
                            title = "Flagged",
                            value = stats!!.flaggedUsers.toString(),
                            icon = Icons.Default.Flag,
                            color = Color(0xFFF44336)
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AdminStatCard(
                            modifier = Modifier.weight(1f),
                            title = "Classified",
                            value = stats!!.classifiedReports.toString(),
                            icon = Icons.Default.CheckCircle,
                            color = Color(0xFF009688)
                        )
                        AdminStatCard(
                            modifier = Modifier.weight(1f),
                            title = "Unclassified",
                            value = stats!!.unclassifiedReports.toString(),
                            icon = Icons.Default.HelpOutline,
                            color = Color(0xFF795548)
                        )
                    }
                }
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        AdminStatCard(
                            modifier = Modifier.weight(1f),
                            title = "Fake Uploads",
                            value = stats!!.fakeUploads.toString(),
                            icon = Icons.Default.BrokenImage,
                            color = Color(0xFFE91E63)
                        )
                        AdminStatCard(
                            modifier = Modifier.weight(1f),
                            title = "Spam Uploads",
                            value = stats!!.spamUploads.toString(),
                            icon = Icons.Default.Report,
                            color = Color(0xFF9C27B0)
                        )
                    }
                }

                // Issue status breakdown
                stats!!.issuesByStatus?.let { statusMap ->
                    if (statusMap.isNotEmpty()) {
                        item {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                            ) {
                                Column(modifier = Modifier.padding(16.dp)) {
                                    Text(
                                        "Issues by Status",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        modifier = Modifier.padding(bottom = 12.dp)
                                    )
                                    statusMap.forEach { (status, count) ->
                                        val statusColor = when (status) {
                                            "submitted" -> Color(0xFFFF9800)
                                            "in_progress" -> Color(0xFF2196F3)
                                            "resolved" -> Color(0xFF4CAF50)
                                            "rejected" -> Color(0xFFF44336)
                                            else -> Color.Gray
                                        }
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 4.dp),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(10.dp)
                                                        .background(statusColor, RoundedCornerShape(2.dp))
                                                )
                                                Spacer(modifier = Modifier.width(8.dp))
                                                Text(
                                                    text = status.replace("_", " ").replaceFirstChar { it.uppercase() },
                                                    fontSize = 14.sp
                                                )
                                            }
                                            Text(
                                                text = count.toString(),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp,
                                                color = statusColor
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                item { Spacer(modifier = Modifier.height(16.dp)) }
            }
        }
    }
}

@Composable
private fun AdminStatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    color: Color
) {
    Card(
        modifier = modifier.shadow(4.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(28.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                fontWeight = FontWeight.Bold,
                fontSize = 24.sp,
                color = color
            )
            Text(
                text = title,
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}


data class ApprovalRequest(
    val uid: String = "",
    val email: String = "",
    val role: String = "",
    val status: String = "",
    val requestId: String = "" // <-- needed to update
)

fun approveUser(uid: String, requestId: String) {
    val db = Firebase.firestore
    db.collection("users").document(uid).update("status", "approved")
    db.collection("approval_requests").document(requestId).update("status", "approved")
}

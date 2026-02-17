package com.example.alris.higher_authority

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.alris.data.AdminStatsResponse
import com.example.alris.data.ApiClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminStatsScreen(navController: NavController) {
    val context = LocalContext.current
    val api = remember { ApiClient.createUserApi(context) }
    var stats by remember { mutableStateOf<AdminStatsResponse?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        try {
            val response = api.getAdminStats()
            if (response.isSuccessful && response.body()?.success == true) {
                stats = response.body()?.data
            } else {
                errorMsg = response.body()?.error ?: "Failed to load stats"
            }
        } catch (e: Exception) {
            errorMsg = "Error: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
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
            stats?.let { data ->
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Title
                    Text(
                        text = "System Overview",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )

                    // Quick Actions Row
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        QuickActionButton(
                            text = "Audit Logs",
                            icon = Icons.Default.History,
                            onClick = { navController.navigate("audit_logs") },
                            modifier = Modifier.weight(1f)
                        )
                        QuickActionButton(
                            text = "Register Auth",
                            icon = Icons.Default.PersonAdd,
                            onClick = { navController.navigate("register_authority") },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Main Stats Cards
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            title = "Users",
                            value = data.totalUsers.toString(),
                            icon = Icons.Default.Group,
                            color = Color(0xFF2196F3),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Reports",
                            value = data.totalReports.toString(),
                            icon = Icons.Default.Description,
                            color = Color(0xFF4CAF50),
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        StatCard(
                            title = "Issues",
                            value = data.totalIssues.toString(),
                            icon = Icons.Default.Warning,
                            color = Color(0xFFFF9800),
                            modifier = Modifier.weight(1f)
                        )
                        StatCard(
                            title = "Flagged",
                            value = data.flaggedUsers.toString(),
                            icon = Icons.Default.Flag,
                            color = Color(0xFFF44336),
                            modifier = Modifier.weight(1f)
                        )
                    }

                    // Additional Stats
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                "Security & Quality",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            StatRow("Fake Uploads", data.fakeUploads.toString(), Icons.Default.ImageNotSupported)
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            StatRow("Spam Uploads", data.spamUploads.toString(), Icons.Default.Block)
                            Divider(modifier = Modifier.padding(vertical = 8.dp))
                            StatRow(
                                "Classified Reports",
                                "${data.classifiedReports} / ${data.totalReports}",
                                Icons.Default.CheckCircle
                            )
                        }
                    }

                    // Issues by Status
                    data.issuesByStatus?.let { statusMap ->
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    "Issues by Status",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                statusMap.entries.forEach { (status, count) ->
                                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = status.replace("_", " ").uppercase(),
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                            Text(
                                                text = count.toString(),
                                                fontWeight = FontWeight.Bold,
                                                style = MaterialTheme.typography.bodyMedium
                                            )
                                        }
                                        Spacer(modifier = Modifier.height(4.dp))
                                        LinearProgressIndicator(
                                            progress = if (data.totalIssues > 0) count.toFloat() / data.totalIssues else 0f,
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(8.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = when (status) {
                                                "resolved" -> Color(0xFF4CAF50)
                                                "in_progress" -> Color(0xFF2196F3)
                                                "rejected" -> Color(0xFFF44336)
                                                else -> Color(0xFFFFC107)
                                            }
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun StatRow(
    label: String,
    value: String,
    icon: ImageVector
) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(text = label, modifier = Modifier.weight(1f))
        Text(text = value, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun QuickActionButton(
    text: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = modifier,
        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
        contentPadding = PaddingValues(vertical = 12.dp),
        shape = RoundedCornerShape(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSecondaryContainer,
            modifier = Modifier.size(20.dp)
        )
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = text,
            color = MaterialTheme.colorScheme.onSecondaryContainer,
            fontWeight = FontWeight.SemiBold
        )
    }
}

package com.example.alris.higher_authority

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.alris.data.Issue
import com.example.alris.data.StatusUpdate
import kotlinx.coroutines.launch
import retrofit2.HttpException

@Composable
fun DepartmentIssuesScreen() {
    val context = LocalContext.current
    val api = remember { ApiClient.createUserApi(context) }
    var issues by remember { mutableStateOf<List<Issue>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }
    var errorMsg by remember { mutableStateOf<String?>(null) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(Unit) {
        try {
            val response = api.getDepartmentIssues()
            if (response.isSuccessful) {
                issues = response.body()?.issues ?: emptyList()
            } else {
                errorMsg = "Error: ${response.code()} ${response.message()}"
            }
        } catch (e: Exception) {
            errorMsg = "Exception: ${e.message}"
        } finally {
            isLoading = false
        }
    }

    Scaffold { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {

            when {
                isLoading -> {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize(Alignment.Center)
                    )
                }
                errorMsg != null -> {
                    Text(
                        text = errorMsg ?: "",
                        modifier = Modifier
                            .fillMaxSize()
                            .wrapContentSize(Alignment.Center),
                        color = Color.Red
                    )
                }
                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(issues) { issue ->
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(8.dp),
                                elevation = CardDefaults.cardElevation(8.dp)
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Text(
                                        text = issue.category ?: "Other",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 18.sp,
                                        color = Color(0xFF1976D2)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "Status: ${issue.status}",
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "Location: ${issue.issue_latitude}, ${issue.issue_longitude}",
                                        fontSize = 12.sp,
                                        color = Color.Gray
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))

                                    if (issue.reports.isNotEmpty()) {
                                        Text("Reports:", fontWeight = FontWeight.Bold)
                                        Spacer(modifier = Modifier.height(4.dp))

                                        issue.reports.forEach { report ->
                                            Text("- ${report.description ?: "No description"}")
                                            report.uploads.forEach { upload ->
                                                Spacer(modifier = Modifier.height(4.dp))
                                                AsyncImage(
                                                    model = ImageRequest.Builder(context)
                                                        .data(upload.url)
                                                        .crossfade(true)
                                                        .build(),
                                                    contentDescription = "Report Image",
                                                    modifier = Modifier
                                                        .fillMaxWidth()
                                                        .height(200.dp)
                                                        .background(Color.LightGray)
                                                )
                                                Spacer(modifier = Modifier.height(8.dp))
                                            }
                                            Divider(color = Color.LightGray, thickness = 1.dp)
                                        }
                                    }

                                    // ===== Status Update Buttons =====
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text("Update Status:", fontWeight = FontWeight.Bold)
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(top = 4.dp),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        val statuses = listOf("ongoing", "resolved", "rejected")
                                        statuses.forEach { newStatus ->
                                            Button(
                                                onClick = {
                                                    coroutineScope.launch {
                                                        try {
                                                            val response = api.updateIssueStatus(
                                                                StatusUpdate(
                                                                    issueId = issue.issue_id,
                                                                    status = newStatus
                                                                )
                                                            )

                                                            if (response.isSuccessful) {
                                                                // Update local state
                                                                issues = issues.map {
                                                                    if (it.issue_id == issue.issue_id) {
                                                                        it.copy(status = newStatus)
                                                                    } else it
                                                                }
                                                            } else {
                                                                errorMsg = "Failed to update: ${response.code()}"
                                                            }
                                                        } catch (e: HttpException) {
                                                            errorMsg = "Exception: ${e.message()}"
                                                        }
                                                    }
                                                },
                                                colors = ButtonDefaults.buttonColors(
                                                    containerColor = when (newStatus) {
                                                        "ongoing" -> Color.Yellow
                                                        "resolved" -> Color.Green
                                                        "rejected" -> Color.Red
                                                        else -> Color.Gray
                                                    }
                                                )
                                            ) {
                                                Text(
                                                    text = newStatus.capitalize(),
                                                    color = Color.Black
                                                )
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
    }
}

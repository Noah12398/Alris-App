package com.example.alris.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

// ============= AUTH MODELS =============
data class VerifyRequest(
    val accessToken: String,
    val role: String,
    val invitationToken: String? = null
)

data class VerifyResponse(
    val status: String,
    val error: String? = null
)

data class RegisterRequest(
    val name: String,
    val email: String,
    val password: String,
    val phone: String? = null
)

data class LoginRequest(
    val email: String,
    val password: String
)

data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val token_type: String = "Bearer"
)

data class RegisterResponse(
    val message: String,
    val user_id: String
)

// ============= USER MODELS =============
data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val phone: String?,
    val created_at: String
)

// ============= REPORT UPLOAD MODELS =============
data class UploadResponse(
    val report: Report,
    val uploads: List<ReportUpload>
)

data class Report(
    val id: String,
    val user_id: String,
    val latitude: Double,
    val longitude: Double,
    val description: String?,
    val is_classified: Boolean = false,
    val created_at: String
)

data class ReportUpload(
    val id: String,
    val report_id: String,
    val filename: String,
    val is_fake: Boolean? = null,
    val is_spam: Boolean? = null,
    val uploaded_at: String? = null
)

// ============= NEARBY ISSUES MODELS =============
data class NearbyIssuesRequest(
    val latitude: Double,
    val longitude: Double
)

data class NearbyIssuesResponse(
    val issues: List<IssueItem>,
    val total: Int,
    val limit: Int,
    val offset: Int,
    val hasMore: Boolean
)

data class IssueItem(
    val id: String,
    val title: String?,
    val description: String?,
    val location: String,  // POINT(lon lat) format from PostGIS
    val category: String?,
    val status: String?,
    val department: String?,
    val distance_meters: Double?,
    val distance_km: Double?,
    val created_at: String?
)

// ============= UI MODELS (for displaying in app) =============
data class ReportPoint(
    val id: String,
    val title: String,
    val description: String,
    val category: ReportCategory,
    val status: ReportStatus,
    val location: String, // POINT(lon lat) - parse to GeoPoint
    val distance_meters: Double,
    val distance_km: Double,
    val created_at: String
)
enum class ReportCategory(
    val display: String,
    val icon: ImageVector,
    val color: Color
) {
    DRAINAGE(
        "Drainage",
        Icons.Default.WaterDrop,
        Color(0xFF4FC3F7)
    ),
    LIGHTING(
        "Lighting",
        Icons.Default.Lightbulb,
        Color(0xFFFFEB3B)
    ),
    WASTE(
        "Waste",
        Icons.Default.Delete,
        Color(0xFF8BC34A)
    ),
    GARBAGE(
        "Garbage",
        Icons.Default.Delete,
        Color(0xFF8BC34A)
    ),
    ROAD(
        "Road",
        Icons.Default.Water,
        Color(0xFF795548)
    ),
    WATER(
        "Water",
        Icons.Default.Water,
        Color(0xFF03A9F4)
    ),
    ELECTRICITY(
        "Electricity",
        Icons.Default.Bolt,
        Color(0xFFFFC107)
    ),
    OTHER(
        "Other",
        Icons.Default.Info,
        Color(0xFF9E9E9E)
    );
}

enum class ReportStatus {
    PENDING,
    IN_PROGRESS,
    APPROVED,
    REJECTED,
    RESOLVED
}

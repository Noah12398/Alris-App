package com.example.alris.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

// ============= GENERIC API RESPONSE WRAPPER =============
// Backend wraps ALL responses in { success: Boolean, data: T?, message: String? }
data class ApiResponse<T>(
    val success: Boolean,
    val data: T?,
    val message: String? = null,
    val error: String? = null
)

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

// Inside { success: true, data: { accessToken, refreshToken, user } }
data class LoginResponse(
    val accessToken: String,
    val refreshToken: String,
    val user: BackendUser?
)

data class BackendUser(
    val id: String,
    val email: String,
    val role: String?,              // "authority" | "higher" | "citizen"
    val name: String? = null,
    val department: String? = null,
    val isInitialized: Boolean? = null,  // camelCase from Drizzle ORM
    val trustScore: Double? = null,
    val isFlagged: Boolean? = null,
    val totalReports: Int? = null
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
    val avatarUrl: String? = null,
    val trustScore: Double? = null,
    val isFlagged: Boolean? = null,
    val totalReports: Int? = null,
    val totalUpvotes: Int? = null,
    val createdAt: String? = null
)

// ============= REPORT UPLOAD MODELS =============
// Inside { success: true, data: { report, uploads } }
data class UploadResponseData(
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
    val created_at: String? = null
)

data class ReportUpload(
    val id: String,
    val report_id: String? = null,
    val filename: String,
    val is_fake: Boolean? = null,
    val is_spam: Boolean? = null,
    val uploaded_at: String? = null
)

// ============= NEARBY ISSUES MODELS =============
// Inside { success: true, data: { issues, total, limit, offset, hasMore } }
data class NearbyIssuesResponse(
    val issues: List<IssueItem>,
    val total: Int,
    val limit: Int,
    val offset: Int,
    val hasMore: Boolean
)

// Backend returns: issue_id, latitude, longitude, category, department,
// status, priority, upvote_count, report_count, distance_meters, distance_km,
// created_at, updated_at, reports[]
data class IssueItem(
    val issue_id: String,
    val latitude: Double,
    val longitude: Double,
    val category: String?,
    val status: String?,
    val department: String?,
    val priority: String? = null,
    val upvote_count: Int? = null,
    val report_count: Int? = null,
    val distance_meters: Double? = null,
    val distance_km: Double? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val reports: List<ReportSummary>? = null,
    val is_upvoted: Boolean? = null
)

// ============= UI MODELS (for displaying in app) =============
data class ReportPoint(
    val id: String,
    val userId: String?, // For Authority to rate user
    val reportId: String?, // For Authority to rate specific report
    val title: String,
    val description: String,
    val category: ReportCategory,
    val status: ReportStatus,
    val latitude: Double,
    val longitude: Double,
    val distance_meters: Double,
    val distance_km: Double,
    val created_at: String,
    val isUpvoted: Boolean = false,
    val upvoteCount: Int = 0
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

// ============= DEPARTMENT ISSUES MODELS =============
// Inside { success: true, data: { issues, total, limit, offset, hasMore } }
data class DepartmentIssuesResponse(
    val issues: List<Issue>,
    val total: Int? = null,
    val limit: Int? = null,
    val offset: Int? = null,
    val hasMore: Boolean? = null
)

data class Issue(
    val issue_id: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val status: String,
    val category: String?,
    val department: String? = null,
    val priority: String? = null,
    val upvote_count: Int? = null,
    val report_count: Int? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val reports: List<ReportSummary> = emptyList()
)

// Detailed issue model for getIssueById endpoint
data class IssueDetail(
    val issue_id: String,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val status: String,
    val category: String?,
    val department: String? = null,
    val priority: String? = null,
    val upvote_count: Int? = null,
    val report_count: Int? = null,
    val description: String? = null,
    val created_at: String? = null,
    val updated_at: String? = null,
    val resolved_at: String? = null,
    val reports: List<ReportSummary> = emptyList()
)

// Wrapper for getIssueById API response: { success: true, data: { issue: {...} } }
data class IssueDetailResponse(
    val issue: IssueDetail
)

data class ReportSummary(
    val report_id: String,
    val user_id: String? = null,
    val description: String?,
    val created_at: String? = null,
    val uploads: List<ReportUploadSummary> = emptyList()
)

data class ReportUploadSummary(
    val id: String? = null,
    val url: String,
    val uploaded_at: String?,
    val is_fake: Boolean?,
    val is_spam: Boolean?
)

// Backend accepts: { issueId, status }
// Valid statuses: "submitted", "in_progress", "resolved", "rejected"
data class StatusUpdate(
    val issueId: String,
    val status: String
)

data class StatusResponse(
    val message: String? = null
)

// ============= AUTHORITY MODELS =============
// Inside { success: true, data: { authority, tempPassword }, message }
data class RegisterLowerAuthorityRequest(
    val email: String
)

data class RegisterLowerAuthorityData(
    val authority: Authority,
    val tempPassword: String
)

data class Authority(
    val id: String,
    val email: String,
    val department: String
)

data class UpdateAuthorityProfileRequest(
    val name: String?,
    val phone: String?,
    val latitude: Double?,
    val longitude: Double?,
    val newPassword: String?
)

// Authority profile model for getAuthorityProfile API
data class AuthorityProfile(
    val id: String,
    val name: String? = null,
    val email: String,
    val phone: String? = null,
    val department: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isInitialized: Boolean = false,
    val role: String? = null,
    val createdAt: String? = null
)

// Inside { success: true, data: { id, name, email, ... } }
data class UpdateAuthorityProfileResponse(
    val id: String? = null,
    val name: String?,
    val email: String? = null,
    val department: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val isInitialized: Boolean? = null
)

// ============= MY REPORTS MODELS =============

data class MyReportsResponse(
    val reports: List<MyReportItem>,
    val total: Int,
    val limit: Int,
    val offset: Int,
    val hasMore: Boolean
)

data class MyReportItem(
    val id: String,
    val latitude: Double,
    val longitude: Double,
    val description: String?,
    val is_classified: Boolean,
    val created_at: String,
    val issue: IssueSummary?,
    val uploads: List<Upload>
)

data class Upload(
    val id: String,
    val filename: String? = null,
    val url: String
)

data class IssueSummary(
    val id: String,
    val department: String?,
    val category: String?,
    val status: String,
    val created_at: String,
    val updated_at: String
)

// ============= NOTIFICATION MODELS =============

data class NotificationResponse(
    val notifications: List<NotificationItem>,
    val total: Int,
    val limit: Int,
    val offset: Int,
    val hasMore: Boolean
)

data class NotificationItem(
    val id: String,
    val recipientId: String,
    val title: String,
    val message: String, // Backend schema field is "message", not "body"
    val type: String?, // "status_update", "system", etc.
    val isRead: Boolean,
    val createdAt: String
)

data class UnreadCountResponse(
    val unreadCount: Int
)

// ============= RATINGS & FLAGGED USERS MODELS =============

data class RateUserRequest(
    val rating: Int,
    val comment: String? = null,
    val reportId: String? = null
)

data class UserRating(
    val id: String,
    val userId: String,
    val ratedBy: String,
    val ratedByRole: String,
    val rating: Int,
    val comment: String?,
    val reportId: String?,
    val createdAt: String? // Backend Drizzle propery is camelCase
)

data class UserRatingsResponse(
    val user: BackendUser,
    val ratings: List<UserRating>,
    val totalRatings: Int
)

data class FlaggedUser(
    val id: String,
    val name: String,
    val email: String,
    val phone: String? = null,
    val trustScore: Double,
    val isFlagged: Boolean,
    val totalReports: Int,
    val createdAt: String? // Backend Drizzle property
)

data class FlaggedUsersResponse(
    val users: List<FlaggedUser>,
    val total: Int,
    val limit: Int,
    val offset: Int,
    val hasMore: Boolean
)

// ============= ADMIN MODELS =============

data class AdminStatsResponse(
    val totalUsers: Int,
    val totalReports: Int,
    val totalIssues: Int,
    val classifiedReports: Int,
    val unclassifiedReports: Int,
    val flaggedUsers: Int,
    val fakeUploads: Int,
    val spamUploads: Int,
    val issuesByStatus: Map<String, Int>? = null
)

data class AuditLogItem(
    val id: String,
    val actorId: String,
    val actorRole: String,
    val action: String,
    val entityType: String,
    val entityId: String,
    val metadata: String? = null,
    val ipAddress: String? = null,
    val createdAt: String
)

data class AuditLogsResponse(
    val logs: List<AuditLogItem>,
    val total: Int,
    val limit: Int,
    val offset: Int,
    val hasMore: Boolean
)

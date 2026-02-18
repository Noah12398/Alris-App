package com.example.alris.data

import android.content.Context
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

// ============= API INTERFACES =============
interface AuthApiService {
    @POST("auth/verify-token")
    suspend fun verifyToken(
        @Body request: VerifyRequest
    ): Response<VerifyResponse>

    @POST("auth/register")
    suspend fun registerUser(@Body request: RegisterRequest): Response<ApiResponse<RegisterResponse>>

    @POST("auth/login")
    suspend fun loginUser(@Body request: LoginRequest): Response<ApiResponse<LoginResponse>>

    @POST("authority/login")
    suspend fun loginAuthority(@Body request: LoginRequest): Response<ApiResponse<LoginResponse>>

    @POST("auth/refresh")
    suspend fun refreshToken(
        @Body body: Map<String, String>
    ): Response<ApiResponse<Map<String, String>>>

    @POST("auth/logout")
    suspend fun logout(
        @Body body: Map<String, String>
    ): Response<ApiResponse<Any>>
}

interface UserApi : AuthApiService {


    @GET("auth/profile")
    suspend fun getUserProfile(): Response<ApiResponse<UserProfile>>

    @Multipart
    @POST("reports")
    suspend fun uploadReport(
        @Part files: List<MultipartBody.Part>,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("description") description: RequestBody?
    ): Response<ApiResponse<UploadResponseData>>

    // GET /issues/nearby?latitude=...&longitude=...&radius=...&limit=...&offset=...
    // Works for both citizens (provide lat/lon) and authorities (backend uses stored location)
    @GET("issues/nearby")
    suspend fun getNearbyIssues(
        @Query("latitude") latitude: Double? = null,
        @Query("longitude") longitude: Double? = null,
        @Query("radius") radius: Int = 10,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<ApiResponse<NearbyIssuesResponse>>



    @GET("issues/department")
    suspend fun getDepartmentIssues(
        @Query("status") status: String? = null,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<ApiResponse<DepartmentIssuesResponse>>

    @PUT("issues/status")
    suspend fun updateIssueStatus(@Body body: StatusUpdate): Response<ApiResponse<Any>>

    @GET("issues/{issueId}")
    suspend fun getIssueById(@Path("issueId") issueId: String): Response<ApiResponse<IssueDetailResponse>>

    @GET("authority/profile")
    suspend fun getAuthorityProfile(): Response<ApiResponse<AuthorityProfile>>

    @POST("authority/register-lower")
    suspend fun registerLowerAuthority(
        @Body body: RegisterLowerAuthorityRequest
    ): Response<ApiResponse<RegisterLowerAuthorityData>>

    @PUT("authority/update-profile")
    suspend fun updateAuthorityProfile(
        @Body request: UpdateAuthorityProfileRequest
    ): Response<ApiResponse<UpdateAuthorityProfileResponse>>



    @GET("reports/my-reports")
    suspend fun getMyReports(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<ApiResponse<MyReportsResponse>>

    @GET("notifications")
    suspend fun getMyNotifications(
        @Query("limit") limit: Int = 20,
        @Query("offset") offset: Int = 0
    ): Response<ApiResponse<NotificationResponse>>

    @GET("notifications/unread-count")
    suspend fun getUnreadCount(): Response<ApiResponse<UnreadCountResponse>>

    @PATCH("notifications/{id}/read")
    suspend fun markAsRead(@Path("id") id: String): Response<ApiResponse<Any>>

    @PATCH("notifications/read-all")
    suspend fun markAllAsRead(
        @Body body: Map<String, String> = emptyMap()
    ): Response<ApiResponse<Any>>

    // ============= NEW ENDPOINTS FOR BACKEND PARITY =============

    @POST("ratings/user/{userId}")
    suspend fun rateUser(
        @Path("userId") userId: String,
        @Body request: RateUserRequest
    ): Response<ApiResponse<Any>>

    @GET("ratings/user/{userId}")
    suspend fun getUserRatings(
        @Path("userId") userId: String
    ): Response<ApiResponse<UserRatingsResponse>>

    @GET("ratings/flagged")
    suspend fun getFlaggedUsers(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): Response<ApiResponse<FlaggedUsersResponse>>

    @retrofit2.http.DELETE("reports/{id}")
    suspend fun deleteReport(
        @Path("id") id: String
    ): Response<ApiResponse<Any>>

    @POST("issues/{id}/upvote")
    suspend fun upvoteIssue(
        @Path("id") id: String
    ): Response<ApiResponse<Any>>

    @retrofit2.http.DELETE("issues/{id}/upvote")
    suspend fun removeUpvote(
        @Path("id") id: String
    ): Response<ApiResponse<Any>>

    // ============= ADMIN ENDPOINTS =============

    @GET("admin/stats")
    suspend fun getAdminStats(): Response<ApiResponse<AdminStatsResponse>>

    @GET("admin/audit-logs")
    suspend fun getAuditLogs(
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0,
        @Query("action") action: String? = null
    ): Response<ApiResponse<AuditLogsResponse>>
}

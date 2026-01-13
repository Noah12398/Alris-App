package com.example.alris.data

import android.content.Context
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Query

// ============= API INTERFACES =============
interface AuthApiService {
    @POST("auth/verify-token")
    suspend fun verifyToken(
        @Body request: VerifyRequest
    ): Response<VerifyResponse>
}

interface UserApi {
    @POST("auth/register")
    suspend fun registerUser(@Body request: RegisterRequest): Response<Unit>

    @POST("auth/login")
    suspend fun loginUser(@Body request: LoginRequest): Response<LoginResponse>

    @GET("me")
    suspend fun getUserProfile(@Header("Authorization") token: String): Response<UserProfile>

    @Multipart
    @POST("reports")
    suspend fun uploadReport(
        @Part files: List<MultipartBody.Part>,
        @Part("latitude") latitude: RequestBody,
        @Part("longitude") longitude: RequestBody,
        @Part("description") description: RequestBody?
    ): Response<UploadResponse>

    // For citizens - requires body with user's current coordinates
    @POST("issues/nearby")
    suspend fun getNearbyIssuesForCitizen(
        @Body body: NearbyIssuesRequest,
        @Query("radius") radius: Int = 10,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): NearbyIssuesResponse

    // For authorities - no body needed, backend uses authority's stored location
    @POST("issues/nearby")
    suspend fun getNearbyIssuesForAuthority(
        @Query("radius") radius: Int = 10,
        @Query("limit") limit: Int = 50,
        @Query("offset") offset: Int = 0
    ): NearbyIssuesResponse

    @POST("authority/login")
    suspend fun loginAuthority(@Body request: LoginRequest): Response<LoginResponse>


    @GET("issues/department")
    suspend fun getDepartmentIssues(): Response<DepartmentIssuesResponse>

    @PUT("issues/status")
    suspend fun updateIssueStatus(@Body body: StatusUpdate): Response<StatusResponse>

    @POST("authority/register-lower")
    suspend fun registerLowerAuthority(
        @Body body: RegisterLowerAuthorityRequest
    ): RegisterLowerAuthorityResponse

    @PUT("authority/update-profile")
    suspend fun updateAuthorityProfile(
        @Body request: UpdateAuthorityProfileRequest
    ): Response<UpdateAuthorityProfileResponse>



}



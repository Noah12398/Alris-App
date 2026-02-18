package com.example.alris.data

import android.util.Log
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.Interceptor
import okhttp3.Response

class TokenRefreshInterceptor(
    private val tokenManager: TokenManager,
    private val authApiService: AuthApiService
) : Interceptor {

    private val mutex = Mutex()

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val initialResponse = chain.proceed(originalRequest)

        // Check for 401 (Unauthorized) OR 403 (Forbidden - used by this backend for expiry)
        if (initialResponse.code == 401 || initialResponse.code == 403) {
            Log.d("TokenRefreshInterceptor", "Received ${initialResponse.code}, attempting token refresh...")

            val currentToken = runBlocking { tokenManager.accessTokenFlow.firstOrNull() }

            // synchronized block to prevent multiple refreshes
            return runBlocking {
                mutex.withLock {
                    val newToken = tokenManager.accessTokenFlow.firstOrNull()

                    // If token changed while we waited for lock, retry with new token
                    if (currentToken != newToken && newToken != null) {
                        Log.d("TokenRefreshInterceptor", "Token already refreshed, retrying...")
                        initialResponse.close()
                        return@runBlocking chain.proceed(
                            originalRequest.newBuilder()
                                .header("Authorization", "Bearer $newToken")
                                .build()
                        )
                    }

                    // Otherwise, we need to refresh
                    val refreshToken = tokenManager.refreshTokenFlow.firstOrNull()
                    if (refreshToken.isNullOrBlank()) {
                        Log.d("TokenRefreshInterceptor", "No refresh token available.")
                        return@runBlocking initialResponse
                    }

                    try {
                        Log.d("TokenRefreshInterceptor", "Calling refresh endpoint...")
                        // FIX: Changed "refreshToken" to "token" to match backend expectation
                        val refreshResponse = authApiService.refreshToken(mapOf("token" to refreshToken))

                        if (refreshResponse.isSuccessful && refreshResponse.body()?.data != null) {
                            val data = refreshResponse.body()?.data
                            val newAccessToken = data?.get("accessToken")
                            val newRefreshToken = data?.get("refreshToken")

                            if (!newAccessToken.isNullOrBlank()) {
                                Log.d("TokenRefreshInterceptor", "Refresh SUCCESS. New token: ${newAccessToken.take(10)}...")
                                
                                tokenManager.saveAccessToken(newAccessToken)
                                if (!newRefreshToken.isNullOrBlank()) {
                                    tokenManager.saveRefreshToken(newRefreshToken)
                                }

                                initialResponse.close()
                                return@runBlocking chain.proceed(
                                    originalRequest.newBuilder()
                                        .header("Authorization", "Bearer $newAccessToken")
                                        .build()
                                )
                            }
                        } else {
                            Log.e("TokenRefreshInterceptor", "Refresh FAILED: ${refreshResponse.code()}")
                            val errorBody = refreshResponse.errorBody()?.string()
                            Log.e("TokenRefreshInterceptor", "Error Body: $errorBody")
                            
                            // Only clear if it was definitely a refresh failure, not a transient network error
                            if (refreshResponse.code() == 401 || refreshResponse.code() == 403) {
                                tokenManager.clearToken()
                            }
                        }
                    } catch (e: Exception) {
                        Log.e("TokenRefreshInterceptor", "Exception during refresh: ${e.message}")
                        e.printStackTrace()
                    }
                }
                // Return original response if refresh failed
                initialResponse
            }
        }

        return initialResponse
    }
}

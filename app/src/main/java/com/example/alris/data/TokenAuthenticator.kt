package com.example.alris.data

import android.util.Log
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Authenticator
import okhttp3.Request
import okhttp3.Response
import okhttp3.Route

class TokenAuthenticator(
    private val tokenManager: TokenManager,
    private val authApiService: AuthApiService
) : Authenticator {

    override fun authenticate(route: Route?, response: Response): Request? {
        Log.d("TokenAuthenticator", "Authenticating response code: ${response.code}")

        // Limit retry attempts to avoid infinite loops
        if (responseCount(response) >= 2) {
            Log.d("TokenAuthenticator", "Too many retry attempts, giving up.")
            return null
        }

        // Synchronously refresh the token
        return runBlocking {
            try {
                // Get the current refresh token
                val refreshToken = tokenManager.refreshTokenFlow.firstOrNull()
                
                if (refreshToken.isNullOrBlank()) {
                    Log.d("TokenAuthenticator", "No refresh token available.")
                    return@runBlocking null
                }

                // Call the refresh endpoint
                val refreshResponse = authApiService.refreshToken(mapOf("refreshToken" to refreshToken))

                val body = refreshResponse.body()

                if (refreshResponse.isSuccessful && body?.data != null) {

                    val newAccessToken = body.data?.get("accessToken")
                    val newRefreshToken = body.data?.get("refreshToken")

                    if (!newAccessToken.isNullOrBlank()) {
                        Log.d("TokenAuthenticator", "Token refresh successful!")
                        
                        // Save new tokens
                        tokenManager.saveAccessToken(newAccessToken)
                        if (!newRefreshToken.isNullOrBlank()) {
                            tokenManager.saveRefreshToken(newRefreshToken)
                        }

                        // Retry the request with the new access token
                        return@runBlocking response.request.newBuilder()
                            .header("Authorization", "Bearer $newAccessToken")
                            .build()
                    }
                } else {
                    Log.e("TokenAuthenticator", "Refresh failed: ${refreshResponse.code()}")
                    // If refresh fails (e.g., refresh token expired), clear tokens and force logout
                    tokenManager.clearToken()
                }
            } catch (e: Exception) {
                Log.e("TokenAuthenticator", "Error refreshing token: ${e.message}")
            }
            null // Return null to give up
        }
    }

    private fun responseCount(response: Response): Int {
        var result = 1
        var prior = response.priorResponse
        while (prior != null) {
            result++
            prior = prior.priorResponse
        }
        return result
    }
}

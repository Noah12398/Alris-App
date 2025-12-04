package com.example.alris.data

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response

class TokenManager(private val context: Context) {
    companion object {
        private val Context.dataStore by preferencesDataStore("auth_prefs")
        val ACCESS_TOKEN = stringPreferencesKey("accessToken")
        val REFRESH_TOKEN = stringPreferencesKey("refreshToken")

    }
    val accessTokenFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[ACCESS_TOKEN]
    }
    suspend fun saveAccessToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[ACCESS_TOKEN] = token
        }
    }
    suspend fun saveRefreshToken(token: String) {
        context.dataStore.edit { prefs ->
            prefs[REFRESH_TOKEN] = token
        }
    }

    suspend fun clearToken() {
        context.dataStore.edit { prefs ->
            prefs.remove(ACCESS_TOKEN)
        }
    }
}
class AuthInterceptor(private val tokenManager: TokenManager) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {

        val request = chain.request()
        Log.d("DEBUG_INTERCEPTOR", "Intercepting: ${request.url}")
        Log.d("DEBUG_INTERCEPTOR", "Method: ${request.method}")

        val path = request.url.encodedPath
        Log.d("DEBUG_INTERCEPTOR", "Path: $path")

        if (path.contains("/authority/login") ||
            path.contains("/auth/register") ||
            path.contains("/auth/login") ||
            path.contains("/api/auth/verify-token")
        ) {
            Log.d("DEBUG_INTERCEPTOR", "Skipping auth header for: $path")
            return chain.proceed(request)
        }

        val token = runBlocking {
            tokenManager.accessTokenFlow.firstOrNull()
        }
        Log.d("DEBUG_INTERCEPTOR", "Token fetched = $token")

        val updatedRequest = if (!token.isNullOrBlank()) {
            Log.d("DEBUG_INTERCEPTOR", "Adding Authorization header")
            request.newBuilder()
                .addHeader("Authorization", "Bearer $token")
                .build()
        } else {
            Log.d("DEBUG_INTERCEPTOR", "No token, sending request without header")
            request
        }

        return chain.proceed(updatedRequest)
    }
}

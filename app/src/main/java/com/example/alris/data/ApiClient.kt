import android.content.Context
import android.util.Log
import com.example.alris.data.AuthInterceptor
import com.example.alris.data.TokenManager
import com.example.alris.data.UserApi
import okhttp3.EventListener
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {
    private const val BASE_URL = "https://alris-node.vercel.app/"

    fun createUserApi(context: Context): UserApi {
        val tokenManager = TokenManager(context)

        // 🔥 Logs every line of request and response
        val loggingInterceptor = HttpLoggingInterceptor { message ->
            Log.d("HTTP_BODY", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BODY
        }

        // 🔥 Detailed network events (DNS, connect, request headers, response headers)
        val eventLogger = object : EventListener() {
            override fun callStart(call: okhttp3.Call) {
                Log.d("HTTP_EVENT", "------------------------------------")
                Log.d("HTTP_EVENT", "Call Started: ${call.request().url}")
            }

            override fun requestHeadersStart(call: okhttp3.Call) {
                Log.d("HTTP_EVENT", "Request Headers:")
            }

            override fun requestBodyStart(call: okhttp3.Call) {
                Log.d("HTTP_EVENT", "Request Body Sending...")
            }

            override fun responseHeadersStart(call: okhttp3.Call) {
                Log.d("HTTP_EVENT", "Response Headers Received")
            }

            override fun responseBodyStart(call: okhttp3.Call) {
                Log.d("HTTP_EVENT", "Reading Response Body...")
            }

            override fun callEnd(call: okhttp3.Call) {
                Log.d("HTTP_EVENT", "Call End: ${call.request().url}")
                Log.d("HTTP_EVENT", "------------------------------------")
            }

            override fun callFailed(call: okhttp3.Call, ioe: java.io.IOException) {
                Log.e("HTTP_EVENT", "Call Failed: ${ioe.message}")
            }
        }

        val okHttpClient = OkHttpClient.Builder()
            .eventListener(eventLogger) // 🔥 THIS ADDS DEEP DEBUG
            .addInterceptor(loggingInterceptor)
            .addInterceptor(AuthInterceptor(tokenManager))
            .addNetworkInterceptor { chain ->
                val request: Request = chain.request()
                Log.d("AFTER_INTERCEPTOR", "FINAL Request URL: ${request.url}")
                Log.d("AFTER_INTERCEPTOR", "FINAL Headers:")
                request.headers.forEach { h ->
                    Log.d("AFTER_INTERCEPTOR", "→ ${h.first}: ${h.second}")
                }

                val response: Response = chain.proceed(request)

                Log.d("AFTER_INTERCEPTOR", "FINAL Response Code: ${response.code}")
                Log.d("AFTER_INTERCEPTOR", "FINAL Response Message: ${response.message}")

                val peek = response.peekBody(Long.MAX_VALUE).string()
                Log.d("AFTER_INTERCEPTOR", "FINAL Response Body: $peek")

                response
            }
            .retryOnConnectionFailure(true)
            .build()

        val retrofit = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

        Log.d("API_CLIENT", "Retrofit created with BASE_URL = $BASE_URL")

        return retrofit.create(UserApi::class.java)
    }
}

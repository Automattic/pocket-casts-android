package au.com.shiftyjelly.pocketcasts.servers.interceptors

import java.net.HttpURLConnection
import okhttp3.Credentials
import okhttp3.Interceptor
import okhttp3.Response

internal class BasicAuthInterceptor : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val response = chain.proceed(originalRequest)

        if (response.code != HttpURLConnection.HTTP_UNAUTHORIZED) {
            return response
        }

        val header = response.header("WWW-Authenticate")
        if (header == null || !header.contains("Basic", ignoreCase = true)) {
            return response
        }

        val httpUrl = originalRequest.url
        if (httpUrl.username.isEmpty() || httpUrl.password.isEmpty()) {
            return response
        }

        response.close()
        val credentials = Credentials.basic(httpUrl.username, httpUrl.password)
        val newRequest = originalRequest.newBuilder()
            .header("Authorization", credentials)
            .build()
        return chain.proceed(newRequest)
    }
}

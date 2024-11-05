package au.com.shiftyjelly.pocketcasts.servers

import okhttp3.Interceptor
import okhttp3.Response

internal class CleanAndRetryInterceptor(
    private val headersToRemove: List<String>,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val response = chain.proceed(originalRequest)
        return if (response.isSuccessful || response.isRedirect) {
            response
        } else {
            response.close()
            val cleanRequest = originalRequest.newBuilder()
                .let { builder -> headersToRemove.fold(builder) { acc, header -> acc.removeHeader(header) } }
                .build()
            chain.proceed(cleanRequest)
        }
    }
}

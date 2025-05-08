package au.com.shiftyjelly.pocketcasts.servers

import okhttp3.Interceptor
import okhttp3.Response

internal class CleanAndRetryInterceptor(
    private val headersToRemove: List<String>,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val response = chain.proceed(originalRequest)
        // response.isRedirect returns true only when redirecting to different resources
        // It doesn't work here as it doesn't account for 304
        return if (response.code in 200..399) {
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

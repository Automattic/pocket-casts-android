package au.com.shiftyjelly.pocketcasts.servers

import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

internal class CleanAndRetryInterceptor(
    private val headersToRemove: List<String>,
    private val retryUserAgent: String? = null,
    private val onRetry: (Request, Response) -> Unit = { _, _ -> },
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
            onRetry(originalRequest, response)
            val cleanRequest = originalRequest.newBuilder()
                .let { builder -> headersToRemove.fold(builder) { acc, header -> acc.removeHeader(header) } }
                .withRetryUserAgent()
                .build()
            chain.proceed(cleanRequest)
        }
    }

    private fun Request.Builder.withRetryUserAgent(): Request.Builder {
        val userAgent = retryUserAgent?.takeIf { FeatureFlag.isEnabled(Feature.RETRY_WITH_PLATFORM_USER_AGENT) }
        return if (userAgent != null) header("User-Agent", userAgent) else this
    }
}

package au.com.shiftyjelly.pocketcasts.servers.interceptors

import java.util.Locale
import okhttp3.HttpUrl
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

internal class InternationalizationInterceptor(
    private val allowedHosts: List<String>,
    private val provideLocale: () -> Locale,
    private val provideRegion: () -> String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val newRequest = if (isHostAllowed(request.url)) {
            request.newBuilder()
                .addHeaderIfNotEmpty(APP_LANGUAGE_HEADER, provideLocale().toIso6391())
                .addHeaderIfNotEmpty(USER_REGION_HEADER, provideRegion())
                .build()
        } else {
            request
        }

        return chain.proceed(newRequest)
    }

    private fun isHostAllowed(url: HttpUrl): Boolean {
        return allowedHosts.any { allowedHost -> url.host.endsWith(allowedHost, ignoreCase = true) }
    }

    private fun Locale.toIso6391() = buildString {
        if (language.isNotEmpty()) {
            append(language)
        }
        if (isNotEmpty() && country.isNotEmpty()) {
            append('-')
            append(country)
        }
    }

    private fun Request.Builder.addHeaderIfNotEmpty(header: String, value: String): Request.Builder {
        return if (value.isNotEmpty()) {
            header(header, value)
        } else {
            this
        }
    }

    companion object {
        const val APP_LANGUAGE_HEADER = "X-App-Language"
        const val USER_REGION_HEADER = "X-User-Region"
    }
}

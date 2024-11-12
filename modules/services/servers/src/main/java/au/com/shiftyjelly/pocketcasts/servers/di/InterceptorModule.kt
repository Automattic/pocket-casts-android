package au.com.shiftyjelly.pocketcasts.servers.di

import au.com.shiftyjelly.pocketcasts.preferences.AccessToken
import au.com.shiftyjelly.pocketcasts.servers.BuildConfig
import au.com.shiftyjelly.pocketcasts.servers.CleanAndRetryInterceptor
import au.com.shiftyjelly.pocketcasts.servers.OkHttpInterceptor
import au.com.shiftyjelly.pocketcasts.servers.interceptors.BasicAuthInterceptor
import au.com.shiftyjelly.pocketcasts.servers.sync.TokenHandler
import au.com.shiftyjelly.pocketcasts.servers.toClientInterceptor
import au.com.shiftyjelly.pocketcasts.servers.toNetworkInterceptor
import com.automattic.android.tracks.crashlogging.CrashLoggingOkHttpInterceptorProvider
import com.automattic.android.tracks.crashlogging.FormattedUrl
import com.automattic.android.tracks.crashlogging.RequestFormatter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.net.HttpURLConnection
import kotlin.time.Duration.Companion.minutes
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor

@InstallIn(SingletonComponent::class)
@Module
object InterceptorModule {
    const val PocketCastsPublicUserAgent = "Pocket Casts"
    const val PocketCastsInternalUserAgent = "$PocketCastsPublicUserAgent/Android/${BuildConfig.VERSION_NAME}"

    private val fiveMinutes = 5.minutes.inWholeSeconds
    private val cacheControlHeader = "Cache-Control"

    private val crashLoggingInterceptor = CrashLoggingOkHttpInterceptorProvider
        .createInstance(object : RequestFormatter {
            override fun formatRequestUrl(request: Request): FormattedUrl {
                return request.url.host.takeIf { it.contains("pocketcasts") } ?: "filtered"
            }
        })

    private val publicUserAgentInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
        request.header("User-Agent", PocketCastsPublicUserAgent)
        chain.proceed(request.build())
    }

    private val internalUserAgentInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
        request.header("User-Agent", PocketCastsInternalUserAgent)
        chain.proceed(request.build())
    }

    private val cacheControlInterceptor = Interceptor { chain ->
        val request = chain.request()
        val originalResponse = chain.proceed(request)
        var responseBuilder = originalResponse.newBuilder()
        if (request.url.host == BuildConfig.SERVER_CACHE_HOST) {
            responseBuilder = responseBuilder.header(cacheControlHeader, "public, max-age=$fiveMinutes")
        }
        responseBuilder.build()
    }

    private val cacheControlTranscriptsInterceptor = Interceptor { chain ->
        val request = chain.request()
        val originalResponse = chain.proceed(request)
        var responseBuilder = originalResponse.newBuilder().removeHeader("Pragma")
        if (request.cacheControl.noCache) {
            responseBuilder = responseBuilder.header(cacheControlHeader, "public, max-age=$fiveMinutes")
        } else if (request.cacheControl.onlyIfCached) {
            responseBuilder.header(cacheControlHeader, "public, only-if-cached, max-stale=${request.cacheControl.maxStaleSeconds}")
        }
        responseBuilder.build()
    }

    private val cleanAndRetryInterceptor = CleanAndRetryInterceptor(
        headersToRemove = listOf(
            // Remove our custom User-Agent. Internal ref: p1730724100345749-slack-C07J5LNP4SF
            "User-Agent",
            // Remove Sentry stuff from requests as well to make it more pure.
            "sentry-trace",
            "baggage",
        ),
    ).toClientInterceptor() // Must be client interceptor. Otherwise calls cannot be retried.

    private val basicAuthInterceptor = BasicAuthInterceptor().toClientInterceptor()

    @Provides
    @TokenInterceptor
    fun provideTokenInterceptor(
        tokenHandler: TokenHandler,
    ): Interceptor {
        val unauthenticatedEndpoints = setOf("security") // Don't attach a token to these methods because they get the token

        fun buildRequestWithToken(original: Request, token: AccessToken?): Request {
            val builder = original.newBuilder()
            if (token != null) {
                builder.addHeader("Authorization", "Bearer ${token.value}")
            }
            return builder.build()
        }

        return Interceptor { chain ->
            val original = chain.request()
            if (unauthenticatedEndpoints.contains(original.url.encodedPathSegments.firstOrNull())) {
                chain.proceed(original)
            } else {
                val token = runBlocking { tokenHandler.getAccessToken() }
                return@Interceptor if (token != null) {
                    val response = chain.proceed(buildRequestWithToken(original, token))
                    if (response.code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                        tokenHandler.invalidateAccessToken()
                        val newToken = runBlocking { tokenHandler.getAccessToken() }
                        chain.proceed(buildRequestWithToken(original, newToken))
                    } else {
                        response
                    }
                } else {
                    chain.proceed(original)
                }
            }
        }
    }

    @Provides
    @Cached
    fun provideCachedInterceptors(): List<OkHttpInterceptor> {
        return buildList {
            add(cacheControlInterceptor.toClientInterceptor())
            add(internalUserAgentInterceptor.toClientInterceptor())
            add(crashLoggingInterceptor.toClientInterceptor())

            if (BuildConfig.DEBUG) {
                val loggingInterceptor = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
                add(loggingInterceptor.toClientInterceptor())
            }
        }
    }

    @Provides
    @NoCache
    fun provideNoCacheInterceptors(): List<OkHttpInterceptor> {
        return buildList {
            add(internalUserAgentInterceptor.toClientInterceptor())
            add(crashLoggingInterceptor.toClientInterceptor())

            if (BuildConfig.DEBUG) {
                val loggingInterceptor = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
                add(loggingInterceptor.toClientInterceptor())
            }
        }
    }

    @Provides
    @NoCacheTokened
    fun provideNoCacheTokenedInterceptors(
        @TokenInterceptor interceptor: Interceptor,
    ): List<OkHttpInterceptor> {
        return buildList {
            add(internalUserAgentInterceptor.toClientInterceptor())
            add(interceptor.toClientInterceptor())
            add(crashLoggingInterceptor.toClientInterceptor())

            if (BuildConfig.DEBUG) {
                val loggingInterceptor = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
                add(loggingInterceptor.toClientInterceptor())
            }
        }
    }

    @Provides
    @Downloads
    fun provideDownloadsInterceptors(): List<OkHttpInterceptor> {
        return buildList {
            add(publicUserAgentInterceptor.toClientInterceptor())
            add(crashLoggingInterceptor.toClientInterceptor())
            add(basicAuthInterceptor)
            add(cleanAndRetryInterceptor)

            if (BuildConfig.DEBUG) {
                val loggingInterceptor = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.HEADERS
                }
                add(loggingInterceptor.toClientInterceptor())
            }
        }
    }

    @Provides
    @Transcripts
    fun provideTranscriptsInterceptors(): List<OkHttpInterceptor> {
        return buildList {
            add(publicUserAgentInterceptor.toClientInterceptor())
            add(cacheControlTranscriptsInterceptor.toClientInterceptor())
            add(crashLoggingInterceptor.toClientInterceptor())

            if (BuildConfig.DEBUG) {
                val loggingInterceptor = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BODY
                }
                add(loggingInterceptor.toClientInterceptor())
            }

            add(cacheControlTranscriptsInterceptor.toNetworkInterceptor())
        }
    }

    @Provides
    @Player
    fun providePlayerInterceptors(): List<OkHttpInterceptor> {
        return buildList {
            add(publicUserAgentInterceptor.toClientInterceptor())
            add(crashLoggingInterceptor.toClientInterceptor())
            add(basicAuthInterceptor)
            add(cleanAndRetryInterceptor)

            if (BuildConfig.DEBUG) {
                val loggingInterceptor = HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.HEADERS
                }
                add(loggingInterceptor.toClientInterceptor())
            }
        }
    }
}

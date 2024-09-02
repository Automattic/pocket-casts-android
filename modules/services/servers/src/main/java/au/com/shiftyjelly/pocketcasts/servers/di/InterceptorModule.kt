package au.com.shiftyjelly.pocketcasts.servers.di

import au.com.shiftyjelly.pocketcasts.preferences.AccessToken
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.servers.BuildConfig
import au.com.shiftyjelly.pocketcasts.servers.OkHttpInterceptor
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
    private val fiveMinutes = 5.minutes.inWholeSeconds
    private val cacheControlHeader = "Cache-Control"

    private val crashLoggingInterceptor = CrashLoggingOkHttpInterceptorProvider
        .createInstance(object : RequestFormatter {
            override fun formatRequestUrl(request: Request): FormattedUrl {
                return request.url.host.takeIf { it.contains("pocketcasts") } ?: "filtered"
            }
        })

    private val userAgentInterceptor = Interceptor { chain ->
        val request = chain.request().newBuilder()
        request.header("User-Agent", Settings.USER_AGENT_POCKETCASTS_SERVER)
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
            add(userAgentInterceptor.toClientInterceptor())
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
            add(userAgentInterceptor.toClientInterceptor())
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
            add(userAgentInterceptor.toClientInterceptor())
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
            add(userAgentInterceptor.toClientInterceptor())
            add(crashLoggingInterceptor.toClientInterceptor())
        }
    }

    @Provides
    @Transcripts
    fun provideTranscriptsInterceptors(): List<OkHttpInterceptor> {
        return buildList {
            add(userAgentInterceptor.toClientInterceptor())
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
    fun providePLayerInterceptors(): List<OkHttpInterceptor> {
        return buildList {
            add(userAgentInterceptor.toClientInterceptor())
            add(crashLoggingInterceptor.toClientInterceptor())
        }
    }
}

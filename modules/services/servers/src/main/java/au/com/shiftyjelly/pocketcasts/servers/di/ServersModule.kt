package au.com.shiftyjelly.pocketcasts.servers.di

import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.localization.BuildConfig
import au.com.shiftyjelly.pocketcasts.models.entity.AnonymousBumpStat
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatusMoshiAdapter
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortTypeMoshiAdapter
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.servers.model.DisplayStyleMoshiAdapter
import au.com.shiftyjelly.pocketcasts.servers.model.ExpandedStyleMoshiAdapter
import au.com.shiftyjelly.pocketcasts.servers.model.ListTypeMoshiAdapter
import au.com.shiftyjelly.pocketcasts.servers.server.ListRepository
import au.com.shiftyjelly.pocketcasts.servers.server.ListWebService
import au.com.shiftyjelly.pocketcasts.servers.sync.old.SyncUpdateResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.old.SyncUpdateResponseParser
import au.com.shiftyjelly.pocketcasts.utils.Util
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import okhttp3.Cache
import okhttp3.Dispatcher
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.File
import java.net.HttpURLConnection
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ServersModule {

    companion object {
        private const val CACHE_SIX_MONTHS = 15552000
        private const val CACHE_FIVE_MINUTES = 300
        private const val HEADER_CACHE_CONTROL = "Cache-Control"

        private val SERVER_CACHE_HOST = Settings.SERVER_CACHE_URL.removePrefix("https://")

        val INTERCEPTOR_USER_AGENT = Interceptor { chain ->
            val request = chain.request().newBuilder()
            request.header("User-Agent", Settings.USER_AGENT_POCKETCASTS_SERVER)
            chain.proceed(request.build())
        }

        val INTERCEPTOR_CACHE_MODIFIER = Interceptor { chain ->
            val request = chain.request()
            val originalResponse = chain.proceed(request)
            var responseBuilder = originalResponse.newBuilder()
            if (request.url.host == SERVER_CACHE_HOST) {
                responseBuilder = responseBuilder.header(HEADER_CACHE_CONTROL, "public, max-age=$CACHE_FIVE_MINUTES")
            }
            responseBuilder.build()
        }

        val INTERCEPTOR_CACHE_SHOW_NOTES_MODIFIER = Interceptor { chain ->
            val request = chain.request()
            val originalResponse = chain.proceed(request)
            originalResponse.newBuilder().header(HEADER_CACHE_CONTROL, "public, max-age=$CACHE_SIX_MONTHS").build()
        }

        @Volatile private var showNotesHttpClient: OkHttpClient? = null
        fun getShowNotesClient(context: Context): OkHttpClient {
            return showNotesHttpClient ?: createShowNotesCacheClient(context).also { showNotesHttpClient = it }
        }

        private fun createShowNotesCacheClient(context: Context): OkHttpClient {
            val cacheSize = 10 * 1024 * 1024 // 10 MB
            val cacheDirectory = File(context.cacheDir.absolutePath, "ShowNotesCache")
            val cache = Cache(cacheDirectory, cacheSize.toLong())
            var builder = OkHttpClient.Builder()
                .addNetworkInterceptor(INTERCEPTOR_CACHE_SHOW_NOTES_MODIFIER)
                .connectTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .cache(cache)

            if (BuildConfig.DEBUG) {
                val logging = HttpLoggingInterceptor()
                logging.level = HttpLoggingInterceptor.Level.BODY
                builder = builder.addInterceptor(logging)
            }

            return builder.build()
        }
    }

    @Provides
    @Singleton
    internal fun provideMoshiBuilder(): Moshi.Builder {
        return Moshi.Builder()
            .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
            .add(SyncUpdateResponse::class.java, SyncUpdateResponseParser())
            .add(EpisodePlayingStatus::class.java, EpisodePlayingStatusMoshiAdapter())
            .add(PodcastsSortType::class.java, PodcastsSortTypeMoshiAdapter())
    }

    @Provides
    @Singleton
    internal fun provideMoshi(moshiBuilder: Moshi.Builder): Moshi {
        return moshiBuilder.build()
    }

    @Provides
    @SyncServerCache
    @Singleton
    internal fun provideSyncServerCache(@ApplicationContext context: Context): Cache {
        val cacheSize = 10 * 1024 * 1024 // 10 MB
        val cacheDirectory = File(context.cacheDir.absolutePath, "HttpCache")
        return Cache(cacheDirectory, cacheSize.toLong())
    }

    @Provides
    internal fun provideOkHttpClientBuilder(@SyncServerCache cache: Cache): OkHttpClient.Builder {
        var builder = OkHttpClient.Builder()
            .addNetworkInterceptor(INTERCEPTOR_CACHE_MODIFIER)
            .addNetworkInterceptor(INTERCEPTOR_USER_AGENT)
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .cache(cache)

        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BODY
            builder = builder.addInterceptor(logging)
        }

        return builder
    }

    @Provides
    @CachedOkHttpClient
    @Singleton
    internal fun provideOkHttpClientCache(okHttpClientBuilder: OkHttpClient.Builder): OkHttpClient {
        return okHttpClientBuilder.build()
    }

    private fun buildRequestWithToken(original: Request, token: String?): Request {
        val builder = original.newBuilder()
        if (token != null) {
            builder.addHeader("Authorization", "Bearer $token")
        }
        return builder.build()
    }

    @Provides
    @TokenInterceptor
    @Singleton
    internal fun provideTokenInterceptor(
        settings: Settings,
        @OnTokenErrorUiShown onTokenErrorUiShown: () -> Unit
    ): Interceptor {
        val unauthenticatedEndpoints = setOf("security") // Don't attach a token to these methods because they get the token
        return Interceptor { chain ->
            val original = chain.request()
            if (unauthenticatedEndpoints.contains(original.url.encodedPathSegments.firstOrNull())) {
                chain.proceed(original)
            } else {
                val token = settings.getSyncToken(onTokenErrorUiShown)
                return@Interceptor if (token != null) {
                    val response = chain.proceed(buildRequestWithToken(original, token))
                    if (response.code == HttpURLConnection.HTTP_UNAUTHORIZED) {
                        settings.invalidateToken()
                        val newToken = settings.getSyncToken(onTokenErrorUiShown)
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
    @OnTokenErrorUiShown
    internal fun provideTokenErrorUiTracker(analyticsTracker: AnalyticsTrackerWrapper): () -> Unit =
        { analyticsTracker.track(AnalyticsEvent.SIGNED_OUT_ALERT_SHOWN) }

    @Provides
    @CachedTokenedOkHttpClient
    @Singleton
    internal fun provideOkHttpClientTokenedCache(okHttpClientBuilder: OkHttpClient.Builder, @TokenInterceptor tokenInterceptor: Interceptor): OkHttpClient {
        return okHttpClientBuilder.addInterceptor(tokenInterceptor).build()
    }

    @Provides
    @ShowNotesCache
    @Singleton
    internal fun provideOkHttpShowNotesCache(@ApplicationContext context: Context): OkHttpClient {
        return getShowNotesClient(context)
    }

    @Provides
    @NoCacheOkHttpClientBuilder
    @Singleton
    internal fun provideOkHttpClientNoCacheBuilder(): OkHttpClient.Builder {
        val dispatcher = Dispatcher()
        dispatcher.maxRequestsPerHost = 5
        var builder = OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .addNetworkInterceptor(INTERCEPTOR_USER_AGENT)
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)

        if (BuildConfig.DEBUG) {
            val logging = HttpLoggingInterceptor()
            logging.level = HttpLoggingInterceptor.Level.BODY
            builder = builder.addInterceptor(logging)
        }

        return builder
    }

    @Provides
    @NoCacheOkHttpClient
    @Singleton
    internal fun provideOkHttpClientNoCache(@NoCacheOkHttpClientBuilder builder: OkHttpClient.Builder): OkHttpClient {
        return builder.build()
    }

    @Provides
    @NoCacheTokenedOkHttpClient
    @Singleton
    internal fun provideOkHttpClientNoCacheTokened(@NoCacheOkHttpClientBuilder builder: OkHttpClient.Builder, @TokenInterceptor tokenInterceptor: Interceptor): OkHttpClient {
        return builder.addInterceptor(tokenInterceptor).build()
    }

    @Provides
    @SyncServerRetrofit
    @Singleton
    internal fun provideApiRetrofit(@CachedOkHttpClient okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .baseUrl(Settings.SERVER_API_URL)
            .client(okHttpClient)
            .build()
    }

    @Provides
    @WpComServerRetrofit
    @Singleton
    internal fun provideWpComApiRetrofit(@CachedOkHttpClient okHttpClient: OkHttpClient): Retrofit {
        val moshi = Moshi.Builder()
            .add(AnonymousBumpStat.Adapter)
            .build()

        return Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .baseUrl(Settings.WP_COM_API_URL)
            .client(okHttpClient)
            .build()
    }

    @Provides
    @RefreshServerRetrofit
    @Singleton
    internal fun provideRefreshRetrofit(@NoCacheTokenedOkHttpClient okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .baseUrl(Settings.SERVER_MAIN_URL)
            .client(okHttpClient)
            .build()
    }

    @Provides
    @PodcastCacheServerRetrofit
    @Singleton
    internal fun providePodcastRetrofit(@CachedTokenedOkHttpClient okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .baseUrl(Settings.SERVER_CACHE_URL)
            .client(okHttpClient)
            .build()
    }

    @Provides
    @OldSyncServerRetrofit
    @Singleton
    internal fun provideSyncOldRetrofit(@NoCacheOkHttpClient okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .baseUrl(Settings.SERVER_API_URL)
            .client(okHttpClient)
            .build()
    }

    @Provides
    @StaticServerRetrofit
    @Singleton
    internal fun provideStaticRetrofit(@CachedOkHttpClient okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .baseUrl(Settings.SERVER_STATIC_URL)
            .client(okHttpClient)
            .build()
    }

    @Provides
    @ListDownloadServerRetrofit
    @Singleton
    internal fun provideListDownloadRetrofit(@NoCacheOkHttpClient okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .baseUrl(Settings.SERVER_LIST_URL)
            .client(okHttpClient)
            .build()
    }

    @Provides
    @ListUploadServerRetrofit
    @Singleton
    internal fun provideListUploadRetrofit(@NoCacheOkHttpClient okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .baseUrl(Settings.SERVER_SHARING_URL)
            .client(okHttpClient)
            .build()
    }

    @Provides
    @DiscoverServerRetrofit
    @Singleton
    internal fun provideRetrofit(@CachedOkHttpClient okHttpClient: OkHttpClient, moshiBuilder: Moshi.Builder): Retrofit {
        moshiBuilder.add(ListTypeMoshiAdapter())
        moshiBuilder.add(DisplayStyleMoshiAdapter())
        moshiBuilder.add(ExpandedStyleMoshiAdapter())
        return Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshiBuilder.build()))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
            .baseUrl(Settings.SERVER_STATIC_URL)
            .client(okHttpClient)
            .build()
    }

    @Provides
    @Singleton
    internal fun provideListWebService(@DiscoverServerRetrofit retrofit: Retrofit): ListWebService {
        return retrofit.create(ListWebService::class.java)
    }

    @Provides
    @Singleton
    internal fun provideDiscoverRepository(listWebService: ListWebService, @ApplicationContext context: Context): ListRepository {
        val platform = if (Util.isAutomotive(context)) "automotive" else "android"
        return ListRepository(
            listWebService,
            platform
        )
    }
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SyncServerCache

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ShowNotesCache

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CachedOkHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NoCacheOkHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NoCacheTokenedOkHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CachedTokenedOkHttpClient

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DiscoverServerRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PodcastCacheServerRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RefreshServerRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class StaticServerRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ListDownloadServerRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ListUploadServerRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SyncServerRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OldSyncServerRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WpComServerRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NoCacheOkHttpClientBuilder

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TokenInterceptor

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class OnTokenErrorUiShown

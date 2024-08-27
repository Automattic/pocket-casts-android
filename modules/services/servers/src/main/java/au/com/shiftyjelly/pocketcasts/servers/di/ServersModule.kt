package au.com.shiftyjelly.pocketcasts.servers.di

import android.accounts.AccountManager
import android.content.Context
import au.com.shiftyjelly.pocketcasts.models.entity.AnonymousBumpStat
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatusMoshiAdapter
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortTypeMoshiAdapter
import au.com.shiftyjelly.pocketcasts.preferences.AccessToken
import au.com.shiftyjelly.pocketcasts.preferences.RefreshToken
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.servers.OkHttpInterceptor
import au.com.shiftyjelly.pocketcasts.servers.adapters.InstantAdapter
import au.com.shiftyjelly.pocketcasts.servers.addInterceptors
import au.com.shiftyjelly.pocketcasts.servers.model.DisplayStyleMoshiAdapter
import au.com.shiftyjelly.pocketcasts.servers.model.ExpandedStyleMoshiAdapter
import au.com.shiftyjelly.pocketcasts.servers.model.ListTypeMoshiAdapter
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServer
import au.com.shiftyjelly.pocketcasts.servers.podcast.TranscriptCacheServer
import au.com.shiftyjelly.pocketcasts.servers.server.ListRepository
import au.com.shiftyjelly.pocketcasts.servers.server.ListWebService
import au.com.shiftyjelly.pocketcasts.servers.sync.LoginIdentity
import au.com.shiftyjelly.pocketcasts.servers.sync.update.SyncUpdateResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.update.SyncUpdateResponseParser
import au.com.shiftyjelly.pocketcasts.utils.Util
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton
import okhttp3.Cache
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.adapter.rxjava2.RxJava2CallAdapterFactory
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.converter.protobuf.ProtoConverterFactory
import retrofit2.create

@Module
@InstallIn(SingletonComponent::class)
class ServersModule {
    companion object {
        fun createCache(folder: String, context: Context, cacheSizeInMB: Int): Cache {
            val cacheSize = cacheSizeInMB * 1024 * 1024
            val cacheDirectory = File(context.cacheDir.absolutePath, folder)
            return Cache(cacheDirectory, cacheSize.toLong())
        }

        fun provideRetrofit(baseUrl: String, okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
            return Retrofit.Builder()
                .addConverterFactory(ProtoConverterFactory.create())
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .addCallAdapterFactory(RxJava2CallAdapterFactory.create())
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .build()
        }
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(InstantAdapter())
            .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
            .add(SyncUpdateResponse::class.java, SyncUpdateResponseParser())
            .add(EpisodePlayingStatus::class.java, EpisodePlayingStatusMoshiAdapter())
            .add(PodcastsSortType::class.java, PodcastsSortTypeMoshiAdapter())
            .add(AccessToken::class.java, AccessToken.Adapter)
            .add(RefreshToken::class.java, RefreshToken.Adapter)
            .add(AnonymousBumpStat.Adapter)
            .add(LoginIdentity.Adapter)
            .add(ListTypeMoshiAdapter())
            .add(DisplayStyleMoshiAdapter())
            .add(ExpandedStyleMoshiAdapter())
            .build()
    }

    @Provides
    @Singleton
    @Raw
    fun provideRawClient(): OkHttpClient {
        return OkHttpClient()
    }

    @Provides
    @Cached
    fun provideCachedCache(@ApplicationContext context: Context): Cache {
        return createCache(folder = "HttpCache", context = context, cacheSizeInMB = 10)
    }

    @Provides
    @Singleton
    @Cached
    fun provideCachedClient(
        @Raw client: OkHttpClient,
        @Cached cache: Cache,
        @Cached interceptors: List<@JvmSuppressWildcards OkHttpInterceptor>,
    ): OkHttpClient {
        return client.newBuilder()
            .cache(cache)
            .addInterceptors(interceptors)
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @NoCache
    fun provideNoCacheClient(
        @Raw client: OkHttpClient,
        @NoCache interceptors: List<@JvmSuppressWildcards OkHttpInterceptor>,
    ): OkHttpClient {
        return client.newBuilder()
            .addInterceptors(interceptors)
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @NoCacheTokened
    fun provideNoCacheTokenedClient(
        @Raw client: OkHttpClient,
        @NoCacheTokened interceptors: List<@JvmSuppressWildcards OkHttpInterceptor>,
    ): OkHttpClient {
        return client.newBuilder()
            .addInterceptors(interceptors)
            .connectTimeout(60, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Downloads
    fun provideDownloadsClient(
        @Raw client: OkHttpClient,
        @Downloads interceptors: List<@JvmSuppressWildcards OkHttpInterceptor>,
    ): OkHttpClient {
        return client.newBuilder()
            // Use a separate dispatcher for downloads.
            .dispatcher(Dispatcher())
            .addInterceptors(interceptors)
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Transcripts
    fun provideTranscriptCache(@ApplicationContext context: Context): Cache {
        return createCache(folder = "TranscriptCache", context = context, cacheSizeInMB = 50)
    }

    @Provides
    @Singleton
    @Transcripts
    internal fun provideTranscriptsClient(
        @Raw client: OkHttpClient,
        @Transcripts cache: Cache,
        @Transcripts interceptors: List<@JvmSuppressWildcards OkHttpInterceptor>,
    ): OkHttpClient {
        return client.newBuilder()
            .cache(cache)
            .addInterceptors(interceptors)
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .cache(cache)
            .build()
    }

    @Provides
    @SyncServerRetrofit
    @Singleton
    internal fun provideApiRetrofit(@Cached okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return provideRetrofit(baseUrl = Settings.SERVER_API_URL, okHttpClient = okHttpClient, moshi = moshi)
    }

    @Provides
    @WpComServerRetrofit
    @Singleton
    internal fun provideWpComApiRetrofit(@Cached okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return Retrofit.Builder()
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .baseUrl(Settings.WP_COM_API_URL)
            .client(okHttpClient)
            .build()
    }

    @Provides
    @RefreshServerRetrofit
    @Singleton
    internal fun provideRefreshRetrofit(@NoCacheTokened okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return provideRetrofit(baseUrl = Settings.SERVER_MAIN_URL, okHttpClient = okHttpClient, moshi = moshi)
    }

    @Provides
    @PodcastCacheServerRetrofit
    @Singleton
    internal fun providePodcastRetrofit(@Cached okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return provideRetrofit(baseUrl = Settings.SERVER_CACHE_URL, okHttpClient = okHttpClient, moshi = moshi)
    }

    @Provides
    @StaticServerRetrofit
    @Singleton
    internal fun provideStaticRetrofit(@Cached okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return provideRetrofit(baseUrl = Settings.SERVER_STATIC_URL, okHttpClient = okHttpClient, moshi = moshi)
    }

    @Provides
    @ListDownloadServerRetrofit
    @Singleton
    internal fun provideListDownloadRetrofit(@NoCache okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return provideRetrofit(baseUrl = Settings.SERVER_LIST_URL, okHttpClient = okHttpClient, moshi = moshi)
    }

    @Provides
    @ListUploadServerRetrofit
    @Singleton
    internal fun provideListUploadRetrofit(@NoCache okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return provideRetrofit(baseUrl = Settings.SERVER_SHARING_URL, okHttpClient = okHttpClient, moshi = moshi)
    }

    @Provides
    @DiscoverServerRetrofit
    @Singleton
    internal fun provideDiscoverRetrofit(@Cached okHttpClient: OkHttpClient, moshi: Moshi): Retrofit {
        return provideRetrofit(baseUrl = Settings.SERVER_STATIC_URL, okHttpClient = okHttpClient, moshi = moshi)
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
            platform,
        )
    }

    @Provides
    @TranscriptRetrofit
    @Singleton
    internal fun provideTranscriptRetrofit(@Transcripts okHttpClient: OkHttpClient): Retrofit {
        return Retrofit.Builder()
            .client(okHttpClient)
            .baseUrl("http://localhost/") // Base URL is required but will be set using the annotation @Url
            .build()
    }

    @Provides
    @Singleton
    internal fun provideAccountManager(@ApplicationContext context: Context): AccountManager {
        return AccountManager.get(context)
    }

    @Provides
    @Singleton
    fun provideCacheServer(@PodcastCacheServerRetrofit retrofit: Retrofit): PodcastCacheServer = retrofit.create()

    @Provides
    @Singleton
    fun provideTranscriptCacheServer(@TranscriptRetrofit retrofit: Retrofit): TranscriptCacheServer = retrofit.create()
}

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Raw

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Cached

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NoCache

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NoCacheTokened

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Downloads

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TokenInterceptor

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Transcripts

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SyncServerRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WpComServerRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RefreshServerRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PodcastCacheServerRetrofit

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
annotation class DiscoverServerRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TranscriptRetrofit

package au.com.shiftyjelly.pocketcasts.servers.di

import android.content.Context
import au.com.shiftyjelly.pocketcasts.models.entity.AnonymousBumpStat
import au.com.shiftyjelly.pocketcasts.models.type.BlazeAdLocation
import au.com.shiftyjelly.pocketcasts.models.type.BlazeAdLocationMoshiAdapter
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatusMoshiAdapter
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortTypeMoshiAdapter
import au.com.shiftyjelly.pocketcasts.preferences.AccessToken
import au.com.shiftyjelly.pocketcasts.preferences.RefreshToken
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.servers.OkHttpInterceptor
import au.com.shiftyjelly.pocketcasts.servers.adapters.ExecutorEnqueueAdapterFactory
import au.com.shiftyjelly.pocketcasts.servers.adapters.InstantAdapter
import au.com.shiftyjelly.pocketcasts.servers.addInterceptors
import au.com.shiftyjelly.pocketcasts.servers.bumpstats.WpComService
import au.com.shiftyjelly.pocketcasts.servers.cdn.StaticService
import au.com.shiftyjelly.pocketcasts.servers.list.ListDownloadService
import au.com.shiftyjelly.pocketcasts.servers.list.ListUploadService
import au.com.shiftyjelly.pocketcasts.servers.model.DisplayStyleMoshiAdapter
import au.com.shiftyjelly.pocketcasts.servers.model.ExpandedStyleMoshiAdapter
import au.com.shiftyjelly.pocketcasts.servers.model.ListTypeMoshiAdapter
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheService
import au.com.shiftyjelly.pocketcasts.servers.podcast.TranscriptService
import au.com.shiftyjelly.pocketcasts.servers.refresh.RefreshService
import au.com.shiftyjelly.pocketcasts.servers.search.AutoCompleteResult
import au.com.shiftyjelly.pocketcasts.servers.search.AutoCompleteSearchService
import au.com.shiftyjelly.pocketcasts.servers.search.CombinedResult
import au.com.shiftyjelly.pocketcasts.servers.server.ListWebService
import au.com.shiftyjelly.pocketcasts.servers.sync.LoginIdentity
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncService
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapters.Rfc3339DateJsonAdapter
import dagger.Lazy
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.reactivex.schedulers.Schedulers
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
class NetworkModule {
    companion object {
        fun createCache(folder: String, context: Context, cacheSizeInMB: Int): Cache {
            val cacheSize = cacheSizeInMB * 1024 * 1024
            val cacheDirectory = File(context.cacheDir.absolutePath, folder)
            return Cache(cacheDirectory, cacheSize.toLong())
        }
    }

    @Provides
    @Singleton
    fun provideDispatcher(): Dispatcher = Dispatcher()

    @Provides
    @Singleton
    @Cached
    fun provideCachedCache(@ApplicationContext context: Context): Cache {
        return createCache(folder = "HttpCache", context = context, cacheSizeInMB = 10)
    }

    @Provides
    @Singleton
    @Transcripts
    fun provideTranscriptCache(@ApplicationContext context: Context): Cache {
        return createCache(folder = "TranscriptCache", context = context, cacheSizeInMB = 50)
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(InstantAdapter())
            .add(Date::class.java, Rfc3339DateJsonAdapter().nullSafe())
            .add(EpisodePlayingStatus::class.java, EpisodePlayingStatusMoshiAdapter())
            .add(PodcastsSortType::class.java, PodcastsSortTypeMoshiAdapter())
            .add(AccessToken::class.java, AccessToken.Adapter)
            .add(RefreshToken::class.java, RefreshToken.Adapter)
            .add(BlazeAdLocation::class.java, BlazeAdLocationMoshiAdapter())
            .add(AutoCompleteResult.jsonAdapter)
            .add(CombinedResult.jsonAdapter)
            .add(AnonymousBumpStat.Adapter)
            .add(LoginIdentity.Adapter)
            .add(ListTypeMoshiAdapter())
            .add(DisplayStyleMoshiAdapter())
            .add(ExpandedStyleMoshiAdapter())
            .build()
    }

    @Provides
    fun provideRetrofitBuilder(moshi: Moshi, dispatcher: Dispatcher): Retrofit.Builder {
        return Retrofit.Builder()
            .addConverterFactory(ProtoConverterFactory.create())
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .addCallAdapterFactory(ExecutorEnqueueAdapterFactory(dispatcher.executorService))
            .addCallAdapterFactory(RxJava2CallAdapterFactory.createWithScheduler(Schedulers.from(dispatcher.executorService)))
    }

    @Provides
    @Singleton
    @Raw
    fun provideRawClient(dispatcher: Dispatcher): OkHttpClient {
        return OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .build()
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
            .addInterceptors(interceptors)
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Transcripts
    fun provideTranscriptsClient(
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
    @Singleton
    @Player
    fun providePlayerClient(
        @Raw client: OkHttpClient,
        @Player interceptors: List<@JvmSuppressWildcards OkHttpInterceptor>,
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
    @Artwork
    fun provideArtworkClient(
        @Raw client: OkHttpClient,
        @Artwork interceptors: List<@JvmSuppressWildcards OkHttpInterceptor>,
    ): OkHttpClient {
        return client.newBuilder()
            .addInterceptors(interceptors)
            .build()
    }

    @Provides
    @SyncServiceRetrofit
    @Singleton
    fun provideApiRetrofit(
        builder: Retrofit.Builder,
        @Cached httpClient: Lazy<OkHttpClient>,
    ): Retrofit {
        return builder
            .baseUrl(Settings.SERVER_API_URL)
            .callFactory { request -> httpClient.get().newCall(request) }
            .build()
    }

    @Provides
    @WpComServiceRetrofit
    @Singleton
    fun provideWpComApiRetrofit(
        builder: Retrofit.Builder,
        @Cached httpClient: Lazy<OkHttpClient>,
    ): Retrofit {
        return builder
            .baseUrl(Settings.WP_COM_API_URL)
            .callFactory { request -> httpClient.get().newCall(request) }
            .build()
    }

    @Provides
    @RefreshServiceRetrofit
    @Singleton
    fun provideRefreshRetrofit(
        builder: Retrofit.Builder,
        @NoCacheTokened httpClient: Lazy<OkHttpClient>,
    ): Retrofit {
        return builder
            .baseUrl(Settings.SERVER_MAIN_URL)
            .callFactory { request -> httpClient.get().newCall(request) }
            .build()
    }

    @Provides
    @PodcastCacheServiceRetrofit
    @Singleton
    fun providePodcastRetrofit(
        builder: Retrofit.Builder,
        @Cached httpClient: Lazy<OkHttpClient>,
    ): Retrofit {
        return builder
            .baseUrl(Settings.SERVER_CACHE_URL)
            .callFactory { request -> httpClient.get().newCall(request) }
            .build()
    }

    @Provides
    @StaticServiceRetrofit
    @Singleton
    fun provideStaticRetrofit(
        builder: Retrofit.Builder,
        @Cached httpClient: Lazy<OkHttpClient>,
    ): Retrofit {
        return builder
            .baseUrl(Settings.SERVER_STATIC_URL)
            .callFactory { request -> httpClient.get().newCall(request) }
            .build()
    }

    @Provides
    @ListDownloadServiceRetrofit
    @Singleton
    fun provideListDownloadRetrofit(
        builder: Retrofit.Builder,
        @NoCache httpClient: Lazy<OkHttpClient>,
    ): Retrofit {
        return builder
            .baseUrl(Settings.SERVER_LIST_URL)
            .callFactory { request -> httpClient.get().newCall(request) }
            .build()
    }

    @Provides
    @ListUploadServiceRetrofit
    @Singleton
    fun provideListUploadRetrofit(
        builder: Retrofit.Builder,
        @NoCache httpClient: Lazy<OkHttpClient>,
    ): Retrofit {
        return builder
            .baseUrl(Settings.SERVER_SHARING_URL)
            .callFactory { request -> httpClient.get().newCall(request) }
            .build()
    }

    @Provides
    @DiscoverServiceRetrofit
    @Singleton
    fun provideDiscoverRetrofit(
        builder: Retrofit.Builder,
        @Cached httpClient: Lazy<OkHttpClient>,
    ): Retrofit {
        return builder
            .baseUrl(Settings.SERVER_STATIC_URL)
            .callFactory { request -> httpClient.get().newCall(request) }
            .build()
    }

    @Provides
    @TranscriptRetrofit
    @Singleton
    fun provideTranscriptRetrofit(
        builder: Retrofit.Builder,
        @Transcripts httpClient: Lazy<OkHttpClient>,
    ): Retrofit {
        return builder
            .baseUrl("http://localhost/") // Base URL is required but will be set using the annotation @Url
            .callFactory { request -> httpClient.get().newCall(request) }
            .build()
    }

    @Provides
    @Singleton
    @SearchRetrofit
    fun provideSearchApiRetrofit(
        builder: Retrofit.Builder,
        @Cached httpClient: Lazy<OkHttpClient>,
    ): Retrofit {
        return builder
            .baseUrl(Settings.SEARCH_API_URL)
            .callFactory { request -> httpClient.get().newCall(request) }
            .build()
    }

    @Provides
    @Singleton
    fun provideListWebService(@DiscoverServiceRetrofit retrofit: Retrofit): ListWebService = retrofit.create()

    @Singleton
    @Provides
    fun provideAutoCompleteSearchService(@SearchRetrofit retrofit: Retrofit): AutoCompleteSearchService = retrofit.create()

    @Provides
    @Singleton
    fun provideCacheService(@PodcastCacheServiceRetrofit retrofit: Retrofit): PodcastCacheService = retrofit.create()

    @Provides
    @Singleton
    fun provideTranscriptCacheService(@TranscriptRetrofit retrofit: Retrofit): TranscriptService = retrofit.create()

    @Provides
    @Singleton
    fun provideStaticService(@StaticServiceRetrofit retrofit: Retrofit): StaticService = retrofit.create()

    @Provides
    @Singleton
    fun provideListUploadService(@ListUploadServiceRetrofit retrofit: Retrofit): ListUploadService = retrofit.create()

    @Provides
    @Singleton
    fun provideListDownloadService(@ListDownloadServiceRetrofit retrofit: Retrofit): ListDownloadService = retrofit.create()

    @Provides
    @Singleton
    fun provideRefreshService(@RefreshServiceRetrofit retrofit: Retrofit): RefreshService = retrofit.create()

    @Provides
    @Singleton
    fun provideWpComService(@WpComServiceRetrofit retrofit: Retrofit): WpComService = retrofit.create()

    @Provides
    @Singleton
    fun provideSyncService(@SyncServiceRetrofit retrofit: Retrofit): SyncService = retrofit.create()
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
annotation class I18nInterceptor

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Transcripts

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Player

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class Artwork

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SyncServiceRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class WpComServiceRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class RefreshServiceRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class PodcastCacheServiceRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class StaticServiceRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ListDownloadServiceRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ListUploadServiceRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DiscoverServiceRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class TranscriptRetrofit

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SearchRetrofit

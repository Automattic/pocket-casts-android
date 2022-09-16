package au.com.shiftyjelly.pocketcasts.wear.di

import android.content.Context
import com.google.android.horologist.networks.data.DataRequestRepository
import com.google.android.horologist.networks.data.InMemoryDataRequestRepository
import com.google.android.horologist.networks.logging.NetworkStatusLogger
import com.google.android.horologist.networks.okhttp.impl.NetworkLoggingEventListenerFactory
import com.google.android.horologist.networks.status.NetworkRepository
import com.google.android.horologist.networks.status.NetworkRepositoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineScope
import okhttp3.Cache
import okhttp3.Call
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.LoggingEventListener
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {
    @Singleton
    @Provides
    fun networkRepository(
        @ApplicationContext application: Context,
        @ForApplicationScope coroutineScope: CoroutineScope
    ): NetworkRepository = NetworkRepositoryImpl.fromContext(
        application,
        coroutineScope
    )

    @Singleton
    @Provides
    fun cache(
        @ApplicationContext application: Context
    ): Cache = Cache(
        application.cacheDir.resolve("HttpCache"),
        10_000_000
    )

    @Singleton
    @Provides
    fun alwaysHttpsInterceptor(): Interceptor = Interceptor {
        var request = it.request()

        if (request.url.scheme == "http") {
            request = request.newBuilder().url(
                request.url.newBuilder().scheme("https").build()
            ).build()
        }

        it.proceed(request)
    }

    @Singleton
    @Provides
    fun okhttpClient(
        cache: Cache,
        alwaysHttpsInterceptor: Interceptor
    ): OkHttpClient {
        return OkHttpClient.Builder().followSslRedirects(false)
            .addInterceptor(alwaysHttpsInterceptor)
            .eventListenerFactory(LoggingEventListener.Factory()).cache(cache).build()
    }

    @Provides
    fun networkLogger(): NetworkStatusLogger = NetworkStatusLogger.Logging

    @Singleton
    @Provides
    fun dataRequestRepository(): DataRequestRepository =
        InMemoryDataRequestRepository()

    @Singleton
    @Provides
    fun networkAwareCallFactory(
        /*appConfig: AppConfig,*/
        okhttpClient: OkHttpClient,
        /*networkingRulesEngine: Provider<NetworkingRulesEngine>,
        highBandwidthNetworkMediator: Provider<HighBandwidthNetworkMediator>,*/
        dataRequestRepository: DataRequestRepository,
        networkRepository: NetworkRepository,
        /*@ForApplicationScope coroutineScope: CoroutineScope,*/
        logger: NetworkStatusLogger,
    ): Call.Factory =
        /*if (appConfig.strictNetworking != null) {
            NetworkSelectingCallFactory(
                networkingRulesEngine.get(),
                highBandwidthNetworkMediator.get(),
                networkRepository,
                dataRequestRepository,
                okhttpClient,
                coroutineScope
            )
        } else {*/
        okhttpClient.newBuilder()
            .eventListenerFactory(
                NetworkLoggingEventListenerFactory(
                    logger,
                    networkRepository,
                    okhttpClient.eventListenerFactory,
                    dataRequestRepository
                )
            )
            .build()
    /*}*/
}

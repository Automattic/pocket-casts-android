package au.com.shiftyjelly.pocketcasts.wear.di

import android.content.Context
import android.net.ConnectivityManager
import au.com.shiftyjelly.pocketcasts.repositories.di.DownloadPhoneOkHttpClient
import au.com.shiftyjelly.pocketcasts.repositories.di.DownloadWearCallFactory
import au.com.shiftyjelly.pocketcasts.repositories.di.DownloadWearRequestBuilder
import au.com.shiftyjelly.pocketcasts.wear.networking.PocketCastsNetworkingRules
import com.google.android.horologist.networks.data.DataRequestRepository
import com.google.android.horologist.networks.data.InMemoryDataRequestRepository
import com.google.android.horologist.networks.data.RequestType
import com.google.android.horologist.networks.highbandwidth.HighBandwidthNetworkMediator
import com.google.android.horologist.networks.highbandwidth.StandardHighBandwidthNetworkMediator
import com.google.android.horologist.networks.logging.NetworkStatusLogger
import com.google.android.horologist.networks.okhttp.NetworkSelectingCallFactory
import com.google.android.horologist.networks.okhttp.impl.NetworkLoggingEventListenerFactory
import com.google.android.horologist.networks.okhttp.impl.RequestTypeHolder.Companion.requestType
import com.google.android.horologist.networks.request.NetworkRequesterImpl
import com.google.android.horologist.networks.rules.NetworkingRules
import com.google.android.horologist.networks.rules.NetworkingRulesEngine
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
import okhttp3.Request
import okhttp3.logging.LoggingEventListener
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

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

    @Provides
    @Singleton
    fun provideNetworkingRules(): NetworkingRules = PocketCastsNetworkingRules

    @Provides
    @Singleton
    fun provideNetworkingRulesEngine(
        networkRepository: NetworkRepository,
        networkingRules: NetworkingRules,
    ): NetworkingRulesEngine =
        NetworkingRulesEngine(
            networkRepository = networkRepository,
            logger = NetworkStatusLogger.Logging,
            networkingRules = networkingRules,
        )

    @Provides
    @Singleton
    fun provideHighBandwidthNetworkMediator(
        connectivityManager: ConnectivityManager,
        @ForApplicationScope coroutineScope: CoroutineScope,
    ): HighBandwidthNetworkMediator =
        StandardHighBandwidthNetworkMediator(
            logger = NetworkStatusLogger.Logging,
            networkRequester = NetworkRequesterImpl(connectivityManager),
            coroutineScope = coroutineScope,
            delayToRelease = 3.seconds,
        )

    @Provides
    @Singleton
    @DownloadWearCallFactory
    fun provideDownloadWearCallFactory(
        highBandwidthNetworkMediator: HighBandwidthNetworkMediator,
        networkRepository: NetworkRepository,
        networkingRulesEngine: NetworkingRulesEngine,
        @DownloadPhoneOkHttpClient phoneCallFactory: OkHttpClient,
        @ForApplicationScope coroutineScope: CoroutineScope,
    ): Call.Factory {

        return NetworkSelectingCallFactory(
            networkingRulesEngine = networkingRulesEngine,
            highBandwidthNetworkMediator = highBandwidthNetworkMediator,
            networkRepository = networkRepository,
            dataRequestRepository = null,
            rootClient = phoneCallFactory,
            coroutineScope = coroutineScope,
            timeout = 5.seconds,
        )
    }

    @Provides
    @DownloadWearRequestBuilder
    fun downloadRequestBuilder(): Request.Builder =
        Request.Builder()
            .requestType(RequestType.MediaRequest.DownloadRequest)

    // FIXME update the provide methods below this point

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
                    NetworkStatusLogger.Logging,
                    networkRepository,
                    okhttpClient.eventListenerFactory,
                    dataRequestRepository
                )
            )
            .build()
    /*}*/
}

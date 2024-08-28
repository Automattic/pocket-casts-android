package au.com.shiftyjelly.pocketcasts.wear.di

import android.content.Context
import android.net.ConnectivityManager
import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.servers.di.Downloads
import au.com.shiftyjelly.pocketcasts.wear.networking.PocketCastsNetworkingRules
import com.google.android.horologist.networks.data.RequestType
import com.google.android.horologist.networks.highbandwidth.HighBandwidthNetworkMediator
import com.google.android.horologist.networks.highbandwidth.StandardHighBandwidthNetworkMediator
import com.google.android.horologist.networks.logging.NetworkStatusLogger
import com.google.android.horologist.networks.okhttp.NetworkSelectingCallFactory
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
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import okhttp3.Call
import okhttp3.OkHttpClient
import okhttp3.Request

@Module
@InstallIn(SingletonComponent::class)
object WearNetworkModule {

    @Singleton
    @Provides
    fun networkRepository(
        @ApplicationContext application: Context,
        @ApplicationScope coroutineScope: CoroutineScope,
    ): NetworkRepository = NetworkRepositoryImpl.fromContext(
        application,
        coroutineScope,
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
        @ApplicationScope coroutineScope: CoroutineScope,
    ): HighBandwidthNetworkMediator =
        StandardHighBandwidthNetworkMediator(
            logger = NetworkStatusLogger.Logging,
            networkRequester = NetworkRequesterImpl(connectivityManager),
            coroutineScope = coroutineScope,
            delayToRelease = 3.seconds,
        )

    @Provides
    fun provideNetworkLogger(): NetworkStatusLogger = NetworkStatusLogger.Logging

    @Provides
    @Singleton
    @Downloads
    fun provideDownloadWearCallFactory(
        highBandwidthNetworkMediator: HighBandwidthNetworkMediator,
        networkRepository: NetworkRepository,
        networkingRulesEngine: NetworkingRulesEngine,
        @Downloads client: OkHttpClient,
        @ApplicationScope coroutineScope: CoroutineScope,
        logger: NetworkStatusLogger,
    ): Call.Factory {
        return NetworkSelectingCallFactory(
            networkingRulesEngine = networkingRulesEngine,
            highBandwidthNetworkMediator = highBandwidthNetworkMediator,
            networkRepository = networkRepository,
            dataRequestRepository = null,
            rootClient = client,
            coroutineScope = coroutineScope,
            timeout = 5.seconds,
            logger = logger,
        )
    }

    @Provides
    @Downloads
    fun downloadRequestBuilder(): Request.Builder {
        return Request.Builder().requestType(RequestType.MediaRequest.DownloadRequest)
    }
}

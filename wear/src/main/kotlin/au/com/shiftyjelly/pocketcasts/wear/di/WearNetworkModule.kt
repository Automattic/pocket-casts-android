package au.com.shiftyjelly.pocketcasts.wear.di

import android.content.Context
import android.net.ConnectivityManager
import au.com.shiftyjelly.pocketcasts.servers.di.HorologistNetworkAwarenessWrapper
import au.com.shiftyjelly.pocketcasts.servers.di.PCRequestType
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.wear.networking.PocketCastsNetworkingRules
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.networks.data.RequestType
import com.google.android.horologist.networks.highbandwidth.HighBandwidthNetworkMediator
import com.google.android.horologist.networks.highbandwidth.StandardHighBandwidthNetworkMediator
import com.google.android.horologist.networks.logging.NetworkStatusLogger
import com.google.android.horologist.networks.okhttp.NetworkAwareCallFactory
import com.google.android.horologist.networks.okhttp.NetworkSelectingCallFactory
import com.google.android.horologist.networks.okhttp.impl.RequestTypeHolder.Companion.requestType
import com.google.android.horologist.networks.okhttp.requestType
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
import okhttp3.OkHttpClient
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds

@Module
@InstallIn(SingletonComponent::class)
object WearNetworkModule {

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
    fun provideNetworkLogger(): NetworkStatusLogger = NetworkStatusLogger.Logging

    @OptIn(ExperimentalHorologistApi::class)
    @Provides
    @Singleton
    fun provideCallFactoryWrapper(
        @ApplicationContext context: Context,
        @au.com.shiftyjelly.pocketcasts.servers.di.ForApplicationScope coroutineScope: CoroutineScope,
        highBandwidthNetworkMediator: HighBandwidthNetworkMediator,
        logger: NetworkStatusLogger,
        networkRepository: NetworkRepository,
        networkingRulesEngine: NetworkingRulesEngine,
    ): HorologistNetworkAwarenessWrapper = object : HorologistNetworkAwarenessWrapper {
        override fun wrap(okHttpClient: OkHttpClient, requestType: PCRequestType) =
            if (Util.isWearOs(context)) {

                val networkSelectingCallFactory = NetworkSelectingCallFactory(
                    networkingRulesEngine = networkingRulesEngine,
                    highBandwidthNetworkMediator = highBandwidthNetworkMediator,
                    networkRepository = networkRepository,
                    dataRequestRepository = null,
                    rootClient = okHttpClient,
                    coroutineScope = coroutineScope,
                    timeout = 5.seconds,
                    logger = logger,
                )

                NetworkAwareCallFactory(
                    delegate = networkSelectingCallFactory,
                    defaultRequestType = requestType.toRequestType(),
                )
            } else {
                okHttpClient
            }
    }

    private fun PCRequestType.toRequestType() = when (this) {
        PCRequestType.Download -> RequestType.MediaRequest.DownloadRequest
        PCRequestType.Api -> RequestType.ApiRequest
    }
}

package au.com.shiftyjelly.pocketcasts.repositories.di

import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncAccountManager
import au.com.shiftyjelly.pocketcasts.servers.di.HorologistNetworkAwarenessWrapper
import au.com.shiftyjelly.pocketcasts.servers.di.PCRequestType
import au.com.shiftyjelly.pocketcasts.servers.sync.TokenHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import okhttp3.Call
import okhttp3.Dispatcher
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit
import javax.inject.Qualifier
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class RepositoryProviderModule {

    @Provides
    @Singleton
    fun provideTokenHandler(syncAccountManager: SyncAccountManager): TokenHandler = syncAccountManager

    @Provides
    @Singleton
    @DownloadCallFactory
    fun downloadCallFactory(
        networkAwarenessWrapper: HorologistNetworkAwarenessWrapper,
    ): Call.Factory {
        val dispatcher = Dispatcher().apply {
            maxRequestsPerHost = 5
        }
        val client = OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()

        return networkAwarenessWrapper.wrap(client, PCRequestType.Download)
    }
}

/**
 * Annotation for providing the Call.Factory used for downloads.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DownloadCallFactory

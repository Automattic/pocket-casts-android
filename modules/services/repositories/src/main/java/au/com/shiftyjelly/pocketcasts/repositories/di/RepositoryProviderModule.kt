package au.com.shiftyjelly.pocketcasts.repositories.di

import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import au.com.shiftyjelly.pocketcasts.crashlogging.di.ProvideApplicationScope
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncAccountManager
import au.com.shiftyjelly.pocketcasts.servers.di.CrashLoggingInterceptor
import au.com.shiftyjelly.pocketcasts.servers.sync.TokenHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.util.concurrent.TimeUnit
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.Dispatcher
import okhttp3.Interceptor
import okhttp3.OkHttpClient

@Module
@InstallIn(SingletonComponent::class)
class RepositoryProviderModule {

    @Provides
    @Singleton
    fun provideTokenHandler(syncAccountManager: SyncAccountManager): TokenHandler = syncAccountManager

    @Provides
    @Singleton
    @DownloadOkHttpClient
    fun downloadOkHttpClient(@CrashLoggingInterceptor crashLoggingInterceptor: Interceptor): OkHttpClient {
        val dispatcher = Dispatcher().apply {
            maxRequestsPerHost = 5
        }
        return OkHttpClient.Builder()
            .dispatcher(dispatcher)
            .addInterceptor(crashLoggingInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @ApplicationScope
    fun coroutineScope(): CoroutineScope =
        CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    fun provideApplicationScope(
        @ApplicationScope appScope: CoroutineScope,
    ): ProvideApplicationScope = ProvideApplicationScope { appScope }

    @Provides
    @Singleton
    @ProcessLifecycle
    fun processLifecycle(): LifecycleOwner = ProcessLifecycleOwner.get()
}

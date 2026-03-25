package au.com.shiftyjelly.pocketcasts.shared.di

import au.com.shiftyjelly.pocketcasts.crashlogging.ConnectionStatusProvider
import au.com.shiftyjelly.pocketcasts.repositories.playback.AppLifecycleProvider
import au.com.shiftyjelly.pocketcasts.repositories.playback.NetworkConnectionWatcher
import au.com.shiftyjelly.pocketcasts.shared.AppLifecycleProviderImpl
import au.com.shiftyjelly.pocketcasts.shared.NetworkConnectionStatusProvider
import au.com.shiftyjelly.pocketcasts.shared.NetworkConnectionWatcherImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class SharedModule {

    @Binds
    @Singleton
    abstract fun provideNetworkConnectionWatcher(networkWatcherImpl: NetworkConnectionWatcherImpl): NetworkConnectionWatcher

    @Binds
    @Singleton
    abstract fun bindAppLifecycleProvider(impl: AppLifecycleProviderImpl): AppLifecycleProvider

    @Binds
    abstract fun bindConnectionStatusProvider(
        networkConnectionStatusProvider: NetworkConnectionStatusProvider,
    ): ConnectionStatusProvider
}

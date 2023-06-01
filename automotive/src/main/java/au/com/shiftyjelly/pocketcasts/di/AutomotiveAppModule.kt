package au.com.shiftyjelly.pocketcasts.di

import au.com.shiftyjelly.pocketcasts.servers.di.HorologistNetworkAwarenessWrapper
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class AutomotiveAppModule {

    @Provides
    @Singleton
    fun provideCallFactoryWrapper(): HorologistNetworkAwarenessWrapper = HorologistNetworkAwarenessWrapper.noopImpl
}

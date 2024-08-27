package au.com.shiftyjelly.pocketcasts.servers.di

import au.com.shiftyjelly.pocketcasts.servers.cdn.StaticServiceManager
import au.com.shiftyjelly.pocketcasts.servers.cdn.StaticServiceManagerImpl
import au.com.shiftyjelly.pocketcasts.servers.list.ListServiceManager
import au.com.shiftyjelly.pocketcasts.servers.list.ListServiceManagerImpl
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServiceManager
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServiceManagerImpl
import au.com.shiftyjelly.pocketcasts.servers.refresh.RefreshServiceManager
import au.com.shiftyjelly.pocketcasts.servers.refresh.RefreshServiceManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class ServersModuleBinds {

    @Binds
    @Singleton
    abstract fun provideRefreshServiceManager(refreshServiceManagerImpl: RefreshServiceManagerImpl): RefreshServiceManager

    @Binds
    @Singleton
    abstract fun provideStaticManager(staticServiceManagerImpl: StaticServiceManagerImpl): StaticServiceManager

    @Binds
    @Singleton
    abstract fun provideShareServiceManager(shareServiceManagerImpl: ListServiceManagerImpl): ListServiceManager

    @Binds
    @Singleton
    abstract fun providePodcastCacheServiceManager(podcastCacheServiceManagerImpl: PodcastCacheServiceManagerImpl): PodcastCacheServiceManager
}

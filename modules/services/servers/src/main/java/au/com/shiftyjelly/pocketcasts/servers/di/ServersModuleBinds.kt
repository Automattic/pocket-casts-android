package au.com.shiftyjelly.pocketcasts.servers.di

import au.com.shiftyjelly.pocketcasts.servers.account.SyncAccountManager
import au.com.shiftyjelly.pocketcasts.servers.account.SyncAccountManagerImpl
import au.com.shiftyjelly.pocketcasts.servers.cdn.StaticServerManager
import au.com.shiftyjelly.pocketcasts.servers.cdn.StaticServerManagerImpl
import au.com.shiftyjelly.pocketcasts.servers.list.ListServerManager
import au.com.shiftyjelly.pocketcasts.servers.list.ListServerManagerImpl
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServerManager
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServerManagerImpl
import au.com.shiftyjelly.pocketcasts.servers.refresh.RefreshServerManager
import au.com.shiftyjelly.pocketcasts.servers.refresh.RefreshServerManagerImpl
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
    abstract fun provideRefreshServerManager(refreshServerManagerImpl: RefreshServerManagerImpl): RefreshServerManager

    @Binds
    @Singleton
    abstract fun provideStaticManager(staticServerManagerImpl: StaticServerManagerImpl): StaticServerManager

    @Binds
    @Singleton
    abstract fun provideShareServerManager(shareServerManagerImpl: ListServerManagerImpl): ListServerManager

    @Binds
    @Singleton
    abstract fun providePodcastCacheServerManager(podcastCacheServerManagerImpl: PodcastCacheServerManagerImpl): PodcastCacheServerManager

    @Binds
    @Singleton
    abstract fun provideSyncAccountManager(syncAccountManagerImpl: SyncAccountManagerImpl): SyncAccountManager
}

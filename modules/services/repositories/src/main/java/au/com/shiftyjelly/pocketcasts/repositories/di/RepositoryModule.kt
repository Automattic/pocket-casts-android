package au.com.shiftyjelly.pocketcasts.repositories.di

import androidx.work.WorkerFactory
import au.com.shiftyjelly.pocketcasts.analytics.AccountStatusInfo
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.SettingsImpl
import au.com.shiftyjelly.pocketcasts.repositories.chromecast.CastManager
import au.com.shiftyjelly.pocketcasts.repositories.chromecast.CastManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearManager
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationDrawer
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationDrawerImpl
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelper
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelperImpl
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlayerFactory
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlayerFactoryImpl
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueueImpl
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.searchhistory.SearchHistoryManager
import au.com.shiftyjelly.pocketcasts.repositories.searchhistory.SearchHistoryManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.user.StatsManager
import au.com.shiftyjelly.pocketcasts.repositories.user.StatsManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.widget.WidgetManager
import au.com.shiftyjelly.pocketcasts.repositories.widget.WidgetManagerImpl
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun provideWorkerFactory(castsWorkerFactory: CastsWorkerFactory): WorkerFactory

    @Binds
    @Singleton
    abstract fun providesSettings(settingsImpl: SettingsImpl): Settings

    @Binds
    @Singleton
    abstract fun providesPodcastManager(podcastManagerImpl: PodcastManagerImpl): PodcastManager

    @Binds
    @Singleton
    abstract fun providesSubscriptionManager(subscriptionManagerImpl: SubscriptionManagerImpl): SubscriptionManager

    @Binds
    @Singleton
    abstract fun providesFolderManager(folderManagerImpl: FolderManagerImpl): FolderManager

    @Binds
    @Singleton
    abstract fun providesEpisodeManager(episodeManagerImpl: EpisodeManagerImpl): EpisodeManager

    @Binds
    @Singleton
    abstract fun providesPlaylistManager(playlistManagerImpl: PlaylistManagerImpl): PlaylistManager

    @Binds
    @Singleton
    abstract fun providesStatsManager(statsManagerImpl: StatsManagerImpl): StatsManager

    @Binds
    @Singleton
    abstract fun providesUpNextQueue(upNextQueueImpl: UpNextQueueImpl): UpNextQueue

    @Binds
    @Singleton
    abstract fun providesDownloadManager(downloadManagerImpl: DownloadManagerImpl): DownloadManager

    @Binds
    @Singleton
    abstract fun providesPlayerFactory(playerFactoryImpl: PlayerFactoryImpl): PlayerFactory

    @Binds
    @Singleton
    abstract fun providesWidgetManager(widgetManagerImpl: WidgetManagerImpl): WidgetManager

    @Binds
    @Singleton
    abstract fun providesCastManager(castManagerImpl: CastManagerImpl): CastManager

    @Binds
    @Singleton
    abstract fun providesNotificationHelper(notificationHelperImpl: NotificationHelperImpl): NotificationHelper

    @Binds
    @Singleton
    abstract fun providesNotificationDrawer(notificationDrawerImpl: NotificationDrawerImpl): NotificationDrawer

    @Binds
    @Singleton
    abstract fun provideUserManager(userManagerImpl: UserManagerImpl): UserManager

    @Binds
    @Singleton
    abstract fun provideUserEpisodeManager(userEpisodeManagerImpl: UserEpisodeManagerImpl): UserEpisodeManager

    @Binds
    @Singleton
    abstract fun provideEndOfYearManager(endOfYearManagerImpl: EndOfYearManagerImpl): EndOfYearManager

    @Binds
    @Singleton
    abstract fun provideSearchHistoryManager(searchHistoryManagerImpl: SearchHistoryManagerImpl): SearchHistoryManager

    @Binds
    @Singleton
    abstract fun provideSyncManager(syncManagerImpl: SyncManagerImpl): SyncManager

    @Binds
    @Singleton
    abstract fun provideAccountStatusInfo(syncManager: SyncManager): AccountStatusInfo
}

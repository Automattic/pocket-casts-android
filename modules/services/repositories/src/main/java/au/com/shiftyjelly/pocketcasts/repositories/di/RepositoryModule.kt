package au.com.shiftyjelly.pocketcasts.repositories.di

import androidx.work.WorkerFactory
import au.com.shiftyjelly.pocketcasts.analytics.AccountStatusInfo
import au.com.shiftyjelly.pocketcasts.crashlogging.CrashReportPermissionCheck
import au.com.shiftyjelly.pocketcasts.crashlogging.ObserveUser
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptType
import au.com.shiftyjelly.pocketcasts.payment.PurchaseApprover
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.SettingsImpl
import au.com.shiftyjelly.pocketcasts.repositories.ads.BlazeAdsManager
import au.com.shiftyjelly.pocketcasts.repositories.ads.BlazeAdsManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.chromecast.CastManager
import au.com.shiftyjelly.pocketcasts.repositories.chromecast.CastManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearManager
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearSync
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearSyncImpl
import au.com.shiftyjelly.pocketcasts.repositories.history.upnext.UpNextHistoryManager
import au.com.shiftyjelly.pocketcasts.repositories.history.upnext.UpNextHistoryManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationDrawer
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationDrawerImpl
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelper
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationHelperImpl
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationManager
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationScheduler
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationSchedulerImpl
import au.com.shiftyjelly.pocketcasts.repositories.nova.ExternalDataManager
import au.com.shiftyjelly.pocketcasts.repositories.nova.ExternalDataManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlayerFactory
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlayerFactoryImpl
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueueImpl
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.podcast.ChapterManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.ChapterManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.podcast.SmartPlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.SmartPlaylistManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.ratings.RatingsManager
import au.com.shiftyjelly.pocketcasts.repositories.ratings.RatingsManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralManager
import au.com.shiftyjelly.pocketcasts.repositories.referrals.ReferralManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.search.SearchAutoCompleteManager
import au.com.shiftyjelly.pocketcasts.repositories.search.SearchAutoCompleteManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.searchhistory.SearchHistoryManager
import au.com.shiftyjelly.pocketcasts.repositories.searchhistory.SearchHistoryManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.subscription.ServerPurchaseApprover
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.sync.AccountManagerStatusInfo
import au.com.shiftyjelly.pocketcasts.repositories.sync.PodcastRefresher
import au.com.shiftyjelly.pocketcasts.repositories.sync.PodcastRefresherImpl
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncAccountManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncAccountManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.transcript.HtmlParser
import au.com.shiftyjelly.pocketcasts.repositories.transcript.JsonParser
import au.com.shiftyjelly.pocketcasts.repositories.transcript.SrtParser
import au.com.shiftyjelly.pocketcasts.repositories.transcript.TranscriptManager
import au.com.shiftyjelly.pocketcasts.repositories.transcript.TranscriptManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.transcript.TranscriptParser
import au.com.shiftyjelly.pocketcasts.repositories.transcript.WebVttParser
import au.com.shiftyjelly.pocketcasts.repositories.user.ObserveTrackableUser
import au.com.shiftyjelly.pocketcasts.repositories.user.StatsManager
import au.com.shiftyjelly.pocketcasts.repositories.user.StatsManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManagerImpl
import au.com.shiftyjelly.pocketcasts.repositories.user.UserSettingsCrashReportPermission
import com.squareup.moshi.Moshi
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoMap
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
    abstract fun bindAutoCompleteSearchManager(impl: SearchAutoCompleteManagerImpl): SearchAutoCompleteManager

    @Binds
    @Singleton
    abstract fun providesSubscriptionManager(subscriptionManagerImpl: SubscriptionManagerImpl): SubscriptionManager

    @Binds
    @Singleton
    abstract fun providesSyncAccountManager(syncAccountManagerImpl: SyncAccountManagerImpl): SyncAccountManager

    @Binds
    @Singleton
    abstract fun providesAccountStatusInfo(accountManagerStatusInfo: AccountManagerStatusInfo): AccountStatusInfo

    @Binds
    @Singleton
    abstract fun providesFolderManager(folderManagerImpl: FolderManagerImpl): FolderManager

    @Binds
    @Singleton
    abstract fun providesEpisodeManager(episodeManagerImpl: EpisodeManagerImpl): EpisodeManager

    @Binds
    @Singleton
    abstract fun providesSmartPlaylistManager(smartPlaylistManagerImpl: SmartPlaylistManagerImpl): SmartPlaylistManager

    @Binds
    @Singleton
    abstract fun providesBookmarkManager(bookmarkManager: BookmarkManagerImpl): BookmarkManager

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
    abstract fun provideRatingsManager(ratingsManagerImpl: RatingsManagerImpl): RatingsManager

    @Binds
    abstract fun provideChapterManager(chapterManagerImpl: ChapterManagerImpl): ChapterManager

    @Binds
    abstract fun provideObserveUser(observeTrackableUser: ObserveTrackableUser): ObserveUser

    @Binds
    abstract fun provideCrashReportPermissionCheck(userSettingsCrashReportPermission: UserSettingsCrashReportPermission): CrashReportPermissionCheck

    @Binds
    abstract fun provideExternalDataManager(externalDataManagerImpl: ExternalDataManagerImpl): ExternalDataManager

    @Binds
    abstract fun provideReferralManager(referralManagerImpl: ReferralManagerImpl): ReferralManager

    @Binds
    abstract fun provideEndOfYearSync(endOfYearSyncImpl: EndOfYearSyncImpl): EndOfYearSync

    @Binds
    abstract fun providePodcastRefresher(podcastRefresherImpl: PodcastRefresherImpl): PodcastRefresher

    @Binds
    abstract fun provideUpNextHistoryManager(upNextHistoryManagerImpl: UpNextHistoryManagerImpl): UpNextHistoryManager

    @Binds
    abstract fun providePurchaseApprover(approver: ServerPurchaseApprover): PurchaseApprover

    @Binds
    abstract fun provideNotificationManager(notificationManagerImpl: NotificationManagerImpl): NotificationManager

    @Binds
    abstract fun provideNotificationScheduler(notificationSchedulerImpl: NotificationSchedulerImpl): NotificationScheduler

    @Binds
    abstract fun provideTranscriptManager(transcriptsManagerImpl: TranscriptManagerImpl): TranscriptManager

    @Binds
    abstract fun providePlaylistManager(playlistManagerImpl: PlaylistManagerImpl): PlaylistManager

    @Binds
    abstract fun provideBlazeAdsManager(blazeAdsManagerImpl: BlazeAdsManagerImpl): BlazeAdsManager

    companion object {
        @Provides
        @IntoMap
        @TranscriptTypeKey(TranscriptType.Vtt)
        fun provideVttParser(): TranscriptParser {
            return WebVttParser()
        }

        @Provides
        @IntoMap
        @TranscriptTypeKey(TranscriptType.Json)
        fun provideJsonParser(moshi: Moshi): TranscriptParser {
            return JsonParser(moshi)
        }

        @Provides
        @IntoMap
        @TranscriptTypeKey(TranscriptType.Srt)
        fun provideSrtParser(): TranscriptParser {
            return SrtParser()
        }

        @Provides
        @IntoMap
        @TranscriptTypeKey(TranscriptType.Html)
        fun provideHtmlParser(): TranscriptParser {
            return HtmlParser()
        }
    }
}

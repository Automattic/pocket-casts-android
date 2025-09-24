package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.entity.CuratedPodcast
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveAfterPlaying
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveInactive
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveLimit
import au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffects
import au.com.shiftyjelly.pocketcasts.models.to.PodcastGrouping
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.models.type.EpisodesSortType
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import kotlinx.coroutines.flow.Flow

interface PodcastManager {

    /** Find methods  */
    fun searchPodcastByTitleBlocking(title: String): Podcast?
    fun findPodcastByUuidBlocking(uuid: String): Podcast?
    suspend fun findPodcastByUuid(uuid: String): Podcast?
    fun findPodcastByUuidRxMaybe(uuid: String): Maybe<Podcast>
    fun podcastByUuidRxFlowable(uuid: String): Flowable<Podcast>
    fun podcastByUuidFlow(uuid: String): Flow<Podcast>
    fun podcastByEpisodeUuidFlow(uuid: String): Flow<Podcast>
    fun podcastSubscriptionsRxFlowable(): Flowable<List<String>>

    fun findSubscribedBlocking(): List<Podcast>
    fun findSubscribedRxSingle(): Single<List<Podcast>>
    fun findSubscribedFlow(): Flow<List<Podcast>>
    suspend fun findSubscribedSorted(): List<Podcast>
    suspend fun findSubscribedNoOrder(): List<Podcast>
    suspend fun findPodcastsInFolder(folderUuid: String): List<Podcast>
    fun findPodcastsInFolderRxSingle(folderUuid: String): Single<List<Podcast>>
    suspend fun findPodcastsNotInFolder(): List<Podcast>
    suspend fun findSubscribedUuids(): List<String>

    fun observePodcastsSortedByLatestEpisode(): Flow<List<Podcast>>
    fun observePodcastsBySortedRecentlyPlayed(): Flow<List<Podcast>>
    fun observePodcastsSortedByUserChoice(folder: Folder): Flow<List<Podcast>>
    fun podcastsOrderByLatestEpisodeRxFlowable(): Flowable<List<Podcast>>
    fun podcastsOrderByRecentlyPlayedEpisodeRxFlowable(): Flowable<List<Podcast>>

    fun subscribedRxFlowable(): Flowable<List<Podcast>>
    suspend fun findPodcastsOrderByTitle(): List<Podcast>
    fun findPodcastsToSyncBlocking(): List<Podcast>
    suspend fun findPodcastsToSync(): List<Podcast>
    suspend fun findPodcastsOrderByLatestEpisode(orderAsc: Boolean): List<Podcast>
    suspend fun findFolderPodcastsOrderByLatestEpisode(folderUuid: String): List<Podcast>
    suspend fun findPodcastsOrderByRecentlyPlayedEpisode(): List<Podcast>
    suspend fun findFolderPodcastsOrderByRecentlyPlayedEpisode(folderUuid: String): List<Podcast>

    fun findPodcastsAutodownloadBlocking(): List<Podcast>

    fun episodeCountByPodcatUuidFlow(uuid: String): Flow<Int>

    /** Add methods  */
    fun subscribeToPodcast(podcastUuid: String, sync: Boolean, shouldAutoDownload: Boolean = true)

    fun subscribeToPodcastRxSingle(podcastUuid: String, sync: Boolean = false, shouldAutoDownload: Boolean = true): Single<Podcast>
    suspend fun subscribeToPodcastOrThrow(podcastUuid: String, sync: Boolean = false, shouldAutoDownload: Boolean = true): Podcast
    fun findOrDownloadPodcastRxSingle(podcastUuid: String, waitForSubscribe: Boolean = false): Single<Podcast>
    fun isSubscribingToPodcasts(): Boolean
    fun getSubscribedPodcastUuidsRxSingle(): Single<List<String>>
    fun isSubscribingToPodcast(podcastUuid: String): Boolean
    fun addPodcastRxSingle(podcastUuid: String, sync: Boolean, subscribed: Boolean, shouldAutoDownload: Boolean): Single<Podcast>

    suspend fun replaceCuratedPodcasts(podcasts: List<CuratedPodcast>)

    /** Update methods  */
    fun updatePodcastBlocking(podcast: Podcast)
    suspend fun updatePodcast(podcast: Podcast)

    suspend fun updateAllAutoDownloadStatus(autoDownloadStatus: Int)
    suspend fun updateAutoDownload(podcastUuids: Collection<String>, isEnabled: Boolean)
    suspend fun updateAllShowNotifications(showNotifications: Boolean)
    fun updateAutoDownloadStatusBlocking(podcast: Podcast, autoDownloadStatus: Int)
    suspend fun updateAutoAddToUpNext(podcast: Podcast, autoAddToUpNext: Podcast.AutoAddUpNext)
    suspend fun updateAutoAddToUpNexts(podcastUuids: List<String>, autoAddToUpNext: Podcast.AutoAddUpNext)
    suspend fun updateAutoAddToUpNextsIf(podcastUuids: List<String>, newValue: Podcast.AutoAddUpNext, onlyIfValue: Podcast.AutoAddUpNext)
    fun updateOverrideGlobalEffectsBlocking(podcast: Podcast, override: Boolean)
    suspend fun updateTrimModeBlocking(podcast: Podcast, trimMode: TrimMode)
    fun updateVolumeBoostedBlocking(podcast: Podcast, override: Boolean)
    fun updatePlaybackSpeedBlocking(podcast: Podcast, speed: Double)
    fun updateEffectsBlocking(podcast: Podcast, effects: PlaybackEffects)
    fun updateEpisodesSortTypeBlocking(podcast: Podcast, episodesSortType: EpisodesSortType)
    suspend fun updateShowNotifications(podcastUuid: String, show: Boolean)
    suspend fun updatePodcastPositions(podcasts: List<Podcast>)
    suspend fun updateRefreshAvailable(podcastUuid: String, refreshAvailable: Boolean)
    suspend fun updateStartFromInSec(podcast: Podcast, autoStartFrom: Int)
    fun updateColorsBlocking(podcastUuid: String, background: Int, tintForLightBg: Int, tintForDarkBg: Int, fabForLightBg: Int, fabForDarkBg: Int, linkForLightBg: Int, linkForDarkBg: Int, colorLastDownloaded: Long)
    fun updateLatestEpisodeBlocking(podcast: Podcast, latestEpisode: PodcastEpisode)
    fun updateGroupingBlocking(podcast: Podcast, grouping: PodcastGrouping)
    suspend fun updateSkipLastInSec(podcast: Podcast, skipLast: Int)
    suspend fun updateShowArchived(podcast: Podcast, showArchived: Boolean)
    suspend fun updateAllShowArchived(showArchived: Boolean)
    suspend fun updateFolderUuid(folderUuid: String?, podcastUuids: List<String>)
    suspend fun updateIsHeaderExpanded(podcastUuid: String, isExpanded: Boolean)

    fun markPodcastUuidAsNotSyncedBlocking(podcastUuid: String)
    suspend fun markAllPodcastsSynced()
    suspend fun markAllPodcastsUnsynced()
    suspend fun markAllPodcastsUnsynced(uuids: Collection<String>)

    fun clearAllDownloadErrorsBlocking()

    /** Remove methods  */
    fun checkForUnusedPodcastsBlocking(playbackManager: PlaybackManager)
    fun deletePodcastIfUnusedBlocking(podcast: Podcast, playbackManager: PlaybackManager): Boolean
    suspend fun deleteAllPodcasts()
    suspend fun unsubscribe(podcastUuid: String, playbackManager: PlaybackManager)
    fun unsubscribeBlocking(podcastUuid: String, playbackManager: PlaybackManager)
    fun unsubscribeAsync(podcastUuid: String, playbackManager: PlaybackManager)

    /** Utility methods  */
    fun countPodcastsBlocking(): Int
    suspend fun countSubscribed(): Int
    fun countSubscribedRxSingle(): Single<Int>
    fun countSubscribedFlow(): Flow<Int>
    fun countDownloadStatusBlocking(downloadStatus: Int): Int
    suspend fun hasEpisodesWithAutoDownloadStatus(downloadStatus: Int): Boolean
    fun countDownloadStatusRxSingle(downloadStatus: Int): Single<Int>
    fun countNotificationsOnBlocking(): Int

    fun refreshPodcastsIfRequired(fromLog: String)
    fun refreshPodcasts(fromLog: String)
    suspend fun refreshPodcastsAfterSignIn()
    suspend fun refreshPodcast(existingPodcast: Podcast, playbackManager: PlaybackManager)

    fun checkForEpisodesToDownloadBlocking(episodeUuidsAdded: List<String>, downloadManager: DownloadManager)

    fun countEpisodesInPodcastWithStatusBlocking(podcastUuid: String, episodeStatus: EpisodeStatusEnum): Int
    fun updateGroupingForAllBlocking(grouping: PodcastGrouping)

    fun buildUserEpisodePodcast(episode: UserEpisode): Podcast
    fun autoAddToUpNextPodcastsRxFlowable(): Flowable<List<Podcast>>
    suspend fun findAutoAddToUpNextPodcasts(): List<Podcast>

    suspend fun refreshPodcastFeed(podcast: Podcast): Boolean

    suspend fun findRandomPodcasts(limit: Int): List<Podcast>

    suspend fun countPlayedEpisodes(podcastUuid: String): Int
    suspend fun countEpisodesByPodcast(podcastUuid: String): Int

    suspend fun updateArchiveSettings(uuid: String, enable: Boolean, afterPlaying: AutoArchiveAfterPlaying, inactive: AutoArchiveInactive)
    suspend fun updateArchiveAfterPlaying(uuid: String, value: AutoArchiveAfterPlaying)
    suspend fun updateArchiveAfterInactive(uuid: String, value: AutoArchiveInactive)
    suspend fun updateArchiveEpisodeLimit(uuid: String, value: AutoArchiveLimit)

    fun updatePodcastLatestEpisodeBlocking(podcast: Podcast)
}

package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.db.helper.TopPodcast
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.TrendingPodcast
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
    fun searchPodcastByTitle(title: String): Podcast?
    fun findPodcastByUuid(uuid: String): Podcast?
    suspend fun findPodcastByUuidSuspend(uuid: String): Podcast?
    fun findPodcastByUuidRx(uuid: String): Maybe<Podcast>
    fun observePodcastByUuid(uuid: String): Flowable<Podcast>
    fun observePodcastByUuidFlow(uuid: String): Flow<Podcast>
    fun observePodcastByEpisodeUuid(uuid: String): Flow<Podcast>
    fun observePodcastSubscriptions(): Flowable<List<String>>

    fun findSubscribed(): List<Podcast>
    fun findSubscribedRx(): Single<List<Podcast>>
    fun findSubscribedFlow(): Flow<List<Podcast>>
    suspend fun findSubscribedSorted(): List<Podcast>
    suspend fun findSubscribedNoOrder(): List<Podcast>
    fun findByUuids(uuids: Collection<String>): List<Podcast>
    suspend fun findPodcastsInFolder(folderUuid: String): List<Podcast>
    fun findPodcastsInFolderSingle(folderUuid: String): Single<List<Podcast>>
    suspend fun findPodcastsNotInFolder(): List<Podcast>
    fun observePodcastsInFolderOrderByUserChoice(folder: Folder): Flowable<List<Podcast>>
    suspend fun findSubscribedUuids(): List<String>

    fun observePodcastsOrderByLatestEpisode(): Flowable<List<Podcast>>
    fun observeSubscribed(): Flowable<List<Podcast>>
    suspend fun findPodcastsOrderByTitle(): List<Podcast>
    fun findPodcastsToSync(): List<Podcast>
    suspend fun findPodcastsOrderByLatestEpisode(orderAsc: Boolean): List<Podcast>
    suspend fun findFolderPodcastsOrderByLatestEpisode(folderUuid: String): List<Podcast>

    fun findPodcastsAutodownload(): List<Podcast>

    fun exists(podcastUuid: String): Boolean
    fun observeEpisodeCountByEpisodeUuid(uuid: String): Flow<Int>

    /** Add methods  */
    fun subscribeToPodcast(podcastUuid: String, sync: Boolean)

    suspend fun subscribeToPodcastSuspend(podcastUuid: String, sync: Boolean = false): Podcast
    fun subscribeToPodcastRx(podcastUuid: String, sync: Boolean = false): Single<Podcast>
    fun findOrDownloadPodcastRx(podcastUuid: String): Single<Podcast>
    fun isSubscribingToPodcasts(): Boolean
    fun getSubscribedPodcastUuids(): Single<List<String>>
    fun isSubscribingToPodcast(podcastUuid: String): Boolean
    fun addPodcast(podcastUuid: String, sync: Boolean, subscribed: Boolean): Single<Podcast>

    fun addFolderPodcast(podcast: Podcast)

    suspend fun replaceTrendingPodcasts(podcasts: List<TrendingPodcast>)

    /** Update methods  */
    fun updatePodcast(podcast: Podcast)

    fun updateAllAutoDownloadStatus(autoDownloadStatus: Int)
    suspend fun updateAllShowNotifications(showNotifications: Boolean)
    fun updateAutoDownloadStatus(podcast: Podcast, autoDownloadStatus: Int)
    suspend fun updateAutoAddToUpNext(podcast: Podcast, autoAddToUpNext: Podcast.AutoAddUpNext)
    suspend fun updateAutoAddToUpNexts(podcastUuids: List<String>, autoAddToUpNext: Podcast.AutoAddUpNext)
    suspend fun updateAutoAddToUpNextsIf(podcastUuids: List<String>, newValue: Podcast.AutoAddUpNext, onlyIfValue: Podcast.AutoAddUpNext)
    fun updateExcludeFromAutoArchive(podcast: Podcast, excludeFromAutoArchive: Boolean)
    fun updateOverrideGlobalEffects(podcast: Podcast, override: Boolean)
    fun updateTrimMode(podcast: Podcast, trimMode: TrimMode)
    fun updateVolumeBoosted(podcast: Podcast, override: Boolean)
    fun updatePlaybackSpeed(podcast: Podcast, speed: Double)
    fun updateEffects(podcast: Podcast, effects: PlaybackEffects)
    fun updateEpisodesSortType(podcast: Podcast, episodesSortType: EpisodesSortType)
    fun updateShowNotifications(podcast: Podcast, show: Boolean)
    suspend fun updatePodcastPositions(podcasts: List<Podcast>)
    fun updateSubscribed(podcast: Podcast, subscribed: Boolean)
    suspend fun updateRefreshAvailable(podcastUuid: String, refreshAvailable: Boolean)
    suspend fun updateStartFromInSec(podcast: Podcast, autoStartFrom: Int)
    fun updateColorLastDownloaded(podcast: Podcast, lastDownloaded: Long)
    fun updateOverrideGobalSettings(podcast: Podcast, override: Boolean)
    fun updateEpisodesToKeep(podcast: Podcast, episodeToKeep: Int)
    fun updateColors(podcastUuid: String, background: Int, tintForLightBg: Int, tintForDarkBg: Int, fabForLightBg: Int, fabForDarkBg: Int, linkForLightBg: Int, linkForDarkBg: Int, colorLastDownloaded: Long)
    fun updateLatestEpisode(podcast: Podcast, latestEpisode: PodcastEpisode)
    fun updateGrouping(podcast: Podcast, grouping: PodcastGrouping)
    suspend fun updateSkipLastInSec(podcast: Podcast, skipLast: Int)
    suspend fun updateShowArchived(podcast: Podcast, showArchived: Boolean)
    suspend fun updateAllShowArchived(showArchived: Boolean)
    suspend fun updateFolderUuid(folderUuid: String?, podcastUuids: List<String>)

    fun markPodcastAsSynced(podcast: Podcast)
    fun markPodcastAsNotSynced(podcast: Podcast)
    fun markPodcastUuidAsNotSynced(podcastUuid: String)
    fun markAllPodcastsSynced()
    suspend fun markAllPodcastsUnsynced()
    fun markAsSubscribed(podcast: Podcast, subscribed: Boolean)

    fun clearAllDownloadErrors()

    /** Remove methods  */
    fun checkForUnusedPodcasts(playbackManager: PlaybackManager)
    fun deletePodcastIfUnused(podcast: Podcast, playbackManager: PlaybackManager): Boolean
    suspend fun deleteAllPodcasts()
    fun unsubscribe(podcastUuid: String, playbackManager: PlaybackManager)
    fun unsubscribeAsync(podcastUuid: String, playbackManager: PlaybackManager)

    /** Utility methods  */
    fun countPodcasts(): Int
    suspend fun countSubscribed(): Int
    fun countSubscribedRx(): Single<Int>
    fun observeCountSubscribed(): Flowable<Int>
    fun countDownloadStatus(downloadStatus: Int): Int
    fun countDownloadStatusRx(downloadStatus: Int): Single<Int>
    fun countNotificationsOn(): Int
    fun countNotificationsOnRx(): Single<Int>

    fun refreshPodcastsIfRequired(fromLog: String)
    fun refreshPodcasts(fromLog: String)
    suspend fun refreshPodcastsAfterSignIn()
    fun refreshPodcastInBackground(existingPodcast: Podcast, playbackManager: PlaybackManager)
    fun reloadFoldersFromServer()

    fun checkForEpisodesToDownload(episodeUuidsAdded: List<String>, downloadManager: DownloadManager)

    fun countEpisodesInPodcastWithStatus(podcastUuid: String, episodeStatus: EpisodeStatusEnum): Int
    fun updateGroupingForAll(grouping: PodcastGrouping)

    fun buildUserEpisodePodcast(episode: UserEpisode): Podcast
    fun observeAutoAddToUpNextPodcasts(): Flowable<List<Podcast>>
    suspend fun findAutoAddToUpNextPodcasts(): List<Podcast>

    suspend fun refreshPodcastFeed(podcastUuid: String): Boolean

    suspend fun findTopPodcasts(fromEpochMs: Long, toEpochMs: Long, limit: Int): List<TopPodcast>

    suspend fun findRandomPodcasts(limit: Int): List<Podcast>

    suspend fun countPlayedEpisodes(podcastUuid: String): Int
    suspend fun countEpisodesByPodcast(podcastUuid: String): Int

    suspend fun updateArchiveSettings(uuid: String, enable: Boolean, afterPlaying: AutoArchiveAfterPlaying, inactive: AutoArchiveInactive)
    suspend fun updateArchiveAfterPlaying(uuid: String, value: AutoArchiveAfterPlaying)
    suspend fun updateArchiveAfterInactive(uuid: String, value: AutoArchiveInactive)
    suspend fun updateArchiveEpisodeLimit(uuid: String, value: AutoArchiveLimit)
}

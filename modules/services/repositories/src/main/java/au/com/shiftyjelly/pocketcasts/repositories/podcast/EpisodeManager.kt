package au.com.shiftyjelly.pocketcasts.repositories.podcast

import androidx.lifecycle.LiveData
import au.com.shiftyjelly.pocketcasts.models.db.helper.ListenedCategory
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.entity.Playable
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlayerEvent
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import kotlinx.coroutines.flow.Flow
import java.util.Date

interface EpisodeManager {

    fun getPodcastUuidToBadgeUnfinished(): Flowable<Map<String, Int>>
    fun getPodcastUuidToBadgeLatest(): Flowable<Map<String, Int>>

    /** Find methods  */
    fun findByUuid(uuid: String): Episode?
    fun findByUuidRx(uuid: String): Maybe<Episode>
    fun observeByUuid(uuid: String): Flowable<Episode>
    fun observePlayableByUuid(uuid: String): Flowable<Playable>
    fun findFirstBySearchQuery(query: String): Episode?

    fun findAll(rowParser: (Episode) -> Boolean)
    fun findEpisodesWhere(queryAfterWhere: String): List<Episode>
    fun findEpisodesByUuids(uuids: Array<String>, ordered: Boolean): List<Episode>
    fun findEpisodesByPodcast(podcast: Podcast): Single<List<Episode>>
    fun findEpisodesByPodcastOrdered(podcast: Podcast): List<Episode>
    fun findEpisodesByPodcastOrderedRx(podcast: Podcast): Single<List<Episode>>
    fun findEpisodesByPodcastOrderedByPublishDate(podcast: Podcast): List<Episode>
    fun findNotificationEpisodes(date: Date): List<Episode>
    fun findLatestUnfinishedEpisodeByPodcast(podcast: Podcast): Episode?
    fun findLatestEpisodeToPlay(): Episode?
    fun observeEpisodesByPodcastOrderedRx(podcast: Podcast): Flowable<List<Episode>>
    fun findPodcastEpisodesForMediaBrowserSearch(podcastUuid: String): List<Episode>
    fun observeEpisodesWhere(queryAfterWhere: String): Flowable<List<Episode>>
    fun observeDownloadingEpisodes(): LiveData<List<Episode>>

    fun findEpisodesToSync(): List<Episode>
    fun findEpisodesForHistorySync(): List<Episode>
    fun markAllEpisodesSynced(episodes: List<Episode>)

    fun findEpisodesDownloading(queued: Boolean = true, waitingForPower: Boolean = true, waitingForWifi: Boolean = true, downloading: Boolean = true): List<Episode>
    fun countEpisodesDownloading(queued: Boolean, waitingForPower: Boolean, waitingForWifi: Boolean, downloading: Boolean): Int

    fun observeDownloadEpisodes(): Flowable<List<Episode>>
    fun observeDownloadedEpisodes(): Flowable<List<Episode>>
    fun observeStarredEpisodes(): Flowable<List<Episode>>
    suspend fun findStarredEpisodes(): List<Episode>

    fun exists(episodeUuid: String): Boolean

    /** Add methods  */
    fun add(episode: Episode, downloadMetaData: Boolean): Boolean
    fun add(episodes: List<Episode>, podcastUuid: String, downloadMetaData: Boolean): List<Episode>

    /** Update methods  */
    fun update(episode: Episode?)

    fun updatePlayedUpTo(episode: Playable?, playedUpTo: Double, forceUpdate: Boolean)
    fun updateDuration(episode: Playable?, durationInSecs: Double, syncChanges: Boolean)
    fun updatePlayingStatus(episode: Playable?, status: EpisodePlayingStatus)
    fun updateEpisodeStatus(episode: Playable?, status: EpisodeStatusEnum)
    fun updateAutoDownloadStatus(episode: Playable?, autoDownloadStatus: Int)
    fun updateDownloadFilePath(episode: Playable?, filePath: String, markAsDownloaded: Boolean)
    fun updateFileType(episode: Playable?, fileType: String)
    fun updateSizeInBytes(episode: Playable?, sizeInBytes: Long)
    fun updateDownloadUrl(episode: Playable?, url: String)
    fun updateDownloadTaskId(episode: Playable, id: String?)
    fun updateLastDownloadAttemptDate(episode: Playable?)
    fun updateDownloadErrorDetails(episode: Playable?, message: String?)
    fun setEpisodeThumbnailStatus(episode: Episode?, thumbnailStatus: Int)

    fun updateAllEpisodeStatus(episodeStatus: EpisodeStatusEnum)

    fun markAsNotPlayed(episode: Playable?)
    fun markAsNotPlayedRx(episode: Episode): Single<Episode>
    suspend fun markAllAsPlayed(playables: List<Playable>, playbackManager: PlaybackManager, podcastManager: PodcastManager)
    fun markedAsPlayedExternally(episode: Episode, playbackManager: PlaybackManager, podcastManager: PodcastManager)
    fun markAsPlayedAsync(episode: Playable?, playbackManager: PlaybackManager, podcastManager: PodcastManager)
    fun markAsPlayed(episode: Playable?, playbackManager: PlaybackManager, podcastManager: PodcastManager)
    fun rxMarkAsPlayed(episode: Episode, playbackManager: PlaybackManager, podcastManager: PodcastManager): Completable
    fun markAsPlaybackError(episode: Playable?, errorMessage: String?)
    fun markAsPlaybackError(episode: Playable?, event: PlayerEvent.PlayerError, isPlaybackRemote: Boolean)
    fun starEpisode(episode: Episode, starred: Boolean)
    suspend fun updateAllStarred(episodes: List<Episode>, starred: Boolean)
    fun toggleStarEpisodeAsync(episode: Episode)
    fun clearPlaybackError(episode: Playable?)
    fun clearDownloadError(episode: Episode?)
    fun archive(episode: Episode, playbackManager: PlaybackManager, sync: Boolean = true)
    fun archivePlayedEpisode(episode: Playable, playbackManager: PlaybackManager, podcastManager: PodcastManager, sync: Boolean)
    fun unarchive(episode: Playable)
    fun archiveAllInList(episodes: List<Episode>, playbackManager: PlaybackManager?)
    fun checkForEpisodesToAutoArchive(playbackManager: PlaybackManager?, podcastManager: PodcastManager)
    fun userHasInteractedWithEpisode(episode: Episode, playbackManager: PlaybackManager): Boolean
    fun clearEpisodePlaybackInteractionDatesBefore(lastCleared: Date)
    suspend fun clearAllEpisodeHistory()
    fun markPlaybackHistorySynced()
    fun stopDownloadAndCleanUp(episodeUuid: String, from: String)
    fun stopDownloadAndCleanUp(episode: Episode, from: String)

    /** Remove methods  */
    fun deleteEpisodesWithoutSync(episodes: List<Episode>, playbackManager: PlaybackManager)

    fun deleteEpisodeWithoutSync(episode: Episode?, playbackManager: PlaybackManager)
    fun deleteEpisodeFile(episode: Playable?, playbackManager: PlaybackManager?, disableAutoDownload: Boolean, updateDatabase: Boolean = true, removeFromUpNext: Boolean = true)
    fun deleteCustomFolderEpisode(episode: Episode?, playbackManager: PlaybackManager)
    fun deleteFinishedEpisodes(playbackManager: PlaybackManager)
    fun deleteDownloadedEpisodeFiles()

    /** Utility methods  */
    fun countEpisodes(): Int
    fun countEpisodesWhere(queryAfterWhere: String): Int
    fun downloadMissingEpisode(episodeUuid: String, podcastUuid: String, skeletonEpisode: Episode, podcastManager: PodcastManager, downloadMetaData: Boolean): Maybe<Playable>

    fun deleteEpisodes(episodes: List<Episode>, playbackManager: PlaybackManager)
    fun unarchiveAllInList(episodes: List<Episode>)
    fun observePlaybackHistoryEpisodes(): Flowable<List<Episode>>
    suspend fun findPlaybackHistoryEpisodes(): List<Episode>
    fun checkPodcastForEpisodeLimit(podcast: Podcast, playbackManager: PlaybackManager?)
    fun checkPodcastForAutoArchive(podcast: Podcast, playbackManager: PlaybackManager?)
    fun episodeCanBeCleanedUp(episode: Episode, playbackManager: PlaybackManager): Boolean
    fun markAsUnplayed(episodes: List<Playable>)
    fun unarchiveAllInListAsync(episodes: List<Episode>)
    suspend fun findPlayableByUuid(uuid: String): Playable?
    fun observeDownloadingEpisodesRx(): Flowable<List<Playable>>
    fun setDownloadFailed(episode: Playable, errorMessage: String)
    fun observeEpisodeCount(queryAfterWhere: String): Flowable<Int>
    suspend fun updatePlaybackInteractionDate(episode: Playable?)
    suspend fun deleteEpisodeFiles(episodes: List<Episode>, playbackManager: PlaybackManager)
    suspend fun findStaleDownloads(): List<Episode>
    fun calculateListeningTime(fromEpochMs: Long, toEpochMs: Long): Flow<Long?>
    fun findListenedCategories(fromEpochMs: Long, toEpochMs: Long): Flow<List<ListenedCategory>>
}

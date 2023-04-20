package au.com.shiftyjelly.pocketcasts.repositories.podcast

import androidx.lifecycle.LiveData
import au.com.shiftyjelly.pocketcasts.models.db.helper.ListenedCategory
import au.com.shiftyjelly.pocketcasts.models.db.helper.ListenedNumbers
import au.com.shiftyjelly.pocketcasts.models.db.helper.LongestEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Playable
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
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
    fun findByUuid(uuid: String): PodcastEpisode?
    fun findByUuidRx(uuid: String): Maybe<PodcastEpisode>
    fun observeByUuid(uuid: String): Flow<PodcastEpisode>
    fun observePlayableByUuid(uuid: String): Flowable<Playable>
    fun findFirstBySearchQuery(query: String): PodcastEpisode?

    fun findAll(rowParser: (PodcastEpisode) -> Boolean)
    fun findEpisodesWhere(queryAfterWhere: String): List<PodcastEpisode>
    fun findEpisodesByUuids(uuids: Array<String>, ordered: Boolean): List<PodcastEpisode>
    fun findEpisodesByPodcast(podcast: Podcast): Single<List<PodcastEpisode>>
    fun findEpisodesByPodcastOrdered(podcast: Podcast): List<PodcastEpisode>
    fun findEpisodesByPodcastOrderedRx(podcast: Podcast): Single<List<PodcastEpisode>>
    fun findEpisodesByPodcastOrderedByPublishDate(podcast: Podcast): List<PodcastEpisode>
    fun findNotificationEpisodes(date: Date): List<PodcastEpisode>
    fun findLatestUnfinishedEpisodeByPodcast(podcast: Podcast): PodcastEpisode?
    fun findLatestEpisodeToPlay(): PodcastEpisode?
    fun observeEpisodesByPodcastOrderedRx(podcast: Podcast): Flowable<List<PodcastEpisode>>
    fun observeEpisodesWhere(queryAfterWhere: String): Flowable<List<PodcastEpisode>>
    fun observeDownloadingEpisodes(): LiveData<List<PodcastEpisode>>

    fun findEpisodesToSync(): List<PodcastEpisode>
    fun findEpisodesForHistorySync(): List<PodcastEpisode>
    fun markAllEpisodesSynced(episodes: List<PodcastEpisode>)

    fun findEpisodesDownloading(queued: Boolean = true, waitingForPower: Boolean = true, waitingForWifi: Boolean = true, downloading: Boolean = true): List<PodcastEpisode>
    fun countEpisodesDownloading(queued: Boolean, waitingForPower: Boolean, waitingForWifi: Boolean, downloading: Boolean): Int

    fun observeDownloadEpisodes(): Flowable<List<PodcastEpisode>>
    fun observeDownloadedEpisodes(): Flowable<List<PodcastEpisode>>
    fun observeStarredEpisodes(): Flowable<List<PodcastEpisode>>
    suspend fun findStarredEpisodes(): List<PodcastEpisode>

    fun exists(episodeUuid: String): Boolean

    /** Add methods  */
    fun add(episode: PodcastEpisode, downloadMetaData: Boolean): Boolean
    fun add(episodes: List<PodcastEpisode>, podcastUuid: String, downloadMetaData: Boolean): List<PodcastEpisode>
    fun insert(episodes: List<PodcastEpisode>)

    /** Update methods  */
    fun update(episode: PodcastEpisode?)

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
    fun setEpisodeThumbnailStatus(episode: PodcastEpisode?, thumbnailStatus: Int)

    fun updateAllEpisodeStatus(episodeStatus: EpisodeStatusEnum)

    fun markAsNotPlayed(episode: Playable?)
    fun markAsNotPlayedRx(episode: PodcastEpisode): Single<PodcastEpisode>
    suspend fun markAllAsPlayed(playables: List<Playable>, playbackManager: PlaybackManager, podcastManager: PodcastManager)
    fun markedAsPlayedExternally(episode: PodcastEpisode, playbackManager: PlaybackManager, podcastManager: PodcastManager)
    fun markAsPlayedAsync(episode: Playable?, playbackManager: PlaybackManager, podcastManager: PodcastManager)
    fun markAsPlayed(episode: Playable?, playbackManager: PlaybackManager, podcastManager: PodcastManager)
    fun rxMarkAsPlayed(episode: PodcastEpisode, playbackManager: PlaybackManager, podcastManager: PodcastManager): Completable
    fun markAsPlaybackError(episode: Playable?, errorMessage: String?)
    fun markAsPlaybackError(episode: Playable?, event: PlayerEvent.PlayerError, isPlaybackRemote: Boolean)
    fun starEpisode(episode: PodcastEpisode, starred: Boolean)
    suspend fun updateAllStarred(episodes: List<PodcastEpisode>, starred: Boolean)
    fun toggleStarEpisodeAsync(episode: PodcastEpisode)
    fun clearPlaybackError(episode: Playable?)
    fun clearDownloadError(episode: PodcastEpisode?)
    fun archive(episode: PodcastEpisode, playbackManager: PlaybackManager, sync: Boolean = true)
    fun archivePlayedEpisode(episode: Playable, playbackManager: PlaybackManager, podcastManager: PodcastManager, sync: Boolean)
    fun unarchive(episode: Playable)
    fun archiveAllInList(episodes: List<PodcastEpisode>, playbackManager: PlaybackManager?)
    fun checkForEpisodesToAutoArchive(playbackManager: PlaybackManager?, podcastManager: PodcastManager)
    fun userHasInteractedWithEpisode(episode: PodcastEpisode, playbackManager: PlaybackManager): Boolean
    fun clearEpisodePlaybackInteractionDatesBefore(lastCleared: Date)
    suspend fun clearAllEpisodeHistory()
    fun markPlaybackHistorySynced()
    fun stopDownloadAndCleanUp(episodeUuid: String, from: String)
    fun stopDownloadAndCleanUp(episode: PodcastEpisode, from: String)

    /** Remove methods  */
    suspend fun deleteAll()
    fun deleteEpisodesWithoutSync(episodes: List<PodcastEpisode>, playbackManager: PlaybackManager)

    fun deleteEpisodeWithoutSync(episode: PodcastEpisode?, playbackManager: PlaybackManager)
    fun deleteEpisodeFile(episode: Playable?, playbackManager: PlaybackManager?, disableAutoDownload: Boolean, updateDatabase: Boolean = true, removeFromUpNext: Boolean = true)
    fun deleteCustomFolderEpisode(episode: PodcastEpisode?, playbackManager: PlaybackManager)
    fun deleteFinishedEpisodes(playbackManager: PlaybackManager)
    fun deleteDownloadedEpisodeFiles()

    /** Utility methods  */
    fun countEpisodes(): Int
    fun countEpisodesWhere(queryAfterWhere: String): Int
    fun downloadMissingEpisode(episodeUuid: String, podcastUuid: String, skeletonEpisode: PodcastEpisode, podcastManager: PodcastManager, downloadMetaData: Boolean): Maybe<Playable>

    fun deleteEpisodes(episodes: List<PodcastEpisode>, playbackManager: PlaybackManager)
    fun unarchiveAllInList(episodes: List<PodcastEpisode>)
    fun observePlaybackHistoryEpisodes(): Flowable<List<PodcastEpisode>>
    suspend fun findPlaybackHistoryEpisodes(): List<PodcastEpisode>
    fun checkPodcastForEpisodeLimit(podcast: Podcast, playbackManager: PlaybackManager?)
    fun checkPodcastForAutoArchive(podcast: Podcast, playbackManager: PlaybackManager?)
    fun episodeCanBeCleanedUp(episode: PodcastEpisode, playbackManager: PlaybackManager): Boolean
    fun markAsUnplayed(episodes: List<Playable>)
    fun unarchiveAllInListAsync(episodes: List<PodcastEpisode>)
    suspend fun findPlayableByUuid(uuid: String): Playable?
    fun observeDownloadingEpisodesRx(): Flowable<List<Playable>>
    fun setDownloadFailed(episode: Playable, errorMessage: String)
    fun observeEpisodeCount(queryAfterWhere: String): Flowable<Int>
    suspend fun updatePlaybackInteractionDate(episode: Playable?)
    suspend fun deleteEpisodeFiles(episodes: List<PodcastEpisode>, playbackManager: PlaybackManager)
    suspend fun findStaleDownloads(): List<PodcastEpisode>
    suspend fun calculateListeningTime(fromEpochMs: Long, toEpochMs: Long): Long?
    suspend fun findListenedCategories(fromEpochMs: Long, toEpochMs: Long): List<ListenedCategory>
    suspend fun findListenedNumbers(fromEpochMs: Long, toEpochMs: Long): ListenedNumbers
    suspend fun findLongestPlayedEpisode(fromEpochMs: Long, toEpochMs: Long): LongestEpisode?
    suspend fun countEpisodesPlayedUpto(fromEpochMs: Long, toEpochMs: Long, playedUpToInSecs: Long): Int
    suspend fun findEpisodeInteractedBefore(fromEpochMs: Long): PodcastEpisode?
    suspend fun countEpisodesInListeningHistory(fromEpochMs: Long, toEpochMs: Long): Int
}

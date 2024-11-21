package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlayerEvent
import io.reactivex.Flowable
import io.reactivex.Maybe
import java.util.Date
import kotlinx.coroutines.flow.Flow

interface EpisodeManager {

    fun getPodcastUuidToBadgeUnfinishedRxFlowable(): Flowable<Map<String, Int>>
    fun getPodcastUuidToBadgeLatestRxFlowable(): Flowable<Map<String, Int>>

    /** Find methods  */

    suspend fun findByUuid(uuid: String): PodcastEpisode?

    @Deprecated("Use findByUuid suspended function instead")
    fun findByUuidRxMaybe(uuid: String): Maybe<PodcastEpisode>

    fun findByUuidFlow(uuid: String): Flow<PodcastEpisode>
    fun findEpisodeByUuidRxFlowable(uuid: String): Flowable<BaseEpisode>
    fun findEpisodeByUuidFlow(uuid: String): Flow<BaseEpisode>
    suspend fun findFirstBySearchQuery(query: String): PodcastEpisode?

    fun findEpisodesWhereBlocking(queryAfterWhere: String, forSubscribedPodcastsOnly: Boolean = true): List<PodcastEpisode>
    fun findEpisodesByPodcastOrderedBlocking(podcast: Podcast): List<PodcastEpisode>
    suspend fun findEpisodesByPodcastOrderedSuspend(podcast: Podcast): List<PodcastEpisode>
    fun findEpisodesByPodcastOrderedByPublishDateBlocking(podcast: Podcast): List<PodcastEpisode>
    suspend fun findEpisodesByPodcastOrderedByPublishDate(podcast: Podcast): List<PodcastEpisode>
    fun findNotificationEpisodesBlocking(date: Date): List<PodcastEpisode>
    fun findLatestUnfinishedEpisodeByPodcastBlocking(podcast: Podcast): PodcastEpisode?
    fun findLatestEpisodeToPlayBlocking(): PodcastEpisode?
    fun findEpisodesByPodcastOrderedRxFlowable(podcast: Podcast): Flowable<List<PodcastEpisode>>
    fun findEpisodesWhereRxFlowable(queryAfterWhere: String): Flowable<List<PodcastEpisode>>

    fun findEpisodesToSyncBlocking(): List<PodcastEpisode>
    fun findEpisodesForHistorySyncBlocking(): List<PodcastEpisode>
    fun markAllEpisodesSyncedBlocking(episodes: List<PodcastEpisode>)

    fun findEpisodesDownloadingBlocking(queued: Boolean = true, waitingForPower: Boolean = true, waitingForWifi: Boolean = true, downloading: Boolean = true): List<PodcastEpisode>

    fun findDownloadEpisodesRxFlowable(): Flowable<List<PodcastEpisode>>
    fun findDownloadedEpisodesRxFlowable(): Flowable<List<PodcastEpisode>>
    fun findStarredEpisodesRxFlowable(): Flowable<List<PodcastEpisode>>
    suspend fun findStarredEpisodes(): List<PodcastEpisode>

    /** Add methods  */
    fun addBlocking(episode: PodcastEpisode, downloadMetaData: Boolean): Boolean
    fun addBlocking(episodes: List<PodcastEpisode>, podcastUuid: String, downloadMetaData: Boolean): List<PodcastEpisode>
    suspend fun add(episodes: List<PodcastEpisode>, podcastUuid: String, downloadMetaData: Boolean): List<PodcastEpisode>
    fun insertBlocking(episodes: List<PodcastEpisode>)

    /** Update methods  */
    fun updateBlocking(episode: PodcastEpisode?)
    suspend fun update(episode: PodcastEpisode?)

    fun updatePlayedUpToBlocking(episode: BaseEpisode?, playedUpTo: Double, forceUpdate: Boolean)
    fun updateDurationBlocking(episode: BaseEpisode?, durationInSecs: Double, syncChanges: Boolean)
    fun updatePlayingStatusBlocking(episode: BaseEpisode?, status: EpisodePlayingStatus)
    suspend fun updateImageUrls(updates: List<ImageUrlUpdate>)
    suspend fun updateEpisodeStatus(episode: BaseEpisode?, status: EpisodeStatusEnum)
    suspend fun updateAutoDownloadStatus(episode: BaseEpisode?, autoDownloadStatus: Int)
    fun updateDownloadFilePathBlocking(episode: BaseEpisode?, filePath: String, markAsDownloaded: Boolean)
    fun updateFileTypeBlocking(episode: BaseEpisode?, fileType: String)
    fun updateSizeInBytesBlocking(episode: BaseEpisode?, sizeInBytes: Long)
    suspend fun updateDownloadTaskId(episode: BaseEpisode, id: String?)
    fun updateLastDownloadAttemptDateBlocking(episode: BaseEpisode?)
    fun updateDownloadErrorDetailsBlocking(episode: BaseEpisode?, message: String?)

    fun updateAllEpisodeStatusBlocking(episodeStatus: EpisodeStatusEnum)

    fun markAsNotPlayedBlocking(episode: BaseEpisode?)
    suspend fun markAllAsPlayed(episodes: List<BaseEpisode>, playbackManager: PlaybackManager, podcastManager: PodcastManager)
    fun markedAsPlayedExternally(episode: PodcastEpisode, playbackManager: PlaybackManager, podcastManager: PodcastManager)
    fun markAsPlayedAsync(episode: BaseEpisode?, playbackManager: PlaybackManager, podcastManager: PodcastManager, shouldShuffleUpNext: Boolean = false)
    fun markAsPlayedBlocking(episode: BaseEpisode?, playbackManager: PlaybackManager, podcastManager: PodcastManager, shouldShuffleUpNext: Boolean = false)
    fun markAsPlaybackErrorBlocking(episode: BaseEpisode?, errorMessage: String?)
    fun markAsPlaybackErrorBlocking(episode: BaseEpisode?, event: PlayerEvent.PlayerError, isPlaybackRemote: Boolean)
    suspend fun starEpisode(episode: PodcastEpisode, starred: Boolean, sourceView: SourceView)
    suspend fun updateAllStarred(episodes: List<PodcastEpisode>, starred: Boolean)
    suspend fun toggleStarEpisode(episode: PodcastEpisode, sourceView: SourceView)
    fun clearPlaybackErrorBlocking(episode: BaseEpisode?)
    fun clearDownloadErrorBlocking(episode: PodcastEpisode?)
    fun archiveBlocking(episode: PodcastEpisode, playbackManager: PlaybackManager, sync: Boolean = true, shouldShuffleUpNext: Boolean = false)
    fun archivePlayedEpisode(episode: BaseEpisode, playbackManager: PlaybackManager, podcastManager: PodcastManager, sync: Boolean)
    fun unarchiveBlocking(episode: BaseEpisode)
    suspend fun archiveAllInList(episodes: List<PodcastEpisode>, playbackManager: PlaybackManager?)
    fun checkForEpisodesToAutoArchiveBlocking(playbackManager: PlaybackManager?, podcastManager: PodcastManager)
    fun userHasInteractedWithEpisode(episode: PodcastEpisode, playbackManager: PlaybackManager): Boolean
    fun clearEpisodePlaybackInteractionDatesBeforeBlocking(lastCleared: Date)
    suspend fun clearAllEpisodeHistory()
    fun markPlaybackHistorySyncedBlocking()
    fun stopDownloadAndCleanUp(episodeUuid: String, from: String)
    fun stopDownloadAndCleanUp(episode: PodcastEpisode, from: String)

    /** Remove methods  */
    suspend fun deleteAll()
    fun deleteEpisodesWithoutSyncBlocking(episodes: List<PodcastEpisode>, playbackManager: PlaybackManager)
    suspend fun deleteEpisodesWithoutSync(episodes: List<PodcastEpisode>, playbackManager: PlaybackManager)

    fun deleteEpisodeWithoutSyncBlocking(episode: PodcastEpisode?, playbackManager: PlaybackManager)
    suspend fun deleteEpisodeFile(episode: BaseEpisode?, playbackManager: PlaybackManager?, disableAutoDownload: Boolean, updateDatabase: Boolean = true, removeFromUpNext: Boolean = true, shouldShuffleUpNext: Boolean = false)

    /** Utility methods  */
    suspend fun countEpisodes(): Int
    fun countEpisodesWhereBlocking(queryAfterWhere: String): Int
    fun downloadMissingEpisodeRxMaybe(episodeUuid: String, podcastUuid: String, skeletonEpisode: PodcastEpisode, podcastManager: PodcastManager, downloadMetaData: Boolean, source: SourceView): Maybe<BaseEpisode>

    fun deleteEpisodes(episodes: List<PodcastEpisode>, playbackManager: PlaybackManager)
    fun unarchiveAllInListBlocking(episodes: List<PodcastEpisode>)
    fun findPlaybackHistoryEpisodesRxFlowable(): Flowable<List<PodcastEpisode>>
    fun filteredPlaybackHistoryEpisodesFlow(query: String): Flow<List<PodcastEpisode>>
    suspend fun findPlaybackHistoryEpisodes(): List<PodcastEpisode>
    fun checkPodcastForEpisodeLimitBlocking(podcast: Podcast, playbackManager: PlaybackManager?)
    fun checkPodcastForAutoArchiveBlocking(podcast: Podcast, playbackManager: PlaybackManager?)
    fun episodeCanBeCleanedUp(episode: PodcastEpisode, playbackManager: PlaybackManager): Boolean
    fun markAsUnplayed(episodes: List<BaseEpisode>)
    suspend fun findEpisodeByUuid(uuid: String): BaseEpisode?
    suspend fun findEpisodesByUuids(uuids: List<String>): List<BaseEpisode>
    fun findDownloadingEpisodesRxFlowable(): Flowable<List<BaseEpisode>>
    fun setDownloadFailedBlocking(episode: BaseEpisode, errorMessage: String)
    fun episodeCountRxFlowable(queryAfterWhere: String): Flowable<Int>
    suspend fun updatePlaybackInteractionDate(episode: BaseEpisode?)
    suspend fun deleteEpisodeFiles(episodes: List<PodcastEpisode>, playbackManager: PlaybackManager, removeFromUpNext: Boolean = true)
    suspend fun findStaleDownloads(): List<PodcastEpisode>
    suspend fun calculatePlayedUptoSumInSecsWithinDays(days: Int): Double

    suspend fun updateDownloadUrl(episode: PodcastEpisode): String?

    suspend fun getAllPodcastEpisodes(pageLimit: Int): Flow<Pair<PodcastEpisode, Int>>
}

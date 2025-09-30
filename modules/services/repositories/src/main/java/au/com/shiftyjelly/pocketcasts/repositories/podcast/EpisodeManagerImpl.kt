package au.com.shiftyjelly.pocketcasts.repositories.podcast

import android.content.Context
import androidx.annotation.OptIn
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.source.UnrecognizedInputFormatException
import androidx.room.withTransaction
import androidx.sqlite.db.SimpleSQLiteQuery
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeAnalytics
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveAfterPlaying
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.models.type.EpisodesSortType
import au.com.shiftyjelly.pocketcasts.models.type.UserEpisodeServerStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.di.IoDispatcher
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadHelper
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.download.UpdateEpisodeDetailsTask
import au.com.shiftyjelly.pocketcasts.repositories.file.FileStorage
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlayerEvent
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServiceManager
import au.com.shiftyjelly.pocketcasts.utils.FileUtil
import au.com.shiftyjelly.pocketcasts.utils.Network
import au.com.shiftyjelly.pocketcasts.utils.days
import au.com.shiftyjelly.pocketcasts.utils.extensions.anyMessageContains
import au.com.shiftyjelly.pocketcasts.utils.extensions.escapeLike
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import au.com.shiftyjelly.pocketcasts.utils.timeIntervalSinceNow
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.rxkotlin.zipWith
import java.io.File
import java.util.Date
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.rx2.asFlowable
import kotlinx.coroutines.withContext
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class EpisodeManagerImpl @Inject constructor(
    private val settings: Settings,
    private val fileStorage: FileStorage,
    private val downloadManager: DownloadManager,
    @ApplicationContext private val context: Context,
    private val appDatabase: AppDatabase,
    private val podcastCacheServiceManager: PodcastCacheServiceManager,
    private val userEpisodeManager: UserEpisodeManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val episodeAnalytics: EpisodeAnalytics,
) : EpisodeManager,
    CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private val episodeDao = appDatabase.episodeDao()
    private val userEpisodeDao = appDatabase.userEpisodeDao()

    override suspend fun findEpisodeByUuid(uuid: String): BaseEpisode? {
        val episode = findByUuid(uuid)
        if (episode != null) {
            return episode
        }

        return userEpisodeManager.findEpisodeByUuid(uuid)
    }

    override suspend fun findEpisodesByUuids(uuids: List<String>): List<BaseEpisode> {
        val episodes = findByUuids(uuids)
        val userEpisodes = userEpisodeManager.findEpisodesByUuids(uuids)
        return episodes + userEpisodes
    }

    override suspend fun findByUuid(uuid: String): PodcastEpisode? = episodeDao.findByUuid(uuid)

    override suspend fun findByUuids(uuids: Collection<String>): List<PodcastEpisode> = episodeDao.findByUuids(uuids)

    @Deprecated("Use findByUuid suspended method instead")
    override fun findByUuidRxMaybe(uuid: String): Maybe<PodcastEpisode> = episodeDao.findByUuidRxMaybe(uuid)

    override fun findByUuidFlow(uuid: String): Flow<PodcastEpisode> = episodeDao.findByUuidFlow(uuid).filterNotNull()

    override fun findEpisodeByUuidRxFlowable(uuid: String): Flowable<BaseEpisode> {
        @Suppress("DEPRECATION")
        return findByUuidRxMaybe(uuid)
            .flatMapPublisher<BaseEpisode> { findByUuidFlow(uuid).asFlowable() }
            .switchIfEmpty(userEpisodeManager.episodeRxFlowable(uuid))
    }

    override fun findEpisodeByUuidFlow(uuid: String): Flow<BaseEpisode> = merge(
        episodeDao.findByUuidFlow(uuid), // if it is a PodcastEpisode
        userEpisodeManager.episodeFlow(uuid), // if it is a UserEpisode
    ).filterNotNull() // because it is not going to be both a PodcastEpisode and a UserEpisode

    override suspend fun findFirstBySearchQuery(query: String): PodcastEpisode? = episodeDao.findFirstBySearchQuery(query)

    /**
     * Find a podcast episodes
     */

    override fun findEpisodesByPodcastOrderedByPublishDateBlocking(podcast: Podcast): List<PodcastEpisode> {
        return episodeDao.findByPodcastOrderPublishedDateDescBlocking(podcastUuid = podcast.uuid)
    }

    override suspend fun findEpisodesByPodcastOrderedByPublishDate(podcast: Podcast): List<PodcastEpisode> {
        return episodeDao.findByPodcastOrderPublishedDateDesc(podcastUuid = podcast.uuid)
    }

    override fun findLatestUnfinishedEpisodeByPodcastBlocking(podcast: Podcast): PodcastEpisode? {
        return episodeDao.findLatestUnfinishedEpisodeByPodcastBlocking(podcastUuid = podcast.uuid)
    }

    override fun findLatestEpisodeToPlayBlocking(): PodcastEpisode? {
        return episodeDao.findLatestEpisodeToPlayBlocking()
    }

    override fun findNotificationEpisodesBlocking(date: Date): List<PodcastEpisode> {
        return episodeDao.findNotificationEpisodesBlocking(date)
    }

    override fun findEpisodesByPodcastOrderedBlocking(podcast: Podcast): List<PodcastEpisode> {
        return when (podcast.episodesSortType) {
            EpisodesSortType.EPISODES_SORT_BY_TITLE_ASC -> episodeDao.findByPodcastOrderTitleAscBlocking(podcastUuid = podcast.uuid)
            EpisodesSortType.EPISODES_SORT_BY_TITLE_DESC -> episodeDao.findByPodcastOrderTitleDescBlocking(podcastUuid = podcast.uuid)
            EpisodesSortType.EPISODES_SORT_BY_DATE_ASC -> episodeDao.findByPodcastOrderPublishedDateAscBlocking(podcastUuid = podcast.uuid)
            EpisodesSortType.EPISODES_SORT_BY_LENGTH_ASC -> episodeDao.findByPodcastOrderDurationAscBlocking(podcastUuid = podcast.uuid)
            EpisodesSortType.EPISODES_SORT_BY_LENGTH_DESC -> episodeDao.findByPodcastOrderDurationDescBlocking(podcastUuid = podcast.uuid)
            else -> episodeDao.findByPodcastOrderPublishedDateDescBlocking(podcastUuid = podcast.uuid)
        }
    }

    override suspend fun findEpisodesByPodcastOrderedSuspend(podcast: Podcast): List<PodcastEpisode> {
        return when (podcast.episodesSortType) {
            EpisodesSortType.EPISODES_SORT_BY_TITLE_ASC -> episodeDao.findByPodcastOrderTitleAsc(podcastUuid = podcast.uuid)
            EpisodesSortType.EPISODES_SORT_BY_TITLE_DESC -> episodeDao.findByPodcastOrderTitleDesc(podcastUuid = podcast.uuid)
            EpisodesSortType.EPISODES_SORT_BY_DATE_ASC -> episodeDao.findByPodcastOrderPublishedDateAsc(podcastUuid = podcast.uuid)
            EpisodesSortType.EPISODES_SORT_BY_LENGTH_ASC -> episodeDao.findByPodcastOrderDurationAsc(podcastUuid = podcast.uuid)
            EpisodesSortType.EPISODES_SORT_BY_LENGTH_DESC -> episodeDao.findByPodcastOrderDurationDesc(podcastUuid = podcast.uuid)
            else -> episodeDao.findByPodcastOrderPublishedDateDesc(podcastUuid = podcast.uuid)
        }
    }

    override fun findEpisodesByPodcastOrderedRxFlowable(podcast: Podcast): Flowable<List<PodcastEpisode>> {
        return when (podcast.episodesSortType) {
            EpisodesSortType.EPISODES_SORT_BY_TITLE_ASC -> episodeDao.findByPodcastOrderTitleAscRxFlowable(podcastUuid = podcast.uuid)
            EpisodesSortType.EPISODES_SORT_BY_TITLE_DESC -> episodeDao.findByPodcastOrderTitleDescRxFlowable(podcastUuid = podcast.uuid)
            EpisodesSortType.EPISODES_SORT_BY_DATE_ASC -> episodeDao.findByPodcastOrderPublishedDateAscRxFlowable(podcastUuid = podcast.uuid)
            EpisodesSortType.EPISODES_SORT_BY_LENGTH_ASC -> episodeDao.findByPodcastOrderDurationAscFlowable(podcastUuid = podcast.uuid)
            EpisodesSortType.EPISODES_SORT_BY_LENGTH_DESC -> episodeDao.findByPodcastOrderDurationDescFlowable(podcastUuid = podcast.uuid)
            else -> episodeDao.findByPodcastOrderPublishedDateDescFlowable(podcastUuid = podcast.uuid)
        }
    }

    override fun findEpisodesWhereBlocking(queryAfterWhere: String, forSubscribedPodcastsOnly: Boolean): List<PodcastEpisode> {
        var query = "SELECT podcast_episodes.* FROM podcast_episodes JOIN podcasts ON podcast_episodes.podcast_id = podcasts.uuid WHERE "
        if (forSubscribedPodcastsOnly) {
            query += "podcasts.subscribed = 1 AND "
        }
        query += queryAfterWhere
        return episodeDao.findEpisodesBlocking(SimpleSQLiteQuery(query))
    }

    override fun episodeCountRxFlowable(queryAfterWhere: String): Flowable<Int> {
        return appDatabase.podcastDao().findUnsubscribedUuidRxFlowable()
            .switchMap {
                val podcastList = it.joinToString(separator = "', '", prefix = "podcast_id NOT IN ('", postfix = "')")
                val query = "SELECT COUNT(*) FROM podcast_episodes WHERE $podcastList AND $queryAfterWhere"
                return@switchMap Flowable.just(query)
            }
            .switchMap {
                episodeDao.countRxFlowable(SimpleSQLiteQuery(it))
            }
    }

    override fun findEpisodesWhereRxFlowable(queryAfterWhere: String): Flowable<List<PodcastEpisode>> {
        return appDatabase.podcastDao().findUnsubscribedUuidRxFlowable()
            .switchMap {
                val podcastList = it.joinToString(separator = "', '", prefix = "podcast_id NOT IN ('", postfix = "')")
                val query = "SELECT podcast_episodes.* FROM podcast_episodes WHERE $podcastList AND $queryAfterWhere"
                return@switchMap Flowable.just(query)
            }
            .switchMap {
                episodeDao.findEpisodesRxFlowable(SimpleSQLiteQuery(it))
            }
    }

    override fun findPlaybackHistoryEpisodesRxFlowable(): Flowable<List<PodcastEpisode>> {
        return episodeDao.findPlaybackHistoryRxFlowable()
    }

    override fun filteredPlaybackHistoryEpisodesFlow(query: String): Flow<List<PodcastEpisode>> {
        return episodeDao.filteredPlaybackHistoryFlow(query.escapeLike('\\'))
    }

    override suspend fun findPlaybackHistoryEpisodes(): List<PodcastEpisode> {
        return episodeDao.findPlaybackHistoryEpisodes()
    }

    @Suppress("USELESS_CAST")
    override fun findDownloadingEpisodesRxFlowable(): Flowable<List<BaseEpisode>> {
        return episodeDao.findDownloadingEpisodesRxFlowable().map { it as List<BaseEpisode> }.mergeWith(userEpisodeManager.downloadUserEpisodesRxFlowable())
    }

    override fun updatePlayedUpToBlocking(episode: BaseEpisode?, playedUpTo: Double, forceUpdate: Boolean) {
        if (playedUpTo < 0 || episode == null) {
            return
        }
        episode.playedUpTo = playedUpTo

        val playedUpToMin = if (forceUpdate) playedUpTo else (playedUpTo - 2.0).toInt().toDouble()
        val playedUpToMax = if (forceUpdate) playedUpTo else (playedUpTo + 2.0).toInt().toDouble()

        if (episode is PodcastEpisode) {
            episodeDao.updatePlayedUpToIfChangedBlocking(
                playedUpTo = playedUpTo,
                playedUpToMin = playedUpToMin,
                playedUpToMax = playedUpToMax,
                modified = System.currentTimeMillis(),
                uuid = episode.uuid,
            )
        } else {
            userEpisodeDao.updatePlayedUpToIfChangedBlocking(
                playedUpTo = playedUpTo,
                playedUpToMin = playedUpToMin,
                playedUpToMax = playedUpToMax,
                modified = System.currentTimeMillis(),
                uuid = episode.uuid,
            )
        }
    }

    @Suppress("NAME_SHADOWING")
    override fun updateDurationBlocking(episode: BaseEpisode?, durationInSecs: Double, syncChanges: Boolean) {
        var syncChanges = syncChanges
        if (durationInSecs <= 0 || episode == null) {
            return
        }

        val currentDuration = episode.duration
        if (currentDuration > 10 && Math.abs(currentDuration - durationInSecs) < 30) {
            // if the same second ignore
            if (currentDuration.toLong() == durationInSecs.toLong()) {
                return
            }
            // only a minor change so just update the db but don't bother syncing
            syncChanges = false
        }
        // ignore if the time is over 24 hours
        if (durationInSecs > 86400.0) {
            return
        }

        episode.duration = durationInSecs

        if (episode is PodcastEpisode) {
            if (syncChanges) {
                episodeDao.updateDurationBlocking(durationInSecs, System.currentTimeMillis(), episode.uuid)
            } else {
                episodeDao.updateDurationNoSyncBlocking(durationInSecs, episode.uuid)
            }
        } else {
            userEpisodeDao.updateDurationBlocking(durationInSecs, episode.uuid)
        }
    }

    override fun updateDownloadErrorDetailsBlocking(episode: BaseEpisode?, message: String?) {
        if (episode == null) return
        episode.downloadErrorDetails = message
        if (episode is PodcastEpisode) {
            episodeDao.updateBlocking(episode)
        } else if (episode is UserEpisode) {
            runBlocking { userEpisodeManager.updateDownloadErrorDetails(episode, message) }
        }
    }

    override suspend fun updateDownloadTaskId(episode: BaseEpisode, id: String?) {
        when (episode) {
            is PodcastEpisode -> episodeDao.updateDownloadTaskId(episode.uuid, id)
            is UserEpisode -> userEpisodeManager.updateDownloadTaskId(episode, id)
        }
    }

    override suspend fun updatePlaybackInteractionDate(episode: BaseEpisode?) {
        if (episode == null) {
            return
        }

        // We don't have a playback interaction date for user episodes, just episodes
        if (episode is PodcastEpisode) {
            episodeDao.updatePlaybackInteractionDate(episode.uuid, System.currentTimeMillis())
        }
    }

    override fun updatePlayingStatusBlocking(episode: BaseEpisode?, status: EpisodePlayingStatus) {
        if (episode == null) {
            return
        }
        episode.playingStatus = status
        if (episode is PodcastEpisode) {
            episodeDao.updatePlayingStatusBlocking(status, System.currentTimeMillis(), episode.uuid)
        } else {
            userEpisodeDao.updatePlayingStatusBlocking(status, System.currentTimeMillis(), episode.uuid)
        }
    }

    override suspend fun updateImageUrls(updates: List<ImageUrlUpdate>) {
        appDatabase.withTransaction {
            updates.forEach { updateImageUrl(it.episodeUuid, it.imageUrl) }
        }
    }

    private suspend fun updateImageUrl(episodeUuid: String, imageUrl: String) {
        findByUuid(episodeUuid)?.let { episode ->
            if (episode.imageUrl != imageUrl) {
                episode.imageUrl = imageUrl
                updateBlocking(episode)
            }
        }
    }

    override suspend fun updateEpisodeStatus(episode: BaseEpisode?, status: EpisodeStatusEnum) {
        episode ?: return
        episode.episodeStatus = status

        if (episode is PodcastEpisode) {
            episodeDao.updateEpisodeStatus(status, episode.uuid)
        } else if (episode is UserEpisode) {
            userEpisodeManager.updateEpisodeStatus(episode, status)
        }
    }

    override fun updateAllEpisodeStatusBlocking(episodeStatus: EpisodeStatusEnum) {
        episodeDao.updateAllEpisodeStatusBlocking(episodeStatus)
    }

    override suspend fun updateAutoDownloadStatus(episode: BaseEpisode?, autoDownloadStatus: Int) {
        episode ?: return
        episode.autoDownloadStatus = autoDownloadStatus

        if (episode is PodcastEpisode) {
            episodeDao.updateAutoDownloadStatus(autoDownloadStatus, episode.uuid)
        } else {
            userEpisodeDao.updateAutoDownloadStatusBlocking(autoDownloadStatus, episode.uuid)
        }
    }

    override fun updateDownloadFilePathBlocking(episode: BaseEpisode?, filePath: String, markAsDownloaded: Boolean) {
        episode ?: return
        episode.downloadedFilePath = filePath
        if (episode is PodcastEpisode) {
            episodeDao.updateDownloadedFilePathBlocking(filePath, episode.uuid)
        } else if (episode is UserEpisode) {
            runBlocking { userEpisodeManager.updateDownloadedFilePath(episode, filePath) }
        }

        if (markAsDownloaded) {
            runBlocking {
                updateEpisodeStatus(episode, EpisodeStatusEnum.DOWNLOADED)
            }
        }
    }

    override fun updateFileTypeBlocking(episode: BaseEpisode?, fileType: String) {
        episode ?: return
        if (episode is PodcastEpisode) {
            episodeDao.updateFileTypeBlocking(fileType, episode.uuid)
        } else if (episode is UserEpisode) {
            episode.fileType = fileType
            runBlocking { userEpisodeManager.updateFileType(episode, fileType) }
        }
    }

    override fun updateSizeInBytesBlocking(episode: BaseEpisode?, sizeInBytes: Long) {
        episode ?: return
        if (episode is PodcastEpisode) {
            episodeDao.updateSizeInBytesBlocking(sizeInBytes, episode.uuid)
        } else if (episode is UserEpisode) {
            episode.sizeInBytes = sizeInBytes
            runBlocking { userEpisodeManager.updateSizeInBytes(episode, sizeInBytes) }
        }
    }

    override fun updateLastDownloadAttemptDateBlocking(episode: BaseEpisode?) {
        episode ?: return
        val now = Date()
        episode.lastDownloadAttemptDate = now
        if (episode is PodcastEpisode) {
            episodeDao.updateLastDownloadAttemptDateBlocking(now, episode.uuid)
        } else if (episode is UserEpisode) {
            runBlocking { userEpisodeManager.updateLastDownloadDate(episode, now) }
        }
    }

    override suspend fun starEpisode(episode: PodcastEpisode, starred: Boolean, sourceView: SourceView) {
        episodeDao.updateStarred(starred, System.currentTimeMillis(), episode.uuid)
        val event =
            if (starred) {
                AnalyticsEvent.EPISODE_STARRED
            } else {
                AnalyticsEvent.EPISODE_UNSTARRED
            }
        episodeAnalytics.trackEvent(event, sourceView, episode.uuid)
    }

    override suspend fun updateAllStarred(episodes: List<PodcastEpisode>, starred: Boolean) {
        episodes.chunked(500).forEach { episodesChunk ->
            episodeDao.updateAllStarred(episodesChunk.map { it.uuid }, starred, System.currentTimeMillis())
        }
    }

    override suspend fun toggleStarEpisode(episode: PodcastEpisode, sourceView: SourceView) {
        // Retrieve the episode to make sure we have the latest starred status
        findByUuid(episode.uuid)?.let {
            episode.isStarred = !it.isStarred
            starEpisode(episode, episode.isStarred, sourceView)
        }
    }

    override fun markAsNotPlayedBlocking(episode: BaseEpisode?) {
        episode ?: return
        updatePlayedUpToBlocking(episode, 0.0, false)
        updatePlayingStatusBlocking(episode, EpisodePlayingStatus.NOT_PLAYED)
        unarchiveBlocking(episode)
    }

    private fun downloadEpisodesFileDetails(episodes: List<PodcastEpisode>) {
        UpdateEpisodeDetailsTask.enqueue(episodes, context)
    }

    override suspend fun markAllAsPlayed(episodes: List<BaseEpisode>, playbackManager: PlaybackManager, podcastManager: PodcastManager) {
        val justEpisodes = episodes.filterIsInstance<PodcastEpisode>()
        justEpisodes.chunked(500).forEach { episodeDao.updateAllPlayingStatus(it.map { it.uuid }, System.currentTimeMillis(), EpisodePlayingStatus.COMPLETED) }
        archiveAllPlayedEpisodes(justEpisodes, playbackManager, podcastManager)

        justEpisodes.forEach { playbackManager.removeEpisode(episodeToRemove = it, source = SourceView.UNKNOWN, userInitiated = false) }

        userEpisodeManager.markAllAsPlayed(episodes.filterIsInstance<UserEpisode>(), playbackManager)
    }

    override fun markAsUnplayed(episodes: List<BaseEpisode>) {
        launch {
            val justEpisodes = episodes.filterIsInstance<PodcastEpisode>()
            justEpisodes.chunked(500).forEach {
                episodeDao.markAllUnplayed(it.map { it.uuid }, System.currentTimeMillis())
            }
            unarchiveAllInListBlocking(justEpisodes)

            val justUserEpisodes = episodes.filterIsInstance<UserEpisode>()
            userEpisodeManager.markAllAsUnplayed(justUserEpisodes)
        }
    }

    override fun markedAsPlayedExternally(episode: PodcastEpisode, playbackManager: PlaybackManager, podcastManager: PodcastManager) {
        playbackManager.removeEpisode(episode, source = SourceView.UNKNOWN, userInitiated = false)

        // Auto archive after playing if the episode isn't already archived
        if (!episode.isArchived) {
            archivePlayedEpisode(episode, playbackManager, podcastManager, sync = true)
        }
    }

    override fun markAsPlayedAsync(episode: BaseEpisode?, playbackManager: PlaybackManager, podcastManager: PodcastManager, shouldShuffleUpNext: Boolean) {
        launch {
            markAsPlayedBlocking(episode, playbackManager, podcastManager, shouldShuffleUpNext)
        }
    }

    override fun markAsPlayedBlocking(episode: BaseEpisode?, playbackManager: PlaybackManager, podcastManager: PodcastManager, shouldShuffleUpNext: Boolean) {
        if (episode == null) {
            return
        }

        playbackManager.removeEpisode(episode, source = SourceView.UNKNOWN, userInitiated = false, shouldShuffleUpNext = shouldShuffleUpNext)

        episode.playingStatus = EpisodePlayingStatus.COMPLETED

        updatePlayingStatusBlocking(episode, EpisodePlayingStatus.COMPLETED)

        // Auto archive after playing
        archivePlayedEpisode(episode, playbackManager, podcastManager, sync = true)

        if (episode is UserEpisode) {
            launch {
                userEpisodeManager.markAsPlayed(episode, playbackManager)
            }
        }
    }

    override fun deleteEpisodesWithoutSyncBlocking(episodes: List<PodcastEpisode>, playbackManager: PlaybackManager) {
        runBlocking {
            deleteEpisodesWithoutSync(episodes, playbackManager)
        }
    }

    override suspend fun deleteEpisodesWithoutSync(episodes: List<PodcastEpisode>, playbackManager: PlaybackManager) {
        if (episodes.isEmpty()) {
            return
        }
        for (episode in episodes) {
            deleteEpisodeFile(episode, playbackManager, disableAutoDownload = false, updateDatabase = false)
        }
        episodeDao.deleteAll(episodes)
    }

    override fun deleteEpisodeWithoutSyncBlocking(episode: PodcastEpisode?, playbackManager: PlaybackManager) {
        episode ?: return

        runBlocking {
            deleteEpisodeFile(episode, playbackManager, disableAutoDownload = false, updateDatabase = false)
        }

        episodeDao.deleteBlocking(episode)
    }

    override suspend fun deleteEpisodeFile(episode: BaseEpisode?, playbackManager: PlaybackManager?, disableAutoDownload: Boolean, updateDatabase: Boolean) {
        episode ?: return

        Timber.d("Deleting episode file ${episode.title}")

        // if the episode is currently downloading, kill the download
        downloadManager.removeEpisodeFromQueue(episode, "file deleted")

        cleanUpDownloadFiles(episode)

        if (updateDatabase) {
            updateDownloadTaskId(episode, null)
            updateEpisodeStatus(episode, EpisodeStatusEnum.NOT_DOWNLOADED)
            if (disableAutoDownload) {
                updateAutoDownloadStatus(episode, PodcastEpisode.AUTO_DOWNLOAD_STATUS_IGNORE)
            }
        }
    }

    private fun cleanUpDownloadFiles(episode: BaseEpisode) {
        // remove the download file if one exists
        episode.downloadedFilePath?.let(FileUtil::deleteFileByPath)

        // remove the temp file as well in case it's there
        val tempFilePath = DownloadHelper.tempPathForEpisode(episode, fileStorage)
        FileUtil.deleteFileByPath(tempFilePath)
    }

    override fun stopDownloadAndCleanUp(episodeUuid: String, from: String) {
        launch {
            findByUuid(episodeUuid)?.let { stopDownloadAndCleanUp(it, from) }
        }
    }

    override fun stopDownloadAndCleanUp(episode: PodcastEpisode, from: String) {
        downloadManager.removeEpisodeFromQueue(episode, from)
        cleanUpDownloadFiles(episode)
    }

    override suspend fun countEpisodes(): Int {
        return episodeDao.count()
    }

    override fun countEpisodesWhereBlocking(queryAfterWhere: String): Int {
        return episodeDao.countWhereBlocking(queryAfterWhere, appDatabase)
    }

    override fun addBlocking(episode: PodcastEpisode, downloadMetaData: Boolean): Boolean {
        val episodes = ArrayList<PodcastEpisode>()
        episodes.add(episode)
        val addedEpisodes = addBlocking(episodes, episode.podcastUuid, downloadMetaData)
        return addedEpisodes.size == 1
    }

    override fun updateBlocking(episode: PodcastEpisode?) {
        episode ?: return
        episodeDao.updateBlocking(episode)
    }

    override suspend fun update(episode: PodcastEpisode?) {
        episode ?: return
        episodeDao.update(episode)
    }

    override suspend fun updateAll(episodes: Collection<PodcastEpisode>) {
        episodeDao.updateAll(episodes)
    }

    override fun setDownloadFailedBlocking(episode: BaseEpisode, errorMessage: String) {
        if (episode is PodcastEpisode) {
            episodeDao.updateDownloadErrorBlocking(episode.uuid, errorMessage, EpisodeStatusEnum.DOWNLOAD_FAILED)
        } else if (episode is UserEpisode) {
            runBlocking {
                userEpisodeManager.updateDownloadErrorDetails(episode, errorMessage)
                userEpisodeManager.updateEpisodeStatus(episode, EpisodeStatusEnum.DOWNLOAD_FAILED)
            }
        }
    }

    override fun clearPlaybackErrorBlocking(episode: BaseEpisode?) {
        if (episode?.playErrorDetails == null) {
            return
        }
        markAsPlaybackErrorBlocking(episode, null)
    }

    override fun clearDownloadErrorBlocking(episode: PodcastEpisode?) {
        episode ?: return
        episodeDao.updateDownloadErrorDetailsBlocking(null, episode.uuid)
        runBlocking {
            updateEpisodeStatus(episode, EpisodeStatusEnum.NOT_DOWNLOADED)
        }
        episode.episodeStatus = EpisodeStatusEnum.NOT_DOWNLOADED
        episode.downloadErrorDetails = null
    }

    override fun archivePlayedEpisode(episode: BaseEpisode, playbackManager: PlaybackManager, podcastManager: PodcastManager, sync: Boolean) {
        launch {
            if (episode !is PodcastEpisode) return@launch
            // check if we are meant to archive after episode is played
            val podcast = podcastManager.findPodcastByUuidBlocking(episode.podcastUuid) ?: return@launch
            val archiveAfterPlaying = podcast.autoArchiveAfterPlaying ?: settings.autoArchiveAfterPlaying.value

            if (archiveAfterPlaying == AutoArchiveAfterPlaying.AfterPlaying && (settings.autoArchiveIncludesStarred.value || !episode.isStarred)) {
                if (sync) {
                    episodeDao.updateArchivedBlocking(true, System.currentTimeMillis(), episode.uuid)
                } else {
                    episodeDao.updateArchivedNoSyncBlocking(true, System.currentTimeMillis(), episode.uuid)
                }
                episode.isArchived = true
                cleanUpEpisode(episode, playbackManager)
            }
        }
    }

    @Suppress("NAME_SHADOWING")
    private suspend fun archiveAllPlayedEpisodes(episodes: List<PodcastEpisode>, playbackManager: PlaybackManager, podcastManager: PodcastManager) {
        val episodesWithoutStarred = if (!settings.autoArchiveIncludesStarred.value) episodes.filter { !it.isStarred } else episodes // Remove starred episodes if we have to
        val episodesByPodcast = episodesWithoutStarred.groupBy { it.podcastUuid }.toMutableMap() // Sort in to podcasts

        for ((podcastUuid, episodes) in episodesByPodcast) {
            val podcast = podcastManager.findPodcastByUuidBlocking(podcastUuid) ?: continue
            val archiveAfterPlaying = podcast.autoArchiveAfterPlaying ?: settings.autoArchiveAfterPlaying.value

            if (archiveAfterPlaying == AutoArchiveAfterPlaying.AfterPlaying) {
                archiveAllInList(episodes, playbackManager)
            }
        }
    }

    override fun archiveBlocking(episode: PodcastEpisode, playbackManager: PlaybackManager, sync: Boolean, shouldShuffleUpNext: Boolean) {
        if (sync) {
            episodeDao.updateArchivedBlocking(true, System.currentTimeMillis(), episode.uuid)
        } else {
            episodeDao.updateArchivedNoSyncBlocking(true, System.currentTimeMillis(), episode.uuid)
        }
        episode.isArchived = true
        runBlocking {
            cleanUpEpisode(episode, playbackManager, shouldShuffleUpNext)
        }
    }

    @Suppress("NAME_SHADOWING")
    private suspend fun cleanUpEpisode(episode: BaseEpisode, playbackManager: PlaybackManager?, shouldShuffleUpNext: Boolean = false) {
        val playbackManager = playbackManager ?: return
        deleteEpisodeFile(episode, playbackManager, disableAutoDownload = true, updateDatabase = true)
        playbackManager.removeEpisode(episode, source = SourceView.UNKNOWN, userInitiated = false, shouldShuffleUpNext = shouldShuffleUpNext)
    }

    override suspend fun findStaleDownloads(): List<PodcastEpisode> {
        return episodeDao.findNotFinishedDownloads()
    }

    override fun unarchiveBlocking(episode: BaseEpisode) {
        if (!episode.isArchived || episode !is PodcastEpisode) {
            return // Nothing to do
        }

        episodeDao.unarchiveBlocking(episode.uuid, System.currentTimeMillis())
    }

    override fun observePodcastUuidToBadgeUnfinished(): Flow<Map<String, Int>> {
        return episodeDao.observeUuidToUnfinishedEpisodeCount()
    }

    override fun observePodcastUuidToBadgeLatest(): Flow<Map<String, Int>> {
        return episodeDao.observeUuidToLatestEpisodeCount()
    }

    override fun markAsPlaybackErrorBlocking(episode: BaseEpisode?, errorMessage: String?) {
        episode ?: return
        episode.playErrorDetails = errorMessage

        when (episode) {
            is PodcastEpisode -> episodeDao.updatePlayErrorDetailsBlocking(errorMessage, episode.uuid)
            is UserEpisode -> userEpisodeDao.updatePlayErrorBlocking(episode.uuid, errorMessage)
        }
    }

    @OptIn(UnstableApi::class)
    override fun markAsPlaybackErrorBlocking(episode: BaseEpisode?, event: PlayerEvent.PlayerError, isPlaybackRemote: Boolean) {
        episode ?: return
        val messageId: Int

        if (event.error == null && !isPlaybackRemote) {
            markAsPlaybackErrorBlocking(episode, event.message)
            return
        } else if (isPlaybackRemote) {
            if (episode is UserEpisode) {
                messageId = if (episode.serverStatus != UserEpisodeServerStatus.UPLOADED) LR.string.error_unable_to_cast_local else LR.string.error_unable_to_play
            } else {
                messageId = LR.string.error_unable_to_cast
            }
        } else if (event.error != null && event.error.cause is UnrecognizedInputFormatException) {
            messageId = LR.string.error_playing_format
        } else if (episode.isDownloaded) {
            val downloadedFilePath = episode.downloadedFilePath
            if (downloadedFilePath?.isNotBlank() == true) {
                val file = File(downloadedFilePath)
                if (file.exists()) {
                    if (file.canRead()) {
                        messageId = LR.string.error_playing_format_external
                    } else {
                        messageId = LR.string.error_storage_permission
                    }
                } else {
                    messageId = LR.string.error_file_not_found
                }
            } else {
                messageId = LR.string.error_file_not_found
            }
        } else {
            if (Network.isConnected(context)) {
                val chtblBlocked = event.error.anyMessageContains("chtbl.com")
                if (chtblBlocked) {
                    messageId = LR.string.error_chartable_streaming
                } else {
                    messageId = LR.string.error_streaming_try_downloading
                }
            } else {
                messageId = LR.string.error_streaming_internet
            }
        }

        val message = context.resources.getString(messageId)

        when (episode) {
            is PodcastEpisode -> episodeDao.updatePlayErrorDetailsBlocking(message, episode.uuid)
            is UserEpisode -> userEpisodeDao.updatePlayErrorBlocking(episode.uuid, message)
        }
    }

    override suspend fun deleteAll() {
        episodeDao.deleteAll()
    }

    override fun deleteEpisodeFilesAsync(episodes: List<PodcastEpisode>, playbackManager: PlaybackManager) {
        launch {
            deleteEpisodeFiles(episodes, playbackManager)
        }
    }

    override suspend fun deleteEpisodeFiles(episodes: List<PodcastEpisode>, playbackManager: PlaybackManager) = withContext(Dispatchers.IO) {
        episodes.toList().forEach {
            deleteEpisodeFile(it, playbackManager, disableAutoDownload = false)
        }
    }

    override fun findDownloadEpisodesRxFlowable(): Flowable<List<PodcastEpisode>> {
        val failedDownloadCutoff = Date().time - 7.days()
        return episodeDao.findDownloadingEpisodesIncludingFailedRxFlowable(failedDownloadCutoff)
    }

    override fun findDownloadedEpisodesRxFlowable(): Flowable<List<PodcastEpisode>> {
        return episodeDao.findDownloadedEpisodesRxFlowable()
    }

    override suspend fun downloadedEpisodesThatHaveNotBeenPlayedCount(): Int {
        return episodeDao.downloadedEpisodesThatHaveNotBeenPlayedCount()
    }

    override fun findStarredEpisodesRxFlowable(): Flowable<List<PodcastEpisode>> {
        return episodeDao.findStarredEpisodesRxFlowable()
    }

    override suspend fun findStarredEpisodes(): List<PodcastEpisode> {
        return episodeDao.findStarredEpisodes()
    }

    override fun findEpisodesDownloadingBlocking(queued: Boolean, waitingForPower: Boolean, waitingForWifi: Boolean, downloading: Boolean): List<PodcastEpisode> {
        val sql = buildEpisodeStatusWhere(queued, waitingForPower, waitingForWifi, downloading)
        return findEpisodesWhereBlocking(sql)
    }

    private fun buildEpisodeStatusWhere(queued: Boolean, waitingForPower: Boolean, waitingForWifi: Boolean, downloading: Boolean): String {
        val status = mutableSetOf<EpisodeStatusEnum>()
        if (queued) {
            status.add(EpisodeStatusEnum.QUEUED)
        }
        if (waitingForPower) {
            status.add(EpisodeStatusEnum.WAITING_FOR_POWER)
        }
        if (waitingForWifi) {
            status.add(EpisodeStatusEnum.WAITING_FOR_WIFI)
        }
        if (downloading) {
            status.add(EpisodeStatusEnum.DOWNLOADING)
        }
        val statusSql = status.joinToString(" OR ") { "episode_status = " + it.ordinal.toString() }
        return "($statusSql)"
    }

    override fun addBlocking(episodes: List<PodcastEpisode>, podcastUuid: String, downloadMetaData: Boolean): List<PodcastEpisode> {
        return runBlocking {
            add(episodes, podcastUuid, downloadMetaData)
        }
    }

    override suspend fun add(episodes: List<PodcastEpisode>, podcastUuid: String, downloadMetaData: Boolean): List<PodcastEpisode> {
        val addedEpisodes = mutableListOf<PodcastEpisode>()
        // add the episodes
        val episodesItr = episodes.iterator()
        while (episodesItr.hasNext()) {
            val episode = episodesItr.next()
            // check if the episode already exists
            val existingEpisode = findByUuid(episode.uuid)
            if (existingEpisode == null) {
                episode.podcastUuid = podcastUuid
                addedEpisodes.add(episode)
            }
        }
        if (addedEpisodes.isNotEmpty()) {
            addedEpisodes.chunked(250).forEach { chunkedEpisodes ->
                episodeDao.insertAllOrIgnore(chunkedEpisodes)
            }
        }

        if (addedEpisodes.isNotEmpty() && downloadMetaData) {
            downloadEpisodesFileDetails(addedEpisodes)
        }

        return addedEpisodes
    }

    override fun insertBlocking(episodes: List<PodcastEpisode>) {
        if (episodes.isNotEmpty()) {
            episodeDao.insertAllBlocking(episodes)
        }
    }

    override fun findEpisodesToSyncBlocking(): List<PodcastEpisode> {
        return episodeDao.findEpisodesToSyncBlocking()
    }

    override suspend fun findEpisodesToSync(): List<PodcastEpisode> {
        return episodeDao.findEpisodesToSync()
    }

    override fun findEpisodesForHistorySyncBlocking(): List<PodcastEpisode> {
        return episodeDao.findEpisodesForHistorySyncBlocking()
    }

    override suspend fun markAllEpisodesSynced(episodes: List<PodcastEpisode>) {
        val episodeUuids = episodes.map { it.uuid }
        episodeUuids.chunked(500).forEach { chunked ->
            episodeDao.markAllSynced(chunked)
        }
    }

    // Playback manager is only optional for UI tests. Should never be optional in the app but can't work out
    // another way without mocking a lot of stuff.
    override suspend fun archiveAllInList(
        episodes: List<PodcastEpisode>,
        playbackManager: PlaybackManager?,
    ) {
        withContext(ioDispatcher) {
            appDatabase.withTransaction {
                episodes.filter { !it.isArchived }.chunked(500).forEach { chunked ->
                    episodeDao.archiveAllInList(chunked.map { it.uuid }, System.currentTimeMillis())
                    playbackManager?.let { playbackManager ->
                        chunked.forEach {
                            cleanUpEpisode(it, playbackManager)
                        }
                    }
                }
            }
        }
    }

    override fun unarchiveAllInListBlocking(episodes: List<PodcastEpisode>) {
        episodes.filter { it.isArchived }.chunked(500).forEach { chunked ->
            episodeDao.unarchiveAllInListBlocking(chunked.map { it.uuid }, System.currentTimeMillis())
        }
    }

    override fun checkForEpisodesToAutoArchiveBlocking(playbackManager: PlaybackManager?, podcastManager: PodcastManager) {
        podcastManager.findSubscribedBlocking().forEach { podcast ->
            checkPodcastForAutoArchiveBlocking(podcast, playbackManager)
        }
    }

    override fun checkPodcastForAutoArchiveBlocking(podcast: Podcast, playbackManager: PlaybackManager?) {
        val now = Date()

        val archiveAfterPlaying = podcast.autoArchiveAfterPlaying ?: settings.autoArchiveAfterPlaying.value
        val autoArchiveAfterPlayingTime = archiveAfterPlaying.timeSeconds * 1000L

        if (autoArchiveAfterPlayingTime > 0) {
            val playedEpisodes = episodeDao.findByEpisodePlayingAndArchiveStatusBlocking(podcast.uuid, EpisodePlayingStatus.COMPLETED, false)
                .filter { (settings.autoArchiveIncludesStarred.value && it.isStarred) || !it.isStarred }
                .filter { it.lastPlaybackInteractionDate != null && now.time - it.lastPlaybackInteractionDate!!.time > autoArchiveAfterPlayingTime }

            runBlocking {
                archiveAllInList(playedEpisodes, playbackManager)
            }
            playedEpisodes.forEach {
                runBlocking {
                    cleanUpEpisode(it, playbackManager)
                }
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Auto archiving played episode ${it.title}")
            }
        }

        val autoArchiveInactive = podcast.autoArchiveInactive ?: settings.autoArchiveInactive.value

        val inactiveTime = autoArchiveInactive.timeSeconds * 1000L
        if (inactiveTime > 0) {
            val inactiveEpisodes = episodeDao.findInactiveEpisodesBlocking(podcast.uuid, Date(now.time - inactiveTime))
                .filter { settings.autoArchiveIncludesStarred.value || !it.isStarred }
            if (inactiveEpisodes.isNotEmpty()) {
                runBlocking {
                    archiveAllInList(inactiveEpisodes, playbackManager)
                }
                inactiveEpisodes.forEach {
                    runBlocking {
                        cleanUpEpisode(it, playbackManager)
                    }
                    LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Auto archiving inactive episode ${it.title}")
                }
            }
        }

        checkPodcastForEpisodeLimitBlocking(podcast, playbackManager)
    }

    override fun checkPodcastForEpisodeLimitBlocking(podcast: Podcast, playbackManager: PlaybackManager?) {
        val episodeLimit = podcast.autoArchiveEpisodeLimit?.value

        if (episodeLimit != null) {
            val allEpisodes = episodeDao.findByPodcastOrderPublishedDateDescBlocking(podcast.uuid).filter { !it.excludeFromEpisodeLimit }
            if (allEpisodes.isNotEmpty() && episodeLimit < allEpisodes.size) {
                val episodesToRemove = allEpisodes.drop(episodeLimit)
                    .filter { !it.isArchived }
                    .filter { (settings.autoArchiveIncludesStarred.value && it.isStarred) || !it.isStarred }
                    .filter { playbackManager?.getCurrentEpisode()?.uuid != it.uuid }
                if (episodesToRemove.isNotEmpty()) {
                    runBlocking {
                        archiveAllInList(episodesToRemove, playbackManager)
                    }
                    episodesToRemove.forEach {
                        runBlocking {
                            cleanUpEpisode(it, playbackManager)
                        }
                        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Auto archiving episode over limit $episodeLimit ${it.title}")
                    }
                }
            }
        }
    }

    override fun userHasInteractedWithEpisode(episode: PodcastEpisode, playbackManager: PlaybackManager): Boolean {
        return episode.isStarred ||
            episode.isArchived ||
            episode.isDownloaded ||
            episode.isFinished ||
            episode.isInProgress ||
            playbackManager.upNextQueue.contains(episode.uuid) ||
            episode.lastPlaybackInteraction != null
    }

    override fun episodeCanBeCleanedUp(episode: PodcastEpisode, playbackManager: PlaybackManager): Boolean {
        return !episode.isStarred && !episode.isDownloaded && !episode.isInProgress && !playbackManager.upNextQueue.contains(episode.uuid)
    }

    override fun clearEpisodePlaybackInteractionDatesBeforeBlocking(lastCleared: Date) {
        return episodeDao.clearEpisodePlaybackInteractionDatesBeforeBlocking(lastCleared)
    }

    override suspend fun clearAllEpisodeHistory() {
        episodeDao.clearAllEpisodePlaybackInteractions()
        settings.setClearHistoryTimeNow()
    }

    override suspend fun clearEpisodeHistory(episodes: List<PodcastEpisode>) {
        episodeDao.clearEpisodePlaybackInteractions(episodes.map { it.uuid })
    }

    override fun markPlaybackHistorySyncedBlocking() {
        return episodeDao.markPlaybackHistorySyncedBlocking()
    }

    /**
     * Try downloading the episode if it is missing. If the server doesn't know about it insert the skeleton episode.
     */
    override fun downloadMissingEpisodeRxMaybe(episodeUuid: String, podcastUuid: String, skeletonEpisode: PodcastEpisode, podcastManager: PodcastManager, downloadMetaData: Boolean, source: SourceView): Maybe<BaseEpisode> {
        return episodeDao.existsRxSingle(episodeUuid)
            .flatMapMaybe { episodeExists ->
                if (episodeExists || podcastUuid == Podcast.userPodcast.uuid) {
                    findEpisodeByUuidRxFlowable(episodeUuid).firstElement()
                } else {
                    podcastCacheServiceManager.getPodcastAndEpisodeSingle(podcastUuid, episodeUuid).flatMapMaybe { response ->
                        val episode = response.episodes.firstOrNull() ?: skeletonEpisode
                        addBlocking(episode, downloadMetaData = downloadMetaData)

                        @Suppress("DEPRECATION")
                        podcastManager.findPodcastByUuidRxMaybe(podcastUuid).zipWith(findByUuidRxMaybe(episodeUuid))
                    }.flatMap { (podcast, episode) ->
                        if (podcast.isAutoDownloadNewEpisodes) {
                            DownloadHelper.addAutoDownloadedEpisodeToQueue(episode, "download missing episode", downloadManager, episodeManager = this, source = source)
                        }
                        return@flatMap Maybe.just(episode)
                    }
                }
            }
    }

    override suspend fun calculatePlayedUptoSumInSecsWithinDays(days: Int): Double {
        val query =
            "last_playback_interaction_date IS NOT NULL AND last_playback_interaction_date > 0 ORDER BY last_playback_interaction_date DESC LIMIT 1000"
        val last1000EpisodesPlayed = findEpisodesWhereBlocking(query, forSubscribedPodcastsOnly = false)
        var totalPlaytime = 0.0
        last1000EpisodesPlayed.forEach { episode ->
            episode.lastPlaybackInteractionDate?.let {
                if (TimeUnit.MILLISECONDS.toDays(it.timeIntervalSinceNow()) < days) {
                    totalPlaytime += episode.playedUpTo
                }
            }
        }
        return totalPlaytime
    }

    /**
     * Get the latest episode url from the server and persist it if it is different from
     * the locally saved downloadUrl if it is different
     * @return the latest download url for the episode
     */
    override suspend fun updateDownloadUrl(episode: PodcastEpisode): String? = withContext(Dispatchers.IO) {
        val newDownloadUrl = podcastCacheServiceManager.getEpisodeUrl(episode)
        if (newDownloadUrl != null && episode.downloadUrl != newDownloadUrl) {
            Timber.i("Updating PodcastEpisode url in database for ${episode.uuid} to $newDownloadUrl")
            episodeDao.updateDownloadUrl(newDownloadUrl, episode.uuid)
        }

        return@withContext newDownloadUrl ?: episode.downloadUrl
    }

    override suspend fun getAllPodcastEpisodes(pageLimit: Int): Flow<Pair<PodcastEpisode, Int>> = flow {
        var offset = 0
        while (true) {
            val episodes = episodeDao.getAllPodcastEpisodes(pageLimit, offset)
            if (episodes.isNotEmpty()) {
                episodes.forEachIndexed { index, episode ->
                    emit(episode to (offset + index))
                }
                offset += pageLimit
            } else {
                break
            }
        }
    }
}

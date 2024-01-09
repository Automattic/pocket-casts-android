package au.com.shiftyjelly.pocketcasts.repositories.podcast

import android.content.Context
import androidx.media3.exoplayer.source.UnrecognizedInputFormatException
import androidx.room.withTransaction
import androidx.sqlite.db.SimpleSQLiteQuery
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeAnalytics
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.db.helper.EpisodesStartedAndCompleted
import au.com.shiftyjelly.pocketcasts.models.db.helper.ListenedCategory
import au.com.shiftyjelly.pocketcasts.models.db.helper.ListenedNumbers
import au.com.shiftyjelly.pocketcasts.models.db.helper.LongestEpisode
import au.com.shiftyjelly.pocketcasts.models.db.helper.YearOverYearListeningTime
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.models.type.EpisodesSortType
import au.com.shiftyjelly.pocketcasts.models.type.UserEpisodeServerStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoArchiveAfterPlayingSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.AutoArchiveInactiveSetting
import au.com.shiftyjelly.pocketcasts.repositories.di.IoDispatcher
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadHelper
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.download.UpdateEpisodeDetailsTask
import au.com.shiftyjelly.pocketcasts.repositories.file.FileStorage
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlayerEvent
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServerManager
import au.com.shiftyjelly.pocketcasts.utils.FileUtil
import au.com.shiftyjelly.pocketcasts.utils.Network
import au.com.shiftyjelly.pocketcasts.utils.days
import au.com.shiftyjelly.pocketcasts.utils.extensions.anyMessageContains
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
    private val podcastCacheServerManager: PodcastCacheServerManager,
    private val userEpisodeManager: UserEpisodeManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
    private val episodeAnalytics: EpisodeAnalytics,
) : EpisodeManager, CoroutineScope {

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

    override suspend fun findByUuid(uuid: String): PodcastEpisode? =
        episodeDao.findByUuid(uuid)

    @Deprecated("Use findByUuid suspended method instead")
    override fun findByUuidSync(uuid: String): PodcastEpisode? =
        episodeDao.findByUuidSync(uuid)

    @Deprecated("Use findByUuid suspended method instead")
    override fun findByUuidRx(uuid: String): Maybe<PodcastEpisode> =
        episodeDao.findByUuidRx(uuid)

    override fun observeByUuid(uuid: String): Flow<PodcastEpisode> =
        episodeDao.observeByUuid(uuid).filterNotNull()

    override fun observeEpisodeByUuidRx(uuid: String): Flowable<BaseEpisode> {
        @Suppress("DEPRECATION")
        return findByUuidRx(uuid)
            .flatMapPublisher<BaseEpisode> { observeByUuid(uuid).asFlowable() }
            .switchIfEmpty(userEpisodeManager.observeEpisodeRx(uuid))
    }

    override fun observeEpisodeByUuid(uuid: String): Flow<BaseEpisode> =
        merge(
            episodeDao.observeByUuid(uuid), // if it is a PodcastEpisode
            userEpisodeManager.observeEpisode(uuid), // if it is a UserEpisode
        ).filterNotNull() // because it is not going to be both a PodcastEpisode and a UserEpisode

    override suspend fun findFirstBySearchQuery(query: String): PodcastEpisode? =
        episodeDao.findFirstBySearchQuery(query)

    /**
     * Find a podcast episodes
     */

    override fun findEpisodesByPodcastOrderedByPublishDate(podcast: Podcast): List<PodcastEpisode> {
        return episodeDao.findByPodcastOrderPublishedDateDesc(podcastUuid = podcast.uuid)
    }

    override fun findLatestUnfinishedEpisodeByPodcast(podcast: Podcast): PodcastEpisode? {
        return episodeDao.findLatestUnfinishedEpisodeByPodcast(podcastUuid = podcast.uuid)
    }

    override fun findLatestEpisodeToPlay(): PodcastEpisode? {
        return episodeDao.findLatestEpisodeToPlay()
    }

    override fun findNotificationEpisodes(date: Date): List<PodcastEpisode> {
        return episodeDao.findNotificationEpisodes(date)
    }

    override fun findEpisodesByPodcastOrdered(podcast: Podcast): List<PodcastEpisode> {
        return when (podcast.episodesSortType) {
            EpisodesSortType.EPISODES_SORT_BY_TITLE_ASC -> episodeDao.findByPodcastOrderTitleAsc(podcastUuid = podcast.uuid)
            EpisodesSortType.EPISODES_SORT_BY_TITLE_DESC -> episodeDao.findByPodcastOrderTitleDesc(podcastUuid = podcast.uuid)
            EpisodesSortType.EPISODES_SORT_BY_DATE_ASC -> episodeDao.findByPodcastOrderPublishedDateAsc(podcastUuid = podcast.uuid)
            EpisodesSortType.EPISODES_SORT_BY_LENGTH_ASC -> episodeDao.findByPodcastOrderDurationAsc(podcastUuid = podcast.uuid)
            EpisodesSortType.EPISODES_SORT_BY_LENGTH_DESC -> episodeDao.findByPodcastOrderDurationDesc(podcastUuid = podcast.uuid)
            else -> episodeDao.findByPodcastOrderPublishedDateDesc(podcastUuid = podcast.uuid)
        }
    }

    override suspend fun findEpisodesByPodcastOrderedSuspend(podcast: Podcast): List<PodcastEpisode> {
        return when (podcast.episodesSortType) {
            EpisodesSortType.EPISODES_SORT_BY_TITLE_ASC -> episodeDao.findByPodcastOrderTitleAscSuspend(podcastUuid = podcast.uuid)
            EpisodesSortType.EPISODES_SORT_BY_TITLE_DESC -> episodeDao.findByPodcastOrderTitleDescSuspend(podcastUuid = podcast.uuid)
            EpisodesSortType.EPISODES_SORT_BY_DATE_ASC -> episodeDao.findByPodcastOrderPublishedDateAscSuspend(podcastUuid = podcast.uuid)
            EpisodesSortType.EPISODES_SORT_BY_LENGTH_ASC -> episodeDao.findByPodcastOrderDurationAscSuspend(podcastUuid = podcast.uuid)
            EpisodesSortType.EPISODES_SORT_BY_LENGTH_DESC -> episodeDao.findByPodcastOrderDurationDescSuspend(podcastUuid = podcast.uuid)
            else -> episodeDao.findByPodcastOrderPublishedDateDescSuspend(podcastUuid = podcast.uuid)
        }
    }

    override fun observeEpisodesByPodcastOrderedRx(podcast: Podcast): Flowable<List<PodcastEpisode>> {
        return when (podcast.episodesSortType) {
            EpisodesSortType.EPISODES_SORT_BY_TITLE_ASC -> episodeDao.observeByPodcastOrderTitleAsc(podcastUuid = podcast.uuid)
            EpisodesSortType.EPISODES_SORT_BY_TITLE_DESC -> episodeDao.observeByPodcastOrderTitleDesc(podcastUuid = podcast.uuid)
            EpisodesSortType.EPISODES_SORT_BY_DATE_ASC -> episodeDao.observeByPodcastOrderPublishedDateAsc(podcastUuid = podcast.uuid)
            EpisodesSortType.EPISODES_SORT_BY_LENGTH_ASC -> episodeDao.observeByPodcastOrderDurationAsc(podcastUuid = podcast.uuid)
            EpisodesSortType.EPISODES_SORT_BY_LENGTH_DESC -> episodeDao.observeByPodcastOrderDurationDesc(podcastUuid = podcast.uuid)
            else -> episodeDao.observeByPodcastOrderPublishedDateDesc(podcastUuid = podcast.uuid)
        }
    }

    override fun findEpisodesWhere(queryAfterWhere: String, forSubscribedPodcastsOnly: Boolean): List<PodcastEpisode> {
        var query = "SELECT podcast_episodes.* FROM podcast_episodes JOIN podcasts ON podcast_episodes.podcast_id = podcasts.uuid WHERE "
        if (forSubscribedPodcastsOnly) {
            query += "podcasts.subscribed = 1 AND "
        }
        query += queryAfterWhere
        return episodeDao.findEpisodes(SimpleSQLiteQuery(query))
    }

    override fun observeEpisodeCount(queryAfterWhere: String): Flowable<Int> {
        return appDatabase.podcastDao().observeUnsubscribedUuid()
            .switchMap {
                val podcastList = it.joinToString(separator = "', '", prefix = "podcast_id NOT IN ('", postfix = "')")
                val query = "SELECT COUNT(*) FROM podcast_episodes WHERE $podcastList AND $queryAfterWhere"
                return@switchMap Flowable.just(query)
            }
            .switchMap {
                episodeDao.observeCount(SimpleSQLiteQuery(it))
            }
    }

    override fun observeEpisodesWhere(queryAfterWhere: String): Flowable<List<PodcastEpisode>> {
        return appDatabase.podcastDao().observeUnsubscribedUuid()
            .switchMap {
                val podcastList = it.joinToString(separator = "', '", prefix = "podcast_id NOT IN ('", postfix = "')")
                val query = "SELECT podcast_episodes.* FROM podcast_episodes WHERE $podcastList AND $queryAfterWhere"
                return@switchMap Flowable.just(query)
            }
            .switchMap {
                episodeDao.observeEpisodes(SimpleSQLiteQuery(it))
            }
    }

    override fun observePlaybackHistoryEpisodes(): Flowable<List<PodcastEpisode>> {
        return episodeDao.observePlaybackHistory()
    }

    override suspend fun findPlaybackHistoryEpisodes(): List<PodcastEpisode> {
        return episodeDao.findPlaybackHistoryEpisodes()
    }

    @Suppress("USELESS_CAST")
    override fun observeDownloadingEpisodesRx(): Flowable<List<BaseEpisode>> {
        return episodeDao.observeDownloadingEpisodesRx().map { it as List<BaseEpisode> }.mergeWith(userEpisodeManager.observeDownloadUserEpisodes())
    }

    override fun updatePlayedUpTo(episode: BaseEpisode?, playedUpTo: Double, forceUpdate: Boolean) {
        if (playedUpTo < 0 || episode == null) {
            return
        }
        episode.playedUpTo = playedUpTo

        val playedUpToMin = if (forceUpdate) playedUpTo else (playedUpTo - 2.0).toInt().toDouble()
        val playedUpToMax = if (forceUpdate) playedUpTo else (playedUpTo + 2.0).toInt().toDouble()

        if (episode is PodcastEpisode) {
            episodeDao.updatePlayedUpToIfChanged(
                playedUpTo = playedUpTo,
                playedUpToMin = playedUpToMin,
                playedUpToMax = playedUpToMax,
                modified = System.currentTimeMillis(),
                uuid = episode.uuid,
            )
        } else {
            userEpisodeDao.updatePlayedUpToIfChanged(
                playedUpTo = playedUpTo,
                playedUpToMin = playedUpToMin,
                playedUpToMax = playedUpToMax,
                modified = System.currentTimeMillis(),
                uuid = episode.uuid,
            )
        }
    }

    @Suppress("NAME_SHADOWING")
    override fun updateDuration(episode: BaseEpisode?, durationInSecs: Double, syncChanges: Boolean) {
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
                episodeDao.updateDuration(durationInSecs, System.currentTimeMillis(), episode.uuid)
            } else {
                episodeDao.updateDurationNoSync(durationInSecs, episode.uuid)
            }
        } else {
            userEpisodeDao.updateDuration(durationInSecs, episode.uuid)
        }
    }

    override fun updateDownloadErrorDetails(episode: BaseEpisode?, message: String?) {
        if (episode == null) return
        episode.downloadErrorDetails = message
        if (episode is PodcastEpisode) {
            episodeDao.update(episode)
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

    override fun updatePlayingStatus(episode: BaseEpisode?, status: EpisodePlayingStatus) {
        if (episode == null) {
            return
        }
        episode.playingStatus = status
        if (episode is PodcastEpisode) {
            episodeDao.updatePlayingStatus(status, System.currentTimeMillis(), episode.uuid)
        } else {
            userEpisodeDao.updatePlayingStatus(status, System.currentTimeMillis(), episode.uuid)
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
                update(episode)
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

    override fun updateAllEpisodeStatus(episodeStatus: EpisodeStatusEnum) {
        episodeDao.updateAllEpisodeStatus(episodeStatus)
    }

    override suspend fun updateAutoDownloadStatus(episode: BaseEpisode?, autoDownloadStatus: Int) {
        episode ?: return
        episode.autoDownloadStatus = autoDownloadStatus

        if (episode is PodcastEpisode) {
            episodeDao.updateAutoDownloadStatus(autoDownloadStatus, episode.uuid)
        } else {
            userEpisodeDao.updateAutoDownloadStatus(autoDownloadStatus, episode.uuid)
        }
    }

    override fun updateDownloadFilePath(episode: BaseEpisode?, filePath: String, markAsDownloaded: Boolean) {
        episode ?: return
        episode.downloadedFilePath = filePath
        if (episode is PodcastEpisode) {
            episodeDao.updateDownloadedFilePath(filePath, episode.uuid)
        } else if (episode is UserEpisode) {
            runBlocking { userEpisodeManager.updateDownloadedFilePath(episode, filePath) }
        }

        if (markAsDownloaded) {
            runBlocking {
                updateEpisodeStatus(episode, EpisodeStatusEnum.DOWNLOADED)
            }
        }
    }

    override fun updateFileType(episode: BaseEpisode?, fileType: String) {
        episode ?: return
        if (episode is PodcastEpisode) {
            episodeDao.updateFileType(fileType, episode.uuid)
        } else if (episode is UserEpisode) {
            episode.fileType = fileType
            runBlocking { userEpisodeManager.updateFileType(episode, fileType) }
        }
    }

    override fun updateSizeInBytes(episode: BaseEpisode?, sizeInBytes: Long) {
        episode ?: return
        if (episode is PodcastEpisode) {
            episodeDao.updateSizeInBytes(sizeInBytes, episode.uuid)
        } else if (episode is UserEpisode) {
            episode.sizeInBytes = sizeInBytes
            runBlocking { userEpisodeManager.updateSizeInBytes(episode, sizeInBytes) }
        }
    }

    override fun updateLastDownloadAttemptDate(episode: BaseEpisode?) {
        episode ?: return
        val now = Date()
        episode.lastDownloadAttemptDate = now
        if (episode is PodcastEpisode) {
            episodeDao.updateLastDownloadAttemptDate(now, episode.uuid)
        } else if (episode is UserEpisode) {
            runBlocking { userEpisodeManager.updateLastDownloadDate(episode, now) }
        }
    }

    override suspend fun starEpisode(episode: PodcastEpisode, starred: Boolean, sourceView: SourceView) {
        episodeDao.updateStarred(starred, System.currentTimeMillis(), episode.uuid)
        val event =
            if (starred) {
                AnalyticsEvent.EPISODE_UNSTARRED
            } else {
                AnalyticsEvent.EPISODE_STARRED
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
            starEpisode(episode, !it.isStarred, sourceView)
        }
    }

    override fun markAsNotPlayed(episode: BaseEpisode?) {
        episode ?: return
        updatePlayedUpTo(episode, 0.0, false)
        updatePlayingStatus(episode, EpisodePlayingStatus.NOT_PLAYED)
        unarchive(episode)
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
            unarchiveAllInList(justEpisodes)

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

    override fun markAsPlayedAsync(episode: BaseEpisode?, playbackManager: PlaybackManager, podcastManager: PodcastManager) {
        launch {
            markAsPlayed(episode, playbackManager, podcastManager)
        }
    }

    override fun markAsPlayed(episode: BaseEpisode?, playbackManager: PlaybackManager, podcastManager: PodcastManager) {
        if (episode == null) {
            return
        }

        playbackManager.removeEpisode(episode, source = SourceView.UNKNOWN, userInitiated = false)

        episode.playingStatus = EpisodePlayingStatus.COMPLETED

        updatePlayingStatus(episode, EpisodePlayingStatus.COMPLETED)

        // Auto archive after playing
        archivePlayedEpisode(episode, playbackManager, podcastManager, sync = true)

        if (episode is UserEpisode) {
            launch {
                userEpisodeManager.markAsPlayed(episode, playbackManager)
            }
        }
    }

    override fun deleteEpisodesWithoutSync(episodes: List<PodcastEpisode>, playbackManager: PlaybackManager) {
        if (episodes.isEmpty()) {
            return
        }
        runBlocking {
            for (episode in episodes) {
                deleteEpisodeFile(episode, playbackManager, disableAutoDownload = false, updateDatabase = false)
            }
        }
        episodeDao.deleteAll(episodes)
    }

    override fun deleteEpisodeWithoutSync(episode: PodcastEpisode?, playbackManager: PlaybackManager) {
        episode ?: return

        runBlocking {
            deleteEpisodeFile(episode, playbackManager, false, false)
        }

        episodeDao.delete(episode)
    }

    override suspend fun deleteEpisodeFile(episode: BaseEpisode?, playbackManager: PlaybackManager?, disableAutoDownload: Boolean, updateDatabase: Boolean, removeFromUpNext: Boolean) {
        episode ?: return

        Timber.d("Deleting episode file ${episode.title}")

        // if the episode is currently downloading, kill the download
        downloadManager.removeEpisodeFromQueue(episode, "file deleted")

        // if the episode is currently playing, then stop it. Note: it will not be stopped if coming from the player as it is controlling the playback logic.
        if (removeFromUpNext) {
            playbackManager?.removeEpisode(episode, source = SourceView.UNKNOWN, userInitiated = false)
        }

        cleanUpDownloadFiles(episode)

        if (updateDatabase) {
            updateEpisodeStatus(episode, EpisodeStatusEnum.NOT_DOWNLOADED)
            if (disableAutoDownload) {
                updateAutoDownloadStatus(episode, PodcastEpisode.AUTO_DOWNLOAD_STATUS_IGNORE)
            }
        }
    }

    private fun cleanUpDownloadFiles(episode: BaseEpisode) {
        // remove the download file if one exists
        episode.downloadedFilePath?.let {
            FileUtil.deleteFileByPath(episode.downloadedFilePath)
        }

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

    override fun countEpisodesWhere(queryAfterWhere: String): Int {
        return episodeDao.countWhere(queryAfterWhere, appDatabase)
    }

    override fun add(episode: PodcastEpisode, downloadMetaData: Boolean): Boolean {
        val episodes = ArrayList<PodcastEpisode>()
        episodes.add(episode)
        val addedEpisodes = add(episodes, episode.podcastUuid, downloadMetaData)
        return addedEpisodes.size == 1
    }

    override fun update(episode: PodcastEpisode?) {
        episode ?: return
        episodeDao.update(episode)
    }

    override fun setDownloadFailed(episode: BaseEpisode, errorMessage: String) {
        if (episode is PodcastEpisode) {
            episodeDao.updateDownloadError(episode.uuid, errorMessage, EpisodeStatusEnum.DOWNLOAD_FAILED)
        } else if (episode is UserEpisode) {
            runBlocking {
                userEpisodeManager.updateDownloadErrorDetails(episode, errorMessage)
                userEpisodeManager.updateEpisodeStatus(episode, EpisodeStatusEnum.DOWNLOAD_FAILED)
            }
        }
    }

    override fun clearPlaybackError(episode: BaseEpisode?) {
        if (episode?.playErrorDetails == null) {
            return
        }
        markAsPlaybackError(episode, null)
    }

    override fun clearDownloadError(episode: PodcastEpisode?) {
        episode ?: return
        episodeDao.updateDownloadErrorDetails(null, episode.uuid)
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
            val podcast = podcastManager.findPodcastByUuid(episode.podcastUuid) ?: return@launch
            val podcastOverrideSettings = podcast.overrideGlobalArchive
            val podcastArchiveAfterPlaying = AutoArchiveAfterPlayingSetting.fromIndex(podcast.autoArchiveAfterPlaying)

            val shouldArchiveBasedOnSettings = shouldArchiveBasedOnSettings(podcastOverrideSettings, podcastArchiveAfterPlaying)

            if (shouldArchiveBasedOnSettings &&
                (settings.autoArchiveIncludeStarred.value || !episode.isStarred)
            ) {
                if (sync) {
                    episodeDao.updateArchived(true, System.currentTimeMillis(), episode.uuid)
                } else {
                    episodeDao.updateArchivedNoSync(true, System.currentTimeMillis(), episode.uuid)
                }
                episode.isArchived = true
                cleanUpEpisode(episode, playbackManager)
            }
        }
    }

    private fun shouldArchiveBasedOnSettings(podcastOverrideSettings: Boolean, podcastArchiveAfterPlaying: AutoArchiveAfterPlayingSetting) =
        (
            (!podcastOverrideSettings && settings.autoArchiveAfterPlaying.value == AutoArchiveAfterPlayingSetting.AfterPlaying) ||
                (podcastArchiveAfterPlaying == AutoArchiveAfterPlayingSetting.AfterPlaying)
            )

    @Suppress("NAME_SHADOWING")
    private suspend fun archiveAllPlayedEpisodes(episodes: List<PodcastEpisode>, playbackManager: PlaybackManager, podcastManager: PodcastManager) {
        val episodesWithoutStarred = if (!settings.autoArchiveIncludeStarred.value) episodes.filter { !it.isStarred } else episodes // Remove starred episodes if we have to
        val episodesByPodcast = episodesWithoutStarred.groupBy { it.podcastUuid }.toMutableMap() // Sort in to podcasts

        for ((podcastUuid, episodes) in episodesByPodcast) {
            val podcast = podcastManager.findPodcastByUuid(podcastUuid) ?: continue
            val podcastArchiveAfterPlaying = AutoArchiveAfterPlayingSetting.fromIndex(podcast.autoArchiveAfterPlaying)
            val shouldArchiveBasedOnSettings = shouldArchiveBasedOnSettings(podcast.overrideGlobalSettings, podcastArchiveAfterPlaying)

            if (shouldArchiveBasedOnSettings) {
                archiveAllInList(episodes, playbackManager)
            }
        }
    }

    override fun archive(episode: PodcastEpisode, playbackManager: PlaybackManager, sync: Boolean) {
        if (sync) {
            episodeDao.updateArchived(true, System.currentTimeMillis(), episode.uuid)
        } else {
            episodeDao.updateArchivedNoSync(true, System.currentTimeMillis(), episode.uuid)
        }
        episode.isArchived = true
        runBlocking {
            cleanUpEpisode(episode, playbackManager)
        }
    }

    @Suppress("NAME_SHADOWING")
    private suspend fun cleanUpEpisode(episode: BaseEpisode, playbackManager: PlaybackManager?) {
        val playbackManager = playbackManager ?: return
        if (episode.isDownloaded || episode.isDownloading || episode.downloadTaskId != null) {
            // FIXME doesn't seem this is necessary since it is handled by deleteEpisodeFile
            downloadManager.removeEpisodeFromQueue(episode, "episode manager")
        }
        deleteEpisodeFile(episode, playbackManager, disableAutoDownload = true, updateDatabase = true, removeFromUpNext = true)

        // FIXME doesn't seem this is necessary since it is handled by deleteEpisodeFile
        playbackManager.removeEpisode(episode, source = SourceView.UNKNOWN, userInitiated = false)
    }

    override suspend fun findStaleDownloads(): List<PodcastEpisode> {
        return episodeDao.findStaleDownloads()
    }

    override fun unarchive(episode: BaseEpisode) {
        if (!episode.isArchived || episode !is PodcastEpisode) {
            return // Nothing to do
        }

        episodeDao.unarchive(episode.uuid, System.currentTimeMillis())
    }

    override fun getPodcastUuidToBadgeUnfinished(): Flowable<Map<String, Int>> {
        return episodeDao.podcastUuidToUnfinishedEpisodeCount()
    }

    override fun getPodcastUuidToBadgeLatest(): Flowable<Map<String, Int>> {
        return episodeDao.podcastUuidToLatestEpisodeCount()
    }

    override fun markAsPlaybackError(episode: BaseEpisode?, errorMessage: String?) {
        episode ?: return
        episode.playErrorDetails = errorMessage

        when (episode) {
            is PodcastEpisode -> episodeDao.updatePlayErrorDetails(errorMessage, episode.uuid)
            is UserEpisode -> userEpisodeDao.updatePlayError(episode.uuid, errorMessage)
        }
    }

    override fun markAsPlaybackError(episode: BaseEpisode?, event: PlayerEvent.PlayerError, isPlaybackRemote: Boolean) {
        episode ?: return
        val messageId: Int

        if (event.error == null && !isPlaybackRemote) {
            markAsPlaybackError(episode, event.message)
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
            is PodcastEpisode -> episodeDao.updatePlayErrorDetails(message, episode.uuid)
            is UserEpisode -> userEpisodeDao.updatePlayError(episode.uuid, message)
        }
    }

    override suspend fun deleteAll() {
        episodeDao.deleteAll()
    }

    override fun deleteEpisodes(episodes: List<PodcastEpisode>, playbackManager: PlaybackManager) {
        val episodesCopy = episodes.toList()
        launch {
            deleteEpisodeFiles(episodesCopy, playbackManager)
        }
    }

    override suspend fun deleteEpisodeFiles(episodes: List<PodcastEpisode>, playbackManager: PlaybackManager, removeFromUpNext: Boolean) = withContext(Dispatchers.IO) {
        episodes.toList().forEach {
            deleteEpisodeFile(it, playbackManager, removeFromUpNext = removeFromUpNext, disableAutoDownload = false)
        }
    }

    override fun observeDownloadEpisodes(): Flowable<List<PodcastEpisode>> {
        val failedDownloadCutoff = Date().time - 7.days()
        return episodeDao.observeDownloadingEpisodesIncludingFailed(failedDownloadCutoff)
    }

    override fun observeDownloadedEpisodes(): Flowable<List<PodcastEpisode>> {
        return episodeDao.observeDownloadedEpisodes()
    }

    override fun observeStarredEpisodes(): Flowable<List<PodcastEpisode>> {
        return episodeDao.observeStarredEpisodes()
    }

    override suspend fun findStarredEpisodes(): List<PodcastEpisode> {
        return episodeDao.findStarredEpisodes()
    }

    override fun findEpisodesDownloading(queued: Boolean, waitingForPower: Boolean, waitingForWifi: Boolean, downloading: Boolean): List<PodcastEpisode> {
        val sql = buildEpisodeStatusWhere(queued, waitingForPower, waitingForWifi, downloading)
        return findEpisodesWhere(sql)
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

    override fun add(episodes: List<PodcastEpisode>, podcastUuid: String, downloadMetaData: Boolean): List<PodcastEpisode> {
        val addedEpisodes = mutableListOf<PodcastEpisode>()
        // add the episodes
        val episodesItr = episodes.iterator()
        while (episodesItr.hasNext()) {
            val episode = episodesItr.next()
            // check if the episode already exists
            val existingEpisode = runBlocking {
                findByUuid(episode.uuid)
            }
            if (existingEpisode == null) {
                episode.podcastUuid = podcastUuid
                addedEpisodes.add(episode)
            }
        }
        if (addedEpisodes.isNotEmpty()) {
            episodeDao.insertAll(addedEpisodes)
        }

        if (episodes.isNotEmpty()) {
            if (downloadMetaData) {
                downloadEpisodesFileDetails(episodes)
            }
        }

        return addedEpisodes
    }

    override fun insert(episodes: List<PodcastEpisode>) {
        if (episodes.isNotEmpty()) {
            episodeDao.insertAll(episodes)
        }
    }

    override fun findEpisodesToSync(): List<PodcastEpisode> {
        return episodeDao.findEpisodesToSync()
    }

    override fun findEpisodesForHistorySync(): List<PodcastEpisode> {
        return episodeDao.findEpisodesForHistorySync()
    }

    override fun markAllEpisodesSynced(episodes: List<PodcastEpisode>) {
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

    override fun unarchiveAllInList(episodes: List<PodcastEpisode>) {
        episodes.filter { it.isArchived }.chunked(500).forEach { chunked ->
            episodeDao.unarchiveAllInList(chunked.map { it.uuid }, System.currentTimeMillis())
        }
    }

    override fun checkForEpisodesToAutoArchive(playbackManager: PlaybackManager?, podcastManager: PodcastManager) {
        podcastManager.findSubscribed().forEach { podcast ->
            checkPodcastForAutoArchive(podcast, playbackManager)
        }
    }

    override fun checkPodcastForAutoArchive(podcast: Podcast, playbackManager: PlaybackManager?) {
        val now = Date()

        val podcastArchivePlaying = AutoArchiveAfterPlayingSetting.fromIndex(podcast.autoArchiveAfterPlaying)
        val autoArchiveSetting = if (podcast.overrideGlobalArchive) podcastArchivePlaying else settings.autoArchiveAfterPlaying.value
        val autoArchiveAfterPlayingTime = autoArchiveSetting.timeSeconds * 1000L

        if (autoArchiveAfterPlayingTime > 0) {
            val playedEpisodes = episodeDao.findByEpisodePlayingAndArchiveStatus(podcast.uuid, EpisodePlayingStatus.COMPLETED, false)
                .filter { (settings.autoArchiveIncludeStarred.value && it.isStarred) || !it.isStarred }
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

        val podcastInactiveSetting = AutoArchiveInactiveSetting.fromIndex(podcast.autoArchiveInactive)
        val inactiveSetting = if (podcast.overrideGlobalArchive) podcastInactiveSetting else settings.autoArchiveInactive.value

        val inactiveTime = inactiveSetting.timeSeconds * 1000L
        if (inactiveTime > 0) {
            val inactiveEpisodes = episodeDao.findInactiveEpisodes(podcast.uuid, Date(now.time - inactiveTime))
                .filter { settings.autoArchiveIncludeStarred.value || !it.isStarred }
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

        checkPodcastForEpisodeLimit(podcast, playbackManager)
    }

    override fun checkPodcastForEpisodeLimit(podcast: Podcast, playbackManager: PlaybackManager?) {
        if (!podcast.overrideGlobalArchive) return

        val episodeLimit = podcast.autoArchiveEpisodeLimit

        if (episodeLimit != null) {
            val allEpisodes = episodeDao.findByPodcastOrderPublishedDateDesc(podcast.uuid).filter { !it.excludeFromEpisodeLimit }
            if (allEpisodes.isNotEmpty() && episodeLimit < allEpisodes.size) {
                val episodesToRemove = allEpisodes.subList(episodeLimit, allEpisodes.size)
                    .filter { !it.isArchived }
                    .filter { (settings.autoArchiveIncludeStarred.value && it.isStarred) || !it.isStarred }
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

    override fun clearEpisodePlaybackInteractionDatesBefore(lastCleared: Date) {
        return episodeDao.clearEpisodePlaybackInteractionDatesBefore(lastCleared)
    }

    override suspend fun clearAllEpisodeHistory() {
        episodeDao.clearAllEpisodePlaybackInteractions()
        settings.setClearHistoryTimeNow()
    }

    override fun markPlaybackHistorySynced() {
        return episodeDao.markPlaybackHistorySynced()
    }

    /**
     * Try downloading the episode if it is missing. If the server doesn't know about it insert the skeleton episode.
     */
    override fun downloadMissingEpisode(episodeUuid: String, podcastUuid: String, skeletonEpisode: PodcastEpisode, podcastManager: PodcastManager, downloadMetaData: Boolean): Maybe<BaseEpisode> {
        return episodeDao.existsRx(episodeUuid)
            .flatMapMaybe { episodeExists ->
                if (episodeExists || podcastUuid == Podcast.userPodcast.uuid) {
                    observeEpisodeByUuidRx(episodeUuid).firstElement()
                } else {
                    podcastCacheServerManager.getPodcastAndEpisodeSingle(podcastUuid, episodeUuid).flatMapMaybe { response ->
                        val episode = response.episodes.firstOrNull() ?: skeletonEpisode
                        add(episode, downloadMetaData = downloadMetaData)

                        @Suppress("DEPRECATION")
                        podcastManager.findPodcastByUuidRx(podcastUuid).zipWith(findByUuidRx(episodeUuid))
                    }.flatMap { (podcast, episode) ->
                        if (podcast.isAutoDownloadNewEpisodes) {
                            DownloadHelper.addAutoDownloadedEpisodeToQueue(episode, "download missing episode", downloadManager, this)
                        }
                        return@flatMap Maybe.just(episode)
                    }
                }
            }
    }

    override suspend fun calculateListeningTime(fromEpochMs: Long, toEpochMs: Long): Long? =
        episodeDao.calculateListeningTime(fromEpochMs, toEpochMs)

    override suspend fun findListenedCategories(fromEpochMs: Long, toEpochMs: Long): List<ListenedCategory> =
        episodeDao.findListenedCategories(fromEpochMs, toEpochMs)

    override suspend fun findListenedNumbers(fromEpochMs: Long, toEpochMs: Long): ListenedNumbers =
        episodeDao.findListenedNumbers(fromEpochMs, toEpochMs)

    override suspend fun findLongestPlayedEpisode(fromEpochMs: Long, toEpochMs: Long): LongestEpisode? =
        episodeDao.findLongestPlayedEpisode(fromEpochMs, toEpochMs)

    override suspend fun countEpisodesPlayedUpto(fromEpochMs: Long, toEpochMs: Long, playedUpToInSecs: Long): Int =
        episodeDao.countEpisodesPlayedUpto(fromEpochMs, toEpochMs, playedUpToInSecs)

    override suspend fun findEpisodeInteractedBefore(fromEpochMs: Long): PodcastEpisode? =
        episodeDao.findEpisodeInteractedBefore(fromEpochMs)

    override suspend fun countEpisodesInListeningHistory(fromEpochMs: Long, toEpochMs: Long): Int =
        episodeDao.findEpisodesCountInListeningHistory(fromEpochMs, toEpochMs)

    override suspend fun calculatePlayedUptoSumInSecsWithinDays(days: Int): Double {
        val query =
            "last_playback_interaction_date IS NOT NULL AND last_playback_interaction_date > 0 ORDER BY last_playback_interaction_date DESC LIMIT 1000"
        val last1000EpisodesPlayed = findEpisodesWhere(query, forSubscribedPodcastsOnly = false)
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

    override suspend fun yearOverYearListeningTime(
        fromEpochMsPreviousYear: Long,
        toEpochMsPreviousYear: Long,
        fromEpochMsCurrentYear: Long,
        toEpochMsCurrentYear: Long,
    ): YearOverYearListeningTime {
        val previousYearListeningTime = episodeDao.calculateListeningTime(fromEpochMsPreviousYear, toEpochMsPreviousYear)
        val currentYearListeningTime = episodeDao.calculateListeningTime(fromEpochMsCurrentYear, toEpochMsCurrentYear)
        return YearOverYearListeningTime(
            totalPlayedTimeLastYear = previousYearListeningTime ?: 0L,
            totalPlayedTimeThisYear = currentYearListeningTime ?: 0L,
        )
    }

    override suspend fun countEpisodesStartedAndCompleted(
        fromEpochMs: Long,
        toEpochMs: Long,
    ) = EpisodesStartedAndCompleted(
        started = episodeDao.countEpisodesStarted(fromEpochMs, toEpochMs),
        completed = episodeDao.countEpisodesCompleted(fromEpochMs, toEpochMs),
    )

    /**
     * Get the latest episode url from the server and persist it if it is different from
     * the locally saved downloadUrl if it is different
     * @return the latest download url for the episode
     */
    override suspend fun updateDownloadUrl(episode: PodcastEpisode): String? =
        withContext(Dispatchers.IO) {
            val newDownloadUrl = podcastCacheServerManager.getEpisodeUrl(episode)
            if (newDownloadUrl != null && episode.downloadUrl != newDownloadUrl) {
                Timber.i("Updating PodcastEpisode url in database for ${episode.uuid} to $newDownloadUrl")
                episodeDao.updateDownloadUrl(newDownloadUrl, episode.uuid)
            }

            return@withContext newDownloadUrl ?: episode.downloadUrl
        }
}

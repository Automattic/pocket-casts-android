package au.com.shiftyjelly.pocketcasts.repositories.podcast

import android.content.Context
import androidx.lifecycle.LiveData
import androidx.paging.PagedList
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.db.helper.ListenedCategory
import au.com.shiftyjelly.pocketcasts.models.db.helper.ListenedNumbers
import au.com.shiftyjelly.pocketcasts.models.db.helper.LongestEpisode
import au.com.shiftyjelly.pocketcasts.models.db.helper.QueryHelper
import au.com.shiftyjelly.pocketcasts.models.db.helper.UserEpisodePodcastSubstitute
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.entity.Playable
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.models.type.EpisodesSortType
import au.com.shiftyjelly.pocketcasts.models.type.UserEpisodeServerStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
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
import com.google.android.exoplayer2.source.UnrecognizedInputFormatException
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.rxkotlin.zipWith
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.util.Date
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class EpisodeManagerImpl @Inject constructor(
    private val settings: Settings,
    private val fileStorage: FileStorage,
    private val downloadManager: DownloadManager,
    @ApplicationContext private val context: Context,
    private val appDatabase: AppDatabase,
    private val podcastCacheServerManager: PodcastCacheServerManager,
    private val userEpisodeManager: UserEpisodeManager
) : EpisodeManager, CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private val episodeDao = appDatabase.episodeDao()
    private val userEpisodeDao = appDatabase.userEpisodeDao()

    override suspend fun findPlayableByUuid(uuid: String): Playable? {
        val episode = findByUuid(uuid)
        if (episode != null) {
            return episode
        }

        return userEpisodeManager.findEpisodeByUuid(uuid)
    }

    override fun findByUuid(uuid: String): Episode? {
        return episodeDao.findByUuid(uuid)
    }

    override fun findByUuidRx(uuid: String): Maybe<Episode> {
        return episodeDao.findByUuidRx(uuid)
    }

    override fun observeByUuid(uuid: String): Flowable<Episode> {
        return episodeDao.observeByUuid(uuid)
    }

    override fun observePlayableByUuid(uuid: String): Flowable<Playable> {
        return findByUuidRx(uuid)
            .flatMapPublisher<Playable> { episodeDao.observeByUuid(uuid) }
            .switchIfEmpty(userEpisodeManager.observeEpisode(uuid))
    }

    override fun findFirstBySearchQuery(query: String): Episode? {
        return episodeDao.findFirstBySearchQuery(query)
    }

    @Suppress("DEPRECATION")
    override fun findAll(rowParser: (Episode) -> Boolean) {
        val pagingConfig = PagedList.Config.Builder()
            .setPageSize(100)
            .setEnablePlaceholders(false)
            .build()
        val episodes = episodeDao.findAll().create()
        val pagedList = PagedList.Builder(episodes, pagingConfig)
            .setFetchExecutor(Executors.newSingleThreadExecutor())
            .setNotifyExecutor(Executors.newSingleThreadExecutor())
            .build()
        for (episode in pagedList) {
            if (!rowParser(episode)) {
                break
            }
        }
    }

    /**
     * Find a podcast episodes
     */
    override fun findEpisodesByPodcast(podcast: Podcast): Single<List<Episode>> {
        return Single.fromCallable { episodeDao.findByPodcastOrderPublishedDateDesc(podcastUuid = podcast.uuid) }
    }

    override fun findEpisodesByPodcastOrderedByPublishDate(podcast: Podcast): List<Episode> {
        return episodeDao.findByPodcastOrderPublishedDateDesc(podcastUuid = podcast.uuid)
    }

    override fun findLatestUnfinishedEpisodeByPodcast(podcast: Podcast): Episode? {
        return episodeDao.findLatestUnfinishedEpisodeByPodcast(podcastUuid = podcast.uuid)
    }

    override fun findLatestEpisodeToPlay(): Episode? {
        return episodeDao.findLatestEpisodeToPlay()
    }

    override fun findNotificationEpisodes(date: Date): List<Episode> {
        return episodeDao.findNotificationEpisodes(date)
    }

    override fun findEpisodesByPodcastOrdered(podcast: Podcast): List<Episode> {
        return when (podcast.episodesSortType) {
            EpisodesSortType.EPISODES_SORT_BY_TITLE_ASC -> episodeDao.findByPodcastOrderTitleAsc(podcastUuid = podcast.uuid)
            EpisodesSortType.EPISODES_SORT_BY_TITLE_DESC -> episodeDao.findByPodcastOrderTitleDesc(podcastUuid = podcast.uuid)
            EpisodesSortType.EPISODES_SORT_BY_DATE_ASC -> episodeDao.findByPodcastOrderPublishedDateAsc(podcastUuid = podcast.uuid)
            EpisodesSortType.EPISODES_SORT_BY_LENGTH_ASC -> episodeDao.findByPodcastOrderDurationAsc(podcastUuid = podcast.uuid)
            EpisodesSortType.EPISODES_SORT_BY_LENGTH_DESC -> episodeDao.findByPodcastOrderDurationDesc(podcastUuid = podcast.uuid)
            else -> episodeDao.findByPodcastOrderPublishedDateDesc(podcastUuid = podcast.uuid)
        }
    }

    override fun findEpisodesByPodcastOrderedRx(podcast: Podcast): Single<List<Episode>> {
        return Single.fromCallable { findEpisodesByPodcastOrdered(podcast) }
    }

    override fun observeEpisodesByPodcastOrderedRx(podcast: Podcast): Flowable<List<Episode>> {
        return when (podcast.episodesSortType) {
            EpisodesSortType.EPISODES_SORT_BY_TITLE_ASC -> episodeDao.observeByPodcastOrderTitleAsc(podcastUuid = podcast.uuid)
            EpisodesSortType.EPISODES_SORT_BY_TITLE_DESC -> episodeDao.observeByPodcastOrderTitleDesc(podcastUuid = podcast.uuid)
            EpisodesSortType.EPISODES_SORT_BY_DATE_ASC -> episodeDao.observeByPodcastOrderPublishedDateAsc(podcastUuid = podcast.uuid)
            EpisodesSortType.EPISODES_SORT_BY_LENGTH_ASC -> episodeDao.observeByPodcastOrderDurationAsc(podcastUuid = podcast.uuid)
            EpisodesSortType.EPISODES_SORT_BY_LENGTH_DESC -> episodeDao.observeByPodcastOrderDurationDesc(podcastUuid = podcast.uuid)
            else -> episodeDao.observeByPodcastOrderPublishedDateDesc(podcastUuid = podcast.uuid)
        }
    }

    override fun findPodcastEpisodesForMediaBrowserSearch(podcastUuid: String): List<Episode> {
        return episodeDao.findPodcastEpisodesForMediaBrowserSearch(podcastUuid)
    }

    override fun findEpisodesWhere(queryAfterWhere: String): List<Episode> {
        return episodeDao.findEpisodes(SimpleSQLiteQuery("SELECT episodes.* FROM episodes JOIN podcasts ON episodes.podcast_id = podcasts.uuid WHERE podcasts.subscribed = 1 AND $queryAfterWhere"))
    }

    override fun observeEpisodeCount(queryAfterWhere: String): Flowable<Int> {
        return appDatabase.podcastDao().observeUnsubscribedUuid()
            .switchMap {
                val podcastList = it.joinToString(separator = "', '", prefix = "podcast_id NOT IN ('", postfix = "')")
                val query = "SELECT COUNT(*) FROM episodes WHERE $podcastList AND $queryAfterWhere"
                return@switchMap Flowable.just(query)
            }
            .switchMap {
                episodeDao.observeCount(SimpleSQLiteQuery(it))
            }
    }

    override fun observeEpisodesWhere(queryAfterWhere: String): Flowable<List<Episode>> {
        return appDatabase.podcastDao().observeUnsubscribedUuid()
            .switchMap {
                val podcastList = it.joinToString(separator = "', '", prefix = "podcast_id NOT IN ('", postfix = "')")
                val query = "SELECT episodes.* FROM episodes WHERE $podcastList AND $queryAfterWhere"
                return@switchMap Flowable.just(query)
            }
            .switchMap {
                episodeDao.observeEpisodes(SimpleSQLiteQuery(it))
            }
    }

    override fun observePlaybackHistoryEpisodes(): Flowable<List<Episode>> {
        return episodeDao.observePlaybackHistory()
    }

    override suspend fun findPlaybackHistoryEpisodes(): List<Episode> {
        return episodeDao.findPlaybackHistoryEpisodes()
    }

    override fun observeDownloadingEpisodes(): LiveData<List<Episode>> {
        return episodeDao.observeDownloadingEpisodes()
    }

    @Suppress("USELESS_CAST")
    override fun observeDownloadingEpisodesRx(): Flowable<List<Playable>> {
        return episodeDao.observeDownloadingEpisodesRx().map { it as List<Playable> }.mergeWith(userEpisodeManager.observeDownloadUserEpisodes())
    }

    override fun findEpisodesByUuids(uuids: Array<String>, ordered: Boolean): List<Episode> {
        if (uuids.isEmpty()) {
            return ArrayList()
        }
        if (ordered) {
            val episodes = ArrayList<Episode>()
            for (uuid in uuids) {
                findByUuid(uuid)?.let { episodes.add(it) }
            }
            return episodes
        } else {
            return findEpisodesWhere("uuid IN " + QueryHelper.convertStringArrayToInStatement(uuids))
        }
    }

    override fun updatePlayedUpTo(episode: Playable?, playedUpTo: Double, forceUpdate: Boolean) {
        if (playedUpTo < 0 || episode == null) {
            return
        }
        episode.playedUpTo = playedUpTo

        val playedUpToMin = if (forceUpdate) playedUpTo else (playedUpTo - 2.0).toInt().toDouble()
        val playedUpToMax = if (forceUpdate) playedUpTo else (playedUpTo + 2.0).toInt().toDouble()

        if (episode is Episode) {
            episodeDao.updatePlayedUpToIfChanged(
                playedUpTo = playedUpTo,
                playedUpToMin = playedUpToMin,
                playedUpToMax = playedUpToMax,
                modified = System.currentTimeMillis(),
                uuid = episode.uuid
            )
        } else {
            userEpisodeDao.updatePlayedUpToIfChanged(
                playedUpTo = playedUpTo,
                playedUpToMin = playedUpToMin,
                playedUpToMax = playedUpToMax,
                modified = System.currentTimeMillis(),
                uuid = episode.uuid
            )
        }
    }

    @Suppress("NAME_SHADOWING")
    override fun updateDuration(episode: Playable?, durationInSecs: Double, syncChanges: Boolean) {
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

        if (episode is Episode) {
            if (syncChanges) {
                episodeDao.updateDuration(durationInSecs, System.currentTimeMillis(), episode.uuid)
            } else {
                episodeDao.updateDurationNoSync(durationInSecs, episode.uuid)
            }
        } else {
            userEpisodeDao.updateDuration(durationInSecs, episode.uuid)
        }
    }

    override fun updateDownloadErrorDetails(episode: Playable?, message: String?) {
        if (episode == null) return
        episode.downloadErrorDetails = message
        if (episode is Episode) {
            episodeDao.update(episode)
        } else if (episode is UserEpisode) {
            runBlocking { userEpisodeManager.updateDownloadErrorDetails(episode, message) }
        }
    }

    override fun updateDownloadTaskId(episode: Playable, id: String?) {
        if (episode is Episode) {
            episodeDao.updateDownloadTaskId(episode.uuid, id)
        } else if (episode is UserEpisode) {
            runBlocking { userEpisodeManager.updateDownloadTaskId(episode, id) }
        }
    }

    override suspend fun updatePlaybackInteractionDate(episode: Playable?) {
        if (episode == null) {
            return
        }

        // We don't have a playback interaction date for user episodes, just episodes
        if (episode is Episode) {
            episodeDao.updatePlaybackInteractionDate(episode.uuid, System.currentTimeMillis())
        }
    }

    override fun updatePlayingStatus(episode: Playable?, status: EpisodePlayingStatus) {
        if (episode == null) {
            return
        }
        episode.playingStatus = status
        if (episode is Episode) {
            episodeDao.updatePlayingStatus(status, System.currentTimeMillis(), episode.uuid)
        } else {
            userEpisodeDao.updatePlayingStatus(status, System.currentTimeMillis(), episode.uuid)
        }
    }

    override fun updateEpisodeStatus(episode: Playable?, status: EpisodeStatusEnum) {
        episode ?: return
        episode.episodeStatus = status

        if (episode is Episode) {
            episodeDao.updateEpisodeStatus(status, episode.uuid)
        } else if (episode is UserEpisode) {
            runBlocking { userEpisodeManager.updateEpisodeStatus(episode, status) }
        }
    }

    override fun updateAllEpisodeStatus(episodeStatus: EpisodeStatusEnum) {
        episodeDao.updateAllEpisodeStatus(episodeStatus)
    }

    override fun updateAutoDownloadStatus(episode: Playable?, autoDownloadStatus: Int) {
        episode ?: return
        episode.autoDownloadStatus = autoDownloadStatus

        if (episode is Episode) {
            episodeDao.updateAutoDownloadStatus(autoDownloadStatus, episode.uuid)
        } else {
            userEpisodeDao.updateAutoDownloadStatus(autoDownloadStatus, episode.uuid)
        }
    }

    override fun updateDownloadFilePath(episode: Playable?, filePath: String, markAsDownloaded: Boolean) {
        episode ?: return
        episode.downloadedFilePath = filePath
        if (episode is Episode) {
            episodeDao.updateDownloadedFilePath(filePath, episode.uuid)
        } else if (episode is UserEpisode) {
            runBlocking { userEpisodeManager.updateDownloadedFilePath(episode, filePath) }
        }

        if (markAsDownloaded) {
            updateEpisodeStatus(episode, EpisodeStatusEnum.DOWNLOADED)
        }
    }

    override fun updateFileType(episode: Playable?, fileType: String) {
        episode ?: return
        if (episode is Episode) {
            episodeDao.updateFileType(fileType, episode.uuid)
        } else if (episode is UserEpisode) {
            episode.fileType = fileType
            runBlocking { userEpisodeManager.updateFileType(episode, fileType) }
        }
    }

    override fun updateSizeInBytes(episode: Playable?, sizeInBytes: Long) {
        episode ?: return
        if (episode is Episode) {
            episodeDao.updateSizeInBytes(sizeInBytes, episode.uuid)
        } else if (episode is UserEpisode) {
            episode.sizeInBytes = sizeInBytes
            runBlocking { userEpisodeManager.updateSizeInBytes(episode, sizeInBytes) }
        }
    }

    override fun updateDownloadUrl(episode: Playable?, url: String) {
        episode ?: return
        if (episode is Episode) {
            episodeDao.updateDownloadUrl(url, episode.uuid)
        } else if (episode is UserEpisode) {
            episode.downloadUrl = url
            // We shouldn't hold on to these urls in the database
        }
    }

    override fun updateLastDownloadAttemptDate(episode: Playable?) {
        episode ?: return
        val now = Date()
        episode.lastDownloadAttemptDate = now
        if (episode is Episode) {
            episodeDao.updateLastDownloadAttemptDate(now, episode.uuid)
        } else if (episode is UserEpisode) {
            runBlocking { userEpisodeManager.updateLastDownloadDate(episode, now) }
        }
    }

    override fun starEpisode(episode: Episode, starred: Boolean) {
        episode.isStarred = starred
        episodeDao.updateStarred(starred, System.currentTimeMillis(), episode.uuid)
    }

    override suspend fun updateAllStarred(episodes: List<Episode>, starred: Boolean) {
        episodes.chunked(500).forEach { episodesChunk ->
            episodeDao.updateAllStarred(episodesChunk.map { it.uuid }, starred, System.currentTimeMillis())
        }
    }

    @OptIn(DelicateCoroutinesApi::class)
    override fun toggleStarEpisodeAsync(episode: Episode) {
        GlobalScope.launch {
            findByUuid(episode.uuid)?.let {
                starEpisode(episode, !it.isStarred)
            }
        }
    }

    override fun markAsNotPlayed(episode: Playable?) {
        episode ?: return
        updatePlayedUpTo(episode, 0.0, false)
        updatePlayingStatus(episode, EpisodePlayingStatus.NOT_PLAYED)
        unarchive(episode)
    }

    private fun downloadEpisodesFileDetails(episodes: List<Episode>?) {
        if (episodes == null || episodes.isEmpty()) {
            return
        }

        val episodeUuids = episodes.map { it.uuid }.toTypedArray()
        val workData = Data.Builder()
            .putStringArray(UpdateEpisodeDetailsTask.INPUT_EPISODE_UUIDS, episodeUuids)
            .build()
        val workRequest = OneTimeWorkRequestBuilder<UpdateEpisodeDetailsTask>()
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .setInputData(workData)
            .build()
        WorkManager.getInstance(context).beginUniqueWork(UpdateEpisodeDetailsTask.TASK_NAME, ExistingWorkPolicy.APPEND, workRequest).enqueue()
    }

    override suspend fun markAllAsPlayed(playables: List<Playable>, playbackManager: PlaybackManager, podcastManager: PodcastManager) {
        val justEpisodes = playables.filterIsInstance<Episode>()
        justEpisodes.chunked(500).forEach { episodeDao.updateAllPlayingStatus(it.map { it.uuid }, System.currentTimeMillis(), EpisodePlayingStatus.COMPLETED) }
        archiveAllPlayedEpisodes(justEpisodes, playbackManager, podcastManager)

        justEpisodes.forEach { playbackManager.removeEpisode(it) }

        userEpisodeManager.markAllAsPlayed(playables.filterIsInstance<UserEpisode>(), playbackManager)
    }

    override fun markAsUnplayed(episodes: List<Playable>) {
        launch {
            val justEpisodes = episodes.filterIsInstance<Episode>()
            justEpisodes.chunked(500).forEach {
                episodeDao.markAllUnplayed(it.map { it.uuid }, System.currentTimeMillis())
            }
            unarchiveAllInList(justEpisodes)

            val justUserEpisodes = episodes.filterIsInstance<UserEpisode>()
            userEpisodeManager.markAllAsUnplayed(justUserEpisodes)
        }
    }

    override fun markedAsPlayedExternally(episode: Episode, playbackManager: PlaybackManager, podcastManager: PodcastManager) {
        playbackManager.removeEpisode(episode)

        // Auto archive after playing if the episode isn't already archived
        if (!episode.isArchived) {
            archivePlayedEpisode(episode, playbackManager, podcastManager, sync = true)
        }
    }

    override fun markAsPlayedAsync(episode: Playable?, playbackManager: PlaybackManager, podcastManager: PodcastManager) {
        launch {
            markAsPlayed(episode, playbackManager, podcastManager)
        }
    }

    override fun markAsPlayed(episode: Playable?, playbackManager: PlaybackManager, podcastManager: PodcastManager) {
        if (episode == null) {
            return
        }

        playbackManager.removeEpisode(episode)

        episode.playingStatus = EpisodePlayingStatus.COMPLETED

        updatePlayingStatus(episode, EpisodePlayingStatus.COMPLETED)

        // Auto archive after playing
        archivePlayedEpisode(episode, playbackManager, podcastManager, sync = true)
    }

    override fun deleteEpisodesWithoutSync(episodes: List<Episode>, playbackManager: PlaybackManager) {
        if (episodes.isEmpty()) {
            return
        }
        for (episode in episodes) {
            deleteEpisodeFile(episode, playbackManager, disableAutoDownload = false, updateDatabase = false)
        }
        episodeDao.deleteAll(episodes)
    }

    override fun deleteEpisodeWithoutSync(episode: Episode?, playbackManager: PlaybackManager) {
        episode ?: return

        deleteEpisodeFile(episode, playbackManager, false, false)

        episodeDao.delete(episode)
    }

    override fun deleteEpisodeFile(episode: Playable?, playbackManager: PlaybackManager?, disableAutoDownload: Boolean, updateDatabase: Boolean, removeFromUpNext: Boolean) {
        episode ?: return

        Timber.d("Deleting episode file ${episode.title}")

        // if the episode is currently downloading, kill the download
        downloadManager.removeEpisodeFromQueue(episode, "file deleted")

        // if the episode is currently playing, then stop it. Note: it will not be stopped if coming from the player as it is controlling the playback logic.
        if (removeFromUpNext) {
            playbackManager?.removeEpisode(episode)
        }

        cleanUpDownloadFiles(episode)

        if (updateDatabase) {
            updateEpisodeStatus(episode, EpisodeStatusEnum.NOT_DOWNLOADED)
            if (disableAutoDownload) {
                updateAutoDownloadStatus(episode, Episode.AUTO_DOWNLOAD_STATUS_IGNORE)
            }
        }
    }

    private fun cleanUpDownloadFiles(episode: Playable) {
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

    override fun stopDownloadAndCleanUp(episode: Episode, from: String) {
        downloadManager.removeEpisodeFromQueue(episode, from)
        cleanUpDownloadFiles(episode)
    }

    override fun countEpisodes(): Int {
        return episodeDao.count()
    }

    override fun countEpisodesWhere(queryAfterWhere: String): Int {
        return episodeDao.countWhere(queryAfterWhere, appDatabase)
    }

    override fun deleteCustomFolderEpisode(episode: Episode?, playbackManager: PlaybackManager) {
        episode ?: return

        playbackManager.removeEpisode(episode)

        episodeDao.delete(episode)
    }

    override fun add(episode: Episode, downloadMetaData: Boolean): Boolean {
        val episodes = ArrayList<Episode>()
        episodes.add(episode)
        val addedEpisodes = add(episodes, episode.podcastUuid, downloadMetaData)
        return addedEpisodes.size == 1
    }

    override fun update(episode: Episode?) {
        episode ?: return
        episodeDao.update(episode)
    }

    override fun setEpisodeThumbnailStatus(episode: Episode?, thumbnailStatus: Int) {
        episode ?: return
        episode.thumbnailStatus = thumbnailStatus
        episodeDao.updateThumbnailStatus(thumbnailStatus, episode.uuid)
    }

    override fun setDownloadFailed(episode: Playable, errorMessage: String) {
        if (episode is Episode) {
            episodeDao.updateDownloadError(episode.uuid, errorMessage, EpisodeStatusEnum.DOWNLOAD_FAILED)
        } else if (episode is UserEpisode) {
            runBlocking {
                userEpisodeManager.updateDownloadErrorDetails(episode, errorMessage)
                userEpisodeManager.updateEpisodeStatus(episode, EpisodeStatusEnum.DOWNLOAD_FAILED)
            }
        }
    }

    override fun clearPlaybackError(episode: Playable?) {
        if (episode?.playErrorDetails == null) {
            return
        }
        markAsPlaybackError(episode, null)
    }

    override fun clearDownloadError(episode: Episode?) {
        episode ?: return
        episodeDao.updateDownloadErrorDetails(null, episode.uuid)
        updateEpisodeStatus(episode, EpisodeStatusEnum.NOT_DOWNLOADED)
        episode.episodeStatus = EpisodeStatusEnum.NOT_DOWNLOADED
        episode.downloadErrorDetails = null
    }

    override fun archivePlayedEpisode(episode: Playable, playbackManager: PlaybackManager, podcastManager: PodcastManager, sync: Boolean) {
        launch {
            if (episode !is Episode) return@launch
            // check if we are meant to archive after episode is played
            val podcast = podcastManager.findPodcastByUuid(episode.podcastUuid) ?: return@launch
            val podcastOverrideSettings = podcast.overrideGlobalArchive
            val podcastArchiveAfterPlaying = Settings.AutoArchiveAfterPlaying.fromIndex(podcast.autoArchiveAfterPlaying)

            val shouldArchiveBasedOnSettings = shouldArchiveBasedOnSettings(podcastOverrideSettings, podcastArchiveAfterPlaying)

            if (shouldArchiveBasedOnSettings &&
                !settings.getAutoArchiveExcludedPodcasts().contains(episode.podcastUuid) &&
                (settings.getAutoArchiveIncludeStarred() || !episode.isStarred)
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

    private fun shouldArchiveBasedOnSettings(podcastOverrideSettings: Boolean, podcastArchiveAfterPlaying: Settings.AutoArchiveAfterPlaying) =
        (
            (!podcastOverrideSettings && settings.getAutoArchiveAfterPlaying() == Settings.AutoArchiveAfterPlaying.AfterPlaying) ||
                (podcastArchiveAfterPlaying == Settings.AutoArchiveAfterPlaying.AfterPlaying)
            )

    @Suppress("NAME_SHADOWING")
    private suspend fun archiveAllPlayedEpisodes(episodes: List<Episode>, playbackManager: PlaybackManager, podcastManager: PodcastManager) {
        val episodesWithoutStarred = if (!settings.getAutoArchiveIncludeStarred()) episodes.filter { !it.isStarred } else episodes // Remove starred episodes if we have to
        val episodesByPodcast = episodesWithoutStarred.groupBy { it.podcastUuid }.toMutableMap() // Sort in to podcasts
        val excludedPodcasts = settings.getAutoArchiveExcludedPodcasts()
        excludedPodcasts.forEach { episodesByPodcast.remove(it) } // Remove all excluded podcasts

        for ((podcastUuid, episodes) in episodesByPodcast) {
            val podcast = podcastManager.findPodcastByUuid(podcastUuid) ?: continue
            val podcastArchiveAfterPlaying = Settings.AutoArchiveAfterPlaying.fromIndex(podcast.autoArchiveAfterPlaying)
            val shouldArchiveBasedOnSettings = shouldArchiveBasedOnSettings(podcast.overrideGlobalSettings, podcastArchiveAfterPlaying)

            if (shouldArchiveBasedOnSettings) {
                archiveAllInList(episodes, playbackManager)
            }
        }
    }

    override fun archive(episode: Episode, playbackManager: PlaybackManager, sync: Boolean) {
        if (sync) {
            episodeDao.updateArchived(true, System.currentTimeMillis(), episode.uuid)
        } else {
            episodeDao.updateArchivedNoSync(true, System.currentTimeMillis(), episode.uuid)
        }
        episode.isArchived = true
        cleanUpEpisode(episode, playbackManager)
    }

    @Suppress("NAME_SHADOWING")
    private fun cleanUpEpisode(episode: Playable, playbackManager: PlaybackManager?) {
        val playbackManager = playbackManager ?: return
        if (episode.isDownloaded || episode.isDownloading || episode.downloadTaskId != null) {
            downloadManager.removeEpisodeFromQueue(episode, "episode manager")
        }
        deleteEpisodeFile(episode, playbackManager, disableAutoDownload = true, updateDatabase = true, removeFromUpNext = true)
        playbackManager.removeEpisode(episode)
    }

    override suspend fun findStaleDownloads(): List<Episode> {
        return episodeDao.findStaleDownloads()
    }

    override fun unarchive(episode: Playable) {
        if (!episode.isArchived || episode !is Episode) {
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

    override fun markAsPlaybackError(episode: Playable?, errorMessage: String?) {
        episode ?: return
        episode.playErrorDetails = errorMessage

        when (episode) {
            is Episode -> episodeDao.updatePlayErrorDetails(errorMessage, episode.uuid)
            is UserEpisode -> userEpisodeDao.updatePlayError(episode.uuid, errorMessage)
        }
    }

    override fun markAsPlaybackError(episode: Playable?, event: PlayerEvent.PlayerError, isPlaybackRemote: Boolean) {
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
            is Episode -> episodeDao.updatePlayErrorDetails(message, episode.uuid)
            is UserEpisode -> userEpisodeDao.updatePlayError(episode.uuid, message)
        }
    }

    override fun deleteFinishedEpisodes(playbackManager: PlaybackManager) {
        val episodes = findEpisodesWhere("episode_status = " + EpisodeStatusEnum.DOWNLOADED.ordinal + " AND playing_status = " + EpisodePlayingStatus.COMPLETED.ordinal)
        if (episodes.isEmpty()) return

        for (episode in episodes) {
            deleteEpisodeFile(episode, playbackManager, true, true)
        }
    }

    override fun deleteDownloadedEpisodeFiles() {
        // remove all the files off the disk, ignore any errors and continue
        try {
            fileStorage.removeDirectoryFiles(fileStorage.podcastDirectory)
            fileStorage.removeDirectoryFiles(fileStorage.tempPodcastDirectory)
        } catch (e: Exception) {
            Timber.e(e)
        }

        val episodes = findEpisodesWhere("episode_status = " + EpisodeStatusEnum.DOWNLOADED.ordinal)
        if (episodes.isEmpty()) return

        for (episode in episodes) {
            updateEpisodeStatus(episode, EpisodeStatusEnum.NOT_DOWNLOADED)
        }
    }

    override fun deleteEpisodes(episodes: List<Episode>, playbackManager: PlaybackManager) {
        val episodesCopy = episodes.toList()
        launch {
            deleteEpisodeFiles(episodesCopy, playbackManager)
        }
    }

    override suspend fun deleteEpisodeFiles(episodes: List<Episode>, playbackManager: PlaybackManager) = withContext(Dispatchers.IO) {
        episodes.toList().forEach {
            deleteEpisodeFile(it, playbackManager, removeFromUpNext = true, disableAutoDownload = false)
        }
    }

    override fun observeDownloadEpisodes(): Flowable<List<Episode>> {
        val failedDownloadCutoff = Date().time - 7.days()
        return episodeDao.observeDownloadingEpisodesIncludingFailed(failedDownloadCutoff)
    }

    override fun observeDownloadedEpisodes(): Flowable<List<Episode>> {
        return episodeDao.observeDownloadedEpisodes()
    }

    override fun observeStarredEpisodes(): Flowable<List<Episode>> {
        return episodeDao.observeStarredEpisodes()
    }

    override suspend fun findStarredEpisodes(): List<Episode> {
        return episodeDao.findStarredEpisodes()
    }

    override fun exists(episodeUuid: String): Boolean {
        return episodeDao.exists(episodeUuid)
    }

    override fun markAsNotPlayedRx(episode: Episode): Single<Episode> {
        return Single.fromCallable {
            markAsNotPlayed(episode)
            episode
        }
    }

    override fun rxMarkAsPlayed(episode: Episode, playbackManager: PlaybackManager, podcastManager: PodcastManager): Completable {
        return Completable.fromAction { markAsPlayed(episode, playbackManager, podcastManager) }
    }

    override fun findEpisodesDownloading(queued: Boolean, waitingForPower: Boolean, waitingForWifi: Boolean, downloading: Boolean): List<Episode> {
        val sql = buildEpisodeStatusWhere(queued, waitingForPower, waitingForWifi, downloading)
        return findEpisodesWhere(sql)
    }

    override fun countEpisodesDownloading(queued: Boolean, waitingForPower: Boolean, waitingForWifi: Boolean, downloading: Boolean): Int {
        val sql = buildEpisodeStatusWhere(queued, waitingForPower, waitingForWifi, downloading)
        return countEpisodesWhere(sql)
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

    override fun add(episodes: List<Episode>, podcastUuid: String, downloadMetaData: Boolean): List<Episode> {
        val addedEpisodes = mutableListOf<Episode>()
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
            episodeDao.insertAll(addedEpisodes)
        }

        if (episodes.isNotEmpty()) {
            if (downloadMetaData) {
                downloadEpisodesFileDetails(episodes)
            }
        }

        return addedEpisodes
    }

    override fun insert(episodes: List<Episode>) {
        if (episodes.isNotEmpty()) {
            episodeDao.insertAll(episodes)
        }
    }

    override fun findEpisodesToSync(): List<Episode> {
        return episodeDao.findEpisodesToSync()
    }

    override fun findEpisodesForHistorySync(): List<Episode> {
        return episodeDao.findEpisodesForHistorySync()
    }

    override fun markAllEpisodesSynced(episodes: List<Episode>) {
        val episodeUuids = episodes.map { it.uuid }
        episodeUuids.chunked(500).forEach { chunked ->
            episodeDao.markAllSynced(chunked)
        }
    }

    // Playback manager is only optional for UI tests. Should never be optional in the app but can't work out
    // another way without mocking a lot of stuff.
    override fun archiveAllInList(episodes: List<Episode>, playbackManager: PlaybackManager?) {
        episodes.filter { !it.isArchived }.chunked(500).forEach { chunked ->
            episodeDao.archiveAllInList(chunked.map { it.uuid }, System.currentTimeMillis())
            playbackManager?.let { playbackManager ->
                chunked.forEach {
                    cleanUpEpisode(it, playbackManager)
                }
            }
        }
    }

    override fun unarchiveAllInListAsync(episodes: List<Episode>) {
        launch {
            unarchiveAllInList(episodes)
        }
    }

    override fun unarchiveAllInList(episodes: List<Episode>) {
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

        val podcastArchivePlaying = Settings.AutoArchiveAfterPlaying.fromIndex(podcast.autoArchiveAfterPlaying)
        val autoArchiveSetting = if (podcast.overrideGlobalArchive) podcastArchivePlaying else settings.getAutoArchiveAfterPlaying()
        val autoArchiveAfterPlayingTime = autoArchiveSetting.timeSeconds * 1000L

        if (autoArchiveAfterPlayingTime > 0) {
            val playedEpisodes = episodeDao.findByEpisodePlayingAndArchiveStatus(podcast.uuid, EpisodePlayingStatus.COMPLETED, false)
                .filter { (settings.getAutoArchiveIncludeStarred() && it.isStarred) || !it.isStarred }
                .filter { it.lastPlaybackInteractionDate != null && now.time - it.lastPlaybackInteractionDate!!.time > autoArchiveAfterPlayingTime }

            archiveAllInList(playedEpisodes, playbackManager)
            playedEpisodes.forEach {
                cleanUpEpisode(it, playbackManager)
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Auto archiving played episode ${it.title}")
            }
        }

        val podcastInactiveSetting = Settings.AutoArchiveInactive.fromIndex(podcast.autoArchiveInactive)
        val inactiveSetting = if (podcast.overrideGlobalArchive) podcastInactiveSetting else settings.getAutoArchiveInactive()

        val inactiveTime = inactiveSetting.timeSeconds * 1000L
        if (inactiveTime > 0) {
            val inactiveEpisodes = episodeDao.findInactiveEpisodes(podcast.uuid, Date(now.time - inactiveTime))
                .filter { settings.getAutoArchiveIncludeStarred() || !it.isStarred }
            if (inactiveEpisodes.isNotEmpty()) {
                archiveAllInList(inactiveEpisodes, playbackManager)
                inactiveEpisodes.forEach {
                    cleanUpEpisode(it, playbackManager)
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
                    .filter { (settings.getAutoArchiveIncludeStarred() && it.isStarred) || !it.isStarred }
                    .filter { playbackManager?.getCurrentEpisode()?.uuid != it.uuid }
                if (episodesToRemove.isNotEmpty()) {
                    archiveAllInList(episodesToRemove, playbackManager)
                    episodesToRemove.forEach {
                        cleanUpEpisode(it, playbackManager)
                        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Auto archiving episode over limit $episodeLimit ${it.title}")
                    }
                }
            }
        }
    }

    override fun userHasInteractedWithEpisode(episode: Episode, playbackManager: PlaybackManager): Boolean {
        return episode.isStarred ||
            episode.isArchived ||
            episode.isDownloaded ||
            episode.isFinished ||
            episode.isInProgress ||
            playbackManager.upNextQueue.contains(episode.uuid) ||
            episode.lastPlaybackInteraction != null
    }

    override fun episodeCanBeCleanedUp(episode: Episode, playbackManager: PlaybackManager): Boolean {
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
    override fun downloadMissingEpisode(episodeUuid: String, podcastUuid: String, skeletonEpisode: Episode, podcastManager: PodcastManager, downloadMetaData: Boolean): Maybe<Playable> {
        return episodeDao.existsRx(episodeUuid)
            .flatMapMaybe { episodeExists ->
                if (episodeExists || podcastUuid == UserEpisodePodcastSubstitute.uuid) {
                    observePlayableByUuid(episodeUuid).firstElement()
                } else {
                    podcastCacheServerManager.getPodcastAndEpisode(podcastUuid, episodeUuid).flatMapMaybe { response ->
                        Timber.i("HistoryManager downloadMissingEpisode episode found? ${response.episodes.firstOrNull() != null}")
                        val episode = response.episodes.firstOrNull() ?: skeletonEpisode
                        add(episode, downloadMetaData = downloadMetaData)
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

    override suspend fun findEpisodeInteractedBefore(fromEpochMs: Long): Episode? =
        episodeDao.findEpisodeInteractedBefore(fromEpochMs)

    override suspend fun countEpisodesInListeningHistory(fromEpochMs: Long, toEpochMs: Long): Int =
        episodeDao.findEpisodesCountInListeningHistory(fromEpochMs, toEpochMs)
}

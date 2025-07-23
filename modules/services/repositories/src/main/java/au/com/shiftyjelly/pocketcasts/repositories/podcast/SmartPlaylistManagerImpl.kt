package au.com.shiftyjelly.pocketcasts.repositories.podcast

import android.content.Context
import android.os.Build
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.SmartPlaylist
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadHelper
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.extensions.calculateCombinedIconId
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationManager
import au.com.shiftyjelly.pocketcasts.repositories.notification.OnboardingNotificationType
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.shortcuts.PocketCastsShortcuts
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import java.util.Calendar
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import timber.log.Timber

class SmartPlaylistManagerImpl @Inject constructor(
    private val settings: Settings,
    private val downloadManager: DownloadManager,
    private val playlistUpdateAnalytics: PlaylistUpdateAnalytics,
    private val syncManager: SyncManager,
    private val notificationManager: NotificationManager,
    @ApplicationContext private val context: Context,
    appDatabase: AppDatabase,
) : SmartPlaylistManager,
    CoroutineScope {

    companion object {
        const val NEW_RELEASE_UUID = "2797DCF8-1C93-4999-B52A-D1849736FA2C"
        const val IN_PROGRESS_UUID = "D89A925C-5CE1-41A4-A879-2751838CE5CE"
        private const val NEW_RELEASE_TITLE = "New Releases"
        private const val IN_PROGRESS_TITLE = "In Progress"
        private const val CREATED_DEFAULT_PLAYLISTS = "createdDefaultPlaylists"
    }

    private val playlistDao = appDatabase.smartPlaylistDao()

    init {
        if (!settings.getBooleanForKey(CREATED_DEFAULT_PLAYLISTS, false)) {
            launch { setupDefaultPlaylists() }
        }
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private fun setupDefaultPlaylists() {
        val existingNewRelease = playlistDao.findByUuidBlocking(NEW_RELEASE_UUID)
        if (existingNewRelease == null) {
            val newRelease = SmartPlaylist()
            newRelease.apply {
                unplayed = true
                partiallyPlayed = true
                audioVideo = SmartPlaylist.AUDIO_VIDEO_FILTER_ALL
                allPodcasts = true
                sortPosition = 0
                title = NEW_RELEASE_TITLE
                downloaded = true
                notDownloaded = true
                filterHours = SmartPlaylist.LAST_2_WEEKS
                uuid = NEW_RELEASE_UUID
                syncStatus = SmartPlaylist.SYNC_STATUS_SYNCED
                iconId = SmartPlaylist.calculateCombinedIconId(colorIndex = 0, iconIndex = 2) // Red clock
            }
            playlistDao.insertBlocking(newRelease)
        } else {
            existingNewRelease.iconId = 10
            playlistDao.updateBlocking(existingNewRelease)
        }

        val existingInProgress = playlistDao.findByUuidBlocking(IN_PROGRESS_UUID)
        if (existingInProgress == null) {
            val inProgress = SmartPlaylist()
            inProgress.apply {
                allPodcasts = true
                audioVideo = SmartPlaylist.AUDIO_VIDEO_FILTER_ALL
                sortPosition = 2
                title = IN_PROGRESS_TITLE
                downloaded = true
                notDownloaded = true
                unplayed = false
                partiallyPlayed = true
                finished = false
                filterHours = SmartPlaylist.LAST_MONTH
                uuid = IN_PROGRESS_UUID
                syncStatus = SmartPlaylist.SYNC_STATUS_SYNCED
                iconId = SmartPlaylist.calculateCombinedIconId(colorIndex = 3, iconIndex = 4) // Purple play
            }
            playlistDao.insertBlocking(inProgress)
        } else {
            existingInProgress.iconId = 43
            playlistDao.updateBlocking(existingInProgress)
        }

        settings.setBooleanForKey(CREATED_DEFAULT_PLAYLISTS, true)
    }

    override fun findAllBlocking(): List<SmartPlaylist> {
        return playlistDao.findAllBlocking()
    }

    override suspend fun findAll(): List<SmartPlaylist> {
        return playlistDao.findAll()
    }

    override fun findAllFlow(): Flow<List<SmartPlaylist>> {
        return playlistDao.findAllFlow()
    }

    override fun findAllRxFlowable(): Flowable<List<SmartPlaylist>> {
        return playlistDao.findAllRxFlowable()
    }

    override fun findByUuidBlocking(playlistUuid: String): SmartPlaylist? {
        return playlistDao.findByUuidBlocking(playlistUuid)
    }

    override suspend fun findByUuid(playlistUuid: String): SmartPlaylist? {
        return playlistDao.findByUuid(playlistUuid)
    }

    override fun findByUuidRxMaybe(playlistUuid: String): Maybe<SmartPlaylist> {
        return playlistDao.findByUuidRxMaybe(playlistUuid)
    }

    override fun findByUuidRxFlowable(playlistUuid: String): Flowable<SmartPlaylist> {
        return playlistDao.findByUuidRxFlowable(playlistUuid)
    }

    override fun findByUuidAsListRxFlowable(playlistUuid: String): Flowable<List<SmartPlaylist>> {
        return playlistDao.findByUuidAsListRxFlowable(playlistUuid)
    }

    override fun findByIdBlocking(id: Long): SmartPlaylist? {
        return playlistDao.findByIdBlocking(id)
    }

    override fun findEpisodesBlocking(smartPlaylist: SmartPlaylist, episodeManager: EpisodeManager, playbackManager: PlaybackManager): List<PodcastEpisode> {
        val where = buildPlaylistWhere(smartPlaylist, playbackManager)
        val orderBy = getPlaylistOrderByString(smartPlaylist)
        val limit = if (smartPlaylist.sortType == PlaylistEpisodeSortType.LastDownloadAttempt) 1000 else 500
        return episodeManager.findEpisodesWhereBlocking("$where ORDER BY $orderBy LIMIT $limit")
    }

    private fun getPlaylistQuery(smartPlaylist: SmartPlaylist, limit: Int?, playbackManager: PlaybackManager): String {
        val where = buildPlaylistWhere(smartPlaylist, playbackManager)
        val orderBy = getPlaylistOrderByString(smartPlaylist)
        return "$where ORDER BY $orderBy" + if (limit != null) " LIMIT $limit" else ""
    }

    override fun observeEpisodesBlocking(smartPlaylist: SmartPlaylist, episodeManager: EpisodeManager, playbackManager: PlaybackManager): Flowable<List<PodcastEpisode>> {
        val limitCount = if (smartPlaylist.sortType == PlaylistEpisodeSortType.LastDownloadAttempt) 1000 else 500
        val queryAfterWhere = getPlaylistQuery(smartPlaylist, limit = limitCount, playbackManager = playbackManager)
        return episodeManager.findEpisodesWhereRxFlowable(queryAfterWhere)
    }

    override fun observeEpisodesPreviewBlocking(smartPlaylist: SmartPlaylist, episodeManager: EpisodeManager, playbackManager: PlaybackManager): Flowable<List<PodcastEpisode>> {
        val queryAfterWhere = getPlaylistQuery(smartPlaylist, limit = 100, playbackManager = playbackManager)
        return episodeManager.findEpisodesWhereRxFlowable(queryAfterWhere)
    }

    private fun getPlaylistOrderByString(smartPlaylist: SmartPlaylist): String? = when (val sortType = smartPlaylist.sortType) {
        PlaylistEpisodeSortType.NewestToOldest,
        PlaylistEpisodeSortType.OldestToNewest,
        -> {
            "published_date " +
                (if (sortType == PlaylistEpisodeSortType.NewestToOldest) "DESC" else "ASC") +
                ", added_date " +
                if (sortType == PlaylistEpisodeSortType.NewestToOldest) "DESC" else "ASC"
        }

        PlaylistEpisodeSortType.ShortestToLongest,
        PlaylistEpisodeSortType.LongestToShortest,
        -> {
            "duration " +
                (if (sortType == PlaylistEpisodeSortType.ShortestToLongest) "ASC" else "DESC") +
                ", added_date DESC"
        }

        PlaylistEpisodeSortType.LastDownloadAttempt -> {
            "last_download_attempt_date DESC, published_date DESC"
        }
    }

    override suspend fun create(smartPlaylist: SmartPlaylist): Long {
        val id = playlistDao.insert(smartPlaylist)
        if (countPlaylists() == 1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            PocketCastsShortcuts.update(
                smartPlaylistManager = this,
                force = true,
                context = context,
                source = PocketCastsShortcuts.Source.CREATE_PLAYLIST,
            )
        }
        return id
    }

    /**
     * A null userPlayListUpdate parameter indicates that  the user did not initiate this update
     */
    override fun updateBlocking(
        smartPlaylist: SmartPlaylist,
        userPlaylistUpdate: UserPlaylistUpdate?,
        isCreatingFilter: Boolean,
    ) {
        if (isCreatingFilter) {
            launch(Dispatchers.IO) {
                notificationManager.updateUserFeatureInteraction(OnboardingNotificationType.Filters)
            }
        }
        playlistDao.updateBlocking(smartPlaylist)
        playlistUpdateAnalytics.update(smartPlaylist, userPlaylistUpdate, isCreatingFilter)
    }

    override suspend fun update(
        smartPlaylist: SmartPlaylist,
        userPlaylistUpdate: UserPlaylistUpdate?,
        isCreatingFilter: Boolean,
    ) {
        playlistDao.update(smartPlaylist)
        playlistUpdateAnalytics.update(smartPlaylist, userPlaylistUpdate, isCreatingFilter)
    }

    override fun updateAllBlocking(smartPlaylists: List<SmartPlaylist>) {
        playlistDao.updateAllBlocking(smartPlaylists)
    }

    override fun updateAutoDownloadStatus(smartPlaylist: SmartPlaylist, autoDownloadEnabled: Boolean, unmeteredOnly: Boolean, powerOnly: Boolean) {
        smartPlaylist.autoDownload = autoDownloadEnabled
        smartPlaylist.autoDownloadUnmeteredOnly = unmeteredOnly
        smartPlaylist.autoDownloadPowerOnly = powerOnly
        val attrs = HashMap<String, Any>()
        attrs["autoDownload"] = autoDownloadEnabled
        attrs["autoDownloadWifiOnly"] = unmeteredOnly
        attrs["autoDownloadPowerOnly"] = powerOnly
        attrs["syncStatus"] = SmartPlaylist.SYNC_STATUS_NOT_SYNCED
    }

    override fun updateAutoDownloadStatusRxCompletable(smartPlaylist: SmartPlaylist, autoDownloadEnabled: Boolean, unmeteredOnly: Boolean, powerOnly: Boolean): Completable {
        return Completable.fromAction { updateAutoDownloadStatus(smartPlaylist, autoDownloadEnabled, unmeteredOnly, powerOnly) }
    }

    override fun createPlaylistBlocking(name: String, iconId: Int, draft: Boolean): SmartPlaylist {
        val smartPlaylist = SmartPlaylist(
            uuid = UUID.randomUUID().toString(),
            syncStatus = SmartPlaylist.SYNC_STATUS_NOT_SYNCED,
            title = name,
            sortPosition = countPlaylistsBlocking() + 1,
            manual = false,
            iconId = iconId,
            draft = draft,
        )

        Timber.d("Creating playlist ${smartPlaylist.uuid}")
        smartPlaylist.id = playlistDao.insertBlocking(smartPlaylist)
        return smartPlaylist
    }

    override fun deleteBlocking(smartPlaylist: SmartPlaylist) {
        val loggedIn = syncManager.isLoggedIn()
        if (loggedIn) {
            smartPlaylist.deleted = true
            markAsNotSyncedBlocking(smartPlaylist)

            // user initiated filter deletion, not update of any playlist properties, so this
            // is not a user initiated update
            updateBlocking(smartPlaylist, userPlaylistUpdate = null)
        }

        if (!loggedIn) {
            deleteSyncedBlocking(smartPlaylist)
        }
    }

    override fun deleteSyncedBlocking() {
        playlistDao.deleteDeletedBlocking()
    }

    override suspend fun resetDb() {
        playlistDao.deleteAll()
        setupDefaultPlaylists()
    }

    override suspend fun deleteSynced(smartPlaylist: SmartPlaylist) {
        playlistDao.delete(smartPlaylist)
    }

    override fun deleteSyncedBlocking(smartPlaylist: SmartPlaylist) {
        playlistDao.deleteBlocking(smartPlaylist)
    }

    override fun countEpisodesRxFlowable(smartPlaylist: SmartPlaylist, episodeManager: EpisodeManager, playbackManager: PlaybackManager): Flowable<Int> {
        val query = getPlaylistQuery(smartPlaylist, limit = null, playbackManager = playbackManager)
        return episodeManager.episodeCountRxFlowable(query)
    }

    override fun countEpisodesBlocking(id: Long?, episodeManager: EpisodeManager, playbackManager: PlaybackManager): Int {
        if (id == null) {
            return 0
        }
        val playlist = findByIdBlocking(id) ?: return 0
        val where = buildPlaylistWhere(playlist, playbackManager)
        return episodeManager.countEpisodesWhereBlocking(where)
    }

    override fun checkForEpisodesToDownloadBlocking(episodeManager: EpisodeManager, playbackManager: PlaybackManager) {
        val allPlaylists = findAllBlocking()
        if (allPlaylists.isEmpty()) return

        for (playlist in allPlaylists) {
            if (!playlist.autoDownload) continue

            findEpisodesBlocking(playlist, episodeManager, playbackManager).take(playlist.autodownloadLimit).forEach { episode ->
                if (episode.isQueued || episode.isDownloaded || episode.isDownloading || episode.isExemptFromAutoDownload) {
                    return@forEach
                }

                DownloadHelper.addAutoDownloadedEpisodeToQueue(episode, "playlist " + playlist.title, downloadManager, episodeManager, source = SourceView.PODCAST_LIST)
            }
        }
    }

    override fun removePodcastFromPlaylistsBlocking(podcastUuid: String) {
        if (podcastUuid.isBlank()) {
            return
        }
        val playlists = findAllBlocking()
        for (playlist in playlists) {
            val podcastUuids = playlist.podcastUuidList.toMutableList()
            if (playlist.allPodcasts || podcastUuids.isEmpty()) {
                continue
            }

            if (podcastUuids.contains(podcastUuid)) {
                podcastUuids.remove(podcastUuid)

                playlist.syncStatus = SmartPlaylist.SYNC_STATUS_NOT_SYNCED
                playlist.podcastUuidList = podcastUuids
                playlistDao.updateBlocking(playlist)
            }
        }
    }

    override fun findFirstByTitleBlocking(title: String): SmartPlaylist? {
        return playlistDao.searchByTitleBlocking(title)
    }

    override fun findPlaylistsToSyncBlocking(): List<SmartPlaylist> {
        return playlistDao.findNotSyncedBlocking()
    }

    /**
     * Build the SQL query for a playlist
     * @param smartPlaylist The playlist to generate the query for
     * @param playbackManager Required if the filter needs to include the currently playing episode regardless of if it meets the filter conditions.
     */
    private fun buildPlaylistWhere(smartPlaylist: SmartPlaylist, playbackManager: PlaybackManager?): String {
        val where = StringBuilder()
        buildFilterEpisodeWhere(smartPlaylist, where, playbackManager)
        return where.toString()
    }

    private fun buildFilterEpisodeWhere(smartPlaylist: SmartPlaylist, where: StringBuilder, playbackManager: PlaybackManager?) {
        val unplayed = smartPlaylist.unplayed
        val finished = smartPlaylist.finished
        val partiallyPlayed = smartPlaylist.partiallyPlayed
        val downloaded = smartPlaylist.downloaded
        val downloading = smartPlaylist.isSystemDownloadsFilter || smartPlaylist.notDownloaded // regular filters no longer have a downloading but the not downloaded one should show in progress downloads
        val notDownloaded = smartPlaylist.notDownloaded
        val audioVideo = smartPlaylist.audioVideo
        val filterHours = smartPlaylist.filterHours
        val starred = smartPlaylist.starred
        val allPodcasts = smartPlaylist.allPodcasts
        val podcastUuids = smartPlaylist.podcastUuidList

        val includeFailedDownloads = smartPlaylist.isSystemDownloadsFilter

        // don't do any constraint if they are all unticked or ticked
        if (!(unplayed && partiallyPlayed && finished) && (unplayed || partiallyPlayed || finished)) {
            val sectionWhere = StringBuilder()
            if (unplayed) {
                sectionWhere.append("playing_status = ").append(EpisodePlayingStatus.NOT_PLAYED.ordinal)
            }
            if (partiallyPlayed) {
                if (sectionWhere.isNotEmpty()) {
                    sectionWhere.append(" OR ")
                }
                sectionWhere.append("playing_status = ").append(EpisodePlayingStatus.IN_PROGRESS.ordinal)
            }
            if (finished) {
                if (sectionWhere.isNotEmpty()) {
                    sectionWhere.append(" OR ")
                }
                sectionWhere.append("playing_status = ").append(EpisodePlayingStatus.COMPLETED.ordinal)
            }
            where.append("(").append(sectionWhere).append(")")
        }

        if (!(downloaded && downloading && notDownloaded) && (downloaded || downloading || notDownloaded)) {
            val sectionWhere = StringBuilder()
            if (downloaded) {
                sectionWhere.append("episode_status = ").append(EpisodeStatusEnum.DOWNLOADED.ordinal)
            }
            if (downloading) {
                if (sectionWhere.isNotEmpty()) {
                    sectionWhere.append(" OR ")
                }
                sectionWhere.append("episode_status IN (")
                    .append(EpisodeStatusEnum.DOWNLOADING.ordinal)
                    .append(",")
                    .append(EpisodeStatusEnum.QUEUED.ordinal)
                    .append(",")
                    .append(EpisodeStatusEnum.WAITING_FOR_POWER.ordinal)
                    .append(",")
                    .append(EpisodeStatusEnum.WAITING_FOR_WIFI.ordinal)
                    .append(")")
            }
            if (includeFailedDownloads) {
                if (sectionWhere.isNotEmpty()) {
                    sectionWhere.append(" OR ")
                }
                // download failed in the last one weeks
                sectionWhere.append("(episode_status = ")
                    .append(EpisodeStatusEnum.DOWNLOAD_FAILED.ordinal)
                    .append(" AND last_download_attempt_date > ")
                    .append(Date().time - 604800000)
                    .append(")")
            }
            if (notDownloaded) {
                if (sectionWhere.isNotEmpty()) {
                    sectionWhere.append(" OR ")
                }
                sectionWhere.append("(episode_status = ")
                    .append(EpisodeStatusEnum.NOT_DOWNLOADED.ordinal)
                    .append(" OR episode_status = ")
                    .append(EpisodeStatusEnum.DOWNLOAD_FAILED.ordinal)
                    .append(")")
            }

            if (where.isNotEmpty()) {
                where.append(" AND ")
            }
            where.append("(").append(sectionWhere).append(")")
        }

        if (audioVideo != SmartPlaylist.AUDIO_VIDEO_FILTER_ALL) {
            if (audioVideo == SmartPlaylist.AUDIO_VIDEO_FILTER_VIDEO_ONLY) {
                if (where.isNotEmpty()) {
                    where.append(" AND ")
                }
                where.append("file_type LIKE 'video/%'")
            }
            if (audioVideo == SmartPlaylist.AUDIO_VIDEO_FILTER_AUDIO_ONLY) {
                if (where.isNotEmpty()) {
                    where.append(" AND ")
                }
                where.append("file_type LIKE 'audio/%'")
            }
        }

        if (filterHours > 0) {
            if (where.isNotEmpty()) {
                where.append(" AND ")
            }
            val tomorrow = Calendar.getInstance()
            tomorrow.add(Calendar.HOUR_OF_DAY, -filterHours)
            where.append("published_date > ").append(tomorrow.timeInMillis)
        }

        if (starred) {
            if (where.isNotEmpty()) {
                where.append(" AND ")
            }
            where.append("starred = 1")
        }

        if (!allPodcasts && podcastUuids.isNotEmpty()) {
            if (where.isNotEmpty()) {
                where.append(" AND ")
            }
            where.append("podcast_id IN (")
            where.append(podcastUuids.joinToString(separator = ",") { value -> "'$value'" })
            where.append(")")
        }

        if (smartPlaylist.filterDuration) {
            if (where.isNotEmpty()) {
                where.append(" AND ")
            }
            val longerThan = smartPlaylist.longerThan * 60
            // we add 59s here to account for how the formatter doesn't show "10m" until you get to 10*60 seconds, that way our visual representation lines up with the filter times
            val shorterThan = smartPlaylist.shorterThan * 60 + 59

            where.append("(duration >= $longerThan AND duration <= $shorterThan) ")
        }

        // leave out the custom folder podcast
        if (where.isNotEmpty()) {
            where.append(" AND ")
        }
        where.append("archived = 0")

        val playingEpisode = playbackManager?.getCurrentEpisode()?.uuid
        val lastLoadedFromPodcastOrFilterUuid = settings.lastAutoPlaySource.value.id
        if (playingEpisode != null && lastLoadedFromPodcastOrFilterUuid == smartPlaylist.uuid) {
            where.insert(0, "(podcast_episodes.uuid = '$playingEpisode' OR (")
            where.append("))")
        }
    }

    private fun markAsNotSyncedBlocking(smartPlaylist: SmartPlaylist) {
        smartPlaylist.syncStatus = SmartPlaylist.SYNC_STATUS_NOT_SYNCED
        playlistDao.updateSyncStatusBlocking(SmartPlaylist.SYNC_STATUS_NOT_SYNCED, smartPlaylist.uuid)
    }

    fun countPlaylists(): Int {
        return playlistDao.countBlocking()
    }

    fun countPlaylistsBlocking(): Int {
        return playlistDao.countBlocking()
    }

    override fun getSystemDownloadsFilter(): SmartPlaylist {
        return SmartPlaylist(
            id = SmartPlaylist.PLAYLIST_ID_SYSTEM_DOWNLOADS,
            uuid = "",
            title = "Downloads",
            manual = false,
            downloaded = true,
            downloading = true,
            notDownloaded = false,
            unplayed = true,
            partiallyPlayed = true,
            finished = true,
            audioVideo = SmartPlaylist.AUDIO_VIDEO_FILTER_ALL,
            allPodcasts = true,
            autoDownload = false,
            sortType = PlaylistEpisodeSortType.LastDownloadAttempt,
        )
    }

    override suspend fun markAllSynced() {
        playlistDao.updateAllSyncStatus(SmartPlaylist.SYNC_STATUS_SYNCED)
    }
}

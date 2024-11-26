package au.com.shiftyjelly.pocketcasts.repositories.podcast

import android.content.Context
import android.os.Build
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadHelper
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.extensions.calculateCombinedIconId
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

private const val NEWRELEASE_UUID = "2797DCF8-1C93-4999-B52A-D1849736FA2C"
private const val INPROGRESS_UUID = "D89A925C-5CE1-41A4-A879-2751838CE5CE"
private const val CREATED_DEFAULT_PLAYLISTS = "createdDefaultPlaylists"

class PlaylistManagerImpl @Inject constructor(
    private val settings: Settings,
    private val downloadManager: DownloadManager,
    private val playlistUpdateAnalytics: PlaylistUpdateAnalytics,
    private val syncManager: SyncManager,
    @ApplicationContext private val context: Context,
    @ApplicationScope private val applicationScope: CoroutineScope,
    appDatabase: AppDatabase,
) : PlaylistManager, CoroutineScope {

    private val playlistDao = appDatabase.playlistDao()

    init {
        if (!settings.getBooleanForKey(CREATED_DEFAULT_PLAYLISTS, false)) {
            launch { setupDefaultPlaylists() }
        }
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private fun setupDefaultPlaylists() {
        val existingNewRelease = playlistDao.findByUuidBlocking(NEWRELEASE_UUID)
        if (existingNewRelease == null) {
            val newRelease = Playlist()
            newRelease.apply {
                unplayed = true
                partiallyPlayed = true
                audioVideo = Playlist.AUDIO_VIDEO_FILTER_ALL
                allPodcasts = true
                sortPosition = 0
                title = "New Releases"
                downloaded = true
                notDownloaded = true
                filterHours = Playlist.LAST_2_WEEKS
                uuid = NEWRELEASE_UUID
                syncStatus = Playlist.SYNC_STATUS_SYNCED
                iconId = Playlist.calculateCombinedIconId(colorIndex = 0, iconIndex = 2) // Red clock
            }
            playlistDao.insertBlocking(newRelease)
        } else {
            existingNewRelease.iconId = 10
            playlistDao.updateBlocking(existingNewRelease)
        }

        val existingInProgress = playlistDao.findByUuidBlocking(INPROGRESS_UUID)
        if (existingInProgress == null) {
            val inProgress = Playlist()
            inProgress.apply {
                allPodcasts = true
                audioVideo = Playlist.AUDIO_VIDEO_FILTER_ALL
                sortPosition = 2
                title = "In Progress"
                downloaded = true
                notDownloaded = true
                unplayed = false
                partiallyPlayed = true
                finished = false
                filterHours = Playlist.LAST_MONTH
                uuid = INPROGRESS_UUID
                syncStatus = Playlist.SYNC_STATUS_SYNCED
                iconId = Playlist.calculateCombinedIconId(colorIndex = 3, iconIndex = 4) // Purple play
            }
            playlistDao.insertBlocking(inProgress)
        } else {
            existingInProgress.iconId = 43
            playlistDao.updateBlocking(existingInProgress)
        }

        settings.setBooleanForKey(CREATED_DEFAULT_PLAYLISTS, true)
    }

    override fun findAllBlocking(): List<Playlist> {
        return playlistDao.findAllBlocking()
    }

    override suspend fun findAll(): List<Playlist> {
        return playlistDao.findAll()
    }

    override fun findAllFlow(): Flow<List<Playlist>> {
        return playlistDao.findAllFlow()
    }

    override fun findAllRxFlowable(): Flowable<List<Playlist>> {
        return playlistDao.findAllRxFlowable()
    }

    override fun findByUuidBlocking(playlistUuid: String): Playlist? {
        return playlistDao.findByUuidBlocking(playlistUuid)
    }

    override suspend fun findByUuid(playlistUuid: String): Playlist? {
        return playlistDao.findByUuid(playlistUuid)
    }

    override fun findByUuidRxMaybe(playlistUuid: String): Maybe<Playlist> {
        return playlistDao.findByUuidRxMaybe(playlistUuid)
    }

    override fun findByUuidRxFlowable(playlistUuid: String): Flowable<Playlist> {
        return playlistDao.findByUuidRxFlowable(playlistUuid)
    }

    override fun findByUuidAsListRxFlowable(playlistUuid: String): Flowable<List<Playlist>> {
        return playlistDao.findByUuidAsListRxFlowable(playlistUuid)
    }

    override fun findByIdBlocking(id: Long): Playlist? {
        return playlistDao.findByIdBlocking(id)
    }

    override fun findEpisodesBlocking(playlist: Playlist, episodeManager: EpisodeManager, playbackManager: PlaybackManager): List<PodcastEpisode> {
        val where = buildPlaylistWhere(playlist, playbackManager)
        val orderBy = getPlaylistOrderByString(playlist)
        val limit = if (playlist.sortOrder() == Playlist.SortOrder.LAST_DOWNLOAD_ATTEMPT_DATE) 1000 else 500
        return episodeManager.findEpisodesWhereBlocking("$where ORDER BY $orderBy LIMIT $limit")
    }

    private fun getPlaylistQuery(playlist: Playlist, limit: Int?, playbackManager: PlaybackManager): String {
        val where = buildPlaylistWhere(playlist, playbackManager)
        val orderBy = getPlaylistOrderByString(playlist)
        return "$where ORDER BY $orderBy" + if (limit != null) " LIMIT $limit" else ""
    }

    override fun observeEpisodesBlocking(playlist: Playlist, episodeManager: EpisodeManager, playbackManager: PlaybackManager): Flowable<List<PodcastEpisode>> {
        val limitCount = if (playlist.sortOrder() == Playlist.SortOrder.LAST_DOWNLOAD_ATTEMPT_DATE) 1000 else 500
        val queryAfterWhere = getPlaylistQuery(playlist, limit = limitCount, playbackManager = playbackManager)
        return episodeManager.findEpisodesWhereRxFlowable(queryAfterWhere)
    }

    override fun observeEpisodesPreviewBlocking(playlist: Playlist, episodeManager: EpisodeManager, playbackManager: PlaybackManager): Flowable<List<PodcastEpisode>> {
        val queryAfterWhere = getPlaylistQuery(playlist, limit = 100, playbackManager = playbackManager)
        return episodeManager.findEpisodesWhereRxFlowable(queryAfterWhere)
    }

    private fun getPlaylistOrderByString(playlist: Playlist): String? = when (playlist.sortOrder()) {
        Playlist.SortOrder.NEWEST_TO_OLDEST,
        Playlist.SortOrder.OLDEST_TO_NEWEST,
        -> {
            "published_date " +
                (if (playlist.sortOrder() == Playlist.SortOrder.NEWEST_TO_OLDEST) "DESC" else "ASC") +
                ", added_date " +
                if (playlist.sortOrder() == Playlist.SortOrder.NEWEST_TO_OLDEST) "DESC" else "ASC"
        }

        Playlist.SortOrder.SHORTEST_TO_LONGEST,
        Playlist.SortOrder.LONGEST_TO_SHORTEST,
        -> {
            "duration " +
                (if (playlist.sortOrder() == Playlist.SortOrder.SHORTEST_TO_LONGEST) "ASC" else "DESC") +
                ", added_date DESC"
        }

        Playlist.SortOrder.LAST_DOWNLOAD_ATTEMPT_DATE -> {
            "last_download_attempt_date DESC, published_date DESC"
        }

        else -> null
    }

    override fun createBlocking(playlist: Playlist): Long {
        val id = playlistDao.insertBlocking(playlist)
        if (countPlaylistsBlocking() == 1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            PocketCastsShortcuts.update(
                playlistManager = this,
                force = true,
                coroutineScope = applicationScope,
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
        playlist: Playlist,
        userPlaylistUpdate: UserPlaylistUpdate?,
        isCreatingFilter: Boolean,
    ) {
        playlistDao.updateBlocking(playlist)
        playlistUpdateAnalytics.update(playlist, userPlaylistUpdate, isCreatingFilter)
    }

    override fun updateAllBlocking(playlists: List<Playlist>) {
        playlistDao.updateAllBlocking(playlists)
    }

    override fun updateAutoDownloadStatus(playlist: Playlist, autoDownloadEnabled: Boolean, unmeteredOnly: Boolean, powerOnly: Boolean) {
        playlist.autoDownload = autoDownloadEnabled
        playlist.autoDownloadUnmeteredOnly = unmeteredOnly
        playlist.autoDownloadPowerOnly = powerOnly
        val attrs = HashMap<String, Any>()
        attrs["autoDownload"] = autoDownloadEnabled
        attrs["autoDownloadWifiOnly"] = unmeteredOnly
        attrs["autoDownloadPowerOnly"] = powerOnly
        attrs["syncStatus"] = Playlist.SYNC_STATUS_NOT_SYNCED
    }

    override fun updateAutoDownloadStatusRxCompletable(playlist: Playlist, autoDownloadEnabled: Boolean, unmeteredOnly: Boolean, powerOnly: Boolean): Completable {
        return Completable.fromAction { updateAutoDownloadStatus(playlist, autoDownloadEnabled, unmeteredOnly, powerOnly) }
    }

    override fun createPlaylistBlocking(name: String, iconId: Int, draft: Boolean): Playlist {
        val playlist = Playlist(
            uuid = UUID.randomUUID().toString(),
            syncStatus = Playlist.SYNC_STATUS_NOT_SYNCED,
            title = name,
            sortPosition = countPlaylistsBlocking() + 1,
            manual = false,
            iconId = iconId,
            draft = draft,
        )

        Timber.d("Creating playlist ${playlist.uuid}")
        playlist.id = playlistDao.insertBlocking(playlist)
        return playlist
    }

    override fun deleteBlocking(playlist: Playlist) {
        val loggedIn = syncManager.isLoggedIn()
        if (loggedIn) {
            playlist.deleted = true
            markAsNotSyncedBlocking(playlist)

            // user initiated filter deletion, not update of any playlist properties, so this
            // is not a user initiated update
            updateBlocking(playlist, userPlaylistUpdate = null)
        }

        if (!loggedIn) {
            deleteSyncedBlocking(playlist)
        }
    }

    override fun deleteSyncedBlocking() {
        playlistDao.deleteDeletedBlocking()
    }

    override suspend fun resetDb() {
        playlistDao.deleteAll()
        setupDefaultPlaylists()
    }

    override fun deleteSyncedBlocking(playlist: Playlist) {
        playlistDao.deleteBlocking(playlist)
    }

    override fun countEpisodesRxFlowable(playlist: Playlist, episodeManager: EpisodeManager, playbackManager: PlaybackManager): Flowable<Int> {
        val query = getPlaylistQuery(playlist, limit = null, playbackManager = playbackManager)
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

                playlist.syncStatus = Playlist.SYNC_STATUS_NOT_SYNCED
                playlist.podcastUuidList = podcastUuids
                playlistDao.updateBlocking(playlist)
            }
        }
    }

    override fun findFirstByTitleBlocking(title: String): Playlist? {
        return playlistDao.searchByTitleBlocking(title)
    }

    override fun findPlaylistsToSyncBlocking(): List<Playlist> {
        return playlistDao.findNotSyncedBlocking()
    }

    /**
     * Build the SQL query for a playlist
     * @param playlist The playlist to generate the query for
     * @param playbackManager Required if the filter needs to include the currently playing episode regardless of if it meets the filter conditions.
     */
    private fun buildPlaylistWhere(playlist: Playlist, playbackManager: PlaybackManager?): String {
        val where = StringBuilder()
        buildFilterEpisodeWhere(playlist, where, playbackManager)
        return where.toString()
    }

    private fun buildFilterEpisodeWhere(playlist: Playlist, where: StringBuilder, playbackManager: PlaybackManager?) {
        val unplayed = playlist.unplayed
        val finished = playlist.finished
        val partiallyPlayed = playlist.partiallyPlayed
        val downloaded = playlist.downloaded
        val downloading = playlist.isSystemDownloadsFilter || playlist.notDownloaded // regular filters no longer have a downloading but the not downloaded one should show in progress downloads
        val notDownloaded = playlist.notDownloaded
        val audioVideo = playlist.audioVideo
        val filterHours = playlist.filterHours
        val starred = playlist.starred
        val allPodcasts = playlist.allPodcasts
        val podcastUuids = playlist.podcastUuidList

        val includeFailedDownloads = playlist.isSystemDownloadsFilter

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

        if (audioVideo != Playlist.AUDIO_VIDEO_FILTER_ALL) {
            if (audioVideo == Playlist.AUDIO_VIDEO_FILTER_VIDEO_ONLY) {
                if (where.isNotEmpty()) {
                    where.append(" AND ")
                }
                where.append("file_type LIKE 'video/%'")
            }
            if (audioVideo == Playlist.AUDIO_VIDEO_FILTER_AUDIO_ONLY) {
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

        if (playlist.filterDuration) {
            if (where.isNotEmpty()) {
                where.append(" AND ")
            }
            val longerThan = playlist.longerThan * 60
            // we add 59s here to account for how the formatter doesn't show "10m" until you get to 10*60 seconds, that way our visual representation lines up with the filter times
            val shorterThan = playlist.shorterThan * 60 + 59

            where.append("(duration >= $longerThan AND duration <= $shorterThan) ")
        }

        // leave out the custom folder podcast
        if (where.isNotEmpty()) {
            where.append(" AND ")
        }
        where.append("archived = 0")

        val playingEpisode = playbackManager?.getCurrentEpisode()?.uuid
        val lastLoadedFromPodcastOrFilterUuid = settings.lastAutoPlaySource.value.id
        if (playingEpisode != null && lastLoadedFromPodcastOrFilterUuid == playlist.uuid) {
            where.insert(0, "(podcast_episodes.uuid = '$playingEpisode' OR (")
            where.append("))")
        }
    }

    private fun markAsNotSyncedBlocking(playlist: Playlist) {
        playlist.syncStatus = Playlist.SYNC_STATUS_NOT_SYNCED
        playlistDao.updateSyncStatusBlocking(Playlist.SYNC_STATUS_NOT_SYNCED, playlist.uuid)
    }

    fun countPlaylistsBlocking(): Int {
        return playlistDao.countBlocking()
    }

    override fun getSystemDownloadsFilter(): Playlist {
        return Playlist(
            id = Playlist.PLAYLIST_ID_SYSTEM_DOWNLOADS,
            uuid = "",
            title = "Downloads",
            manual = false,
            downloaded = true,
            downloading = true,
            notDownloaded = false,
            unplayed = true,
            partiallyPlayed = true,
            finished = true,
            audioVideo = Playlist.AUDIO_VIDEO_FILTER_ALL,
            allPodcasts = true,
            autoDownload = false,
            sortId = Playlist.SortOrder.LAST_DOWNLOAD_ATTEMPT_DATE.value,
        )
    }

    override fun markAllSyncedBlocking() {
        playlistDao.updateAllSyncStatusBlocking(Playlist.SYNC_STATUS_SYNCED)
    }
}

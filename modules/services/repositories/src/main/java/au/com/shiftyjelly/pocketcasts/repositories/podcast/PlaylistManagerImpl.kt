package au.com.shiftyjelly.pocketcasts.repositories.podcast

import android.content.Context
import android.os.Build
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadHelper
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.extensions.calculateCombinedIconId
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.shortcuts.PocketCastsShortcuts
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Calendar
import java.util.Date
import java.util.UUID
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

private const val NEWRELEASE_UUID = "2797DCF8-1C93-4999-B52A-D1849736FA2C"
private const val INPROGRESS_UUID = "D89A925C-5CE1-41A4-A879-2751838CE5CE"
private const val CREATED_DEFAULT_PLAYLISTS = "createdDefaultPlaylists"

class PlaylistManagerImpl @Inject constructor(
    private val settings: Settings,
    private val downloadManager: DownloadManager,
    private val analyticsTracker: AnalyticsTrackerWrapper,
    @ApplicationContext private val context: Context,
    appDatabase: AppDatabase
) : PlaylistManager, CoroutineScope {

    companion object {
        const val ENABLED_KEY = "enabled"
        const val LIMIT_KEY = "limit"
        const val GROUP_KEY = "group"
        const val SOURCE_KEY = "source"
    }

    private val playlistDao = appDatabase.playlistDao()

    init {
        if (!settings.getBooleanForKey(CREATED_DEFAULT_PLAYLISTS, false)) {
            launch { setupDefaultPlaylists() }
        }
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    private fun setupDefaultPlaylists() {
        val existingNewRelease = playlistDao.findByUUID(NEWRELEASE_UUID)
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
            playlistDao.insert(newRelease)
        } else {
            existingNewRelease.iconId = 10
            playlistDao.update(existingNewRelease)
        }

        val existingInProgress = playlistDao.findByUUID(INPROGRESS_UUID)
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
            playlistDao.insert(inProgress)
        } else {
            existingInProgress.iconId = 43
            playlistDao.update(existingInProgress)
        }

        settings.setBooleanForKey(CREATED_DEFAULT_PLAYLISTS, true)
    }

    override fun findAll(): List<Playlist> {
        return playlistDao.findAll()
    }

    override suspend fun findAllSuspend(): List<Playlist> {
        return playlistDao.findAllSuspend()
    }

    override fun count(): Int {
        return playlistDao.count()
    }

    override fun observeAll(): Flowable<List<Playlist>> {
        return playlistDao.observeAll()
    }

    override fun findByUuid(playlistUuid: String): Playlist? {
        return playlistDao.findByUUID(playlistUuid)
    }

    override fun findByUuidRx(playlistUuid: String): Maybe<Playlist> {
        return playlistDao.findByUUIDRx(playlistUuid)
    }

    override fun observeByUuid(playlistUuid: String): Flowable<Playlist> {
        return playlistDao.observeByUUID(playlistUuid)
    }

    override fun observeByUuidAsList(playlistUuid: String): Flowable<List<Playlist>> {
        return playlistDao.observeByUUIDAsList(playlistUuid)
    }

    override fun findById(id: Long): Playlist? {
        return playlistDao.findById(id)
    }

    override fun findEpisodes(playlist: Playlist, episodeManager: EpisodeManager, playbackManager: PlaybackManager): List<Episode> {
        val where = buildPlaylistWhere(playlist, playbackManager)
        val orderBy = getPlaylistOrderByString(playlist)
        val limit = if (playlist.sortId == Playlist.PLAYLIST_SORT_LAST_DOWNLOAD_ATTEMPT_DATE) 1000 else 500
        return episodeManager.findEpisodesWhere("$where ORDER BY $orderBy LIMIT $limit")
    }

    private fun getPlaylistQuery(playlist: Playlist, limit: Int?, playbackManager: PlaybackManager): String {
        val where = buildPlaylistWhere(playlist, playbackManager)
        val orderBy = getPlaylistOrderByString(playlist)
        return "$where ORDER BY $orderBy" + if (limit != null) " LIMIT $limit" else ""
    }

    override fun observeEpisodes(playlist: Playlist, episodeManager: EpisodeManager, playbackManager: PlaybackManager): Flowable<List<Episode>> {
        val limitCount = if (playlist.sortId == Playlist.PLAYLIST_SORT_LAST_DOWNLOAD_ATTEMPT_DATE) 1000 else 500
        val queryAfterWhere = getPlaylistQuery(playlist, limit = limitCount, playbackManager = playbackManager)
        return episodeManager.observeEpisodesWhere(queryAfterWhere)
    }

    override fun observeEpisodesPreview(playlist: Playlist, episodeManager: EpisodeManager, playbackManager: PlaybackManager): Flowable<List<Episode>> {
        val queryAfterWhere = getPlaylistQuery(playlist, limit = 100, playbackManager = playbackManager)
        return episodeManager.observeEpisodesWhere(queryAfterWhere)
    }

    private fun getPlaylistOrderByString(playlist: Playlist): String? {
        if (playlist.sortId == Playlist.PLAYLIST_SORT_NEWEST_TO_OLDEST || playlist.sortId == Playlist.PLAYLIST_SORT_OLDEST_TO_NEWEST) {
            return "published_date " + (if (playlist.sortId == Playlist.PLAYLIST_SORT_NEWEST_TO_OLDEST) "DESC" else "ASC") +
                ", added_date " + if (playlist.sortId == Playlist.PLAYLIST_SORT_NEWEST_TO_OLDEST) "DESC" else "ASC"
        } else if (playlist.sortId == Playlist.PLAYLIST_SORT_SHORTEST_TO_LONGEST || playlist.sortId == Playlist.PLAYLIST_SORT_LONGEST_TO_SHORTEST) {
            return "duration " + (if (playlist.sortId == Playlist.PLAYLIST_SORT_SHORTEST_TO_LONGEST) "ASC" else "DESC") + ", added_date DESC"
        } else if (playlist.sortId == Playlist.PLAYLIST_SORT_LAST_DOWNLOAD_ATTEMPT_DATE) {
            return "last_download_attempt_date DESC, published_date DESC"
        }
        return null
    }

    override fun create(playlist: Playlist): Long {
        val id = playlistDao.insert(playlist)
        if (countPlaylists() == 1 && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            PocketCastsShortcuts.update(this, true, context)
        }
        return id
    }

    /**
     * A null UserPlayListUpdate parameter indicates that the user did not initiate this update
     */
    override fun update(playlist: Playlist, userPlaylistUpdate: UserPlaylistUpdate?) {
        playlistDao.update(playlist)
        sendPlaylistUpdateAnalytics(playlist, userPlaylistUpdate)
    }

    override fun updateAll(playlists: List<Playlist>) {
        playlistDao.updateAll(playlists)
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
//        databaseManager.updateAttributes(playlist, attrs)
    }

    override fun rxUpdateAutoDownloadStatus(playlist: Playlist, autoDownloadEnabled: Boolean, unmeteredOnly: Boolean, powerOnly: Boolean): Completable {
        return Completable.fromAction { updateAutoDownloadStatus(playlist, autoDownloadEnabled, unmeteredOnly, powerOnly) }
    }

    override fun createPlaylist(name: String, iconId: Int, draft: Boolean): Playlist {
        val playlist = Playlist(
            uuid = UUID.randomUUID().toString(),
            syncStatus = Playlist.SYNC_STATUS_NOT_SYNCED,
            title = name,
            sortPosition = countPlaylists() + 1,
            manual = false,
            iconId = iconId,
            draft = draft
        )

        Timber.d("Creating playlist ${playlist.uuid}")
        playlist.id = playlistDao.insert(playlist)
        return playlist
    }

    override fun delete(playlist: Playlist) {
        val loggedIn = settings.isLoggedIn()
        if (loggedIn) {
            playlist.deleted = true
            markAsNotSynced(playlist)

            // user initiated filter deletion, not update of any playlist properties, so this
            // is not a user initiated update
            update(playlist, userPlaylistUpdate = null)
        }

        if (!loggedIn) {
            deleteSynced(playlist)
        }
    }

    override fun deleteSynced() {
        playlistDao.deleteDeleted()
    }

    override fun deleteSynced(playlist: Playlist) {
        playlistDao.delete(playlist)
    }

    override fun countEpisodesRx(playlist: Playlist, episodeManager: EpisodeManager, playbackManager: PlaybackManager): Flowable<Int> {
        val query = getPlaylistQuery(playlist, limit = null, playbackManager = playbackManager)
        return episodeManager.observeEpisodeCount(query)
    }

    override fun countEpisodes(id: Long?, episodeManager: EpisodeManager, playbackManager: PlaybackManager): Int {
        if (id == null) {
            return 0
        }
        val playlist = findById(id) ?: return 0
        val where = buildPlaylistWhere(playlist, playbackManager)
        return episodeManager.countEpisodesWhere(where)
    }

    override fun savePlaylistsOrder(playlists: List<Playlist>) {
        playlistDao.updateSortPositions(playlists)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            PocketCastsShortcuts.update(this, true, context)
        }
    }

    override fun checkForEpisodesToDownload(episodeManager: EpisodeManager, playbackManager: PlaybackManager) {
        val allPlaylists = findAll()
        if (allPlaylists.isEmpty()) return

        for (playlist in allPlaylists) {
            if (!playlist.autoDownload) continue

            findEpisodes(playlist, episodeManager, playbackManager).take(playlist.autodownloadLimit).forEach { episode ->
                if (episode.isQueued || episode.isDownloaded || episode.isDownloading || episode.isExemptFromAutoDownload) {
                    return@forEach
                }

                DownloadHelper.addAutoDownloadedEpisodeToQueue(episode, "playlist " + playlist.title, downloadManager, episodeManager)
            }
        }
    }

    override fun removePodcastFromPlaylists(podcastUuid: String) {
        if (podcastUuid.isBlank()) {
            return
        }
        val playlists = findAll()
        for (playlist in playlists) {
            val podcastUuids = playlist.podcastUuidList.toMutableList()
            if (playlist.allPodcasts || podcastUuids.isEmpty()) {
                continue
            }

            if (podcastUuids.contains(podcastUuid)) {
                podcastUuids.remove(podcastUuid)

                playlist.syncStatus = Playlist.SYNC_STATUS_NOT_SYNCED
                playlist.podcastUuidList = podcastUuids
                playlistDao.update(playlist)
            }
        }
    }

    override fun findFirstByTitle(title: String): Playlist? {
        return playlistDao.searchByTitle(title)
    }

    override fun findPlaylistsToSync(): List<Playlist> {
        return playlistDao.findNotSynced()
    }

    private fun sendPlaylistUpdateAnalytics(
        playlist: Playlist,
        userPlaylistUpdate: UserPlaylistUpdate?
    ) {
        // Don't send a filter updated event if the playlist is being created or if
        // the user did not initiate the update
        if (!playlist.draft && userPlaylistUpdate != null) {
            userPlaylistUpdate.properties.map { playlistProperty ->
                when (playlistProperty) {

                    is FilterUpdatedEvent -> {
                        val properties = mapOf(
                            GROUP_KEY to playlistProperty.groupValue,
                            SOURCE_KEY to userPlaylistUpdate.source.analyticsValue
                        )
                        analyticsTracker.track(AnalyticsEvent.FILTER_UPDATED, properties)
                    }

                    is PlaylistProperty.AutoDownload -> {
                        val properties = mapOf(
                            SOURCE_KEY to userPlaylistUpdate.source.analyticsValue,
                            ENABLED_KEY to playlistProperty.enabled
                        )
                        analyticsTracker.track(AnalyticsEvent.FILTER_AUTO_DOWNLOAD_UPDATED, properties)
                    }

                    is PlaylistProperty.AutoDownloadLimit -> {
                        val properties = mapOf(LIMIT_KEY to playlistProperty.limit)
                        analyticsTracker.track(AnalyticsEvent.FILTER_AUTO_DOWNLOAD_LIMIT_UPDATED, properties)
                    }

                    PlaylistProperty.Color,
                    PlaylistProperty.FilterName,
                    PlaylistProperty.Icon,
                    PlaylistProperty.Sort -> { /* Do nothing. These are handled by other events. */ }
                }
            }
        }
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
        if (playingEpisode != null && playbackManager.lastLoadedFromPodcastOrPlaylistUuid == playlist.uuid) {
            where.insert(0, "(episodes.uuid = '$playingEpisode' OR (")
            where.append("))")
        }
    }

    override fun countEpisodesNotCompleted(playlist: Playlist, episodeManager: EpisodeManager, playbackManager: PlaybackManager): Int {
        return episodeManager.countEpisodesWhere("episodes.archived = 0 AND episodes.playing_status != " + EpisodePlayingStatus.COMPLETED.ordinal + " AND " + buildPlaylistWhere(playlist, null))
    }

    override fun countEpisodesDownloading(playlist: Playlist, episodeManager: EpisodeManager, playbackManager: PlaybackManager): Int {
        return episodeManager.countEpisodesWhere(
            "episodes.archived = 0 AND (episodes.episode_status = " + EpisodeStatusEnum.DOWNLOADING.ordinal + " OR episodes.episode_status = " + EpisodeStatusEnum.QUEUED.ordinal + " OR episodes.episode_status = " + EpisodeStatusEnum.WAITING_FOR_WIFI.ordinal + " OR episodes.episode_status = " + EpisodeStatusEnum.WAITING_FOR_POWER.ordinal + ") AND " + buildPlaylistWhere(
                playlist,
                null
            )
        )
    }

    override fun countEpisodesNotDownloaded(playlist: Playlist, episodeManager: EpisodeManager, playbackManager: PlaybackManager): Int {
        return episodeManager.countEpisodesWhere(
            "episodes.archived = 0 AND (episodes.episode_status = " + EpisodeStatusEnum.NOT_DOWNLOADED.ordinal + " OR episodes.episode_status = " + EpisodeStatusEnum.DOWNLOAD_FAILED.ordinal + ") AND " + buildPlaylistWhere(
                playlist,
                null
            )
        )
    }

    private fun markAsNotSynced(playlist: Playlist) {
        playlist.syncStatus = Playlist.SYNC_STATUS_NOT_SYNCED
        playlistDao.updateSyncStatus(Playlist.SYNC_STATUS_NOT_SYNCED, playlist.uuid)
    }

    fun countPlaylists(): Int {
        return playlistDao.count()
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
            sortId = Playlist.PLAYLIST_SORT_LAST_DOWNLOAD_ATTEMPT_DATE
        )
    }

    override fun markAllSynced() {
        playlistDao.updateAllSyncStatus(Playlist.SYNC_STATUS_SYNCED)
    }
}

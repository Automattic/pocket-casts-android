package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationManager
import au.com.shiftyjelly.pocketcasts.repositories.notification.OnboardingNotificationType
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.DefaultPlaylistsInitializer
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import java.util.Calendar
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import timber.log.Timber

class SmartPlaylistManagerImpl @Inject constructor(
    private val settings: Settings,
    private val playlistUpdateAnalytics: PlaylistUpdateAnalytics,
    private val syncManager: SyncManager,
    private val notificationManager: NotificationManager,
    appDatabase: AppDatabase,
    private val playlistsInitializer: DefaultPlaylistsInitializer,
    @ApplicationScope private val scope: CoroutineScope,
) : SmartPlaylistManager {

    private val playlistDao = appDatabase.smartPlaylistDao()

    override fun findAllBlocking(): List<PlaylistEntity> {
        return playlistDao.findAllBlocking()
    }

    override suspend fun findAll(): List<PlaylistEntity> {
        return playlistDao.findAll()
    }

    override fun findAllFlow(): Flow<List<PlaylistEntity>> {
        return playlistDao.findAllFlow()
    }

    override fun findAllRxFlowable(): Flowable<List<PlaylistEntity>> {
        return playlistDao.findAllRxFlowable()
    }

    override fun findByUuidBlocking(playlistUuid: String): PlaylistEntity? {
        return playlistDao.findByUuidBlocking(playlistUuid)
    }

    override suspend fun findByUuid(playlistUuid: String): PlaylistEntity? {
        return playlistDao.findByUuid(playlistUuid)
    }

    override fun findByUuidRxMaybe(playlistUuid: String): Maybe<PlaylistEntity> {
        return playlistDao.findByUuidRxMaybe(playlistUuid)
    }

    override fun findByUuidRxFlowable(playlistUuid: String): Flowable<PlaylistEntity> {
        return playlistDao.findByUuidRxFlowable(playlistUuid)
    }

    override fun findByUuidAsListRxFlowable(playlistUuid: String): Flowable<List<PlaylistEntity>> {
        return playlistDao.findByUuidAsListRxFlowable(playlistUuid)
    }

    override fun findByIdBlocking(id: Long): PlaylistEntity? {
        return playlistDao.findByIdBlocking(id)
    }

    override fun findEpisodesBlocking(playlist: PlaylistEntity, episodeManager: EpisodeManager, playbackManager: PlaybackManager): List<PodcastEpisode> {
        val where = buildPlaylistWhere(playlist, playbackManager)
        val orderBy = getPlaylistOrderByString(playlist)
        return episodeManager.findEpisodesWhereBlocking("$where ORDER BY $orderBy LIMIT 500")
    }

    private fun getPlaylistQuery(playlist: PlaylistEntity, limit: Int?, playbackManager: PlaybackManager): String {
        val where = buildPlaylistWhere(playlist, playbackManager)
        val orderBy = getPlaylistOrderByString(playlist)
        return "$where ORDER BY $orderBy" + if (limit != null) " LIMIT $limit" else ""
    }

    override fun observeEpisodesBlocking(playlist: PlaylistEntity, episodeManager: EpisodeManager, playbackManager: PlaybackManager): Flowable<List<PodcastEpisode>> {
        val queryAfterWhere = getPlaylistQuery(playlist, limit = 500, playbackManager = playbackManager)
        return episodeManager.findEpisodesWhereRxFlowable(queryAfterWhere)
    }

    override fun observeEpisodesPreviewBlocking(playlist: PlaylistEntity, episodeManager: EpisodeManager, playbackManager: PlaybackManager): Flowable<List<PodcastEpisode>> {
        val queryAfterWhere = getPlaylistQuery(playlist, limit = 100, playbackManager = playbackManager)
        return episodeManager.findEpisodesWhereRxFlowable(queryAfterWhere)
    }

    private fun getPlaylistOrderByString(playlist: PlaylistEntity): String? = when (val sortType = playlist.sortType) {
        PlaylistEpisodeSortType.NewestToOldest,
        PlaylistEpisodeSortType.DragAndDrop,
        PlaylistEpisodeSortType.OldestToNewest,
        -> {
            "published_date " +
                (if (sortType == PlaylistEpisodeSortType.NewestToOldest || sortType == PlaylistEpisodeSortType.DragAndDrop) "DESC" else "ASC") +
                ", added_date " +
                if (sortType == PlaylistEpisodeSortType.NewestToOldest || sortType == PlaylistEpisodeSortType.DragAndDrop) "DESC" else "ASC"
        }

        PlaylistEpisodeSortType.ShortestToLongest,
        PlaylistEpisodeSortType.LongestToShortest,
        -> {
            "duration " +
                (if (sortType == PlaylistEpisodeSortType.ShortestToLongest) "ASC" else "DESC") +
                ", added_date DESC"
        }
    }

    override suspend fun create(playlist: PlaylistEntity): Long {
        return playlistDao.insert(playlist)
    }

    /**
     * A null userPlayListUpdate parameter indicates that  the user did not initiate this update
     */
    override fun updateBlocking(
        playlist: PlaylistEntity,
        userPlaylistUpdate: UserPlaylistUpdate?,
        isCreatingFilter: Boolean,
    ) {
        if (isCreatingFilter) {
            scope.launch {
                notificationManager.updateUserFeatureInteraction(OnboardingNotificationType.Filters)
            }
        }
        playlistDao.updateBlocking(playlist)
        playlistUpdateAnalytics.update(playlist, userPlaylistUpdate, isCreatingFilter)
    }

    override suspend fun update(
        playlist: PlaylistEntity,
        userPlaylistUpdate: UserPlaylistUpdate?,
        isCreatingFilter: Boolean,
    ) {
        playlistDao.update(playlist)
        playlistUpdateAnalytics.update(playlist, userPlaylistUpdate, isCreatingFilter)
    }

    override fun updateAllBlocking(playlists: List<PlaylistEntity>) {
        playlistDao.updateAllBlocking(playlists)
    }

    override fun updateAutoDownloadStatus(playlist: PlaylistEntity, autoDownloadEnabled: Boolean) {
        playlist.autoDownload = autoDownloadEnabled
    }

    override fun createPlaylistBlocking(name: String, iconId: Int, draft: Boolean): PlaylistEntity {
        val playlist = PlaylistEntity(
            uuid = UUID.randomUUID().toString(),
            syncStatus = PlaylistEntity.SYNC_STATUS_NOT_SYNCED,
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

    override fun deleteBlocking(playlist: PlaylistEntity) {
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
        playlistsInitializer.initialize(force = true)
    }

    override suspend fun deleteSynced(playlist: PlaylistEntity) {
        playlistDao.delete(playlist)
    }

    override fun deleteSyncedBlocking(playlist: PlaylistEntity) {
        playlistDao.deleteBlocking(playlist)
    }

    override fun countEpisodesRxFlowable(playlist: PlaylistEntity, episodeManager: EpisodeManager, playbackManager: PlaybackManager): Flowable<Int> {
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

    override suspend fun removePodcastFromPlaylists(podcastUuid: String) {
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

                playlist.syncStatus = PlaylistEntity.SYNC_STATUS_NOT_SYNCED
                playlist.podcastUuidList = podcastUuids
                playlistDao.update(playlist)
            }
        }
    }

    override fun findFirstByTitleBlocking(title: String): PlaylistEntity? {
        return playlistDao.searchByTitleBlocking(title)
    }

    override fun findPlaylistsToSyncBlocking(): List<PlaylistEntity> {
        return playlistDao.findNotSyncedBlocking()
    }

    /**
     * Build the SQL query for a playlist
     * @param playlist The playlist to generate the query for
     * @param playbackManager Required if the filter needs to include the currently playing episode regardless of if it meets the filter conditions.
     */
    private fun buildPlaylistWhere(playlist: PlaylistEntity, playbackManager: PlaybackManager?): String {
        val where = StringBuilder()
        buildFilterEpisodeWhere(playlist, where, playbackManager)
        return where.toString()
    }

    private fun buildFilterEpisodeWhere(playlist: PlaylistEntity, where: StringBuilder, playbackManager: PlaybackManager?) {
        val unplayed = playlist.unplayed
        val finished = playlist.finished
        val partiallyPlayed = playlist.partiallyPlayed
        val downloaded = playlist.downloaded
        val downloading = playlist.notDownloaded // regular filters no longer have a downloading but the not downloaded one should show in progress downloads
        val notDownloaded = playlist.notDownloaded
        val audioVideo = playlist.audioVideo
        val filterHours = playlist.filterHours
        val starred = playlist.starred
        val allPodcasts = playlist.allPodcasts
        val podcastUuids = playlist.podcastUuidList

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

        if (audioVideo != PlaylistEntity.AUDIO_VIDEO_FILTER_ALL) {
            if (audioVideo == PlaylistEntity.AUDIO_VIDEO_FILTER_VIDEO_ONLY) {
                if (where.isNotEmpty()) {
                    where.append(" AND ")
                }
                where.append("file_type LIKE 'video/%'")
            }
            if (audioVideo == PlaylistEntity.AUDIO_VIDEO_FILTER_AUDIO_ONLY) {
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

    private fun markAsNotSyncedBlocking(playlist: PlaylistEntity) {
        playlist.syncStatus = PlaylistEntity.SYNC_STATUS_NOT_SYNCED
        playlistDao.updateSyncStatusBlocking(PlaylistEntity.SYNC_STATUS_NOT_SYNCED, playlist.uuid)
    }

    fun countPlaylists(): Int {
        return playlistDao.countBlocking()
    }

    fun countPlaylistsBlocking(): Int {
        return playlistDao.countBlocking()
    }

    override suspend fun markAllSynced() {
        playlistDao.updateAllSyncStatus(PlaylistEntity.SYNC_STATUS_SYNCED)
    }
}

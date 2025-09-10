package au.com.shiftyjelly.pocketcasts.repositories.sync.data

import androidx.room.withTransaction
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PlaylistEntity
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.extensions.toInstant
import au.com.shiftyjelly.pocketcasts.servers.extensions.toTimestamp
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import com.google.protobuf.boolValue
import com.google.protobuf.int32Value
import com.google.protobuf.int64Value
import com.google.protobuf.stringValue
import com.pocketcasts.service.api.PlaylistSyncResponse
import com.pocketcasts.service.api.Record
import com.pocketcasts.service.api.SyncUserPlaylist
import com.pocketcasts.service.api.addedOrNull
import com.pocketcasts.service.api.allPodcastsOrNull
import com.pocketcasts.service.api.audioVideoOrNull
import com.pocketcasts.service.api.downloadedOrNull
import com.pocketcasts.service.api.episodeSlugOrNull
import com.pocketcasts.service.api.filterDurationOrNull
import com.pocketcasts.service.api.filterHoursOrNull
import com.pocketcasts.service.api.finishedOrNull
import com.pocketcasts.service.api.iconIdOrNull
import com.pocketcasts.service.api.isDeletedOrNull
import com.pocketcasts.service.api.longerThanOrNull
import com.pocketcasts.service.api.manualOrNull
import com.pocketcasts.service.api.notDownloadedOrNull
import com.pocketcasts.service.api.partiallyPlayedOrNull
import com.pocketcasts.service.api.podcastSlugOrNull
import com.pocketcasts.service.api.podcastUuidsOrNull
import com.pocketcasts.service.api.publishedOrNull
import com.pocketcasts.service.api.record
import com.pocketcasts.service.api.shorterThanOrNull
import com.pocketcasts.service.api.sortPositionOrNull
import com.pocketcasts.service.api.sortTypeOrNull
import com.pocketcasts.service.api.starredOrNull
import com.pocketcasts.service.api.syncPlaylistEpisode
import com.pocketcasts.service.api.syncUserPlaylist
import com.pocketcasts.service.api.titleOrNull
import com.pocketcasts.service.api.unplayedOrNull
import com.pocketcasts.service.api.urlOrNull
import java.time.Instant
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class PlaylistSync(
    private val syncManager: SyncManager,
    private val appDatabase: AppDatabase,
) {
    private val playlistDao = appDatabase.playlistDao()
    private val useManualPlaylists get() = FeatureFlag.isEnabled(Feature.PLAYLISTS_REBRANDING, immutable = true)

    suspend fun fullSync() {
        processServerPlaylists(
            serverPlaylists = syncManager.getPlaylistsOrThrow().playlistsList,
            getUuid = { playlist -> playlist.originalUuid },
            isDeleted = { playlist -> playlist.isDeletedOrNull?.value == true },
            isManual = { playlist -> playlist.manualOrNull?.value == true },
            applyServerPlaylist = { localPlaylist, serverPlaylist -> localPlaylist.applyServerPlaylist(serverPlaylist) },
            getManualEpisodes = { serverPlaylist -> toManualEpisodes(serverPlaylist) },
        )
    }

    suspend fun processIncrementalResponse(serverPlaylists: List<SyncUserPlaylist>) {
        processServerPlaylists(
            serverPlaylists = serverPlaylists,
            getUuid = { playlist -> playlist.originalUuid },
            isDeleted = { playlist -> playlist.isDeletedOrNull?.value == true },
            isManual = { playlist -> playlist.manualOrNull?.value == true },
            applyServerPlaylist = { localPlaylist, serverPlaylist -> localPlaylist.applyServerPlaylist(serverPlaylist) },
            getManualEpisodes = { serverPlaylist -> toManualEpisodes(serverPlaylist) },
        )
    }

    private suspend fun <T> processServerPlaylists(
        serverPlaylists: List<T>,
        getUuid: (T) -> String,
        isDeleted: (T) -> Boolean,
        isManual: (T) -> Boolean,
        applyServerPlaylist: (PlaylistEntity, T) -> PlaylistEntity,
        getManualEpisodes: (T) -> List<ManualPlaylistEpisode>,
    ) {
        val deletedPlaylists = serverPlaylists.filter(isDeleted)
        val remainingPlaylist = serverPlaylists - deletedPlaylists
        val remainingPlaylistsMap = remainingPlaylist.associateBy(getUuid)

        appDatabase.withTransaction {
            playlistDao.deleteAllPlaylistsIn(deletedPlaylists.map(getUuid))

            val existingPlaylists = playlistDao.getAllPlaylistsIn(remainingPlaylist.map(getUuid))
            val existingPlaylistUuids = existingPlaylists.map(PlaylistEntity::uuid)
            existingPlaylists.forEach { playlist ->
                val serverPlaylist = remainingPlaylistsMap[playlist.uuid] ?: return@forEach
                applyServerPlaylist(playlist, serverPlaylist)
            }
            val newPlaylists = remainingPlaylist
                .filter { playlist ->
                    if (useManualPlaylists) {
                        true
                    } else {
                        !isManual(playlist)
                    }
                }
                .mapNotNull { serverPlaylist ->
                    if (getUuid(serverPlaylist) !in existingPlaylistUuids) {
                        applyServerPlaylist(PlaylistEntity(), serverPlaylist)
                    } else {
                        null
                    }
                }
            playlistDao.upsertAllPlaylists(existingPlaylists + newPlaylists)

            if (useManualPlaylists) {
                remainingPlaylist.forEach { playlist ->
                    playlistDao.deleteAllManualEpisodes(getUuid(playlist))
                    playlistDao.upsertManualEpisodes(getManualEpisodes(playlist))
                }
            }
        }
    }

    suspend fun incrementalData(): List<Record> {
        val playlists = playlistDao
            .getAllUnsyncedPlaylists()
            .filter { playlist ->
                if (useManualPlaylists) {
                    true
                } else {
                    !playlist.manual
                }
            }
        return withContext(Dispatchers.Default) {
            playlists.map { localPlaylist ->
                record {
                    playlist = syncUserPlaylist {
                        // Set both UUIDs as it is important server side due to case sensitivity
                        uuid = localPlaylist.uuid
                        originalUuid = localPlaylist.uuid

                        isDeleted = boolValue {
                            value = localPlaylist.deleted
                        }
                        title = stringValue {
                            value = localPlaylist.title
                        }
                        allPodcasts = boolValue {
                            value = localPlaylist.allPodcasts
                        }
                        podcastUuids = stringValue {
                            value = localPlaylist.podcastUuids.orEmpty()
                        }
                        audioVideo = int32Value {
                            value = localPlaylist.audioVideo
                        }
                        notDownloaded = boolValue {
                            value = localPlaylist.notDownloaded
                        }
                        downloaded = boolValue {
                            value = localPlaylist.downloaded
                        }
                        finished = boolValue {
                            value = localPlaylist.finished
                        }
                        partiallyPlayed = boolValue {
                            value = localPlaylist.partiallyPlayed
                        }
                        unplayed = boolValue {
                            value = localPlaylist.unplayed
                        }
                        starred = boolValue {
                            value = localPlaylist.starred
                        }
                        manual = boolValue {
                            value = localPlaylist.manual
                        }
                        localPlaylist.sortPosition?.let { position ->
                            sortPosition = int32Value {
                                value = position
                            }
                        }
                        sortType = int32Value {
                            value = localPlaylist.sortType.serverId
                        }
                        iconId = int32Value {
                            value = localPlaylist.iconId
                        }
                        filterHours = int32Value {
                            value = localPlaylist.filterHours
                        }
                        filterDuration = boolValue {
                            value = localPlaylist.filterDuration
                        }
                        longerThan = int32Value {
                            value = localPlaylist.longerThan
                        }
                        shorterThan = int32Value {
                            value = localPlaylist.shorterThan
                        }
                        if (localPlaylist.manual) {
                            val localEpisodes = playlistDao.getManualPlaylistEpisodesForSync(localPlaylist.uuid)
                            episodeOrder.addAll(localEpisodes.map(ManualPlaylistEpisode::episodeUuid))
                            episodes.addAll(localEpisodes.filterNot(ManualPlaylistEpisode::isSynced).map(::toServerEpisode))
                        }
                    }
                }
            }
        }
    }
}

private fun PlaylistEntity.applyServerPlaylist(serverPlaylist: PlaylistSyncResponse) = apply {
    syncStatus = PlaylistEntity.SYNC_STATUS_SYNCED
    uuid = serverPlaylist.originalUuid
    title = serverPlaylist.title
    serverPlaylist.manualOrNull?.value?.let { value ->
        manual = value
    }
    serverPlaylist.iconIdOrNull?.value?.let { value ->
        iconId = value
    }
    serverPlaylist.sortPositionOrNull?.value?.let { value ->
        sortPosition = value
    }
    serverPlaylist.sortTypeOrNull?.value?.let(PlaylistEpisodeSortType::fromServerId)?.let { value ->
        sortType = value
    }
    serverPlaylist.unplayedOrNull?.value?.let { value ->
        unplayed = value
    }
    serverPlaylist.partiallyPlayedOrNull?.value?.let { value ->
        partiallyPlayed = value
    }
    serverPlaylist.finishedOrNull?.value?.let { value ->
        finished = value
    }
    serverPlaylist.downloadedOrNull?.value?.let { value ->
        downloaded = value
    }
    serverPlaylist.notDownloadedOrNull?.value?.let { value ->
        notDownloaded = value
    }
    serverPlaylist.audioVideoOrNull?.value?.let { value ->
        audioVideo = value
    }
    serverPlaylist.filterHoursOrNull?.value?.let { value ->
        filterHours = value
    }
    serverPlaylist.starredOrNull?.value?.let { value ->
        starred = value
    }
    serverPlaylist.allPodcastsOrNull?.value?.let { value ->
        allPodcasts = value
    }
    podcastUuids = serverPlaylist.podcastUuids
    serverPlaylist.filterDurationOrNull?.value?.let { value ->
        filterDuration = value
    }
    serverPlaylist.longerThanOrNull?.value?.let { value ->
        longerThan = value
    }
    serverPlaylist.shorterThanOrNull?.value?.let { value ->
        shorterThan = value
    }
}

private fun PlaylistEntity.applyServerPlaylist(serverPlaylist: SyncUserPlaylist) = apply {
    syncStatus = PlaylistEntity.SYNC_STATUS_SYNCED
    uuid = serverPlaylist.originalUuid
    serverPlaylist.titleOrNull?.value?.let { value ->
        title = value
    }
    serverPlaylist.manualOrNull?.value?.let { value ->
        manual = value
    }
    serverPlaylist.iconIdOrNull?.value?.let { value ->
        iconId = value
    }
    serverPlaylist.sortPositionOrNull?.value?.let { value ->
        sortPosition = value
    }
    serverPlaylist.sortTypeOrNull?.value?.let(PlaylistEpisodeSortType::fromServerId)?.let { value ->
        sortType = value
    }
    serverPlaylist.unplayedOrNull?.value?.let { value ->
        unplayed = value
    }
    serverPlaylist.partiallyPlayedOrNull?.value?.let { value ->
        partiallyPlayed = value
    }
    serverPlaylist.finishedOrNull?.value?.let { value ->
        finished = value
    }
    serverPlaylist.downloadedOrNull?.value?.let { value ->
        downloaded = value
    }
    serverPlaylist.notDownloadedOrNull?.value?.let { value ->
        notDownloaded = value
    }
    serverPlaylist.audioVideoOrNull?.value?.let { value ->
        audioVideo = value
    }
    serverPlaylist.filterHoursOrNull?.value?.let { value ->
        filterHours = value
    }
    serverPlaylist.starredOrNull?.value?.let { value ->
        starred = value
    }
    serverPlaylist.allPodcastsOrNull?.value?.let { value ->
        allPodcasts = value
    }
    podcastUuids = serverPlaylist.podcastUuidsOrNull?.value
    serverPlaylist.filterDurationOrNull?.value?.let { value ->
        filterDuration = value
    }
    serverPlaylist.longerThanOrNull?.value?.let { value ->
        longerThan = value
    }
    serverPlaylist.shorterThanOrNull?.value?.let { value ->
        shorterThan = value
    }
}

private fun toServerEpisode(localEpisode: ManualPlaylistEpisode) = syncPlaylistEpisode {
    episode = localEpisode.episodeUuid
    podcast = localEpisode.podcastUuid
    added = int64Value {
        value = localEpisode.addedAt.toEpochMilli()
    }
    published = localEpisode.publishedAt.toTimestamp()
    title = stringValue {
        value = localEpisode.title
    }
    localEpisode.downloadUrl?.let { localUrl ->
        url = stringValue {
            value = localUrl
        }
    }
    podcastSlug = stringValue {
        value = localEpisode.podcastSlug
    }
    episodeSlug = stringValue {
        value = localEpisode.episodeSlug
    }
}

private fun toManualEpisodes(serverPlaylist: PlaylistSyncResponse): List<ManualPlaylistEpisode> {
    return serverPlaylist.episodesList.map { episode ->
        ManualPlaylistEpisode(
            playlistUuid = serverPlaylist.originalUuid,
            episodeUuid = episode.episode,
            podcastUuid = episode.podcast,
            title = episode.titleOrNull?.value.orEmpty(),
            addedAt = episode.addedOrNull?.value?.let(Instant::ofEpochMilli) ?: Instant.now(),
            publishedAt = episode.publishedOrNull?.toInstant() ?: Instant.ofEpochMilli(0),
            downloadUrl = episode.urlOrNull?.value,
            episodeSlug = episode.episodeSlugOrNull?.value.orEmpty(),
            podcastSlug = episode.podcastSlugOrNull?.value.orEmpty(),
            sortPosition = serverPlaylist.episodeOrderList.indexOf(episode.episode).takeIf { it != -1 } ?: Int.MAX_VALUE,
            isSynced = true,
        )
    }
}

private fun toManualEpisodes(serverPlaylist: SyncUserPlaylist): List<ManualPlaylistEpisode> {
    return serverPlaylist.episodesList.map { episode ->
        ManualPlaylistEpisode(
            playlistUuid = serverPlaylist.originalUuid,
            episodeUuid = episode.episode,
            podcastUuid = episode.podcast,
            title = episode.titleOrNull?.value.orEmpty(),
            addedAt = episode.addedOrNull?.value?.let(Instant::ofEpochMilli) ?: Instant.now(),
            publishedAt = episode.publishedOrNull?.toInstant() ?: Instant.ofEpochMilli(0),
            downloadUrl = episode.urlOrNull?.value,
            episodeSlug = episode.episodeSlugOrNull?.value.orEmpty(),
            podcastSlug = episode.podcastSlugOrNull?.value.orEmpty(),
            sortPosition = serverPlaylist.episodeOrderList.indexOf(episode.episode).takeIf { it != -1 } ?: Int.MAX_VALUE,
            isSynced = true,
        )
    }
}

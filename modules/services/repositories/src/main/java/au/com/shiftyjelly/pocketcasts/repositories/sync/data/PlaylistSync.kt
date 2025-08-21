package au.com.shiftyjelly.pocketcasts.repositories.sync.data

import androidx.room.withTransaction
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.SmartPlaylist
import au.com.shiftyjelly.pocketcasts.models.type.PlaylistEpisodeSortType
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import com.google.protobuf.boolValue
import com.google.protobuf.int32Value
import com.google.protobuf.stringValue
import com.pocketcasts.service.api.PlaylistSyncResponse
import com.pocketcasts.service.api.Record
import com.pocketcasts.service.api.SyncUserPlaylist
import com.pocketcasts.service.api.allPodcastsOrNull
import com.pocketcasts.service.api.audioVideoOrNull
import com.pocketcasts.service.api.downloadedOrNull
import com.pocketcasts.service.api.filterDurationOrNull
import com.pocketcasts.service.api.filterHoursOrNull
import com.pocketcasts.service.api.finishedOrNull
import com.pocketcasts.service.api.iconIdOrNull
import com.pocketcasts.service.api.isDeletedOrNull
import com.pocketcasts.service.api.longerThanOrNull
import com.pocketcasts.service.api.manualOrNull
import com.pocketcasts.service.api.notDownloadedOrNull
import com.pocketcasts.service.api.partiallyPlayedOrNull
import com.pocketcasts.service.api.podcastUuidsOrNull
import com.pocketcasts.service.api.record
import com.pocketcasts.service.api.shorterThanOrNull
import com.pocketcasts.service.api.sortPositionOrNull
import com.pocketcasts.service.api.sortTypeOrNull
import com.pocketcasts.service.api.starredOrNull
import com.pocketcasts.service.api.syncUserPlaylist
import com.pocketcasts.service.api.titleOrNull
import com.pocketcasts.service.api.unplayedOrNull
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

internal class PlaylistSync(
    private val syncManager: SyncManager,
    private val appDatabase: AppDatabase,
) {
    private val playlistDao = appDatabase.playlistDao()

    suspend fun fullSync() {
        processServerPlaylists(
            serverPlaylists = syncManager.getPlaylistsOrThrow().playlistsList,
            getUuid = { playlist -> playlist.uuid },
            isDeleted = { playlist -> playlist.isDeletedOrNull?.value == true },
            isManual = { playlist -> playlist.manualOrNull?.value == true },
            applyServerPlaylist = { localPlaylist, serverPlaylist -> localPlaylist.applyServerPlaylist(serverPlaylist) },
        )
    }

    suspend fun processIncrementalResponse(serverPlaylists: List<SyncUserPlaylist>) {
        processServerPlaylists(
            serverPlaylists = serverPlaylists,
            getUuid = { playlist -> playlist.uuid },
            isDeleted = { playlist -> playlist.isDeletedOrNull?.value == true },
            isManual = { playlist -> playlist.manualOrNull?.value == true },
            applyServerPlaylist = { localPlaylist, serverPlaylist -> localPlaylist.applyServerPlaylist(serverPlaylist) },
        )
    }

    private suspend fun <T> processServerPlaylists(
        serverPlaylists: List<T>,
        getUuid: (T) -> String,
        isDeleted: (T) -> Boolean,
        isManual: (T) -> Boolean,
        applyServerPlaylist: (SmartPlaylist, T) -> SmartPlaylist,
    ) {
        val deletedPlaylists = serverPlaylists.filter(isDeleted)
        val remainingPlaylist = serverPlaylists - deletedPlaylists
        val remainingPlaylistsMap = remainingPlaylist.associateBy(getUuid)
        // Manual playlists are not supported at the moment
        val (_, smartPlaylists) = remainingPlaylist.partition(isManual)

        appDatabase.withTransaction {
            playlistDao.deleteAll(deletedPlaylists.map(getUuid))

            val existingPlaylists = playlistDao.getAllPlaylists(smartPlaylists.map(getUuid))
            val existingPlaylistUuids = existingPlaylists.map(SmartPlaylist::uuid)
            existingPlaylists.forEach { playlist ->
                val serverPlaylist = remainingPlaylistsMap[playlist.uuid] ?: return@forEach
                applyServerPlaylist(playlist, serverPlaylist)
            }
            val newPlaylists = smartPlaylists.mapNotNull { serverPlaylist ->
                if (getUuid(serverPlaylist) !in existingPlaylistUuids) {
                    applyServerPlaylist(SmartPlaylist(), serverPlaylist)
                } else {
                    null
                }
            }
            playlistDao.upsertAllPlaylists(existingPlaylists + newPlaylists)
        }
    }

    suspend fun incrementalData(): List<Record> {
        val playlists = playlistDao.getAllUnsynced()
        return withContext(Dispatchers.Default) {
            playlists.map { localPlaylist ->
                record {
                    playlist = syncUserPlaylist {
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
                    }
                }
            }
        }
    }
}

private fun SmartPlaylist.applyServerPlaylist(serverPlaylist: PlaylistSyncResponse) = apply {
    syncStatus = SmartPlaylist.SYNC_STATUS_SYNCED
    uuid = serverPlaylist.uuid
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

private fun SmartPlaylist.applyServerPlaylist(serverPlaylist: SyncUserPlaylist) = apply {
    syncStatus = SmartPlaylist.SYNC_STATUS_SYNCED
    uuid = serverPlaylist.uuid
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

package au.com.shiftyjelly.pocketcasts.repositories.sync.data

import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.type.EpisodesSortType
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.servers.extensions.toDate
import au.com.shiftyjelly.pocketcasts.servers.extensions.toTimestamp
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.google.protobuf.boolValue
import com.google.protobuf.int32Value
import com.google.protobuf.stringValue
import com.pocketcasts.service.api.Record
import com.pocketcasts.service.api.SyncUserPodcast
import com.pocketcasts.service.api.UserPodcastResponse
import com.pocketcasts.service.api.autoSkipLastOrNull
import com.pocketcasts.service.api.autoStartFromOrNull
import com.pocketcasts.service.api.dateAddedOrNull
import com.pocketcasts.service.api.episodesSortOrderOrNull
import com.pocketcasts.service.api.folderUuidOrNull
import com.pocketcasts.service.api.record
import com.pocketcasts.service.api.sortPositionOrNull
import com.pocketcasts.service.api.subscribedOrNull
import com.pocketcasts.service.api.syncUserPodcast
import java.util.Date
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext

internal class PodcastSync(
    private val podcastManager: PodcastManager,
    private val playbackManager: PlaybackManager,
    private val missingPodcastsSemaphore: Semaphore,
) {
    suspend fun fullSync(serverPodcasts: List<UserPodcastResponse>) {
        val localPodcasts = podcastManager.findSubscribedNoOrder()
        val serverPodcastMap = serverPodcasts.associateBy(UserPodcastResponse::getUuid)
        val localPodcastMap = localPodcasts.associateBy(Podcast::uuid)

        val serverMissingUuids = localPodcastMap.keys - serverPodcastMap.keys
        podcastManager.markAllPodcastsUnsynced(serverMissingUuids)

        val localMissingUuids = serverPodcastMap.keys - localPodcastMap.keys

        coroutineScope {
            localMissingUuids.mapNotNull(serverPodcastMap::get).forEach { serverPodcast ->
                launch {
                    subscribeToPodcast(
                        serverPodcast = serverPodcast,
                        getUuid = UserPodcastResponse::getUuid,
                        applyServerPodcast = { localPodcast, serverPodcast -> localPodcast.applyServerPodcast(serverPodcast) },
                    )
                }
            }
        }

        val existingUuids = localPodcastMap.keys.intersect(serverPodcastMap.keys)
        existingUuids.forEach { uuid ->
            val localPodcast = localPodcastMap[uuid]
            val serverPodcast = serverPodcastMap[uuid]
            if (serverPodcast != null && localPodcast != null) {
                localPodcast.applyServerPodcast(serverPodcast)
                podcastManager.updatePodcast(localPodcast)
            }
        }
    }

    suspend fun incrementalData(): List<Record> {
        val podcasts = podcastManager.findPodcastsToSync()
        return withContext(Dispatchers.Default) {
            podcasts.map { localPodcast ->
                record {
                    podcast = syncUserPodcast {
                        uuid = localPodcast.uuid
                        folderUuid = stringValue {
                            value = localPodcast.folderUuid?.takeIf(String::isNotBlank) ?: Folder.HOME_FOLDER_UUID
                        }
                        isDeleted = boolValue {
                            value = !localPodcast.isSubscribed
                        }
                        subscribed = boolValue {
                            value = localPodcast.isSubscribed
                        }
                        autoStartFrom = int32Value {
                            value = localPodcast.startFromSecs
                        }
                        autoSkipLast = int32Value {
                            value = localPodcast.skipLastSecs
                        }
                        sortPosition = int32Value {
                            value = localPodcast.sortPosition
                        }
                        episodesSortOrder = int32Value {
                            value = localPodcast.episodesSortType.serverId
                        }
                        localPodcast.addedDate?.toTimestamp()?.let { timestamp ->
                            dateAdded = timestamp
                        }
                    }
                }
            }
        }
    }

    suspend fun processIncrementalResponse(serverPodcasts: List<SyncUserPodcast>) {
        coroutineScope {
            serverPodcasts.forEach { serverPodcast ->
                launch {
                    val localPodcast = podcastManager.findPodcastByUuid(serverPodcast.uuid)
                    if (localPodcast == null) {
                        syncPodcast(serverPodcast)
                    } else {
                        syncPodcast(localPodcast, serverPodcast)
                    }
                }
            }
        }
    }

    private suspend fun syncPodcast(serverPodcast: SyncUserPodcast) {
        if (serverPodcast.subscribedOrNull?.value == true) {
            subscribeToPodcast(
                serverPodcast = serverPodcast,
                getUuid = SyncUserPodcast::getUuid,
                applyServerPodcast = { localPodcast, serverPodcast -> localPodcast.applyServerPodcast(serverPodcast) },
            )
        }
    }

    private suspend fun syncPodcast(localPodcast: Podcast, serverPodcast: SyncUserPodcast) {
        if (serverPodcast.subscribedOrNull?.value == true) {
            localPodcast.syncStatus = Podcast.SYNC_STATUS_SYNCED
            localPodcast.isSubscribed = true
            localPodcast.applyServerPodcast(serverPodcast)
            podcastManager.updatePodcast(localPodcast)
        } else if (localPodcast.isSubscribed) {
            podcastManager.unsubscribe(localPodcast.uuid, playbackManager)
        }
    }

    private suspend fun <T> subscribeToPodcast(
        serverPodcast: T,
        getUuid: (T) -> String,
        applyServerPodcast: (Podcast, T) -> Podcast,
    ) = missingPodcastsSemaphore.withPermit {
        runCatching {
            val localPodcast = podcastManager.subscribeToPodcastOrThrow(getUuid(serverPodcast), sync = false, shouldAutoDownload = false).apply {
                isHeaderExpanded = false
                applyServerPodcast(this, serverPodcast)
            }
            podcastManager.updatePodcast(localPodcast)
        }.onFailure { failure -> LogBuffer.w("DataSync", "Failed to subscribe to podcast: ${getUuid(serverPodcast)}") }
    }
}

private fun Podcast.applyServerPodcast(serverPodcast: UserPodcastResponse) = apply {
    addedDate = minOf(
        addedDate ?: Date(),
        serverPodcast.dateAddedOrNull?.toDate() ?: Date(),
    )
    folderUuid = serverPodcast.folderUuidOrNull?.value
    serverPodcast.sortPositionOrNull?.value?.let { value ->
        sortPosition = value
    }
    EpisodesSortType.fromServerId(serverPodcast.episodesSortOrder)?.let { value ->
        episodesSortType = value
    }
    startFromSecs = serverPodcast.autoStartFrom
    skipLastSecs = serverPodcast.autoSkipLast
}

private fun Podcast.applyServerPodcast(serverPodcast: SyncUserPodcast) = apply {
    addedDate = serverPodcast.dateAddedOrNull?.toDate()
    folderUuid = serverPodcast.folderUuidOrNull?.value
    serverPodcast.sortPositionOrNull?.value?.let { value ->
        sortPosition = value
    }
    serverPodcast.episodesSortOrderOrNull?.value?.let(EpisodesSortType::fromServerId)?.let { value ->
        episodesSortType = value
    }
    serverPodcast.autoStartFromOrNull?.value?.let { value ->
        startFromSecs = value
    }
    serverPodcast.autoSkipLastOrNull?.value?.let { value ->
        skipLastSecs = value
    }
}

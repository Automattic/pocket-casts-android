package au.com.shiftyjelly.pocketcasts.repositories.sync.data

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import com.pocketcasts.service.api.Record
import com.pocketcasts.service.api.folderOrNull
import com.pocketcasts.service.api.podcastOrNull
import com.pocketcasts.service.api.syncUpdateRequest
import java.time.Instant
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import timber.log.Timber

class DataSyncProcess(
    private val syncManager: SyncManager,
    private val podcastManager: PodcastManager,
    private val folderManager: FolderManager,
    private val playbackManager: PlaybackManager,
    private val settings: Settings,
) {
    private val podcastSync = PodcastSync(podcastManager, playbackManager)
    private val folderSync = FoldersSync(folderManager)

    suspend fun sync() {
        runCatching {
            if (!syncManager.isLoggedIn()) {
                Timber.d("Delete marked playlists")
                return
            }

            val lastSyncTime = getLastSyncTime()
            syncData(lastSyncTime)
            Timber.d("Sync settings")
            Timber.d("Sync cloud files")
            Timber.d("Sync broken files")
            Timber.d("Sync playback history")
            Timber.d("Sync podcast ratings")
        }
    }

    private suspend fun syncData(lastSyncTime: Instant) {
        val isInitialSync = lastSyncTime == Instant.EPOCH
        if (isInitialSync) {
            syncFullData()
        } else if (settings.getHomeGridNeedsRefresh()) {
            Timber.d("Sync home grid")
        }
        Timber.d("Sync up next")
        if (!isInitialSync) {
            syncIncrementalData(lastSyncTime)
        }
    }

    private suspend fun syncFullData() {
        val lastSyncAt = syncManager.getLastSyncAtOrThrow()

        Timber.d("Sync stats")
        val homePageData = syncManager.getHomeFolderOrThrow()
        podcastSync.fullSync(homePageData.podcastsList)
        folderSync.fullSync(homePageData.foldersList)
        Timber.d("Sync playlists")
        Timber.d("Sync bookmarks")

        settings.setLastModified(lastSyncAt)
    }

    private suspend fun syncIncrementalData(lastSyncTime: Instant) {
        val request = coroutineScope {
            syncUpdateRequest {
                deviceUtcTimeMs = System.currentTimeMillis()
                lastModified = lastSyncTime.toEpochMilli()
                deviceId = settings.getUniqueDeviceId()
                records.addAll(
                    listOf(
                        async { podcastSync.incrementalData() },
                        async { folderSync.incrementalData() },
                    ).awaitAll().flatten(),
                )
            }
        }
        val response = syncManager.syncUpdateOrThrow(request)
        val records = response.recordsList
        podcastSync.processIncrementalResponse(records.mapNotNull(Record::podcastOrNull))
        folderSync.processIncrementalResponse(records.mapNotNull(Record::folderOrNull))
    }

    private fun getLastSyncTime() = runCatching {
        Instant.parse(settings.getLastModified())
    }.getOrDefault(Instant.EPOCH)
}

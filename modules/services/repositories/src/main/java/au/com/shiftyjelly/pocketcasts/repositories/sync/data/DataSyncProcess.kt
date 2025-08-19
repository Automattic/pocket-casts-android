package au.com.shiftyjelly.pocketcasts.repositories.sync.data

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncSettingsTask
import com.pocketcasts.service.api.Record
import com.pocketcasts.service.api.episodeOrNull
import com.pocketcasts.service.api.folderOrNull
import com.pocketcasts.service.api.podcastOrNull
import com.pocketcasts.service.api.syncUpdateRequest
import java.time.Instant
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import timber.log.Timber

class DataSyncProcess(
    private val syncManager: SyncManager,
    private val podcastManager: PodcastManager,
    private val folderManager: FolderManager,
    private val episodeManager: EpisodeManager,
    private val playbackManager: PlaybackManager,
    private val settings: Settings,
) {
    private val podcastSync = PodcastSync(podcastManager, playbackManager, missingPodcastsSemaphore = Semaphore(permits = 10))
    private val folderSync = FoldersSync(folderManager)
    private val episodeSync = EpisodeSync(episodeManager, podcastManager, playbackManager, settings)

    suspend fun sync(): Result<Unit> {
        if (!syncManager.isLoggedIn()) {
            Timber.d("Delete marked playlists")
            return Result.success(Unit)
        }

        return runCatching {
            val lastSyncTime = getLastSyncTime()
            val newSyncTime = syncData(lastSyncTime)
            syncSettings(lastSyncTime)
            Timber.d("Sync cloud files")
            Timber.d("Sync broken files")
            Timber.d("Sync playback history")
            Timber.d("Sync podcast ratings")
            settings.setLastModified(newSyncTime.toString())
        }
    }

    private suspend fun syncData(lastSyncTime: Instant): Instant {
        val isInitialSync = lastSyncTime == Instant.EPOCH
        return if (isInitialSync) {
            syncFullData()
        } else {
            if (settings.getHomeGridNeedsRefresh()) {
                Timber.d("Sync home grid")
            }
            syncIncrementalData(lastSyncTime)
        }
    }

    private suspend fun syncFullData(): Instant {
        val lastSyncAt = syncManager.getLastSyncAtOrThrow()

        Timber.d("Sync stats")
        val homePageData = syncManager.getHomeFolderOrThrow()
        podcastSync.fullSync(homePageData.podcastsList)
        folderSync.fullSync(homePageData.foldersList)
        Timber.d("Sync playlists")
        Timber.d("Sync bookmarks")

        return runCatching { Instant.parse(lastSyncAt) }.getOrDefault(Instant.now())
    }

    private suspend fun syncIncrementalData(lastSyncTime: Instant): Instant {
        val request = coroutineScope {
            syncUpdateRequest {
                deviceUtcTimeMs = System.currentTimeMillis()
                lastModified = lastSyncTime.toEpochMilli()
                deviceId = settings.getUniqueDeviceId()
                records.addAll(
                    listOf(
                        async { podcastSync.incrementalData() },
                        async { folderSync.incrementalData() },
                        async { episodeSync.incrementalData() },
                    ).awaitAll().flatten(),
                )
            }
        }
        val response = syncManager.syncUpdateOrThrow(request)
        val records = response.recordsList
        episodeSync.processIncrementalResponse(records.mapNotNull(Record::episodeOrNull))
        podcastSync.processIncrementalResponse(records.mapNotNull(Record::podcastOrNull))
        folderSync.processIncrementalResponse(records.mapNotNull(Record::folderOrNull))
        return Instant.ofEpochMilli(response.lastModified)
    }

    private suspend fun syncSettings(lastSyncTime: Instant) {
        SyncSettingsTask.run(settings, lastSyncTime, syncManager)
    }

    private fun getLastSyncTime() = runCatching {
        Instant.parse(settings.getLastModified())
    }.getOrDefault(Instant.EPOCH)
}

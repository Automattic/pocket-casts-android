package au.com.shiftyjelly.pocketcasts.repositories.sync.data

import android.content.Context
import androidx.lifecycle.asFlow
import androidx.work.Operation
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.UserPodcastRating
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.file.FileStorage
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.ratings.RatingsManager
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncHistoryTask
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.UpNextSyncWorker
import au.com.shiftyjelly.pocketcasts.repositories.user.StatsManager
import au.com.shiftyjelly.pocketcasts.servers.extensions.toDate
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncSettingsTask
import au.com.shiftyjelly.pocketcasts.utils.AppPlatform
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.pocketcasts.service.api.Record
import com.pocketcasts.service.api.SyncUpdateRequest
import com.pocketcasts.service.api.bookmarkOrNull
import com.pocketcasts.service.api.episodeOrNull
import com.pocketcasts.service.api.folderOrNull
import com.pocketcasts.service.api.modifiedAtOrNull
import com.pocketcasts.service.api.playlistOrNull
import com.pocketcasts.service.api.podcastOrNull
import com.pocketcasts.service.api.syncUpdateRequest
import java.time.Instant
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes
import kotlin.time.measureTimedValue
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.withTimeoutOrNull

class DataSyncProcess(
    private val syncManager: SyncManager,
    private val podcastManager: PodcastManager,
    private val folderManager: FolderManager,
    private val episodeManager: EpisodeManager,
    private val userEpisodeManager: UserEpisodeManager,
    private val playbackManager: PlaybackManager,
    private val statsManager: StatsManager,
    private val subscriptionManager: SubscriptionManager,
    private val ratingsManager: RatingsManager,
    private val appDatabase: AppDatabase,
    private val settings: Settings,
    private val fileStorage: FileStorage,
    private val context: Context,
) {
    private val logger = DataSyncLogger()
    private val podcastSync = PodcastSync(podcastManager, playbackManager, missingPodcastsSemaphore = Semaphore(permits = 10))
    private val folderSync = FoldersSync(folderManager)
    private val episodeSync = EpisodeSync(episodeManager, podcastManager, playbackManager, settings)
    private val playlistSync = PlaylistSync(syncManager, appDatabase)
    private val bookmarkSync = BookmarkSync(syncManager, appDatabase)
    private val deviceSync = DeviceSync(statsManager, settings)
    private val missingEpisodesSync = MissingEpisodesSync(syncManager, appDatabase)

    suspend fun sync(): Result<Unit> {
        return logProcess("data") {
            runCatching {
                if (!syncManager.isLoggedIn()) {
                    appDatabase.playlistDao().deleteMarkedPlaylists()
                } else {
                    val lastSyncTime = getLastSyncTime()
                    val newSyncTime = syncData(lastSyncTime)
                    settings.setLastModified(newSyncTime.toString())
                    syncSettings(lastSyncTime)
                    syncCloudFiles()
                    syncBrokenFiles()
                    syncPlaybackHistory()
                    syncPodcastRatings()
                }
            }
        }
    }

    private suspend fun syncData(lastSyncTime: Instant): Instant {
        val isInitialSync = lastSyncTime == Instant.EPOCH
        return if (isInitialSync) {
            val syncTime = syncFullData()
            syncUpNext()
            syncMissingEpisodes()
            syncTime
        } else {
            syncUpNext()
            val syncTime = syncIncrementalData(lastSyncTime)
            syncMissingEpisodes()
            syncTime
        }
    }

    private suspend fun syncFullData(): Instant {
        return logProcess("data-full") {
            val lastSyncAt = logProcess("request-last-sync-data-full") {
                syncManager.getLastSyncAtOrThrow()
            }
            logProcess("device-data-full") {
                deviceSync.syncStats()
            }
            val homePageData = logProcess("request-home-data-full") {
                syncManager.getHomeFolderOrThrow()
            }
            logProcess("podcasts-data-full") {
                podcastSync.fullSync(homePageData.podcastsList)
            }
            logProcess("folders-data-full") {
                folderSync.fullSync(homePageData.foldersList)
            }
            logProcess("playlists-data-full") {
                playlistSync.fullSync()
            }
            logProcess("bookmarks-data-full") {
                bookmarkSync.fullSync()
            }
            runCatching { Instant.parse(lastSyncAt) }.getOrDefault(Instant.now())
        }
    }

    private suspend fun syncIncrementalData(lastSyncTime: Instant): Instant {
        return logProcess("data-incremental") {
            val response = logProcess("request-data-incremental") {
                val request = createIncrementalRequest(lastSyncTime)
                syncManager.syncUpdateOrThrow(request)
            }
            val records = response.recordsList
            logProcess("episode-data-incremental") {
                episodeSync.processIncrementalResponse(records.mapNotNull(Record::episodeOrNull))
            }
            logProcess("podcasts-data-incremental") {
                podcastSync.processIncrementalResponse(records.mapNotNull(Record::podcastOrNull))
            }
            logProcess("folders-data-incremental") {
                folderSync.processIncrementalResponse(records.mapNotNull(Record::folderOrNull))
            }
            logProcess("playlists-data-incremental") {
                playlistSync.processIncrementalResponse(records.mapNotNull(Record::playlistOrNull))
            }
            logProcess("bookmarks-data-incremental") {
                bookmarkSync.processIncrementalResponse(records.mapNotNull(Record::bookmarkOrNull))
            }
            logProcess("device-data-incremental") {
                deviceSync.syncStats()
            }
            Instant.ofEpochMilli(response.lastModified)
        }
    }

    private suspend fun syncUpNext() {
        logProcess("up-next") {
            val operation = UpNextSyncWorker.enqueue(syncManager, context)
            operation?.awaitOperation("Up Next", timeoutDuration = 1.minutes)
        }
    }

    private suspend fun syncMissingEpisodes() {
        logProcess("missing-episodes") {
            missingEpisodesSync.sync()
        }
    }

    private suspend fun syncSettings(lastSyncTime: Instant) {
        logProcess("settings") {
            SyncSettingsTask.run(settings, lastSyncTime, syncManager).getOrThrow()
        }
    }

    private suspend fun syncCloudFiles() {
        logProcess("cloud-files") {
            val subscription = subscriptionManager.fetchFreshSubscription()
            if (subscription != null) {
                userEpisodeManager.syncFiles(playbackManager)
            }
        }
    }

    private suspend fun syncBrokenFiles() {
        val firstSync = settings.isFirstSyncRun()
        if (firstSync) {
            logProcess("broken-files") {
                fileStorage.fixBrokenFiles(episodeManager)
                settings.setFirstSyncRun(false)
            }
        }
    }

    private suspend fun syncPlaybackHistory() {
        // We don't use playback history on Wear OS
        if (!Util.isWearOs(context)) {
            logProcess("playback-history") {
                val operation = SyncHistoryTask.scheduleToRun(context)
                operation.awaitOperation("Playback History", timeoutDuration = 5.minutes)
            }
        }
    }

    private suspend fun syncPodcastRatings() {
        // Rating are available only on the Phone platform
        if (Util.getAppPlatform(context) == AppPlatform.Phone) {
            logProcess("ratings") {
                val ratings = syncManager.getPodcastRatings()?.podcastRatingsList
                    ?.mapNotNull { serverRating ->
                        serverRating.modifiedAtOrNull?.toDate()?.let { modifiedAt ->
                            UserPodcastRating(
                                podcastUuid = serverRating.podcastUuid,
                                rating = serverRating.podcastRating,
                                modifiedAt = modifiedAt,
                            )
                        }
                    }
                if (ratings != null) {
                    ratingsManager.updateUserRatings(ratings)
                }
            }
        }
    }

    private suspend fun createIncrementalRequest(lastSyncTime: Instant): SyncUpdateRequest = coroutineScope {
        syncUpdateRequest {
            deviceUtcTimeMs = System.currentTimeMillis()
            lastModified = lastSyncTime.toEpochMilli()
            deviceId = settings.getUniqueDeviceId()
            records.addAll(
                listOf(
                    async { podcastSync.incrementalData() },
                    async { folderSync.incrementalData() },
                    async { episodeSync.incrementalData() },
                    async { playlistSync.incrementalData() },
                    async { bookmarkSync.incrementalData() },
                    async { deviceSync.incrementalData() },
                ).awaitAll().flatten(),
            )
        }
    }

    private fun getLastSyncTime() = runCatching {
        Instant.parse(settings.getLastModified())
    }.getOrDefault(Instant.EPOCH)

    private inline fun <T> logProcess(name: String, process: DataSyncLogger.() -> T): T {
        return logger.logProcess(name) { logger.process() }
    }

    private suspend fun Operation.awaitOperation(
        operationName: String,
        timeoutDuration: Duration,
    ) {
        val state = withTimeoutOrNull(timeoutDuration) {
            state.asFlow().first { state ->
                when (state) {
                    is Operation.State.SUCCESS -> true
                    is Operation.State.FAILURE -> true
                    else -> false
                }
            }
        }
        when (state) {
            is Operation.State.SUCCESS -> Unit
            is Operation.State.FAILURE -> logger.logError("$operationName failed to sync", state.throwable)
            null -> logger.logInfo("$operationName didn't sync in time (it still runs in the background)")
            else -> logger.logInfo("Unexpected $operationName sync state: $state")
        }
    }
}

internal class DataSyncLogger {
    private val uuid = UUID.randomUUID()

    inline fun <T> logProcess(name: String, process: () -> T): T {
        logInfo("$name sync start")
        return runCatching { measureTimedValue { process() } }
            .onSuccess { (_, duration) ->
                logInfo("$name sync done - $duration")
            }
            .onFailure { failure ->
                if (failure is CancellationException) {
                    logInfo("$name sync cancelled")
                } else {
                    logError("$name sync failed", failure)
                }
            }
            .getOrThrow()
            .value
    }

    fun logInfo(message: String) {
        LogBuffer.i("DataSync", "$message - $uuid")
    }

    fun logError(message: String, error: Throwable) {
        LogBuffer.e("DataSync", error, "$message - $uuid")
    }
}

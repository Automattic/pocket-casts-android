package au.com.shiftyjelly.pocketcasts.repositories.sync

import android.content.Context
import android.os.Build
import android.os.SystemClock
import androidx.annotation.VisibleForTesting
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.entity.ChapterIndices
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveAfterPlaying
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveInactive
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveLimit
import au.com.shiftyjelly.pocketcasts.models.to.PodcastGrouping
import au.com.shiftyjelly.pocketcasts.models.to.StatsBundle
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodesSortType
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.models.type.SyncStatus
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.BuildConfig
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.file.FileStorage
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.FolderManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.shortcuts.PocketCastsShortcuts
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.repositories.user.StatsManager
import au.com.shiftyjelly.pocketcasts.servers.extensions.toDate
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServerManager
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncSettingsTask
import au.com.shiftyjelly.pocketcasts.servers.sync.update.SyncUpdateResponse
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.extensions.toIsoString
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.automattic.android.tracks.crashlogging.CrashLogging
import com.google.protobuf.Timestamp
import com.google.protobuf.boolValue
import com.google.protobuf.doubleValue
import com.google.protobuf.int32Value
import com.google.protobuf.int64Value
import com.google.protobuf.stringValue
import com.google.protobuf.timestamp
import com.pocketcasts.service.api.PodcastFolder
import com.pocketcasts.service.api.Record
import com.pocketcasts.service.api.SyncUpdateRequest
import com.pocketcasts.service.api.SyncUserDevice
import com.pocketcasts.service.api.UserPodcastResponse
import com.pocketcasts.service.api.boolSetting
import com.pocketcasts.service.api.dateAddedOrNull
import com.pocketcasts.service.api.doubleSetting
import com.pocketcasts.service.api.folderUuidOrNull
import com.pocketcasts.service.api.int32Setting
import com.pocketcasts.service.api.podcastSettings
import com.pocketcasts.service.api.record
import com.pocketcasts.service.api.sortPositionOrNull
import com.pocketcasts.service.api.syncUpdateRequest
import com.pocketcasts.service.api.syncUserBookmark
import com.pocketcasts.service.api.syncUserDevice
import com.pocketcasts.service.api.syncUserEpisode
import com.pocketcasts.service.api.syncUserFolder
import com.pocketcasts.service.api.syncUserPlaylist
import com.pocketcasts.service.api.syncUserPodcast
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import io.reactivex.schedulers.Schedulers
import java.time.Instant
import java.util.Date
import kotlin.coroutines.CoroutineContext
import kotlin.math.roundToInt
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.rx2.rxCompletable
import kotlinx.coroutines.rx2.rxSingle
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber

class PodcastSyncProcess(
    val context: Context,
    val applicationScope: CoroutineScope,
    var settings: Settings,
    var episodeManager: EpisodeManager,
    var podcastManager: PodcastManager,
    var playlistManager: PlaylistManager,
    var bookmarkManager: BookmarkManager,
    var statsManager: StatsManager,
    var fileStorage: FileStorage,
    var playbackManager: PlaybackManager,
    var podcastCacheServerManager: PodcastCacheServerManager,
    var userEpisodeManager: UserEpisodeManager,
    var subscriptionManager: SubscriptionManager,
    var folderManager: FolderManager,
    var syncManager: SyncManager,
    val crashLogging: CrashLogging,
    val analyticsTracker: AnalyticsTrackerWrapper,
) : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    fun run(): Completable {
        if (!syncManager.isLoggedIn()) {
            playlistManager.deleteSynced()

            Timber.i("SyncProcess: User not logged in")
            return Completable.complete()
        }

        val lasSyncTimeString = settings.getLastModified()
        val lastSyncTime = runCatching { Instant.parse(lasSyncTimeString) }
            .onFailure { Timber.e(it, "Could not convert lastModified String to Long: $lasSyncTimeString") }
            .getOrDefault(Instant.EPOCH)

        val downloadObservable = if (lastSyncTime == Instant.EPOCH) {
            performFullSync().andThen(syncUpNext())
        } else {
            if (settings.getHomeGridNeedsRefresh()) {
                Timber.i("SyncProcess: Refreshing home grid")
                performHomeGridRefresh()
                    .andThen(syncUpNext())
                    .andThen(performIncrementalSync(lastSyncTime))
            } else {
                syncUpNext()
                    .andThen(performIncrementalSync(lastSyncTime))
            }
        }
        val syncUpNextObservable = downloadObservable
            .andThen(syncSettings())
            .andThen(syncCloudFiles())
            .andThen(firstSyncChanges())
            .andThen(
                if (Util.isWearOs(context)) {
                    // We don't use the play history on wear os, so we can skip this potentially large call
                    Completable.complete()
                } else {
                    syncPlayHistory()
                },
            )
        return syncUpNextObservable
            .doOnError { throwable ->
                crashLogging.sendReport(throwable, message = "Sync failed")
                LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, throwable, "SyncProcess: Sync failed")
            }
            .doOnComplete {
                Timber.i("SyncProcess: Sync success")
            }
    }

    @VisibleForTesting
    fun performIncrementalSync(lastSyncTime: Instant): Completable =
        if (FeatureFlag.isEnabled(Feature.SETTINGS_SYNC)) {
            rxCompletable {
                performIncrementalSyncSuspend(lastSyncTime)
            }
        } else {
            @Suppress("DEPRECATION")
            oldPerformIncrementalSync(lastSyncTime)
        }

    private suspend fun performIncrementalSyncSuspend(lastSyncTime: Instant) {
        val episodesToSync = episodeManager.findEpisodesToSync()
        val syncUpdateRequest = getSyncUpdateRequest(lastSyncTime, episodesToSync)
        if (BuildConfig.DEBUG) {
            Timber.i("incremental sync request: $syncUpdateRequest")
        }
        val protobufResponse = syncManager.userSyncUpdate(syncUpdateRequest)
        if (BuildConfig.DEBUG) {
            Timber.i("incremental sync response: $protobufResponse")
        }
        val syncUpdateResponse = SyncUpdateResponse.fromProtobufSyncUpdateResponse(protobufResponse)
        processServerResponse(
            response = syncUpdateResponse,
            episodes = episodesToSync,
        ).await()
    }

    @Suppress("DEPRECATION")
    @Deprecated("This can be removed when Feature.SETTINGS_SYNC flag is removed")
    private fun oldPerformIncrementalSync(lastSyncTime: Instant): Completable {
        val uploadData = uploadChanges()
        val uploadObservable = syncManager.syncUpdate(uploadData.first, lastSyncTime)
        return uploadObservable.flatMap {
            processServerResponse(it, uploadData.second)
        }.ignoreElement()
    }

    private fun getSyncUpdateRequest(
        lastSyncTime: Instant,
        episodesToSync: List<PodcastEpisode>,
    ): SyncUpdateRequest {
        try {
            return syncUpdateRequest {
                deviceUtcTimeMs = System.currentTimeMillis()
                lastModified = lastSyncTime.toEpochMilli()

                val podcasts = podcastManager.findPodcastsToSync()
                val podcastRecords = podcasts.map { toRecord(it) }
                records.addAll(podcastRecords)

                val episodeRecords = episodesToSync.map { toRecord(it) }
                records.addAll(episodeRecords)

                val folderRecords = folderManager.findFoldersToSync().map { toRecord(it) }
                records.addAll(folderRecords)

                val playlistRecords = playlistManager.findPlaylistsToSync().map { toRecord(it) }
                records.addAll(playlistRecords)

                val bookmarkRecords = bookmarkManager.findBookmarksToSync().map { toRecord(it) }
                records.addAll(bookmarkRecords)

                getSyncUserDevice()?.let { syncUserDevice ->
                    val syncUserDeviceRecord = record {
                        this.device = syncUserDevice
                    }
                    records.add(syncUserDeviceRecord)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Unable to upload podcast to sync.")
            throw PocketCastsSyncException(e)
        }
    }

    private fun performFullSync(): Completable {
        // grab the last sync date before we begin
        return syncManager.getLastSyncAt()
            .flatMapCompletable { lastSyncAt ->
                cacheStats()
                    .andThen(downloadAndImportHomeFolder())
                    .andThen(downloadAndImportFilters())
                    .andThen(rxCompletable { downloadAndImportBookmarks() })
                    .andThen(Completable.fromAction { settings.setLastModified(lastSyncAt) })
            }
    }

    private fun downloadAndImportHomeFolder(): Completable {
        // get all the current podcasts and folder uuids before the sync
        val localPodcasts = podcastManager.findSubscribedRx()
        val localFolderUuids = folderManager.findFoldersSingle().map { it.map { folder -> folder.uuid } }
        // get all the users podcast uuids from the server
        val serverHomeFolder = rxSingle { syncManager.getHomeFolder() }
        return Singles.zip(serverHomeFolder, localPodcasts, localFolderUuids)
            .flatMapCompletable { (serverHomeFolder, localPodcasts, localFolderUuids) ->
                importPodcastsFullSync(serverPodcasts = serverHomeFolder.podcastsList, localPodcasts = localPodcasts)
                    .andThen(importFoldersFullSync(serverFolders = serverHomeFolder.foldersList, localFolderUuids = localFolderUuids))
            }
    }

    private fun performHomeGridRefresh(): Completable {
        return downloadAndImportHomeFolder()
            .andThen(markAllPodcastsUnsynced())
    }

    private fun markAllPodcastsUnsynced(): Completable {
        return rxCompletable {
            podcastManager.markAllPodcastsUnsynced()
        }
    }

    private fun importPodcastsFullSync(serverPodcasts: List<UserPodcastResponse>, localPodcasts: List<Podcast>): Completable {
        val localUuids = localPodcasts.map { podcast -> podcast.uuid }.toSet()
        val localPodcastsMap = localPodcasts.associateBy { it.uuid }
        val serverUuids = serverPodcasts.map { it.uuid }.toSet()
        val serverPodcastsMap = serverPodcasts.associateBy { it.uuid }
        // mark the podcasts missing from the server as not synced
        val serverMissingUuids = localUuids - serverUuids
        val markMissingNotSynced = Observable.fromIterable(serverMissingUuids).flatMapCompletable { uuid -> Completable.fromAction { podcastManager.markPodcastUuidAsNotSynced(uuid) } }
        // subscribe to each podcast
        val localMissingUuids = serverUuids - localUuids
        val subscribeToPodcasts = Observable
            .fromIterable(localMissingUuids)
            .flatMap(
                { uuid ->
                    Observable.just(uuid)
                        .subscribeOn(Schedulers.io())
                        .flatMapMaybe { importPodcast(serverPodcastsMap[it]) }
                },
                5,
            )
            .ignoreElements()
        // update existing podcasts
        val existingUuids = localUuids.intersect(serverUuids)
        val updatePodcasts = Observable
            .fromIterable(existingUuids)
            .flatMapCompletable { uuid ->
                val serverPodcast = serverPodcastsMap[uuid]
                val localPodcast = localPodcastsMap[uuid]
                updatePodcastSyncValues(localPodcast, serverPodcast)
            }
        return markMissingNotSynced.andThen(subscribeToPodcasts).andThen(updatePodcasts)
    }

    private fun importFoldersFullSync(serverFolders: List<PodcastFolder>, localFolderUuids: List<String>): Completable {
        val serverUuids = serverFolders.map { it.folderUuid }
        val serverMissingUuids = localFolderUuids - serverUuids
        // delete the local folders that aren't on the server
        val deleteLocalFolders = Observable.fromIterable(serverMissingUuids).flatMapCompletable { uuid ->
            rxCompletable {
                folderManager.deleteSynced(uuid)
            }
        }
        // upsert the rest of the folders
        val upsertServerFolders = Observable.fromIterable(serverFolders.mapNotNull { it.toFolder() }).flatMapCompletable { folder ->
            rxCompletable {
                folderManager.upsertSynced(folder)
            }
        }
        return deleteLocalFolders.andThen(upsertServerFolders)
    }

    private fun PodcastFolder.toFolder(): Folder? {
        val dateAdded = dateAdded?.toDate()
        if (folderUuid == null || name == null || dateAdded == null) {
            return null
        }
        return Folder(
            uuid = folderUuid,
            name = name,
            color = color,
            addedDate = dateAdded,
            sortPosition = sortPosition,
            podcastsSortType = PodcastsSortType.fromServerId(podcastsSortType),
            deleted = false,
            syncModified = Folder.SYNC_MODIFIED_FROM_SERVER,
        )
    }

    private fun downloadAndImportFilters(): Completable {
        return syncManager.getFilters()
            .flatMapCompletable { filters -> importFilters(filters) }
    }

    private suspend fun downloadAndImportBookmarks() {
        val bookmarks = syncManager.getBookmarks()
        importBookmarks(bookmarks)
    }

    private fun importPodcast(podcastResponse: UserPodcastResponse?): Maybe<Podcast> {
        val podcastUuid = podcastResponse?.uuid ?: return Maybe.empty()
        return podcastManager.subscribeToPodcastRx(podcastUuid = podcastUuid, sync = false)
            .flatMap { podcast -> updatePodcastSyncValues(podcast, podcastResponse).toSingleDefault(podcast) }
            .toMaybe()
            .doOnError { LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, "Could not import server podcast $podcastResponse.uuid", it) }
            .onErrorComplete()
    }

    private fun updatePodcastSyncValues(podcast: Podcast?, podcastResponse: UserPodcastResponse?): Completable = rxCompletable {
        if (podcast == null || podcastResponse == null) {
            return@rxCompletable
        }

        if (FeatureFlag.isEnabled(Feature.SETTINGS_SYNC)) {
            applyPodcastSyncUpdatesToPodcast(podcast, SyncUpdateResponse.PodcastSync.fromUserPodcastResponse(podcastResponse))
        } else {
            // use the oldest local or server added date
            val serverAddedDate = podcastResponse.dateAddedOrNull?.toDate() ?: Date()
            val localAddedDate = podcast.addedDate
            val resolvedAddedDate = if (localAddedDate == null || serverAddedDate < localAddedDate) serverAddedDate else localAddedDate

            podcast.apply {
                startFromSecs = podcastResponse.autoStartFrom
                skipLastSecs = podcastResponse.autoSkipLast
                folderUuid = podcastResponse.folderUuidOrNull?.value
                sortPosition = podcastResponse.sortPositionOrNull?.value ?: podcast.sortPosition
                addedDate = resolvedAddedDate
            }
        }

        podcastManager.updatePodcast(podcast)
    }

    private fun syncUpNext(): Completable {
        return Completable.fromAction {
            val startTime = SystemClock.elapsedRealtime()
            UpNextSyncJob.run(syncManager, context)
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Refresh - sync up next - ${String.format("%d ms", SystemClock.elapsedRealtime() - startTime)}")
        }
    }

    private fun syncPlayHistory(): Completable {
        return Completable.fromAction {
            val startTime = SystemClock.elapsedRealtime()
            SyncHistoryTask.scheduleToRun(context)
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Refresh - sync history - ${String.format("%d ms", SystemClock.elapsedRealtime() - startTime)}")
        }
    }

    private fun syncSettings(): Completable {
        return rxCompletable {
            val startTime = SystemClock.elapsedRealtime()
            SyncSettingsTask.run(context, settings, syncManager)
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Refresh - sync settings - ${String.format("%d ms", SystemClock.elapsedRealtime() - startTime)}")
        }
    }

    private fun syncCloudFiles(): Completable {
        return subscriptionManager.getSubscriptionStatus(allowCache = false)
            .flatMapCompletable {
                if (it is SubscriptionStatus.Paid) {
                    rxCompletable { userEpisodeManager.syncFiles(playbackManager) }
                } else {
                    Completable.complete()
                }
            }
    }

    @Suppress("DEPRECATION")
    @Deprecated("This should no longer be used once the SETTINGS_SYNC feature flag is removed/permanently-enabled.")
    private fun uploadChanges(): Pair<String, List<PodcastEpisode>> {
        val records = JSONArray()
        uploadPodcastChanges(records)
        val episodes = uploadEpisodesChanges(records)
        uploadPlaylistChanges(records)
        uploadFolderChanges(records)
        uploadBookmarksChanges(records)
        uploadStatChanges(records)

        val data = JSONObject()

        data.put("records", records)

        return Pair(data.toString(), episodes)
    }

    @Deprecated("This should no longer be used once the SETTINGS_SYNC feature flag is removed/permanently-enabled.")
    private fun uploadFolderChanges(records: JSONArray) {
        try {
            val folders = folderManager.findFoldersToSync()
            for (folder in folders) {
                val fields = JSONObject()

                try {
                    fields.put("folder_uuid", folder.uuid)
                    fields.put("is_deleted", if (folder.deleted) "1" else "0")
                    fields.put("name", folder.name)
                    fields.put("color", folder.color)
                    fields.put("sort_position", folder.sortPosition)
                    fields.put("podcasts_sort_type", folder.podcastsSortType.serverId)
                    fields.put("date_added", folder.addedDate.toIsoString())

                    val record = JSONObject()
                    record.put("fields", fields)
                    record.put("type", "UserFolder")

                    records.put(record)
                } catch (e: JSONException) {
                    Timber.e(e, "Unable to upload folder")
                    throw PocketCastsSyncException(e)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Unable to upload folders to sync.")
            throw PocketCastsSyncException(e)
        }
    }

    @Deprecated("This should no longer be used once the SETTINGS_SYNC feature flag is removed/permanently-enabled.")
    private fun uploadPlaylistChanges(records: JSONArray) {
        try {
            val playlists = playlistManager.findPlaylistsToSync()
            for (playlist in playlists) {
                val fields = JSONObject()

                if (playlist.manual) {
                    continue
                }

                try {
                    fields.put("uuid", playlist.uuid)
                    fields.put("is_deleted", if (playlist.deleted) "1" else "0")
                    fields.put("title", playlist.title)
                    fields.put("all_podcasts", if (playlist.allPodcasts) "1" else "0")
                    fields.put("podcast_uuids", playlist.podcastUuids)
                    fields.put("episode_uuids", null)
                    fields.put("audio_video", playlist.audioVideo)
                    fields.put("not_downloaded", if (playlist.notDownloaded) "1" else "0")
                    fields.put("downloaded", if (playlist.downloaded) "1" else "0")
                    fields.put("downloading", if (playlist.downloading) "1" else "0")
                    fields.put("finished", if (playlist.finished) "1" else "0")
                    fields.put("partially_played", if (playlist.partiallyPlayed) "1" else "0")
                    fields.put("unplayed", if (playlist.unplayed) "1" else "0")
                    fields.put("starred", if (playlist.starred) "1" else "0")
                    fields.put("manual", "0")
                    fields.put("sort_position", playlist.sortPosition)
                    fields.put("sort_type", playlist.sortId)
                    fields.put("icon_id", playlist.iconId)
                    fields.put("filter_hours", playlist.filterHours)
                    fields.put("filter_duration", playlist.filterDuration)
                    fields.put("longer_than", playlist.longerThan)
                    fields.put("shorter_than", playlist.shorterThan)

                    val record = JSONObject()
                    record.put("fields", fields)
                    record.put("type", "UserPlaylist")

                    records.put(record)
                } catch (e: JSONException) {
                    Timber.e(e, "Unable to save playlist")
                    throw PocketCastsSyncException(e)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Unable to upload playlist to sync.")
            throw PocketCastsSyncException(e)
        }
    }

    @VisibleForTesting
    internal fun getSyncUserDevice(): SyncUserDevice? =
        if (statsManager.isSynced(settings) || statsManager.isEmpty) {
            null
        } else {
            syncUserDevice {
                deviceId = stringValue { value = settings.getUniqueDeviceId() }
                deviceType = int32Value { value = ANDROID_DEVICE_TYPE }
                timeSilenceRemoval = int64Value { value = statsManager.timeSavedSilenceRemovalSecs }
                timeSkipping = int64Value { value = statsManager.timeSavedSkippingSecs }
                timeIntroSkipping = int64Value { value = statsManager.timeSavedSkippingIntroSecs }
                timeVariableSpeed = int64Value { value = statsManager.timeSavedVariableSpeedSecs }
                timeListened = int64Value { value = statsManager.totalListeningTimeSecs }
                timesStartedAt = int64Value { value = statsManager.statsStartTimeSecs }
            }
        }

    @Deprecated("This should no longer be used once the SETTINGS_SYNC feature flag is removed/permanently-enabled.")
    private fun uploadStatChanges(records: JSONArray) {
        if (statsManager.isSynced(settings) || statsManager.isEmpty) {
            return
        }
        try {
            val fields = JSONObject()
            val stats = statsManager.localStatsInServerFormat
            val itr = stats.keys.iterator()
            while (itr.hasNext()) {
                val key = itr.next()

                fields.put(key, stats[key])
            }

            fields.put(StatsBundle.SERVER_KEY_SKIPPING, statsManager.timeSavedSkippingSecs)
            fields.put(StatsBundle.SERVER_KEY_AUTO_SKIPPING, statsManager.timeSavedSkippingIntroSecs)
            fields.put(StatsBundle.SERVER_KEY_VARIABLE_SPEED, statsManager.timeSavedVariableSpeedSecs)
            fields.put(StatsBundle.SERVER_KEY_TOTAL_LISTENED, statsManager.totalListeningTimeSecs)
            fields.put(StatsBundle.SERVER_KEY_STARTED_AT, statsManager.statsStartTimeSecs)

            val record = JSONObject()
            record.put("fields", fields)
            record.put("type", "UserDevice")

            records.put(record)
        } catch (e: JSONException) {
            Timber.e(e, "Unable to save stats")
            throw PocketCastsSyncException(e)
        }
    }

    @Deprecated("This should no longer be used once the SETTINGS_SYNC feature flag is removed/permanently-enabled.")
    private fun uploadPodcastChanges(records: JSONArray) {
        try {
            val podcasts = podcastManager.findPodcastsToSync()
            for (podcast in podcasts) {
                try {
                    val fields = JSONObject().apply {
                        put("uuid", podcast.uuid)
                        put("is_deleted", if (podcast.isSubscribed) "0" else "1")
                        put("auto_start_from", podcast.startFromSecs)
                        put("auto_skip_last", podcast.skipLastSecs)
                        put("folder_uuid", if (podcast.folderUuid.isNullOrEmpty()) Folder.homeFolderUuid else podcast.folderUuid)
                        put("sort_position", podcast.sortPosition)
                        put("date_added", podcast.addedDate?.toIsoString())
                    }
                    val record = JSONObject().apply {
                        put("fields", fields)
                        put("type", "UserPodcast")
                    }
                    records.put(record)
                } catch (e: JSONException) {
                    Timber.e(e, "Unable to save podcast")
                    throw PocketCastsSyncException(e)
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Unable to upload podcast to sync.")
            throw PocketCastsSyncException(e)
        }
    }

    @Deprecated("This should no longer be used once the SETTINGS_SYNC feature flag is removed/permanently-enabled.")
    private fun uploadEpisodesChanges(records: JSONArray): List<PodcastEpisode> {
        try {
            val episodes = episodeManager.findEpisodesToSync()
            episodes.forEach { episode ->
                uploadEpisodeChanges(episode, records)
            }

            return episodes
        } catch (e: Exception) {
            Timber.e(e, "Unable to load episodes to sync.")
            throw PocketCastsSyncException(e)
        }
    }

    private fun uploadEpisodeChanges(episode: PodcastEpisode, records: JSONArray) {
        val fields = JSONObject()

        try {
            val playingStatus = when (episode.playingStatus) {
                EpisodePlayingStatus.IN_PROGRESS -> 2
                EpisodePlayingStatus.COMPLETED -> 3
                else -> 1
            }

            fields.put("uuid", episode.uuid)
            episode.playingStatusModified?.let { playingStatusModified ->
                fields.put("playing_status", playingStatus)
                fields.put("playing_status_modified", playingStatusModified)
            }

            episode.starredModified?.let { starredModified ->
                fields.put("starred", if (episode.isStarred) "1" else "0")
                fields.put("starred_modified", starredModified)
            }
            episode.playedUpToModified?.let { playedUpToModified ->
                fields.put("played_up_to", episode.playedUpTo)
                fields.put("played_up_to_modified", playedUpToModified)
            }
            episode.durationModified?.let { durationModified ->
                val duration = episode.duration
                if (duration != 0.0) {
                    fields.put("duration", duration)
                    fields.put("duration_modified", durationModified)
                }
            }
            episode.archivedModified?.let { archiveModified ->
                fields.put("is_deleted", if (episode.isArchived) "1" else "0")
                fields.put("is_deleted_modified", archiveModified)
            }
            fields.put("user_podcast_uuid", episode.podcastUuid)

            episode.deselectedChaptersModified?.let { deselectedChaptersModified ->
                fields.put("deselected_chapters", ChapterIndices.toString(episode.deselectedChapters))
                fields.put("deselected_chapters_modified", deselectedChaptersModified.time)
            }

            val record = JSONObject().apply {
                put("fields", fields)
                put("type", "UserEpisode")
            }

            records.put(record)
        } catch (e: JSONException) {
            Timber.e(e, "Unable to save episode")
        }
    }

    @Deprecated("This should no longer be used once the SETTINGS_SYNC feature flag is removed/permanently-enabled.")
    private fun uploadBookmarksChanges(records: JSONArray) {
        try {
            val bookmarks = bookmarkManager.findBookmarksToSync()
            bookmarks.forEach { bookmark ->
                @Suppress("DEPRECATION")
                uploadBookmarkChanges(bookmark, records)
            }
        } catch (e: Exception) {
            Timber.e(e, "Unable to load bookmarks to sync.")
            throw PocketCastsSyncException(e)
        }
    }

    @Deprecated("This should no longer be used once the SETTINGS_SYNC feature flag is removed/permanently-enabled.")
    private fun uploadBookmarkChanges(bookmark: Bookmark, records: JSONArray) {
        try {
            val fields = JSONObject().apply {
                put("bookmark_uuid", bookmark.uuid)
                put("podcast_uuid", bookmark.podcastUuid)
                put("episode_uuid", bookmark.episodeUuid)
                put("time", bookmark.timeSecs)
                put("created_at", bookmark.createdAt.toIsoString())
            }
            bookmark.titleModified?.let { titleModified ->
                fields.put("title", bookmark.title)
                fields.put("title_modified", titleModified)
            }
            bookmark.deletedModified?.let { deletedModified ->
                fields.put("is_deleted", if (bookmark.deleted) "1" else "0")
                fields.put("is_deleted_modified", deletedModified)
            }
            val record = JSONObject().apply {
                put("fields", fields)
                put("type", "UserBookmark")
            }
            records.put(record)
        } catch (e: JSONException) {
            crashLogging.sendReport(e)
            Timber.e(e, "Unable to save bookmark")
        }
    }

    private fun processServerResponse(response: SyncUpdateResponse, episodes: List<PodcastEpisode>): Single<String> {
        if (response.lastModified == null) {
            return Single.error(Exception("Server response doesn't return a last modified"))
        }
        // import episodes first so that newly added podcasts get their own episodes from the server.
        return markAllLocalItemsSynced(episodes)
            .andThen(importEpisodes(response.episodes))
            .andThen(importPodcasts(response.podcasts))
            .andThen(importFilters(response.playlists))
            .andThen(importFolders(response.folders))
            .andThen(rxCompletable { importBookmarks(response.bookmarks) })
            .andThen(updateSettings(response))
            .andThen(updateShortcuts(response.playlists))
            .andThen(cacheStats())
            .toSingle { response.lastModified }
    }

    private fun cacheStats(): Completable {
        return rxCompletable {
            statsManager.cacheMergedStats()
            statsManager.setSyncStatus(true)
        }
    }

    private fun markAllLocalItemsSynced(episodes: List<PodcastEpisode>): Completable {
        return Completable.fromAction {
            podcastManager.markAllPodcastsSynced()
            episodeManager.markAllEpisodesSynced(episodes)
            playlistManager.markAllSynced()
            folderManager.markAllSynced()
        }
    }

    private fun firstSyncChanges(): Completable {
        return Completable.fromAction {
            val firstSync = settings.isFirstSyncRun()
            if (firstSync) {
                runBlocking { fileStorage.fixBrokenFiles(episodeManager) }
                settings.setFirstSyncRun(false)
            }
        }
    }

    private fun updateSettings(response: SyncUpdateResponse): Completable {
        return Completable.fromAction {
            settings.setLastModified(response.lastModified)
        }
    }

    private fun updateShortcuts(playlists: List<Playlist>): Completable {
        // if any playlists have changed update the launcher shortcuts
        if (playlists.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            PocketCastsShortcuts.update(
                playlistManager = playlistManager,
                force = true,
                coroutineScope = applicationScope,
                context = context,
                source = PocketCastsShortcuts.Source.UPDATE_SHORTCUTS,
            )
        }
        return Completable.complete()
    }

    private fun importPodcasts(podcasts: List<SyncUpdateResponse.PodcastSync>): Completable {
        return Observable.fromIterable(podcasts)
            .flatMap(
                { podcastSync ->
                    Observable.just(podcastSync)
                        .subscribeOn(Schedulers.computation())
                        .flatMap { importPodcast(it).toObservable() }
                },
                10,
            )
            .ignoreElements()
    }

    private fun importEpisodes(episodes: List<SyncUpdateResponse.EpisodeSync>): Completable {
        return Observable.fromIterable(episodes)
            .flatMap { episode -> importEpisode(episode).toObservable() }
            .ignoreElements()
    }

    private fun importFilters(playlists: List<Playlist>): Completable {
        return Observable.fromIterable(playlists)
            .flatMap { playlist -> importPlaylist(playlist).toObservable() }
            .ignoreElements()
    }

    private fun importFolders(folders: List<Folder>): Completable {
        return Observable.fromIterable(folders)
            .flatMapCompletable { folder -> importFolder(folder) }
    }

    private suspend fun importBookmarks(bookmarks: List<Bookmark>) {
        for (bookmark in bookmarks) {
            importBookmark(bookmark)
        }
    }

    private fun importFolder(sync: Folder): Completable {
        return rxCompletable {
            if (sync.deleted) {
                folderManager.deleteSynced(sync.uuid)
            } else {
                folderManager.upsertSynced(sync)
            }
        }
    }

    private fun importPlaylist(sync: Playlist): Maybe<Playlist> {
        return Maybe.fromCallable<Playlist> {
            val uuid = sync.uuid
            if (uuid.isBlank()) {
                return@fromCallable null
            }
            // manual playlists are no longer supported
            if (sync.manual) {
                return@fromCallable null
            }

            var playlist = playlistManager.findByUuidSync(uuid)
            if (sync.deleted) {
                playlist?.let { playlistManager.deleteSynced(it) }
                return@fromCallable null
            }

            if (playlist == null) {
                playlist = Playlist(uuid = sync.uuid)
            }

            with(playlist) {
                title = sync.title
                audioVideo = sync.audioVideo
                notDownloaded = sync.notDownloaded
                downloaded = sync.downloaded
                downloading = sync.downloading
                finished = sync.finished
                partiallyPlayed = sync.partiallyPlayed
                unplayed = sync.unplayed
                starred = sync.starred
                manual = sync.manual
                sortPosition = sync.sortPosition
                sortId = sync.sortId
                iconId = sync.iconId
                allPodcasts = sync.allPodcasts
                podcastUuids = sync.podcastUuids
                filterHours = sync.filterHours
                syncStatus = Playlist.SYNC_STATUS_SYNCED
                filterDuration = sync.filterDuration
                longerThan = sync.longerThan
                shorterThan = sync.shorterThan
            }

            if (playlist.id == null) {
                playlist.id = playlistManager.create(playlist)
            } else {
                playlistManager.update(playlist, userPlaylistUpdate = null)
            }

            return@fromCallable playlist
        }
    }

    private fun importPodcast(sync: SyncUpdateResponse.PodcastSync): Maybe<Podcast> {
        val uuid = sync.uuid
        if (uuid.isNullOrBlank()) {
            return Maybe.empty()
        }

        val podcast = podcastManager.findPodcastByUuid(uuid)
        return if (podcast == null) {
            importServerPodcast(sync)
        } else {
            importExistingPodcast(podcast, sync)
        }
    }

    private fun importServerPodcast(podcastSync: SyncUpdateResponse.PodcastSync): Maybe<Podcast> {
        // don't import podcasts deleted or aren't subscribed too
        val isSubscribed = podcastSync.subscribed
        val podcastUuid = podcastSync.uuid
        if (podcastSync.subscribed && isSubscribed && podcastUuid != null) {
            return podcastManager.subscribeToPodcastRx(podcastUuid, sync = false)
                .doOnSuccess { podcast ->
                    applyPodcastSyncUpdatesToPodcast(podcast, podcastSync)
                    podcastManager.updatePodcast(podcast)
                }
                .toMaybe()
                .doOnError { LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, "Could not import server podcast $podcastUuid", it) }
                .onErrorComplete()
        } else {
            return Maybe.empty<Podcast>()
        }
    }

    private fun importExistingPodcast(podcast: Podcast, podcastSync: SyncUpdateResponse.PodcastSync): Maybe<Podcast> {
        if (podcastSync.subscribed) {
            podcast.syncStatus = Podcast.SYNC_STATUS_SYNCED
            podcast.isSubscribed = true
            applyPodcastSyncUpdatesToPodcast(podcast, podcastSync)

            podcastManager.updatePodcast(podcast)
        } else if (podcast.isSubscribed && !podcastSync.subscribed) { // Unsubscribed on the server but subscribed on device
            Timber.d("Unsubscribing from podcast $podcast during sync")
            podcastManager.unsubscribe(podcast.uuid, playbackManager)
        }
        return Maybe.just(podcast)
    }

    private fun applyPodcastSyncUpdatesToPodcast(podcast: Podcast, podcastSync: SyncUpdateResponse.PodcastSync) {
        podcast.addedDate = podcastSync.dateAdded
        podcast.folderUuid = podcastSync.folderUuid
        podcastSync.sortPosition?.let { podcast.sortPosition = it }
        withSyncValue(podcast.episodesSortTypeModified, podcastSync.episodesSortOrderModified, podcastSync.episodesSortOrder) {
            podcast.episodesSortType = EpisodesSortType.fromServerId(it) ?: EpisodesSortType.EPISODES_SORT_BY_TITLE_ASC
        }
        withSyncValue(podcast.startFromModified, podcastSync.startFromModified, podcastSync.startFromSecs) {
            podcast.startFromSecs = it
        }
        withSyncValue(podcast.skipLastModified, podcastSync.skipLastModified, podcastSync.skipLastSecs) {
            podcast.skipLastSecs = it
        }
        withSyncValue(podcast.autoAddToUpNextModified, podcastSync.addToUpNextLocalSettingModified, podcastSync.addToUpNextLocalSetting) {
            podcast.autoAddToUpNext = it
        }
        withSyncValue(podcast.overrideGlobalEffectsModified, podcastSync.useCustomPlaybackEffectsModified, podcastSync.useCustomPlaybackEffects) {
            podcast.overrideGlobalEffects = it
        }
        withSyncValue(podcast.playbackSpeedModified, podcastSync.playbackSpeedModified, podcastSync.playbackSpeed) {
            podcast.playbackSpeed = it
        }
        withSyncValue(podcast.trimModeModified, podcastSync.trimSilenceModified, podcastSync.trimSilence) {
            podcast.trimMode = TrimMode.fromServerId(it)
        }
        withSyncValue(podcast.volumeBoostedModified, podcastSync.useVolumeBoostModified, podcastSync.useVolumeBoost) {
            podcast.isVolumeBoosted = it
        }
        withSyncValue(podcast.showNotificationsModified, podcastSync.showNotificationsModified, podcastSync.showNotifications) {
            podcast.isShowNotifications = it
        }
        withSyncValue(podcast.overrideGlobalArchiveModified, podcastSync.autoArchiveModified, podcastSync.autoArchive) {
            podcast.overrideGlobalArchive = it
        }
        withSyncValue(podcast.autoArchiveAfterPlayingModified, podcastSync.autoArchivePlayedModified, podcastSync.autoArchivePlayed) {
            podcast.autoArchiveAfterPlaying = AutoArchiveAfterPlaying.fromServerId(it) ?: AutoArchiveAfterPlaying.defaultValue(context)
        }
        withSyncValue(podcast.autoArchiveInactiveModified, podcastSync.autoArchiveInactiveModified, podcastSync.autoArchiveInactive) {
            podcast.autoArchiveInactive = AutoArchiveInactive.fromIndex(it) ?: AutoArchiveInactive.Default
        }
        withSyncValue(podcast.autoArchiveEpisodeLimitModified, podcastSync.autoArchiveEpisodeLimitModified, podcastSync.autoArchiveEpisodeLimit) {
            podcast.autoArchiveEpisodeLimit = AutoArchiveLimit.fromServerId(it) ?: AutoArchiveLimit.None
        }
        withSyncValue(podcast.groupingModified, podcastSync.episodeGroupingModified, podcastSync.episodeGrouping) {
            podcast.grouping = PodcastGrouping.fromServerId(it) ?: PodcastGrouping.None
        }
        withSyncValue(podcast.showArchivedModified, podcastSync.showArchivedModified, podcastSync.showArchived) {
            podcast.showArchived = it
        }
    }

    private fun <T> withSyncValue(localDate: Date?, remoteDate: Date?, remoteValue: T?, update: (T) -> Unit) {
        val newValue = when {
            remoteValue == null || remoteDate == null -> null
            remoteDate > (localDate ?: Date(0)) -> remoteValue
            else -> null
        }
        newValue?.let(update)
    }

    fun importEpisode(episodeSync: SyncUpdateResponse.EpisodeSync): Maybe<PodcastEpisode> {
        val uuid = episodeSync.uuid ?: return Maybe.empty()

        // check if the episode already exists
        val episode = runBlocking {
            episodeManager.findByUuid(uuid)
        }
        return if (episode == null) {
            Maybe.empty()
        } else {
            importExistingEpisode(episodeSync, episode).toMaybe()
        }
    }

    private fun importExistingEpisode(sync: SyncUpdateResponse.EpisodeSync, episode: PodcastEpisode): Single<PodcastEpisode> {
        return Single.fromCallable {
            val playingEpisodeUuid = playbackManager.getCurrentEpisode()?.uuid
            val episodeInPlayer = playingEpisodeUuid != null && episode.uuid == playingEpisodeUuid
            val isPlaying = playbackManager.isPlaying()
            val isEpisodePlaying = episodeInPlayer && isPlaying

            sync.starred?.let {
                episode.isStarred = it
                episode.starredModified = null
            }

            sync.duration?.let {
                if (it > 0) {
                    episode.duration = it
                    episode.durationModified = null
                }
            }

            sync.isArchived?.let { newIsArchive ->
                if (episode.isArchived == newIsArchive) return@let

                if (isEpisodePlaying) {
                    // if we're playing this episode, marked the archive status as unsynced because the server might have a different one to us now
                    episode.archivedModified = System.currentTimeMillis()
                } else {
                    episode.archivedModified = null
                    if (newIsArchive) {
                        episodeManager.archive(episode, playbackManager, sync = false)
                    } else {
                        episode.isArchived = false
                        episode.lastArchiveInteraction = Date().time
                    }
                }
            }

            sync.playingStatus?.let { newPlayingStatus ->
                if (episode.playingStatus == newPlayingStatus) return@let

                if (isEpisodePlaying) {
                    // if we're playing this episode, marked the status as unsynced because the server might have a different one to us now
                    episode.playingStatusModified = System.currentTimeMillis()
                } else {
                    episode.playingStatusModified = null
                    episode.playingStatus = newPlayingStatus
                    if (episode.isFinished) {
                        episodeManager.markedAsPlayedExternally(episode, playbackManager, podcastManager)
                    }
                }
            }

            sync.playedUpTo?.let { playedUpTo ->
                if (playedUpTo < 0 || isEpisodePlaying) {
                    return@let
                }

                // don't update if times are very close
                val currentUpTo = episode.playedUpTo
                if (playedUpTo < currentUpTo - 5 || playedUpTo > currentUpTo + 2) {
                    episode.playedUpTo = playedUpTo
                    episode.playedUpToModified = null
                    if (episodeInPlayer) {
                        val diffSeconds = (playedUpTo - currentUpTo).roundToInt()
                        if (diffSeconds < 0) {
                            analyticsTracker.track(
                                AnalyticsEvent.PLAYBACK_EPISODE_POSITION_CHANGED_ON_SYNC,
                                mapOf(
                                    "position_change" to diffSeconds,
                                    "is_downloaded" to episode.isDownloaded,
                                    "episode_uuid" to episode.uuid,
                                    "podcast_uuid" to episode.podcastOrSubstituteUuid,
                                ),
                            )
                        }
                        playbackManager.seekIfPlayingToTimeMs(episode.uuid, (playedUpTo * 1000).toInt())
                    }
                }
            }

            sync.deselectedChapters?.let {
                episode.deselectedChapters = it
                episode.deselectedChaptersModified = sync.deselectedChaptersModified?.let(::Date)
            }

            episodeManager.update(episode)

            episode
        }
    }

    private suspend fun importBookmark(bookmark: Bookmark) {
        if (bookmark.deleted) {
            bookmarkManager.deleteSynced(bookmark.uuid)
        } else {
            bookmarkManager.upsertSynced(bookmark.copy(syncStatus = SyncStatus.SYNCED))
        }
    }

    companion object {

        @VisibleForTesting
        internal val ANDROID_DEVICE_TYPE = 2

        @VisibleForTesting
        internal fun toRecord(podcast: Podcast): Record =
            record {
                this.podcast = syncUserPodcast {
                    podcast.addedDate?.toProtobufTimestamp()?.let { timestamp ->
                        dateAdded = timestamp
                    }

                    folderUuid = stringValue {
                        val folderUuid = podcast.folderUuid
                        value = if (folderUuid.isNullOrEmpty()) {
                            Folder.homeFolderUuid
                        } else {
                            folderUuid
                        }
                    }

                    // In older versions of Pocket Casts it was possible to subscribe or delete
                    // a podcast. Send both values to ensure we don't break backward compatibility.
                    isDeleted = boolValue { value = !podcast.isSubscribed }
                    subscribed = boolValue { value = podcast.isSubscribed }

                    uuid = podcast.uuid

                    settings = podcastSettings {
                        autoStartFrom = int32Setting {
                            value = int32Value { value = podcast.startFromSecs }
                            modifiedAt = podcast.startFromModified.toProtobufTimestampOrFallback()
                        }
                        autoSkipLast = int32Setting {
                            value = int32Value { value = podcast.skipLastSecs }
                            modifiedAt = podcast.skipLastModified.toProtobufTimestampOrFallback()
                        }
                        addToUpNext = boolSetting {
                            value = boolValue { value = podcast.addToUpNextSyncSetting }
                            modifiedAt = podcast.autoAddToUpNextModified.toProtobufTimestampOrFallback()
                        }
                        episodesSortOrder = int32Setting {
                            value = int32Value { value = podcast.episodesSortType.serverId }
                            modifiedAt = podcast.episodesSortTypeModified.toProtobufTimestampOrFallback()
                        }
                        addToUpNextPosition = int32Setting {
                            value = int32Value { value = podcast.addToUpNextPositionSyncSetting }
                            modifiedAt = podcast.autoAddToUpNextModified.toProtobufTimestampOrFallback()
                        }
                        playbackEffects = boolSetting {
                            value = boolValue { value = podcast.overrideGlobalEffects }
                            modifiedAt = podcast.overrideGlobalEffectsModified.toProtobufTimestampOrFallback()
                        }
                        playbackSpeed = doubleSetting {
                            value = doubleValue { value = podcast.playbackSpeed }
                            modifiedAt = podcast.playbackSpeedModified.toProtobufTimestampOrFallback()
                        }
                        trimSilence = int32Setting {
                            value = int32Value { value = podcast.trimMode.serverId }
                            modifiedAt = podcast.trimModeModified.toProtobufTimestampOrFallback()
                        }
                        volumeBoost = boolSetting {
                            value = boolValue { value = podcast.isVolumeBoosted }
                            modifiedAt = podcast.volumeBoostedModified.toProtobufTimestampOrFallback()
                        }
                        notification = boolSetting {
                            value = boolValue { value = podcast.isShowNotifications }
                            modifiedAt = podcast.showNotificationsModified.toProtobufTimestampOrFallback()
                        }
                        autoArchive = boolSetting {
                            value = boolValue { value = podcast.overrideGlobalArchive }
                            modifiedAt = podcast.overrideGlobalArchiveModified.toProtobufTimestampOrFallback()
                        }
                        podcast.autoArchiveAfterPlaying?.serverId?.let { serverId ->
                            autoArchivePlayed = int32Setting {
                                value = int32Value { value = serverId }
                                modifiedAt = podcast.autoArchiveAfterPlayingModified.toProtobufTimestampOrFallback()
                            }
                        }
                        podcast.autoArchiveInactive?.serverId?.let { serverId ->
                            autoArchiveInactive = int32Setting {
                                value = int32Value { value = serverId }
                                modifiedAt = podcast.autoArchiveInactiveModified.toProtobufTimestampOrFallback()
                            }
                        }
                        autoArchiveEpisodeLimit = int32Setting {
                            value = int32Value { value = podcast.autoArchiveEpisodeLimit?.serverId ?: AutoArchiveLimit.None.serverId }
                            modifiedAt = podcast.autoArchiveEpisodeLimitModified.toProtobufTimestampOrFallback()
                        }
                        episodeGrouping = int32Setting {
                            value = int32Value { value = podcast.grouping.serverId }
                            modifiedAt = podcast.groupingModified.toProtobufTimestampOrFallback()
                        }
                        showArchived = boolSetting {
                            value = boolValue { value = podcast.showArchived }
                            modifiedAt = podcast.showArchivedModified.toProtobufTimestampOrFallback()
                        }
                    }

                    sortPosition = int32Value { value = podcast.sortPosition }
                }
            }

        @VisibleForTesting
        internal fun toRecord(episode: PodcastEpisode): Record =
            record {
                this.episode = syncUserEpisode {
                    uuid = episode.uuid
                    podcastUuid = episode.podcastUuid

                    episode.playingStatusModified?.let { episodePlayingStatusModified ->
                        playingStatus = int32Value { value = episode.playingStatus.toInt() }
                        playingStatusModified = int64Value { value = episodePlayingStatusModified }
                    }

                    episode.starredModified?.let { episodeStarredModified ->
                        starred = boolValue { value = episode.isStarred }
                        starredModified = int64Value { value = episodeStarredModified }
                    }

                    episode.playedUpToModified?.let { episodePlayedUpToModified ->
                        playedUpTo = int64Value { value = episode.playedUpTo.toLong() }
                        playedUpToModified = int64Value { value = episodePlayedUpToModified }
                    }

                    episode.durationModified?.let { episodeDurationModified ->
                        val episodeDuration = episode.duration
                        if (episodeDuration != 0.0) {
                            duration = int64Value { value = episodeDuration.toLong() }
                            durationModified = int64Value { value = episodeDurationModified }
                        }
                    }

                    episode.archivedModified?.let { episodeArchivedModified ->
                        isDeleted = boolValue { value = episode.isArchived }
                        isDeletedModified = int64Value { value = episodeArchivedModified }
                    }

                    episode.deselectedChaptersModified?.let {
                        deselectedChapters = ChapterIndices.toString(episode.deselectedChapters)
                        deselectedChaptersModified = int64Value { value = it.time }
                    }
                }
            }

        private fun toRecord(folder: Folder): Record =
            record {
                this.folder = syncUserFolder {
                    folderUuid = folder.uuid
                    isDeleted = folder.deleted
                    name = folder.name
                    color = folder.color
                    sortPosition = folder.sortPosition
                    podcastsSortType = folder.podcastsSortType.serverId
                    dateAdded = folder.addedDate.toProtobufTimestamp()
                }
            }

        private fun toRecord(playlist: Playlist): Record =
            record {
                this.playlist = syncUserPlaylist {
                    uuid = playlist.uuid
                    originalUuid = playlist.uuid // DO NOT REMOVE: server side this field is important, because it will remain the same upper/lower case
                    isDeleted = boolValue { value = playlist.deleted }
                    title = stringValue { value = playlist.title }
                    allPodcasts = boolValue { value = playlist.allPodcasts }
                    playlist.podcastUuids?.let {
                        podcastUuids = stringValue { value = it }
                    }
                    audioVideo = int32Value { value = playlist.audioVideo }
                    notDownloaded = boolValue { value = playlist.notDownloaded }
                    downloaded = boolValue { value = playlist.downloaded }
                    downloading = boolValue { value = playlist.downloading }
                    finished = boolValue { value = playlist.finished }
                    partiallyPlayed = boolValue { value = playlist.partiallyPlayed }
                    unplayed = boolValue { value = playlist.unplayed }
                    starred = boolValue { value = playlist.starred }
                    manual = boolValue { value = playlist.manual }
                    playlist.sortPosition?.let {
                        sortPosition = int32Value { value = it }
                    }
                    sortType = int32Value { value = playlist.sortId }
                    iconId = int32Value { value = playlist.iconId }
                    filterHours = int32Value { value = playlist.filterHours }
                    filterDuration = boolValue { value = playlist.filterDuration }
                    longerThan = int32Value { value = playlist.longerThan }
                    shorterThan = int32Value { value = playlist.shorterThan }
                }
            }

        @VisibleForTesting
        internal fun toRecord(bookmark: Bookmark): Record =
            record {
                this.bookmark = syncUserBookmark {
                    bookmarkUuid = bookmark.uuid
                    podcastUuid = bookmark.podcastUuid
                    episodeUuid = bookmark.episodeUuid
                    time = int32Value { value = bookmark.timeSecs }
                    createdAt = bookmark.createdAt.toProtobufTimestamp()
                    bookmark.titleModified?.let { bookmarkTitleModified ->
                        title = stringValue { value = bookmark.title }
                        titleModified = int64Value { value = bookmarkTitleModified }
                    }
                    bookmark.deletedModified?.let { bookmarkDeletedModified ->
                        isDeleted = boolValue { value = bookmark.deleted }
                        isDeletedModified = int64Value { value = bookmarkDeletedModified }
                    }
                }
            }
    }
}

private fun Date.toProtobufTimestamp(): Timestamp {
    val instant = toInstant()
    return timestamp {
        seconds = instant.epochSecond
        nanos = instant.nano
    }
}

private fun Date?.toProtobufTimestampOrFallback(): Timestamp {
    val instant = this?.toInstant() ?: UserSetting.DefaultFallbackTimestamp
    return timestamp {
        seconds = instant.epochSecond
        nanos = instant.nano
    }
}

private val Podcast.addToUpNextSyncSetting get() = autoAddToUpNext != Podcast.AutoAddUpNext.OFF
private val Podcast.addToUpNextPositionSyncSetting get() = when (autoAddToUpNext) {
    Podcast.AutoAddUpNext.OFF, Podcast.AutoAddUpNext.PLAY_LAST -> 0
    Podcast.AutoAddUpNext.PLAY_NEXT -> 1
}
private val SyncUpdateResponse.PodcastSync.addToUpNextLocalSetting get() = when (addToUpNext) {
    false -> Podcast.AutoAddUpNext.OFF
    true -> when (addToUpNextPosition) {
        0 -> Podcast.AutoAddUpNext.PLAY_LAST
        1 -> Podcast.AutoAddUpNext.PLAY_NEXT
        else -> null
    }
    else -> null
}
private val SyncUpdateResponse.PodcastSync.addToUpNextLocalSettingModified get() = addToUpNextModified?.let { addModified ->
    addToUpNextPositionModified?.let { positionModified ->
        maxOf(addModified, positionModified)
    }
}

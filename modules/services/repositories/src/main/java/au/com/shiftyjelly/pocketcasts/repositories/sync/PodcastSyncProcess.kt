package au.com.shiftyjelly.pocketcasts.repositories.sync

import android.content.Context
import android.os.Build
import android.os.SystemClock
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Playlist
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.to.StatsBundle
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
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
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServerManagerImpl
import au.com.shiftyjelly.pocketcasts.servers.sync.FolderResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.PodcastResponse
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncServerManager
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncSettingsTask
import au.com.shiftyjelly.pocketcasts.servers.sync.old.SyncOldServerManager
import au.com.shiftyjelly.pocketcasts.servers.sync.old.SyncUpdateResponse
import au.com.shiftyjelly.pocketcasts.utils.SentryHelper
import au.com.shiftyjelly.pocketcasts.utils.extensions.parseIsoDate
import au.com.shiftyjelly.pocketcasts.utils.extensions.toIsoString
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import io.reactivex.Completable
import io.reactivex.Maybe
import io.reactivex.Observable
import io.reactivex.Single
import io.reactivex.rxkotlin.Singles
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.rx2.rxCompletable
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import timber.log.Timber
import java.util.Date
import kotlin.coroutines.CoroutineContext

class PodcastSyncProcess(
    val context: Context,
    var settings: Settings,
    var episodeManager: EpisodeManager,
    var podcastManager: PodcastManager,
    var playlistManager: PlaylistManager,
    var statsManager: StatsManager,
    var fileStorage: FileStorage,
    var playbackManager: PlaybackManager,
    var syncOldServerManager: SyncOldServerManager,
    var syncServerManager: SyncServerManager,
    var podcastCacheServerManager: PodcastCacheServerManagerImpl,
    var userEpisodeManager: UserEpisodeManager,
    var subscriptionManager: SubscriptionManager,
    var folderManager: FolderManager
) : CoroutineScope {
    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default

    fun run(): Completable {
        if (!settings.isLoggedIn()) {
            playlistManager.deleteSynced()

            Timber.i("SyncProcess: User not logged in")
            return Completable.complete()
        }

        val lastModified = settings.getLastModified()

        val downloadObservable = if (lastModified == null) {
            performFullSync().andThen(syncUpNext())
        } else {
            if (settings.getHomeGridNeedsRefresh()) {
                Timber.i("SyncProcess: Refreshing home grid")
                performHomeGridRefresh()
                    .andThen(syncUpNext())
                    .andThen(performIncrementalSync(lastModified))
            } else {
                syncUpNext()
                    .andThen(performIncrementalSync(lastModified))
            }
        }
        val syncUpNextObservable = downloadObservable
            .andThen(syncSettings())
            .andThen(syncCloudFiles())
            .andThen(firstSyncChanges())
            .andThen(syncPlayHistory())
        return syncUpNextObservable
            .doOnError { throwable ->
                SentryHelper.recordException("Sync failed", throwable)
                LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, throwable, "SyncProcess: Sync failed")
            }
            .doOnComplete {
                Timber.i("SyncProcess: Sync success")
            }
    }

    private fun performIncrementalSync(lastModified: String): Completable {
        val uploadData = uploadChanges()
        val uploadObservable = syncOldServerManager.syncUpdate(uploadData.first, lastModified)
        val downloadObservable = uploadObservable.flatMap {
            processServerResponse(it, uploadData.second)
        }.ignoreElement()
        return downloadObservable
    }

    private fun performFullSync(): Completable {
        // grab the last sync date before we begin
        return syncServerManager.getLastSyncAt()
            .flatMapCompletable { lastSyncAt ->
                cacheStats()
                    .andThen(downloadAndImportHomeFolder())
                    .andThen(downloadAndImportFilters())
                    .andThen(Completable.fromAction { settings.setLastModified(lastSyncAt) })
            }
    }

    private fun downloadAndImportHomeFolder(): Completable {
        // get all the current podcasts and folder uuids before the sync
        val localPodcasts = podcastManager.findSubscribedRx()
        val localFolderUuids = folderManager.findFoldersSingle().map { it.map { folder -> folder.uuid } }
        // get all the users podcast uuids from the server
        val serverHomeFolder = syncServerManager.getHomeFolder()
        return Singles.zip(serverHomeFolder, localPodcasts, localFolderUuids)
            .flatMapCompletable { (serverHomeFolder, localPodcasts, localFolderUuids) ->
                importPodcastsFullSync(serverPodcasts = serverHomeFolder.podcasts ?: emptyList(), localPodcasts = localPodcasts)
                    .andThen(importFoldersFullSync(serverFolders = serverHomeFolder.folders ?: emptyList(), localFolderUuids = localFolderUuids))
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

    private fun importPodcastsFullSync(serverPodcasts: List<PodcastResponse>, localPodcasts: List<Podcast>): Completable {
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
                }, 5
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

    private fun importFoldersFullSync(serverFolders: List<FolderResponse>, localFolderUuids: List<String>): Completable {
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

    private fun downloadAndImportFilters(): Completable {
        return syncServerManager.getFilters()
            .flatMapCompletable { filters -> importFilters(filters) }
    }

    private fun importPodcast(podcastResponse: PodcastResponse?): Maybe<Podcast> {
        val podcastUuid = podcastResponse?.uuid ?: return Maybe.empty()
        return podcastManager.subscribeToPodcastRx(podcastUuid = podcastUuid, sync = false)
            .flatMap { podcast -> updatePodcastSyncValues(podcast, podcastResponse).toSingleDefault(podcast) }
            .toMaybe()
            .doOnError { LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, "Could not import server podcast $podcastResponse.uuid", it) }
            .onErrorComplete()
    }

    private fun updatePodcastSyncValues(podcast: Podcast?, podcastResponse: PodcastResponse?): Completable = rxCompletable {
        if (podcast == null || podcastResponse == null) {
            return@rxCompletable
        }
        // use the oldest local or server added date
        val serverAddedDate = podcastResponse.dateAdded?.parseIsoDate() ?: Date()
        val localAddedDate = podcast.addedDate
        val addedDate = if (localAddedDate == null || serverAddedDate < localAddedDate) serverAddedDate else localAddedDate

        podcastManager.updateSyncData(
            podcast = podcast,
            startFromSecs = podcastResponse.autoStartFrom ?: podcast.startFromSecs,
            skipLastSecs = podcastResponse.autoSkipLast ?: podcast.skipLastSecs,
            folderUuid = podcastResponse.folderUuid,
            sortPosition = podcastResponse.sortPosition ?: podcast.sortPosition,
            addedDate = addedDate
        )
    }

    private fun syncUpNext(): Completable {
        return Completable.fromAction {
            val startTime = SystemClock.elapsedRealtime()
            UpNextSyncJob.run(settings, context)
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
            SyncSettingsTask.run(settings, syncServerManager)
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Refresh - sync settings - ${String.format("%d ms", SystemClock.elapsedRealtime() - startTime)}")
        }
    }

    private fun syncCloudFiles(): Completable {
        return subscriptionManager.getSubscriptionStatus(allowCache = false)
            .flatMapCompletable {
                if (it is SubscriptionStatus.Plus) {
                    rxCompletable { userEpisodeManager.syncFiles(playbackManager) }
                } else {
                    Completable.complete()
                }
            }
    }

    private fun uploadChanges(): Pair<String, List<Episode>> {
        val records = JSONArray()
        uploadPodcastChanges(records)
        val episodes = uploadEpisodesChanges(records)
        uploadPlaylistChanges(records)
        uploadFolderChanges(records)
        uploadStatChanges(records)

        val data = JSONObject()

        data.put("records", records)

        return Pair(data.toString(), episodes)
    }

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

    private fun uploadEpisodesChanges(records: JSONArray): List<Episode> {
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

    private fun uploadEpisodeChanges(episode: Episode, records: JSONArray) {
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

            val record = JSONObject().apply {
                put("fields", fields)
                put("type", "UserEpisode")
            }

            records.put(record)
        } catch (e: JSONException) {
            Timber.e(e, "Unable to save episode")
        }
    }

    private fun processServerResponse(response: SyncUpdateResponse, episodes: List<Episode>): Single<String> {
        if (response.lastModified == null) {
            return Single.error(Exception("Server response doesn't return a last modified"))
        }
        // import episodes first so that newly added podcasts get their own episodes from the server.
        return markAllLocalItemsSynced(episodes)
            .andThen(importEpisodes(response.episodes))
            .andThen(importPodcasts(response.podcasts))
            .andThen(importFilters(response.playlists))
            .andThen(importFolders(response.folders))
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

    private fun markAllLocalItemsSynced(episodes: List<Episode>): Completable {
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
                fileStorage.fixBrokenFiles(episodeManager)
                settings.setFirstSyncRun(false)
            }
        }
    }

    private fun updateSettings(response: SyncUpdateResponse): Completable {
        return Completable.fromAction {
            settings.setLastModified(response.lastModified)
            settings.setLastSyncTime(System.currentTimeMillis())
        }
    }

    private fun updateShortcuts(playlists: List<Playlist>): Completable {
        // if any playlists have changed update the launcher shortcuts
        if (playlists.isNotEmpty() && Build.VERSION.SDK_INT >= Build.VERSION_CODES.N_MR1) {
            PocketCastsShortcuts.update(playlistManager, true, context)
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
                10
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

            var playlist = playlistManager.findByUuid(uuid)
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
        if (uuid == null || uuid.isNullOrBlank()) {
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
                    podcast.startFromSecs = podcastSync.startFromSecs ?: 0
                    podcast.skipLastSecs = podcastSync.skipLastSecs ?: 0
                    podcast.addedDate = podcastSync.dateAdded
                    podcastSync.sortPosition?.let { podcast.sortPosition = it }
                    podcastSync.folderUuid?.let { podcast.folderUuid = it }
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
            podcast.isSubscribed = podcastSync.subscribed
            podcastSync.startFromSecs?.let { podcast.startFromSecs = it }
            podcastSync.skipLastSecs?.let { podcast.skipLastSecs = it }
            podcastSync.sortPosition?.let { podcast.sortPosition = it }
            podcast.folderUuid = podcastSync.folderUuid
            podcast.addedDate = podcastSync.dateAdded

            podcastManager.updatePodcast(podcast)
        } else if (podcast.isSubscribed && !podcastSync.subscribed) { // Unsubscribed on the server but subscribed on device
            Timber.d("Unsubscribing from podcast $podcast during sync")
            podcastManager.unsubscribe(podcast.uuid, playbackManager)
        }
        return Maybe.just(podcast)
    }

    private fun importEpisode(episodeSync: SyncUpdateResponse.EpisodeSync): Maybe<Episode> {
        val uuid = episodeSync.uuid ?: return Maybe.empty()

        // check if the episode already exists
        val episode = episodeManager.findByUuid(uuid)
        return if (episode == null) {
            Maybe.empty()
        } else {
            importExistingEpisode(episodeSync, episode).toMaybe()
        }
    }

    private fun importExistingEpisode(sync: SyncUpdateResponse.EpisodeSync, episode: Episode): Single<Episode> {
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
                if (playedUpTo < currentUpTo - 2 || playedUpTo > currentUpTo + 2) {
                    episode.playedUpTo = playedUpTo
                    episode.playedUpToModified = null
                    if (episodeInPlayer) {
                        playbackManager.seekIfPlayingToTimeMs(episode.uuid, (playedUpTo * 1000).toInt())
                    }
                }
            }

            episodeManager.update(episode)

            episode
        }
    }
}

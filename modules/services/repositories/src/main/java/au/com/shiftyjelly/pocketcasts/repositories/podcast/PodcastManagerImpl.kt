package au.com.shiftyjelly.pocketcasts.repositories.podcast

import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.CuratedPodcast
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveAfterPlaying
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveInactive
import au.com.shiftyjelly.pocketcasts.models.to.AutoArchiveLimit
import au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffects
import au.com.shiftyjelly.pocketcasts.models.to.PodcastGrouping
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.models.type.EpisodesSortType
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadHelper
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.extensions.getUrlForArtwork
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.refresh.RefreshPodcastsTask
import au.com.shiftyjelly.pocketcasts.repositories.refresh.RefreshPodcastsThread
import au.com.shiftyjelly.pocketcasts.repositories.sync.PodcastRefresher
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.refresh.RefreshServiceManager
import au.com.shiftyjelly.pocketcasts.servers.refresh.UpdatePodcastResponse.EpisodeFound
import au.com.shiftyjelly.pocketcasts.servers.refresh.UpdatePodcastResponse.Retry
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.jakewharton.rxrelay2.PublishRelay
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.BackpressureStrategy
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.schedulers.Schedulers
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.rx2.asFlowable
import kotlinx.coroutines.rx2.await
import kotlinx.coroutines.rx2.rxMaybe
import timber.log.Timber

class PodcastManagerImpl @Inject constructor(
    private val episodeManager: EpisodeManager,
    private val smartPlaylistManager: SmartPlaylistManager,
    private val settings: Settings,
    @ApplicationContext private val context: Context,
    private val subscribeManager: SubscribeManager,
    private val refreshServiceManager: RefreshServiceManager,
    private val syncManager: SyncManager,
    private val podcastRefresher: PodcastRefresher,
    @ApplicationScope private val applicationScope: CoroutineScope,
    appDatabase: AppDatabase,
) : PodcastManager,
    CoroutineScope {

    companion object {
        private const val FIVE_MINUTES_IN_MILLIS = (5 * 60 * 1000).toLong()
        private const val TAG = "PodcastManager"
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default
    private val unsubscribeRelay = PublishRelay.create<String>()
    private val podcastDao = appDatabase.podcastDao()
    private val episodeDao = appDatabase.episodeDao()
    private val playlistDao = appDatabase.playlistDao()

    override suspend fun unsubscribe(podcastUuid: String, playbackManager: PlaybackManager) {
        try {
            val podcast = podcastDao.findPodcastByUuid(podcastUuid) ?: return
            val episodes = episodeManager.findEpisodesByPodcastOrderedSuspend(podcast)

            podcast.isSubscribed = false
            podcast.syncStatus = Podcast.SYNC_STATUS_NOT_SYNCED
            podcast.isShowNotifications = false
            podcast.autoDownloadStatus = Podcast.AUTO_DOWNLOAD_OFF
            podcast.autoAddToUpNext = Podcast.AutoAddUpNext.OFF
            podcast.autoArchiveAfterPlaying = AutoArchiveAfterPlaying.defaultValue(context)
            podcast.autoArchiveInactive = AutoArchiveInactive.Default
            podcast.autoArchiveEpisodeLimit = null
            podcast.overrideGlobalArchive = false
            podcast.folderUuid = null
            podcastDao.updateSuspend(podcast)

            episodeManager.deleteEpisodeFilesAsync(episodes, playbackManager)
            smartPlaylistManager.removePodcastFromPlaylists(podcastUuid)

            unsubscribeRelay.accept(podcastUuid)
        } catch (t: Throwable) {
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, t, "Could not unsubscribe from $podcastUuid")
        }
    }

    override fun unsubscribeBlocking(podcastUuid: String, playbackManager: PlaybackManager) {
        runBlocking {
            unsubscribe(podcastUuid, playbackManager)
        }
    }

    override fun unsubscribeAsync(podcastUuid: String, playbackManager: PlaybackManager) {
        launch {
            unsubscribe(podcastUuid, playbackManager)
        }
    }

    /**
     * Download and add podcast to the database. Or if it exists already just mark is as subscribed.
     */
    override fun subscribeToPodcast(podcastUuid: String, sync: Boolean, shouldAutoDownload: Boolean) {
        subscribeManager.subscribeOnQueue(podcastUuid, sync, shouldAutoDownload)
    }

    /**
     * Download and add podcast to the database. Or if it exists already just mark is as subscribed.
     * Do this now rather than adding it to a queue.
     */
    override fun subscribeToPodcastRxSingle(podcastUuid: String, sync: Boolean, shouldAutoDownload: Boolean): Single<Podcast> {
        return addPodcastRxSingle(podcastUuid = podcastUuid, sync = sync, subscribed = true, shouldAutoDownload = shouldAutoDownload)
    }

    override suspend fun subscribeToPodcastOrThrow(podcastUuid: String, sync: Boolean, shouldAutoDownload: Boolean): Podcast {
        return addPodcastRxSingle(podcastUuid = podcastUuid, sync = sync, subscribed = true, shouldAutoDownload = shouldAutoDownload).await()
    }

    /**
     * If the podcast isn't already in the database add it as unsubscribed.
     */
    override fun findOrDownloadPodcastRxSingle(podcastUuid: String, waitForSubscribe: Boolean): Single<Podcast> {
        return rxMaybe {
            if (waitForSubscribe) {
                findPodcastOrWaitForSubscribe(podcastUuid)
            } else {
                findPodcastByUuid(podcastUuid)
            }
        }
            .switchIfEmpty(subscribeManager.addPodcastRxSingle(podcastUuid, sync = false, subscribed = false, shouldAutoDownload = false).toMaybe())
            .toSingle()
    }

    private suspend fun findPodcastOrWaitForSubscribe(
        podcastUuid: String,
        retries: Int = 5,
        waitMs: Long = 1000L,
    ): Podcast? {
        val existingPodcast = findPodcastByUuid(podcastUuid)
        if (existingPodcast != null) {
            return existingPodcast
        }
        // if the podcast is being subscribed to wait for it to be added
        if (subscribeManager.isSubscribingToPodcast(podcastUuid)) {
            // retry 5 times to see if the podcast has been added
            for (retryCount in 1..retries) {
                Timber.i("Waiting for podcast to subscribe: $podcastUuid retryCount: $retryCount")
                // wait 1 second before checking again
                delay(waitMs)
                val podcast = findPodcastByUuid(podcastUuid)
                if (podcast != null) {
                    return podcast
                }
            }
        }
        return null
    }

    override fun addPodcastRxSingle(podcastUuid: String, sync: Boolean, subscribed: Boolean, shouldAutoDownload: Boolean): Single<Podcast> {
        return subscribeManager.addPodcastRxSingle(podcastUuid = podcastUuid, sync = sync, subscribed = subscribed, shouldAutoDownload = shouldAutoDownload)
    }

    override fun isSubscribingToPodcast(podcastUuid: String): Boolean {
        return subscribeManager.isSubscribingToPodcast(podcastUuid)
    }

    override fun isSubscribingToPodcasts(): Boolean {
        return subscribeManager.getSubscribingPodcastUuids().isNotEmpty()
    }

    override fun getSubscribedPodcastUuidsRxSingle(): Single<List<String>> {
        // get the podcasts from the database
        val databasePodcasts = podcastDao.findSubscribedRxSingle()
        // use just the uuids
        val databaseUuids = databasePodcasts.map { podcasts -> podcasts.map { it.uuid } }
        // add the uuids of podcasts currently being added
        val addQueuedUuids = databaseUuids.map { uuids ->
            val allUuids = HashSet(uuids)
            allUuids.addAll(subscribeManager.getSubscribingPodcastUuids())
            allUuids.toList()
        }
        return addQueuedUuids
    }

    override fun podcastSubscriptionsRxFlowable(): Flowable<List<String>> {
        return subscribeManager.subscriptionChangedRelay
            .mergeWith(unsubscribeRelay)
            .flatMap { getSubscribedPodcastUuidsRxSingle().toObservable() } // Every time the subscriptions change, reload the subscribed list and pass it on
            .subscribeOn(Schedulers.io())
            .toFlowable(BackpressureStrategy.LATEST)
    }

    override fun findPodcastsToSyncBlocking(): List<Podcast> {
        return podcastDao.findNotSyncedBlocking()
    }

    override suspend fun findPodcastsToSync(): List<Podcast> {
        return podcastDao.findNotSynced()
    }

    override fun findPodcastsAutodownloadBlocking(): List<Podcast> {
        return podcastDao.findPodcastsAutoDownloadBlocking()
    }

    override fun episodeCountByPodcatUuidFlow(uuid: String): Flow<Int> {
        return podcastDao.episodeCountFlow(uuid)
    }

    override fun refreshPodcastsIfRequired(fromLog: String) {
        // if it's been more than 5 minutes since the last refresh, do another one
        val lastUpdateTime = settings.getLastRefreshTime()
        if (System.currentTimeMillis() - lastUpdateTime > FIVE_MINUTES_IN_MILLIS) {
            refreshPodcasts(fromLog)
        }
    }

    override fun refreshPodcasts(fromLog: String) {
        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Running refresh from $fromLog")
        RefreshPodcastsTask.runNow(context, applicationScope)
    }

    override suspend fun refreshPodcastsAfterSignIn() {
        RefreshPodcastsThread.clearLastRefreshTime()
        markAllPodcastsUnsynced()
        refreshPodcasts("login")
    }

    override suspend fun refreshPodcast(existingPodcast: Podcast, playbackManager: PlaybackManager) {
        podcastRefresher.refreshPodcast(existingPodcast, playbackManager)
    }

    override fun checkForUnusedPodcastsBlocking(playbackManager: PlaybackManager) {
        podcastDao.findUnsubscribedBlocking().forEach { podcast ->
            deletePodcastIfUnusedBlocking(podcast, playbackManager)
        }
    }

    override fun deletePodcastIfUnusedBlocking(podcast: Podcast, playbackManager: PlaybackManager): Boolean {
        // we don't delete podcasts that haven't been synced or you're still subscribed to
        if ((syncManager.isLoggedIn() && podcast.isNotSynced) || podcast.isSubscribed || runBlocking { isPodcastInManualPlaylist(podcast) }) {
            return false
        }

        // we don't delete podcasts added to the phone in the last week. This is to prevent stuff you just leave open in discover from being removed
        val addedDate = podcast.addedDate
        val oneWeekAgoMs = System.currentTimeMillis() - 7.days.inWholeMilliseconds
        if (addedDate != null && addedDate.time > oneWeekAgoMs) {
            return false
        }

        // podcasts can be deleted if all of the episodes are haven't been interacted with
        val episodes = episodeManager.findEpisodesByPodcastOrderedBlocking(podcast)
        var podcastHasChangedEpisodes = false
        var latestPlaybackInteraction = 0L
        val deleteEpisodes = mutableListOf<PodcastEpisode>()
        for (episode in episodes) {
            // find the latest playback interaction
            episode.lastPlaybackInteraction?.let { interaction ->
                if (interaction > latestPlaybackInteraction) {
                    latestPlaybackInteraction = interaction
                }
            }
            // don't delete the podcast if any of the episodes have been interacted with
            if (episodeManager.userHasInteractedWithEpisode(episode, playbackManager)) {
                podcastHasChangedEpisodes = true
                continue
            }
            // bulk delete or it takes 10 seconds on a large podcast
            deleteEpisodes.add(episode)
        }
        // don't delete the episodes or podcast if the latest playback interaction happening in the last month
        val oneMonthAgoMs = System.currentTimeMillis() - 30.days.inWholeMilliseconds
        if (latestPlaybackInteraction > oneMonthAgoMs) {
            return false
        }
        if (deleteEpisodes.isNotEmpty()) {
            episodeManager.deleteEpisodesWithoutSyncBlocking(deleteEpisodes, playbackManager)
        }
        if (!podcastHasChangedEpisodes) {
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Removing unused podcast ${podcast.title}")
            // this podcast isn't needed, delete it
            podcastDao.deleteBlocking(podcast)

            return true
        }

        return false
    }

    override suspend fun deleteAllPodcasts() {
        podcastDao.deleteAll()
    }

    override suspend fun findSubscribedUuids(): List<String> {
        return podcastDao.findSubscribedUuids()
    }

    override fun findPodcastByUuidBlocking(uuid: String): Podcast? {
        return podcastDao.findByUuidBlocking(uuid)
    }

    override suspend fun findPodcastByUuid(uuid: String): Podcast? {
        return podcastDao.findPodcastByUuid(uuid)
    }

    override fun findPodcastByUuidRxMaybe(uuid: String): Maybe<Podcast> {
        return Maybe.fromCallable { findPodcastByUuidBlocking(uuid) }
    }

    override fun podcastByUuidRxFlowable(uuid: String): Flowable<Podcast> {
        return podcastDao.findByUuidRxFlowable(uuid)
    }

    override fun podcastByUuidFlow(uuid: String): Flow<Podcast> {
        return podcastDao.findByUuidFlow(uuid)
    }

    override fun podcastByEpisodeUuidFlow(uuid: String): Flow<Podcast> {
        return flow {
            val episode = episodeDao.findByUuid(uuid)
            if (episode != null) {
                emitAll(podcastDao.findByUuidFlow(episode.podcastUuid))
            }
        }
    }

    override suspend fun findPodcastsInFolder(folderUuid: String): List<Podcast> {
        return podcastDao.findPodcastsInFolder(folderUuid)
    }

    override fun findPodcastsInFolderRxSingle(folderUuid: String): Single<List<Podcast>> {
        return podcastDao.findPodcastsInFolderRxSingle(folderUuid)
    }

    override suspend fun findPodcastsNotInFolder(): List<Podcast> {
        return podcastDao.findPodcastsNotInFolder()
    }

    override fun findSubscribedBlocking(): List<Podcast> {
        return podcastDao.findSubscribedBlocking()
    }

    override suspend fun findSubscribedNoOrder(): List<Podcast> {
        return podcastDao.findSubscribedNoOrder()
    }

    override suspend fun findSubscribedSorted(): List<Podcast> {
        val sortType = settings.podcastsSortType.value
        return when (sortType) {
            // use a query to get the podcasts ordered by episode release date or recently played episodes
            PodcastsSortType.EPISODE_DATE_NEWEST_TO_OLDEST -> findPodcastsOrderByLatestEpisode(orderAsc = false)
            PodcastsSortType.RECENTLY_PLAYED -> findPodcastsOrderByRecentlyPlayedEpisode()
            else -> podcastDao.findSubscribedNoOrder().sortedWith(sortType.podcastComparator)
        }
    }

    override fun findSubscribedRxSingle(): Single<List<Podcast>> {
        return Single.fromCallable { findSubscribedBlocking() }
    }

    override fun findSubscribedFlow(): Flow<List<Podcast>> {
        return podcastDao.findSubscribedFlow()
    }

    override fun observePodcastsSortedByLatestEpisode(): Flow<List<Podcast>> {
        return podcastDao.observeSubscribedOrderByLatestEpisode(orderAsc = false)
    }

    override fun observePodcastsBySortedRecentlyPlayed(): Flow<List<Podcast>> {
        return podcastDao.observePodcastsOrderByRecentlyPlayedEpisode()
    }

    override fun podcastsOrderByLatestEpisodeRxFlowable(): Flowable<List<Podcast>> {
        return observePodcastsSortedByLatestEpisode().asFlowable()
    }

    override fun podcastsOrderByRecentlyPlayedEpisodeRxFlowable(): Flowable<List<Podcast>> {
        return observePodcastsBySortedRecentlyPlayed().asFlowable()
    }

    override fun observePodcastsSortedByUserChoice(folder: Folder): Flow<List<Podcast>> {
        val sort = folder.podcastsSortType
        return when (sort) {
            PodcastsSortType.DATE_ADDED_NEWEST_TO_OLDEST -> podcastDao.observeFolderOrderByAddedDate(folder.uuid, sort.isAsc())
            PodcastsSortType.EPISODE_DATE_NEWEST_TO_OLDEST -> podcastDao.observeFolderOrderByLatestEpisode(folder.uuid, sort.isAsc())
            PodcastsSortType.RECENTLY_PLAYED -> podcastDao.observePodcastsOrderByRecentlyPlayedEpisode(folder.uuid)
            PodcastsSortType.DRAG_DROP -> podcastDao.observeFolderOrderByUserSort(folder.uuid)
            else -> podcastDao.observeFolderOrderByName(folder.uuid, sort.isAsc())
        }
    }

    override fun subscribedRxFlowable(): Flowable<List<Podcast>> {
        return podcastDao.findSubscribedRxFlowable()
    }

    override suspend fun findPodcastsOrderByLatestEpisode(orderAsc: Boolean): List<Podcast> {
        return podcastDao.findSubscribedOrderByLatestEpisode(orderAsc)
    }

    override suspend fun findFolderPodcastsOrderByLatestEpisode(folderUuid: String): List<Podcast> {
        return podcastDao.findFolderPodcastsOrderByLatestEpisodeBlocking(folderUuid)
    }

    override suspend fun findPodcastsOrderByRecentlyPlayedEpisode(): List<Podcast> {
        return podcastDao.findPodcastsOrderByRecentlyPlayedEpisode()
    }

    override suspend fun findFolderPodcastsOrderByRecentlyPlayedEpisode(folderUuid: String): List<Podcast> {
        return podcastDao.findPodcastsOrderByRecentlyPlayedEpisode(folderUuid)
    }

    override suspend fun findPodcastsOrderByTitle(): List<Podcast> {
        return podcastDao.findSubscribedNoOrder().sortedWith(PodcastsSortType.NAME_A_TO_Z.podcastComparator)
    }

    override fun searchPodcastByTitleBlocking(title: String): Podcast? {
        return podcastDao.searchByTitleBlocking("%$title%")
    }

    override fun markPodcastUuidAsNotSyncedBlocking(podcastUuid: String) {
        updateSyncStatusBlocking(podcastUuid, Podcast.SYNC_STATUS_NOT_SYNCED)
    }

    private fun updateSyncStatusBlocking(podcastUuid: String, syncStatus: Int) {
        podcastDao.updateSyncStatusBlocking(syncStatus, podcastUuid)
    }

    override fun updateGroupingBlocking(podcast: Podcast, grouping: PodcastGrouping) {
        podcastDao.updateGroupingBlocking(grouping, podcast.uuid)
    }

    override fun updateGroupingForAllBlocking(grouping: PodcastGrouping) {
        podcastDao.updatePodcastGroupingForAllBlocking(grouping)
    }

    override suspend fun markAllPodcastsUnsynced() {
        podcastDao.updateAllSubscribedSyncStatus(Podcast.SYNC_STATUS_NOT_SYNCED)
    }

    override suspend fun markAllPodcastsUnsynced(uuids: Collection<String>) {
        podcastDao.updateAllSyncStatus(Podcast.SYNC_STATUS_NOT_SYNCED, uuids)
    }

    override suspend fun markAllPodcastsSynced() {
        podcastDao.updateAllSyncStatus(Podcast.SYNC_STATUS_SYNCED)
    }

    override fun clearAllDownloadErrorsBlocking() {
        episodeDao.clearAllDownloadErrorsBlocking(EpisodeStatusEnum.NOT_DOWNLOADED, EpisodeStatusEnum.DOWNLOAD_FAILED)
    }

    /**
     * Count all podcasts in the database. This includes deleted podcasts and the custom folder.
     * To exclude deleted podcasts and the custom folder use countSubscribed
     */
    override fun countPodcastsBlocking(): Int {
        return podcastDao.countBlocking()
    }

    override suspend fun countSubscribed(): Int {
        return podcastDao.countSubscribed()
    }

    override fun countSubscribedRxSingle(): Single<Int> {
        return podcastDao.countSubscribedRxSingle()
    }

    override fun countSubscribedFlow(): Flow<Int> {
        return podcastDao.countSubscribedFlow()
    }

    override fun countDownloadStatusBlocking(downloadStatus: Int): Int {
        return podcastDao.countDownloadStatusBlocking(downloadStatus)
    }

    override suspend fun hasEpisodesWithAutoDownloadStatus(downloadStatus: Int): Boolean {
        return podcastDao.hasEpisodesWithAutoDownloadStatus(downloadStatus)
    }

    override fun countDownloadStatusRxSingle(downloadStatus: Int): Single<Int> {
        return Single.fromCallable { countDownloadStatusBlocking(downloadStatus) }
    }

    override fun countNotificationsOnBlocking(): Int {
        return podcastDao.countNotificationsOnBlocking()
    }

    // WARNING: only call this when NEW episodes are added, not old ones
    override fun updatePodcastLatestEpisodeBlocking(podcast: Podcast) {
        // get the most recent episode details
        val episode = episodeDao.findLatestBlocking(podcast.uuid) ?: return
        val latestEpisodeUuid = episode.uuid
        val latestEpisodeDate = episode.publishedDate
        podcastDao.updateLatestEpisodeBlocking(episodeUuid = latestEpisodeUuid, publishedDate = latestEpisodeDate, podcastUuid = podcast.uuid)
    }

    override suspend fun replaceCuratedPodcasts(podcasts: List<CuratedPodcast>) {
        podcastDao.replaceAllCuratedPodcasts(podcasts)
    }

    override fun updatePodcastBlocking(podcast: Podcast) {
        podcastDao.updateBlocking(podcast)
    }

    override suspend fun updatePodcast(podcast: Podcast) {
        podcastDao.updateSuspend(podcast)
    }

    override suspend fun updateAllAutoDownloadStatus(autoDownloadStatus: Int) {
        podcastDao.updateAllAutoDownloadStatus(autoDownloadStatus)
    }

    override suspend fun updateAutoDownload(podcastUuids: Collection<String>, isEnabled: Boolean) {
        val status = if (isEnabled) Podcast.AUTO_DOWNLOAD_NEW_EPISODES else Podcast.AUTO_DOWNLOAD_OFF
        podcastDao.updateAutoDownloadStatus(podcastUuids, status)
    }

    override suspend fun updateAllShowNotifications(showNotifications: Boolean) {
        if (showNotifications) {
            settings.notifyRefreshPodcast.set(true, updateModifiedAt = true)
        }
        podcastDao.updateAllShowNotifications(showNotifications)
    }

    override fun updateAutoDownloadStatusBlocking(podcast: Podcast, autoDownloadStatus: Int) {
        podcastDao.updateAutoDownloadStatusBlocking(autoDownloadStatus, podcast.uuid)
    }

    override suspend fun updateAutoAddToUpNext(podcast: Podcast, autoAddToUpNext: Podcast.AutoAddUpNext) {
        podcastDao.updateAutoAddToUpNext(autoAddToUpNext, podcast.uuid)
    }

    override suspend fun updateAutoAddToUpNexts(podcastUuids: List<String>, autoAddToUpNext: Podcast.AutoAddUpNext) {
        podcastDao.updateAutoAddToUpNexts(autoAddToUpNext, podcastUuids)
    }

    override suspend fun updateAutoAddToUpNextsIf(
        podcastUuids: List<String>,
        newValue: Podcast.AutoAddUpNext,
        onlyIfValue: Podcast.AutoAddUpNext,
    ) {
        podcastDao.updateAutoAddToUpNextsIf(podcastUuids, newValue.databaseInt, onlyIfValue.databaseInt)
    }

    override fun updateOverrideGlobalEffectsBlocking(podcast: Podcast, override: Boolean) {
        podcast.overrideGlobalEffects = override
        podcastDao.updateOverrideGlobalEffectsBlocking(override, podcast.uuid)
    }

    override suspend fun updateTrimModeBlocking(podcast: Podcast, trimMode: TrimMode) {
        podcast.trimMode = trimMode
        podcastDao.updateTrimSilenceModeBlocking(trimMode, podcast.uuid)
    }

    override fun updateVolumeBoostedBlocking(podcast: Podcast, override: Boolean) {
        podcast.isVolumeBoosted = override
        podcastDao.updateVolumeBoostedBlocking(override, podcast.uuid)
    }

    override fun updatePlaybackSpeedBlocking(podcast: Podcast, speed: Double) {
        podcast.playbackSpeed = speed
        podcastDao.updatePlaybackSpeedBlocking(speed, podcast.uuid)
    }

    override fun updateEffectsBlocking(podcast: Podcast, effects: PlaybackEffects) {
        podcastDao.updateEffectsBlocking(effects.playbackSpeed, effects.isVolumeBoosted, effects.trimMode, podcast.uuid)
        launch {
            updateTrimModeBlocking(podcast, effects.trimMode)
        }
    }

    override fun updateEpisodesSortTypeBlocking(podcast: Podcast, episodesSortType: EpisodesSortType) {
        podcastDao.updateEpisodesSortTypeBlocking(episodesSortType, podcast.uuid)
    }

    override suspend fun updateShowNotifications(podcastUuid: String, show: Boolean) {
        if (show) {
            settings.notifyRefreshPodcast.set(true, updateModifiedAt = true)
        }
        podcastDao.updateShowNotifications(podcastUuid, show)
    }

    override suspend fun updateRefreshAvailable(podcastUuid: String, refreshAvailable: Boolean) {
        podcastDao.updateRefreshAvailable(refreshAvailable, podcastUuid)
    }

    override suspend fun updateStartFromInSec(podcast: Podcast, autoStartFrom: Int) {
        podcastDao.updateStartFrom(autoStartFrom, podcast.uuid)
    }

    override suspend fun updateSkipLastInSec(podcast: Podcast, skipLast: Int) {
        podcastDao.updateSkipLast(skipLast, podcast.uuid)
    }

    override fun updateColorsBlocking(podcastUuid: String, background: Int, tintForLightBg: Int, tintForDarkBg: Int, fabForLightBg: Int, fabForDarkBg: Int, linkForLightBg: Int, linkForDarkBg: Int, colorLastDownloaded: Long) {
        try {
            podcastDao.updateColorsBlocking(background, tintForLightBg, tintForDarkBg, fabForLightBg, fabForDarkBg, linkForLightBg, linkForDarkBg, colorLastDownloaded, podcastUuid)
        } catch (e: Exception) {
            Timber.e(e, "Podcast colors update failed.")
        }
    }

    override fun updateLatestEpisodeBlocking(podcast: Podcast, latestEpisode: PodcastEpisode) {
        if (latestEpisode.uuid.isBlank()) {
            return
        }

        podcastDao.updateLatestEpisodeBlocking(latestEpisode.uuid, latestEpisode.publishedDate, podcastUuid = podcast.uuid)
    }

    override fun checkForEpisodesToDownloadBlocking(episodeUuidsAdded: List<String>, downloadManager: DownloadManager) {
        Timber.i("Auto download podcasts checkForEpisodesToDownload. Episodes %s", episodeUuidsAdded.size)

        val podcastUuidToAutoDownload = HashMap<String, Boolean>()
        val podcastUuidToDownloadCount = HashMap<String, Int>()

        for (podcast in findSubscribedBlocking()) {
            podcastUuidToAutoDownload[podcast.uuid] = podcast.isAutoDownloadNewEpisodes
        }

        val uuidToAdded = HashMap<String, Boolean>()
        for (episodeUuid in episodeUuidsAdded) {
            val episode = runBlocking {
                episodeManager.findByUuid(episodeUuid)
            } ?: continue
            val autoDownload = podcastUuidToAutoDownload[episode.podcastUuid]
            Timber.i(
                "Auto download " + episode.title +
                    " autoDownload: " + (autoDownload?.toString() ?: "null") +
                    " isQueued: " + episode.isQueued +
                    " isDownloaded: " + episode.isDownloaded +
                    " isDownloading: " + episode.isDownloading +
                    " isFinished: " + episode.isFinished,
            )

            if (autoDownload == null ||
                !autoDownload ||
                episode.isQueued ||
                episode.isDownloaded ||
                episode.isDownloading ||
                episode.isFinished ||
                episode.isArchived ||
                episode.isExemptFromAutoDownload
            ) {
                continue
            }

            val currentDownloadCount = podcastUuidToDownloadCount.getOrDefault(episode.podcastUuid, 0)
            if (currentDownloadCount >= settings.autoDownloadLimit.value.episodeCount) {
                continue // Skip to the next episode since it already downloaded the limit of episodes for this podcast
            }

            DownloadHelper.addAutoDownloadedEpisodeToQueue(episode, "podcast auto download " + episode.podcastUuid, downloadManager, episodeManager, source = SourceView.UNKNOWN)
            uuidToAdded[episodeUuid] = java.lang.Boolean.TRUE

            // Update the track of how many episodes were downloaded for this podcast
            podcastUuidToDownloadCount[episode.podcastUuid] = currentDownloadCount + 1
        }
    }

    override fun countEpisodesInPodcastWithStatusBlocking(podcastUuid: String, episodeStatus: EpisodeStatusEnum): Int {
        return podcastDao.countEpisodesInPodcastWithStatusBlocking(podcastUuid, episodeStatus)
    }

    override suspend fun updateShowArchived(podcast: Podcast, showArchived: Boolean) {
        podcastDao.updateShowArchived(podcast.uuid, showArchived)
    }

    override suspend fun updateAllShowArchived(showArchived: Boolean) {
        podcastDao.updateAllShowArchived(showArchived)
    }

    override suspend fun updateFolderUuid(folderUuid: String?, podcastUuids: List<String>) {
        if (podcastUuids.isEmpty()) {
            return
        }
        podcastDao.updateFolderUuid(folderUuid, podcastUuids)
    }

    override suspend fun updateIsHeaderExpanded(podcastUuid: String, isExpanded: Boolean) {
        podcastDao.updateIsHeaderExpanded(podcastUuid, isExpanded)
    }

    override suspend fun updatePodcastPositions(podcasts: List<Podcast>) {
        podcastDao.updateSortPositions(podcasts)
    }

    override fun buildUserEpisodePodcast(episode: UserEpisode): Podcast {
        return Podcast.userPodcast.copy(thumbnailUrl = episode.getUrlForArtwork())
    }

    override fun autoAddToUpNextPodcastsRxFlowable(): Flowable<List<Podcast>> {
        return podcastDao.findAutoAddToUpNextPodcastsRxFlowable()
    }

    override suspend fun findAutoAddToUpNextPodcasts(): List<Podcast> {
        return podcastDao.findAutoAddToUpNextPodcasts()
    }

    override suspend fun refreshPodcastFeed(podcast: Podcast): Boolean {
        var response = refreshServiceManager.updatePodcast(
            podcastUuid = podcast.uuid,
            lastEpisodeUuid = podcast.latestEpisodeUuid,
        )
        LogBuffer.i(TAG, "Refresh podcast feed: $response")

        while (response is Retry) {
            delay(response.retryAfter.seconds)
            response = refreshServiceManager.pollUpdatePodcast(response.location)
            LogBuffer.i(TAG, "Refresh podcast feed poll: $response")
        }

        if (response is EpisodeFound) {
            refreshPodcasts("Refresh podcast feed")
            return true
        } else {
            return false
        }
    }

    override suspend fun findRandomPodcasts(limit: Int): List<Podcast> {
        return podcastDao.findRandomPodcasts(limit)
    }

    override suspend fun updateArchiveSettings(uuid: String, enable: Boolean, afterPlaying: AutoArchiveAfterPlaying, inactive: AutoArchiveInactive) {
        podcastDao.updateArchiveSettings(uuid, enable, afterPlaying, inactive)
    }

    override suspend fun updateArchiveAfterPlaying(uuid: String, value: AutoArchiveAfterPlaying) {
        podcastDao.updateArchiveAfterPlaying(uuid, value)
    }

    override suspend fun updateArchiveAfterInactive(uuid: String, value: AutoArchiveInactive) {
        podcastDao.updateArchiveAfterInactive(uuid, value)
    }

    override suspend fun updateArchiveEpisodeLimit(uuid: String, value: AutoArchiveLimit) {
        podcastDao.updateArchiveEpisodeLimit(uuid, value)
    }

    override suspend fun countPlayedEpisodes(podcastUuid: String): Int {
        return episodeDao.countPlayedEpisodes(podcastUuid)
    }

    override suspend fun countEpisodesByPodcast(podcastUuid: String): Int {
        return episodeDao.countEpisodesByPodcast(podcastUuid)
    }

    private suspend fun isPodcastInManualPlaylist(podcast: Podcast): Boolean {
        val podcastsInPlaylists = if (FeatureFlag.isEnabled(Feature.PLAYLISTS_REBRANDING, immutable = true)) {
            playlistDao.getPodcastsAddedToManualPlaylists()
        } else {
            emptyList()
        }
        return podcast.uuid in podcastsInPlaylists
    }
}

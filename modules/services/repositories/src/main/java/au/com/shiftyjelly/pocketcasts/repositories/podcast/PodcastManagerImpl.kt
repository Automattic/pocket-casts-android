package au.com.shiftyjelly.pocketcasts.repositories.podcast

import android.content.Context
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.db.helper.UserEpisodePodcastSubstitute
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.entity.Folder
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffects
import au.com.shiftyjelly.pocketcasts.models.to.PodcastGrouping
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.models.type.EpisodesSortType
import au.com.shiftyjelly.pocketcasts.models.type.PodcastsSortType
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadHelper
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.extensions.getUrlForArtwork
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.refresh.RefreshPodcastsTask
import au.com.shiftyjelly.pocketcasts.servers.extensions.wasCached
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServerManager
import au.com.shiftyjelly.pocketcasts.servers.refresh.RefreshServerManager
import au.com.shiftyjelly.pocketcasts.utils.DateUtil
import au.com.shiftyjelly.pocketcasts.utils.Optional
import au.com.shiftyjelly.pocketcasts.utils.StringUtil
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.jakewharton.rxrelay2.PublishRelay
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.BackpressureStrategy
import io.reactivex.Completable
import io.reactivex.Flowable
import io.reactivex.Maybe
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import timber.log.Timber
import java.util.Calendar
import java.util.Date
import javax.inject.Inject
import kotlin.coroutines.CoroutineContext

class PodcastManagerImpl @Inject constructor(
    private val episodeManager: EpisodeManager,
    private val playlistManager: PlaylistManager,
    private val settings: Settings,
    @ApplicationContext private val context: Context,
    private val subscribeManager: SubscribeManager,
    private val cacheServerManager: PodcastCacheServerManager,
    private val refreshServerManager: RefreshServerManager,
    appDatabase: AppDatabase
) : PodcastManager, CoroutineScope {

    companion object {
        private const val FIVE_MINUTES_IN_MILLIS = (5 * 60 * 1000).toLong()
    }

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.Default
    private val unsubscribeRelay = PublishRelay.create<String>()
    private val podcastDao = appDatabase.podcastDao()
    private val episodeDao = appDatabase.episodeDao()

    override fun unsubscribe(podcastUuid: String, playbackManager: PlaybackManager) {
        try {
            podcastDao.findByUuid(podcastUuid)?.let { podcast ->
                val episodes = episodeManager.findEpisodesByPodcastOrdered(podcast)
                episodeManager.deleteEpisodes(episodes, playbackManager)

                if (settings.isLoggedIn()) {
                    podcast.isSubscribed = false
                    podcast.syncStatus = Podcast.SYNC_STATUS_NOT_SYNCED
                    podcast.isShowNotifications = false
                    podcast.autoDownloadStatus = Podcast.AUTO_DOWNLOAD_OFF
                    podcast.autoAddToUpNext = 0
                    podcast.autoArchiveAfterPlaying = 0
                    podcast.autoArchiveInactive = 0
                    podcast.autoArchiveEpisodeLimit = null
                    podcast.overrideGlobalArchive = false
                    podcast.folderUuid = null
                    podcastDao.update(podcast)
                } else {
                    // if they aren't signed in, just blow it all away
                    podcastDao.delete(podcast)
                    episodeDao.deleteAll(episodes)
                }
                playlistManager.removePodcastFromPlaylists(podcastUuid)

                unsubscribeRelay.accept(podcastUuid)
            }
        } catch (t: Throwable) {
            LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, t, "Could not unsubscribe from $podcastUuid")
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
    override fun subscribeToPodcast(podcastUuid: String, sync: Boolean) {
        subscribeManager.subscribeOnQueue(podcastUuid, sync)
    }

    /**
     * Download and add podcast to the database. Or if it exists already just mark is as subscribed.
     * Do this now rather than adding it to a queue.
     */
    override fun subscribeToPodcastRx(podcastUuid: String, sync: Boolean): Single<Podcast> {
        return subscribeManager.addPodcast(podcastUuid, sync, subscribed = true)
    }

    /**
     * If the podcast isn't already in the database add it as unsubscribed.
     */
    override fun findOrDownloadPodcastRx(podcastUuid: String): Single<Podcast> {
        return findPodcastByUuidRx(podcastUuid)
            .switchIfEmpty(subscribeManager.addPodcast(podcastUuid, sync = false, subscribed = false).toMaybe())
            .toSingle()
    }

    override fun isSubscribingToPodcast(podcastUuid: String): Boolean {
        return subscribeManager.isSubscribingToPodcast(podcastUuid)
    }

    override fun isSubscribingToPodcasts(): Boolean {
        return subscribeManager.getSubscribingPodcastUuids().isNotEmpty()
    }

    override fun getSubscribedPodcastUuids(): Single<List<String>> {
        // get the podcasts from the database
        val databasePodcasts = podcastDao.findSubscribedRx()
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

    override fun observePodcastSubscriptions(): Flowable<List<String>> {
        return subscribeManager.subscriptionChangedRelay
            .mergeWith(unsubscribeRelay)
            .flatMap { getSubscribedPodcastUuids().toObservable() } // Every time the subscriptions change, reload the subscribed list and pass it on
            .subscribeOn(Schedulers.io())
            .toFlowable(BackpressureStrategy.LATEST)
    }

    override fun findPodcastsToSync(): List<Podcast> {
        return podcastDao.findNotSynced()
    }

    override fun findPodcastsAutodownload(): List<Podcast> {
        return podcastDao.findPodcastsAutodownload()
    }

    override fun exists(podcastUuid: String): Boolean {
        return podcastDao.exists(podcastUuid)
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
        RefreshPodcastsTask.runNow(context)
    }

    @Suppress("NAME_SHADOWING")
    override fun refreshPodcastInBackground(existingPodcast: Podcast, playbackManager: PlaybackManager) {
        launch {
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Refreshing podcast ${existingPodcast.uuid}")
            val updatedPodcast = cacheServerManager.getPodcastResponse(existingPodcast.uuid, episodeLimit = Settings.LIMIT_MAX_PODCAST_EPISODES)
                .map {
                    val responsePodcast = it.body()?.toPodcast()
                    if (it.wasCached()) {
                        Optional.empty<Podcast>()
                    } else {
                        Optional.of(responsePodcast)
                    }
                }
                .observeOn(AndroidSchedulers.mainThread())
                .subscribeOn(Schedulers.io())
                .onErrorReturnItem(Optional.empty())
                .blockingGet()

            updatedPodcast.get()?.let { updatedPodcast ->
                val originalPodcast = existingPodcast.copy()
                existingPodcast.title = updatedPodcast.title
                existingPodcast.author = updatedPodcast.author
                existingPodcast.podcastCategory = updatedPodcast.podcastCategory
                existingPodcast.podcastDescription = updatedPodcast.podcastDescription
                existingPodcast.estimatedNextEpisode = updatedPodcast.estimatedNextEpisode
                existingPodcast.episodeFrequency = updatedPodcast.episodeFrequency
                existingPodcast.refreshAvailable = updatedPodcast.refreshAvailable
                val existingEpisodes = episodeManager.findEpisodesByPodcastOrderedByPublishDate(existingPodcast)
                val mostRecentEpisode = existingEpisodes.firstOrNull()
                val insertEpisodes = mutableListOf<Episode>()
                updatedPodcast.episodes.map { newEpisode ->
                    val existingEpisode = existingEpisodes.find { it.uuid == newEpisode.uuid }
                    if (existingEpisode != null) {
                        val originalEpisode = existingEpisode.copy()
                        existingEpisode.title = newEpisode.title
                        existingEpisode.downloadUrl = newEpisode.downloadUrl
                        // as new episodes are added a task is run to get the content type and file size from the server file as it is more reliable
                        if (existingEpisode.fileType.isNullOrBlank()) {
                            existingEpisode.fileType = newEpisode.fileType
                        }
                        if (existingEpisode.sizeInBytes <= 0) {
                            existingEpisode.sizeInBytes = newEpisode.sizeInBytes
                        }
                        if (existingEpisode.duration <= 0) {
                            existingEpisode.duration = newEpisode.duration
                        }
                        existingEpisode.publishedDate = newEpisode.publishedDate
                        existingEpisode.season = newEpisode.season
                        existingEpisode.number = newEpisode.number
                        existingEpisode.type = newEpisode.type
                        // only update the db if the fields have changed
                        if (originalEpisode != existingEpisode) {
                            episodeManager.update(existingEpisode)
                        }
                    } else {
                        // don't add anything newer than the latest episode so it runs through the refresh logic (auto download, auto add to Up Next etc
                        if (!existingPodcast.isSubscribed || (mostRecentEpisode != null && newEpisode.publishedDate.before(mostRecentEpisode.publishedDate))) {
                            newEpisode.podcastUuid = existingPodcast.uuid
                            newEpisode.episodeStatus = EpisodeStatusEnum.NOT_DOWNLOADED
                            newEpisode.playingStatus = EpisodePlayingStatus.NOT_PLAYED

                            // for podcast you're subscribed to, if we find episodes older than a week, we add them in as archived so they don't flood your filters, etc
                            val newEpisodeIs7DaysOld = if (mostRecentEpisode != null) {
                                DateUtil.daysBetweenTwoDates(newEpisode.publishedDate, mostRecentEpisode.publishedDate) >= 7
                            } else {
                                true
                            }
                            newEpisode.isArchived = existingPodcast.isSubscribed && newEpisodeIs7DaysOld

                            newEpisode.archivedModified = Date().time
                            newEpisode.lastArchiveInteraction = Date().time
                            // give it an old added date so it doesn't trigger a new episode notification
                            newEpisode.addedDate = existingPodcast.addedDate ?: Date()
                            existingPodcast.addEpisode(newEpisode)
                            insertEpisodes.add(newEpisode)
                        }
                    }
                }
                if (insertEpisodes.isNotEmpty()) {
                    episodeManager.add(insertEpisodes, podcastUuid = existingPodcast.uuid, downloadMetaData = false)
                }
                val episodeUuidsToDelete = existingEpisodes.map { it.uuid }.subtract(updatedPodcast.episodes.map { it.uuid })
                val calendar = Calendar.getInstance()
                calendar.add(Calendar.DAY_OF_MONTH, -14)
                val twoWeeksAgo = calendar.time
                val episodesToDelete = episodeUuidsToDelete.mapNotNull { uuid -> existingEpisodes.find { it.uuid == uuid } }.filter { it.addedDate.before(twoWeeksAgo) && episodeManager.episodeCanBeCleanedUp(it, playbackManager) }
                if (episodesToDelete.isNotEmpty()) {
                    episodeManager.deleteEpisodesWithoutSync(episodesToDelete, playbackManager)
                }

                if (originalPodcast != existingPodcast) {
                    LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Refresh required update for podcast ${existingPodcast.uuid}")
                    updatePodcast(existingPodcast)
                }
            }
        }
    }

    override fun reloadFoldersFromServer() {
        settings.setHomeGridNeedsRefresh(true)
        refreshPodcasts("reload folders")
    }

    override fun checkForUnusedPodcasts(playbackManager: PlaybackManager) {
        podcastDao.findUnsubscribed().forEach { podcast ->
            deletePodcastIfUnused(podcast, playbackManager)
        }
    }

    override fun deletePodcastIfUnused(podcast: Podcast, playbackManager: PlaybackManager): Boolean {
        // we don't delete podcasts that haven't been synced or you're still subscribed to
        if ((settings.isLoggedIn() && podcast.isNotSynced) || podcast.isSubscribed) {
            return false
        }

        // we don't delete podcasts added to the phone in the last week. This is to prevent stuff you just leave open in discover from being removed
        val addedDate = podcast.addedDate
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_YEAR, -7)
        val oneWeekAgo = calendar.time
        if (addedDate != null && addedDate > oneWeekAgo) {
            return false
        }

        // podcasts can be deleted if all of the episodes are haven't been interacted with
        val episodes = episodeManager.findEpisodesByPodcastOrdered(podcast)
        var podcastHasChangedEpisodes = false
        val deleteEpisodes = mutableListOf<Episode>()
        for (episode in episodes) {
            if (episodeManager.userHasInteractedWithEpisode(episode, playbackManager)) {
                podcastHasChangedEpisodes = true
                continue
            }
            // bulk delete or it takes 10 seconds on a large podcast
            deleteEpisodes.add(episode)
        }
        if (deleteEpisodes.isNotEmpty()) {
            episodeManager.deleteEpisodesWithoutSync(deleteEpisodes, playbackManager)
        }
        if (!podcastHasChangedEpisodes) {
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Removing unused podcast ${podcast.title}")
            // this podcast isn't needed, delete it
            podcastDao.delete(podcast)

            return true
        }

        return false
    }

    override fun findPodcastByUuid(uuid: String): Podcast? {
        return podcastDao.findByUuid(uuid)
    }

    override suspend fun findPodcastByUuidSuspend(uuid: String): Podcast? {
        return podcastDao.findPodcastByUuidSuspend(uuid)
    }

    override fun findPodcastByUuidRx(uuid: String): Maybe<Podcast> {
        return Maybe.fromCallable { findPodcastByUuid(uuid) }
    }

    override fun observePodcastByUuid(uuid: String): Flowable<Podcast> {
        return podcastDao.observeByUuid(uuid)
    }

    override fun findByUuids(uuids: Collection<String>): List<Podcast> {
        return podcastDao.findByUuids(uuids.toTypedArray())
    }

    override suspend fun findPodcastsInFolder(folderUuid: String): List<Podcast> {
        return podcastDao.findPodcastsInFolder(folderUuid)
    }

    override fun findPodcastsInFolderSingle(folderUuid: String): Single<List<Podcast>> {
        return podcastDao.findPodcastsInFolderSingle(folderUuid)
    }

    override suspend fun findPodcastsNotInFolder(): List<Podcast> {
        return podcastDao.findPodcastsNotInFolder()
    }

    override fun findSubscribed(): List<Podcast> {
        return podcastDao.findSubscribed()
    }

    override suspend fun findSubscribedNoOrder(): List<Podcast> {
        return podcastDao.findSubscribedNoOrder()
    }

    override suspend fun findSubscribedSorted(): List<Podcast> {
        val sortType = settings.getPodcastsSortType()
        // use a query to get the podcasts ordered by episode release date
        if (sortType == PodcastsSortType.EPISODE_DATE_NEWEST_TO_OLDEST) {
            return findPodcastsOrderByLatestEpisode(orderAsc = false)
        }
        return podcastDao.findSubscribedNoOrder().sortedWith(sortType.podcastComparator)
    }

    override fun findSubscribedRx(): Single<List<Podcast>> {
        return Single.fromCallable { findSubscribed() }
    }

    override fun findSubscribedFlow(): Flow<List<Podcast>> {
        return podcastDao.findSubscribedFlow()
    }

    override fun observePodcastsOrderByLatestEpisode(): Flowable<List<Podcast>> {
        return podcastDao.observeSubscribedOrderByLatestEpisode(orderAsc = false)
    }

    override fun observePodcastsInFolderOrderByUserChoice(folder: Folder): Flowable<List<Podcast>> {
        val sort = folder.podcastsSortType
        return when (sort) {
            PodcastsSortType.DATE_ADDED_OLDEST_TO_NEWEST -> podcastDao.observeFolderOrderByAddedDate(folder.uuid, sort.isAsc())
            PodcastsSortType.EPISODE_DATE_NEWEST_TO_OLDEST -> podcastDao.observeFolderOrderByLatestEpisode(folder.uuid, sort.isAsc())
            PodcastsSortType.DRAG_DROP -> podcastDao.observeFolderOrderByUserSort(folder.uuid)
            else -> podcastDao.observeFolderOrderByName(folder.uuid, sort.isAsc())
        }
    }

    override fun observeSubscribed(): Flowable<List<Podcast>> {
        return podcastDao.observeSubscribed()
    }

    override suspend fun findPodcastsOrderByLatestEpisode(orderAsc: Boolean): List<Podcast> {
        return podcastDao.findSubscribedOrderByLatestEpisode(orderAsc)
    }

    override suspend fun findFolderPodcastsOrderByLatestEpisode(folderUuid: String): List<Podcast> {
        return podcastDao.findFolderPodcastsOrderByLatestEpisode(folderUuid)
    }

    override suspend fun findPodcastsOrderByTitle(): List<Podcast> {
        return podcastDao.findSubscribedNoOrder().sortedWith(PodcastsSortType.NAME_A_TO_Z.podcastComparator)
    }

    override fun searchPodcastByTitle(title: String): Podcast? {
        return podcastDao.searchByTitle("%$title%")
    }

    override fun markPodcastAsSynced(podcast: Podcast) {
        updateSyncStatus(podcast.uuid, Podcast.SYNC_STATUS_SYNCED)
    }

    override fun markPodcastAsNotSynced(podcast: Podcast) {
        updateSyncStatus(podcast.uuid, Podcast.SYNC_STATUS_NOT_SYNCED)
    }

    override fun markPodcastUuidAsNotSynced(podcastUuid: String) {
        updateSyncStatus(podcastUuid, Podcast.SYNC_STATUS_NOT_SYNCED)
    }

    private fun updateSyncStatus(podcastUuid: String, syncStatus: Int) {
        podcastDao.updateSyncStatus(syncStatus, podcastUuid)
    }

    override fun updateGrouping(podcast: Podcast, grouping: PodcastGrouping) {
        podcastDao.updateGrouping(PodcastGrouping.All.indexOf(grouping), podcast.uuid)
    }

    override fun updateGroupingForAll(grouping: PodcastGrouping) {
        podcastDao.updatePodcastGroupingForAll(PodcastGrouping.All.indexOf(grouping))
    }

    override suspend fun markAllPodcastsUnsynced() {
        podcastDao.updateAllSubscribedSyncStatus(Podcast.SYNC_STATUS_NOT_SYNCED)
    }

    override fun markAllPodcastsSynced() {
        podcastDao.updateAllSyncStatus(Podcast.SYNC_STATUS_SYNCED)
    }

    override fun markAsSubscribed(podcast: Podcast, subscribed: Boolean) {
        val podcastUuid = podcast.uuid
        if (StringUtil.isBlank(podcastUuid)) {
            return
        }
        podcast.isSubscribed = subscribed
        updateSubscribed(podcast, subscribed)
        updateSyncStatus(podcast.uuid, Podcast.SYNC_STATUS_NOT_SYNCED)
    }

    override fun clearAllDownloadErrors() {
        episodeDao.clearAllDownloadErrors(EpisodeStatusEnum.NOT_DOWNLOADED, EpisodeStatusEnum.DOWNLOAD_FAILED)
    }

    /**
     * Count all podcasts in the database. This includes deleted podcasts and the custom folder.
     * To exclude deleted podcasts and the custom folder use countSubscribed
     */
    override fun countPodcasts(): Int {
        return podcastDao.count()
    }

    override fun countSubscribed(): Int {
        return podcastDao.countSubscribed()
    }

    override fun countSubscribedRx(): Single<Int> {
        return podcastDao.countSubscribedRx()
    }

    override fun observeCountSubscribed(): Flowable<Int> {
        return podcastDao.observeCountSubscribed()
    }

    override fun countDownloadStatus(downloadStatus: Int): Int {
        return podcastDao.countDownloadStatus(downloadStatus)
    }

    override fun countDownloadStatusRx(downloadStatus: Int): Single<Int> {
        return Single.fromCallable { countDownloadStatus(downloadStatus) }
    }

    override fun countNotificationsOn(): Int {
        return podcastDao.countNotificationsOn()
    }

    override fun countNotificationsOnRx(): Single<Int> {
        return Single.fromCallable { podcastDao.countNotificationsOn() }
    }

    // WARNING: only call this when NEW episodes are added, not old ones
    private fun updateLatestEpisodeUuid(podcastUuid: String) {
        // get the most recent episode details
        val episode = episodeDao.findLatest(podcastUuid) ?: return
        val latestEpisodeUuid = episode.uuid
        val latestEpisodeDate = episode.publishedDate
        podcastDao.updateLatestEpisode(episodeUuid = latestEpisodeUuid, publishedDate = latestEpisodeDate, podcastUuid = podcastUuid)
    }

    override fun addFolderPodcast(podcast: Podcast) {
        podcastDao.insert(podcast)
    }

    override fun getPodcastEpisodesListOrderBy(podcast: Podcast): String {
        return when (podcast.episodesSortType) {
            EpisodesSortType.EPISODES_SORT_BY_TITLE_ASC -> "UPPER(title) ASC"
            EpisodesSortType.EPISODES_SORT_BY_TITLE_DESC -> "UPPER(title) DESC"
            EpisodesSortType.EPISODES_SORT_BY_DATE_ASC -> "published_date ASC"
            EpisodesSortType.EPISODES_SORT_BY_DATE_DESC -> "published_date DESC"
            EpisodesSortType.EPISODES_SORT_BY_LENGTH_ASC -> "duration ASC"
            EpisodesSortType.EPISODES_SORT_BY_LENGTH_DESC -> "duration DESC"
        }
    }

    override fun updatePodcast(podcast: Podcast) {
        podcastDao.update(podcast)
    }

    override fun updateAllAutoDownloadStatus(autoDownloadStatus: Int) {
        podcastDao.updateAllAutoDownloadStatus(autoDownloadStatus)
    }

    override fun updateAllShowNotifications(showNotifications: Boolean) {
        podcastDao.updateAllShowNotifications(showNotifications)
    }

    override fun updateAllShowNotificationsRx(showNotifications: Boolean): Completable {
        return Completable.fromAction { updateAllShowNotifications(showNotifications) }
    }

    override fun updateAutoDownloadStatus(podcast: Podcast, autoDownloadStatus: Int) {
        podcast.autoDownloadStatus = autoDownloadStatus
        podcastDao.updateAutoDownloadStatus(autoDownloadStatus, podcast.uuid)
    }

    override suspend fun updateAutoAddToUpNext(podcast: Podcast, autoAddToUpNext: Int) {
        podcastDao.updateAutoAddToUpNext(autoAddToUpNext, podcast.uuid)
    }

    override suspend fun updateAutoAddToUpNexts(podcastUuids: List<String>, autoAddToUpNext: Int) {
        podcastDao.updateAutoAddToUpNexts(autoAddToUpNext, podcastUuids)
    }

    override suspend fun updateAutoAddToUpNextsIf(podcastUuids: List<String>, newValue: Int, onlyIfValue: Int) {
        podcastDao.updateAutoAddToUpNextsIf(podcastUuids, newValue, onlyIfValue)
    }

    override fun updateExcludeFromAutoArchive(podcast: Podcast, excludeFromAutoArchive: Boolean) {
        podcast.excludeFromAutoArchive = excludeFromAutoArchive
        podcastDao.updateExcludeFromAutoArchive(excludeFromAutoArchive, podcast.uuid)
    }

    override fun updateOverrideGlobalEffects(podcast: Podcast, override: Boolean) {
        podcast.overrideGlobalEffects = override
        podcastDao.updateOverrideGlobalEffects(override, podcast.uuid)
    }

    override fun updateTrimMode(podcast: Podcast, trimMode: TrimMode) {
        val isOn = trimMode != TrimMode.OFF
        podcast.trimMode = trimMode
        podcast.isSilenceRemoved = isOn
        podcastDao.updateTrimSilenceMode(trimMode, isOn, podcast.uuid)
    }

    override fun updateVolumeBoosted(podcast: Podcast, override: Boolean) {
        podcast.isVolumeBoosted = override
        podcastDao.updateVolumeBoosted(override, podcast.uuid)
    }

    override fun updatePlaybackSpeed(podcast: Podcast, speed: Double) {
        podcast.playbackSpeed = speed
        podcastDao.updatePlaybackSpeed(speed, podcast.uuid)
    }

    override fun updateEffects(podcast: Podcast, effects: PlaybackEffects) {
        podcast.playbackEffects = effects
        podcastDao.updateEffects(effects.playbackSpeed, effects.isVolumeBoosted, effects.trimMode != TrimMode.OFF, podcast.uuid)
        updateTrimMode(podcast, effects.trimMode)
    }

    override fun updateEpisodesSortType(podcast: Podcast, episodesSortType: EpisodesSortType) {
        podcastDao.updateEpisodesSortType(episodesSortType, podcast.uuid)
    }

    override fun updateShowNotifications(podcast: Podcast, show: Boolean) {
        podcastDao.updateShowNotifications(show, podcast.uuid)
    }

    override fun updateSubscribed(podcast: Podcast, subscribed: Boolean) {
        podcastDao.updateSubscribed(subscribed, podcast.uuid)
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

    override fun updateColorLastDownloaded(podcast: Podcast, lastDownloaded: Long) {
        podcastDao.updateColorLastDownloaded(lastDownloaded, podcast.uuid)
    }

    override fun updateOverrideGobalSettings(podcast: Podcast, override: Boolean) {
        podcastDao.updateOverrideGobalSettings(override, podcast.uuid)
    }

    override fun updateEpisodesToKeep(podcast: Podcast, episodeToKeep: Int) {
        podcastDao.updateEpisodesToKeep(episodeToKeep, podcast.uuid)
    }

    override fun updateColors(podcastUuid: String, background: Int, tintForLightBg: Int, tintForDarkBg: Int, fabForLightBg: Int, fabForDarkBg: Int, linkForLightBg: Int, linkForDarkBg: Int, colorLastDownloaded: Long) {
        try {
            podcastDao.updateColors(background, tintForLightBg, tintForDarkBg, fabForLightBg, fabForDarkBg, linkForLightBg, linkForDarkBg, colorLastDownloaded, podcastUuid)
        } catch (e: Exception) {
            Timber.e(e, "Podcast colors update failed.")
        }
    }

    override fun updateLatestEpisode(podcast: Podcast, latestEpisode: Episode) {
        if (StringUtil.isBlank(latestEpisode.uuid)) {
            return
        }

        podcastDao.updateLatestEpisode(latestEpisode.uuid, latestEpisode.publishedDate, podcastUuid = podcast.uuid)
    }

    override fun checkForEpisodesToDownload(episodeUuidsAdded: List<String>, downloadManager: DownloadManager) {
        Timber.i("Auto download podcasts checkForEpisodesToDownload. Episodes %s", episodeUuidsAdded.size)
        val podcastUuidToAutoDownload = HashMap<String, Boolean>()
        for (podcast in findSubscribed()) {
            podcastUuidToAutoDownload[podcast.uuid] = podcast.isAutoDownloadNewEpisodes
        }

        val uuidToAdded = HashMap<String, Boolean>()
        for (episodeUuid in episodeUuidsAdded) {
            val episode = episodeManager.findByUuid(episodeUuid) ?: continue
            val autoDownload = podcastUuidToAutoDownload[episode.podcastUuid]
            Timber.i(
                "Auto download " + episode.title +
                    " autoDownload: " + (autoDownload?.toString() ?: "null") +
                    " isQueued: " + episode.isQueued +
                    " isDownloaded: " + episode.isDownloaded +
                    " isDownloading: " + episode.isDownloading +
                    " isFinished: " + episode.isFinished
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

            DownloadHelper.addAutoDownloadedEpisodeToQueue(episode, "podcast auto download " + episode.podcastUuid, downloadManager, episodeManager)
            uuidToAdded[episodeUuid] = java.lang.Boolean.TRUE
        }
    }

    override fun countEpisodesInPodcastWithStatus(podcastUuid: String, episodeStatus: EpisodeStatusEnum): Int {
        return podcastDao.countEpisodesInPodcastWithStatus(podcastUuid, episodeStatus)
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

    override suspend fun updateSyncData(podcast: Podcast, startFromSecs: Int, skipLastSecs: Int, folderUuid: String?, sortPosition: Int, addedDate: Date) {
        podcastDao.updateSyncData(
            uuid = podcast.uuid,
            startFromSecs = startFromSecs,
            skipLastSecs = skipLastSecs,
            folderUuid = folderUuid,
            sortPosition = sortPosition,
            addedDate = addedDate
        )
    }

    /**
     * Will return true if all the podcasts are have auto download turned on.
     */
    private fun isAutoDownloadingAllPodcasts(): Boolean {
        return if (countPodcasts() == 0) false else podcastDao.countDownloadStatus(Podcast.AUTO_DOWNLOAD_NEW_EPISODES) == podcastDao.countSubscribed()
    }

    private fun isNotificationsForAllPodcasts(): Boolean {
        return if (countPodcasts() == 0) false else podcastDao.countNotificationsOn() == podcastDao.countSubscribed()
    }

    override suspend fun updatePodcastPositions(podcasts: List<Podcast>) {
        podcastDao.updateSortPositions(podcasts)
    }

    override fun buildUserEpisodePodcast(episode: UserEpisode): Podcast {
        return Podcast(
            uuid = UserEpisodePodcastSubstitute.uuid,
            title = UserEpisodePodcastSubstitute.title,
            thumbnailUrl = episode.getUrlForArtwork()
        )
    }

    override fun observeAutoAddToUpNextPodcasts(): Flowable<List<Podcast>> {
        return podcastDao.observeAutoAddToUpNextPodcasts()
    }

    override suspend fun findAutoAddToUpNextPodcasts(): List<Podcast> {
        return podcastDao.findAutoAddToUpNextPodcasts()
    }

    override suspend fun refreshPodcastFeed(podcastUuid: String): Boolean {
        return refreshServerManager.refreshPodcastFeed(podcastUuid).isSuccessful
    }
}

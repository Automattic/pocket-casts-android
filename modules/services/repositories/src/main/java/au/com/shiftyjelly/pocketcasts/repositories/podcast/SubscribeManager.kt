package au.com.shiftyjelly.pocketcasts.repositories.podcast

import android.annotation.SuppressLint
import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.ChapterIndices
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast.Companion.AUTO_DOWNLOAD_NEW_EPISODES
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.AutoDownloadLimitSetting
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.models.type.EpisodesSortType
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadHelper
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.cdn.ArtworkColors
import au.com.shiftyjelly.pocketcasts.servers.cdn.StaticServiceManager
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServiceManager
import au.com.shiftyjelly.pocketcasts.servers.sync.PodcastEpisodesResponse
import au.com.shiftyjelly.pocketcasts.utils.Optional
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import coil.executeBlocking
import coil.imageLoader
import coil.request.CachePolicy
import com.jakewharton.rxrelay2.PublishRelay
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function4
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.rx2.rxCompletable
import timber.log.Timber

@Singleton
class SubscribeManager @Inject constructor(
    val appDatabase: AppDatabase,
    val podcastCacheServiceManager: PodcastCacheServiceManager,
    private val staticServiceManager: StaticServiceManager,
    private val syncManager: SyncManager,
    private val episodeManager: EpisodeManager,
    private val downloadManager: DownloadManager,
    @ApplicationContext val context: Context,
    val settings: Settings,
) {

    private val subscribeRelay: PublishRelay<PodcastSubscribe> by lazy { setupSubscribeRelay() }
    val subscriptionChangedRelay: PublishRelay<String> = PublishRelay.create()

    private val uuidsInQueue = HashSet<String>()
    private val podcastDao = appDatabase.podcastDao()
    private val episodeDao = appDatabase.episodeDao()
    private val imageRequestFactory = PocketCastsImageRequestFactory(context, isDarkTheme = true)

    data class PodcastSubscribe(val podcastUuid: String, val sync: Boolean, val shouldAutoDownload: Boolean)

    @SuppressLint("CheckResult")
    private fun setupSubscribeRelay(): PublishRelay<PodcastSubscribe> {
        val source = PublishRelay.create<PodcastSubscribe>()
        source
            .observeOn(Schedulers.io())
            .doOnNext { info -> Timber.i("Adding podcast to addPodcast queue ${info.podcastUuid}") }
            .flatMap({ info ->
                // shouldAutoDownload = true because the user manually subscribed to the podcast,
                // so we want to automatically download episodes at this moment.
                addPodcastRxSingle(info.podcastUuid, sync = info.sync, subscribed = true, shouldAutoDownload = info.shouldAutoDownload).toObservable()
            }, true, 5)
            .doOnError { throwable -> LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, throwable, "Could not subscribe to podcast") }
            .subscribeBy(
                onNext = { podcast ->
                    uuidsInQueue.remove(podcast.uuid)
                    Timber.i("Subscribed successfully to podcast ${podcast.uuid}")
                    subscriptionChangedRelay.accept(podcast.uuid)
                },
            )
        return source
    }

    /**
     * Subscribe to a podcast on a background queue.
     */
    fun subscribeOnQueue(podcastUuid: String, sync: Boolean = false, shouldAutoDownload: Boolean) {
        if (uuidsInQueue.contains(podcastUuid)) {
            return
        }
        uuidsInQueue.add(podcastUuid)
        subscribeRelay.accept(PodcastSubscribe(podcastUuid, sync, shouldAutoDownload))
    }

    /**
     * Subscribe to a podcast and wait.
     */
    fun addPodcastRxSingle(podcastUuid: String, sync: Boolean = false, subscribed: Boolean = false, shouldAutoDownload: Boolean): Single<Podcast> {
        return subscribeToExistingOrServerPodcastRxSingle(podcastUuid, sync, subscribed, shouldAutoDownload)
            .flatMap {
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Adding podcast $podcastUuid to database")
                cacheArtworkRxCompletable(it).toSingleDefault(it)
            }
            .doOnSuccess {
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Added podcast $podcastUuid to database")
                // update the notification time as any podcasts added after this date will be ignored
                settings.setNotificationLastSeenToNow()

                if (canDownloadEpisodesAfterFollowPodcast(subscribed, shouldAutoDownload)) {
                    podcastDao.findByUuidBlocking(podcastUuid)?.let { podcast ->
                        val episodes = episodeManager.findEpisodesByPodcastOrderedByPublishDateBlocking(podcast)
                        val numberOfEpisodes = settings.autoDownloadLimit.value.episodeCount

                        episodes.take(numberOfEpisodes).forEach { episode ->
                            if (episode.isQueued || episode.isDownloaded || episode.isDownloading || episode.isExemptFromAutoDownload) {
                                return@forEach
                            }

                            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Auto Downloading $numberOfEpisodes episodes after subscribing to $podcastUuid")

                            DownloadHelper.addAutoDownloadedEpisodeToQueue(
                                episode,
                                "Auto Download after subscribing to $podcastUuid",
                                downloadManager,
                                episodeManager,
                                source = SourceView.DOWNLOADS,
                            )
                        }
                    }
                }
            }
    }

    private fun cacheArtworkRxCompletable(podcast: Podcast): Completable {
        return Completable.fromAction {
            val request = imageRequestFactory.create(podcast)
                .newBuilder()
                .memoryCachePolicy(CachePolicy.DISABLED)
                .build()
            context.imageLoader.executeBlocking(request)
        }.onErrorComplete()
    }

    fun isSubscribingToPodcast(podcastUuid: String): Boolean {
        return uuidsInQueue.contains(podcastUuid)
    }

    fun getSubscribingPodcastUuids(): Set<String> {
        return uuidsInQueue
    }

    private fun subscribeToExistingOrServerPodcastRxSingle(podcastUuid: String, sync: Boolean, subscribed: Boolean, shouldAutoDownload: Boolean): Single<Podcast> {
        // check if the podcast exists already
        val subscribedObservable = podcastDao.isSubscribedToPodcastRxSingle(podcastUuid)
        return subscribedObservable.flatMap { isSubscribed ->
            // download the podcast json and add to the database if it doesn't exist
            if (isSubscribed) {
                subscribeToExistingPodcastRxSingle(podcastUuid, sync)
            } else {
                subscribeToServerPodcastRxSingle(podcastUuid, sync, subscribed, shouldAutoDownload)
            }
        }
    }

    private fun subscribeToExistingPodcastRxSingle(podcastUuid: String, sync: Boolean): Single<Podcast> {
        // set subscribed to true and update the sync status
        val updateObservable = podcastDao.updateSubscribedRxCompletable(subscribed = true, uuid = podcastUuid)
            .andThen(podcastDao.updateSyncStatusRxCompletable(syncStatus = if (sync) Podcast.SYNC_STATUS_NOT_SYNCED else Podcast.SYNC_STATUS_SYNCED, uuid = podcastUuid))
            .andThen(Completable.fromAction { podcastDao.updateGroupingBlocking(settings.podcastGroupingDefault.value, podcastUuid) })
            .andThen(rxCompletable { podcastDao.updateShowArchived(podcastUuid, settings.showArchivedDefault.value) })
        // return the final podcast
        val findObservable = podcastDao.findByUuidRxMaybe(podcastUuid)
        return updateObservable.andThen(findObservable.toSingle())
    }

    private fun subscribeToServerPodcastRxSingle(podcastUuid: String, sync: Boolean, subscribed: Boolean, shouldAutoDownload: Boolean): Single<Podcast> {
        // download the podcast
        val podcastObservable = downloadPodcastRxSingle(podcastUuid)
            .doOnSuccess { podcast ->
                // mark sync status
                podcast.syncStatus = if (sync) Podcast.SYNC_STATUS_NOT_SYNCED else Podcast.SYNC_STATUS_SYNCED
                podcast.isSubscribed = subscribed
                podcast.grouping = settings.podcastGroupingDefault.value
                podcast.showArchived = settings.showArchivedDefault.value
                podcastDao.findByUuidBlocking(podcastUuid)?.let { localPodcast ->
                    podcast.copyPlaybackEffects(
                        sourcePodcast = localPodcast,
                    )
                }
                if (canDownloadEpisodesAfterFollowPodcast(subscribed, shouldAutoDownload)) {
                    LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Update auto download status for $podcastUuid")
                    podcast.autoDownloadStatus = AUTO_DOWNLOAD_NEW_EPISODES
                }
            }
        // add the podcast
        val insertPodcastObservable = podcastObservable.flatMap { podcast ->
            podcastDao.insertRxSingle(podcast)
        }
        // insert episodes
        return insertPodcastObservable.flatMap { podcast -> subscribeInsertEpisodesRxCompletable(podcast).toSingle { podcast } }
    }

    private fun canDownloadEpisodesAfterFollowPodcast(
        subscribed: Boolean,
        shouldAutoDownload: Boolean,
    ): Boolean = subscribed &&
        settings.autoDownloadOnFollowPodcast.value &&
        shouldAutoDownload

    private fun downloadPodcastRxSingle(podcastUuid: String): Single<Podcast> {
        // download the podcast
        val serverPodcastObservable = podcastCacheServiceManager.getPodcast(podcastUuid)
            .subscribeOn(Schedulers.io())
            .doOnSuccess { Timber.i("Downloaded episodes success podcast $podcastUuid") }
        // download the colors
        val colorObservable = staticServiceManager.getColorsSingle(podcastUuid)
            .subscribeOn(Schedulers.io())
            .doOnSuccess { Timber.i("Downloaded colors success podcast $podcastUuid") }
            .onErrorReturn { Optional.empty() }
        // keep expanded or collapsed header state
        val isHeaderExpandedObservable = podcastDao.findByUuidRxMaybe(podcastUuid)
            .subscribeOn(Schedulers.io())
            .map { it.isHeaderExpanded }
            .toSingle(true)
        // find all podcasts from the database
        val allPodcastsObservable = podcastDao.findSubscribedRxSingle().subscribeOn(Schedulers.io())
        // group the server podcast and all the existing podcasts to calculate the new podcast properties
        val cleanPodcastObservable = Single.zip(
            serverPodcastObservable,
            colorObservable,
            isHeaderExpandedObservable,
            allPodcastsObservable,
            Function4<Podcast, Optional<ArtworkColors>, Boolean, List<Podcast>, Podcast> { podcast, colors, isHeaderExpanded, allPodcasts ->
                cleanPodcast(podcast, colors, isHeaderExpanded, allPodcasts)
            },
        )
        // add sync information
        if (syncManager.isLoggedIn()) {
            val syncPodcastObservable = syncManager.getPodcastEpisodesRxSingle(podcastUuid).subscribeOn(Schedulers.io())
            return Single.zip(cleanPodcastObservable, syncPodcastObservable, BiFunction<Podcast, PodcastEpisodesResponse, Podcast>(this::mergeSyncPodcast))
                .onErrorResumeNext(cleanPodcastObservable)
        } else {
            return cleanPodcastObservable
        }
    }

    private fun subscribeInsertEpisodesRxCompletable(podcast: Podcast): Completable {
        // insert the episodes
        return Completable.fromAction {
            podcast.episodes.chunked(250).forEach { episodes ->
                episodeDao.insertAllBlocking(episodes)
            }
        }
            // make sure the podcast has the latest episode uuid
            .andThen(updateLatestEpisodeUuidRxCompletable(podcast.uuid))
    }

    private fun cleanPodcast(
        podcast: Podcast,
        colors: Optional<ArtworkColors>,
        isHeaderExpanded: Boolean,
        allPodcasts: List<Podcast>,
    ): Podcast {
        // mark as subscribed
        podcast.isSubscribed = true
        // if all the podcasts have auto download selected then also auto download this podcast
        var allAutoDownloading = true
        // if all the podcasts have episode update notifications selected then also turn it on for this podcast too
        var allSendingNotifications = true
        // set the position of the episode to the size + 1
        var count = 0
        var foundEpisodes = false
        for (existingPodcast in allPodcasts) {
            count++
            if (!existingPodcast.isSubscribed) {
                continue
            }
            foundEpisodes = true
            if (!existingPodcast.isAutoDownloadNewEpisodes) {
                allAutoDownloading = false
            }
            if (!existingPodcast.isShowNotifications) {
                allSendingNotifications = false
            }
        }
        podcast.autoDownloadStatus = if (foundEpisodes && allAutoDownloading) Podcast.AUTO_DOWNLOAD_NEW_EPISODES else Podcast.AUTO_DOWNLOAD_OFF
        podcast.isShowNotifications = foundEpisodes && allSendingNotifications
        podcast.sortPosition = count
        podcast.episodesSortType = if (podcast.episodesSortType.ordinal == 0) EpisodesSortType.EPISODES_SORT_BY_DATE_DESC else podcast.episodesSortType
        podcast.episodes.firstOrNull()?.let { episode ->
            podcast.latestEpisodeUuid = episode.uuid
            podcast.latestEpisodeDate = episode.publishedDate
        }
        // give the podcasts and episodes the same added date so we can tell which are new episodes or added with podcast when calculating notifications
        podcast.addedDate = Date()
        // copy colors
        colors.ifPresent { it.copyToPodcast(podcast) }

        for (episode in podcast.episodes) {
            cleanEpisode(episode, podcast)
        }

        podcast.isHeaderExpanded = isHeaderExpanded

        return podcast
    }

    private fun cleanEpisode(episode: PodcastEpisode, podcast: Podcast): PodcastEpisode {
        episode.addedDate = podcast.addedDate ?: Date()
        episode.podcastUuid = podcast.uuid
        episode.playedUpTo = 0.0
        episode.playingStatus = EpisodePlayingStatus.NOT_PLAYED
        episode.episodeStatus = EpisodeStatusEnum.NOT_DOWNLOADED
        return episode
    }

    // WARNING: only call this when NEW episodes are added, not old ones
    private fun updateLatestEpisodeUuidRxCompletable(podcastUuid: String): Completable {
        return episodeDao.findLatestRxMaybe(podcastUuid)
            .flatMapCompletable { episode -> podcastDao.updateLatestEpisodeRxCompletable(episode.uuid, episode.publishedDate, podcastUuid) }
    }

    /**
     * Merge the user's podcast sync data into the podcast
     */
    private fun mergeSyncPodcast(podcast: Podcast, response: PodcastEpisodesResponse): Podcast {
        podcast.startFromSecs = response.autoStartFrom ?: 0
        val uuidToSyncEpisode = response.episodes.orEmpty().associateBy({ it.uuid }, { it })
        for (episode in podcast.episodes) {
            val syncEpisode = uuidToSyncEpisode[episode.uuid] ?: continue
            episode.isStarred = syncEpisode.starred ?: false
            episode.playedUpTo = syncEpisode.playedUpTo?.toDouble() ?: 0.toDouble()
            episode.isArchived = syncEpisode.isArchived ?: false
            episode.deselectedChapters = ChapterIndices.fromString(syncEpisode.deselectedChapters)
            episode.setPlayingStatusInt(syncEpisode.playingStatus ?: 1)
            val duration = syncEpisode.duration ?: 0
            if (duration > 0) {
                episode.duration = duration.toDouble()
            }
        }
        return podcast
    }
}

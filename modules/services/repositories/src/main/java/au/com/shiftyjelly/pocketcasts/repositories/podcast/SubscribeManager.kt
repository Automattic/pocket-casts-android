package au.com.shiftyjelly.pocketcasts.repositories.podcast

import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.FirebaseAnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PodcastGrouping
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.models.type.EpisodeStatusEnum
import au.com.shiftyjelly.pocketcasts.models.type.EpisodesSortType
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImageLoader
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.cdn.ArtworkColors
import au.com.shiftyjelly.pocketcasts.servers.cdn.StaticServerManager
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServerManager
import au.com.shiftyjelly.pocketcasts.servers.sync.PodcastEpisodesResponse
import au.com.shiftyjelly.pocketcasts.utils.Optional
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import com.jakewharton.rxrelay2.PublishRelay
import dagger.hilt.android.qualifiers.ApplicationContext
import io.reactivex.Completable
import io.reactivex.Single
import io.reactivex.functions.BiFunction
import io.reactivex.functions.Function3
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.rx2.rxCompletable
import timber.log.Timber
import java.util.Date
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SubscribeManager @Inject constructor(
    val appDatabase: AppDatabase,
    val podcastCacheServerManager: PodcastCacheServerManager,
    private val staticServerManager: StaticServerManager,
    private val syncManager: SyncManager,
    @ApplicationContext val context: Context,
    val settings: Settings
) {

    private val subscribeRelay: PublishRelay<PodcastSubscribe> by lazy { setupSubscribeRelay() }
    val subscriptionChangedRelay: PublishRelay<String> = PublishRelay.create()

    private val uuidsInQueue = HashSet<String>()
    private val podcastDao = appDatabase.podcastDao()
    private val episodeDao = appDatabase.episodeDao()
    private val imageLoader = PodcastImageLoader(context = context, isDarkTheme = true, transformations = emptyList())

    data class PodcastSubscribe(val podcastUuid: String, val sync: Boolean)

    private fun setupSubscribeRelay(): PublishRelay<PodcastSubscribe> {
        val source = PublishRelay.create<PodcastSubscribe>()
        source
            .observeOn(Schedulers.io())
            .doOnNext { info -> Timber.i("Adding podcast to addPodcast queue ${info.podcastUuid}") }
            .flatMap({ info -> addPodcast(info.podcastUuid, sync = info.sync, subscribed = true).toObservable() }, true, 5)
            .doOnError { throwable -> LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, throwable, "Could not subscribe to podcast") }
            .subscribeBy(
                onNext = { podcast ->
                    uuidsInQueue.remove(podcast.uuid)
                    Timber.i("Subscribed successfully to podcast ${podcast.uuid}")
                    subscriptionChangedRelay.accept(podcast.uuid)
                }
            )
        return source
    }

    /**
     * Subscribe to a podcast on a background queue.
     */
    fun subscribeOnQueue(podcastUuid: String, sync: Boolean = false) {
        // We only want to track subscriptions on this device, not ones from sync.
        // Sync doesn't go through this method
        FirebaseAnalyticsTracker.subscribedToPodcast()

        if (uuidsInQueue.contains(podcastUuid)) {
            return
        }
        uuidsInQueue.add(podcastUuid)
        subscribeRelay.accept(PodcastSubscribe(podcastUuid, sync))
    }

    /**
     * Subscribe to a podcast and wait.
     */
    fun addPodcast(podcastUuid: String, sync: Boolean = false, subscribed: Boolean = false): Single<Podcast> {
        return subscribeToExistingOrServerPodcast(podcastUuid, sync, subscribed)
            .flatMap {
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Adding podcast $podcastUuid to database")
                cacheArtwork(it).toSingleDefault(it)
            }
            .doOnSuccess {
                LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Added podcast $podcastUuid to database")
                // update the notification time as any podcasts added after this date will be ignored
                settings.setNotificationLastSeenToNow()
            }
    }

    private fun cacheArtwork(podcast: Podcast): Completable {
        return Completable.fromAction { imageLoader.cacheSubscribedArtwork(podcast) }.onErrorComplete()
    }

    fun isSubscribingToPodcast(podcastUuid: String): Boolean {
        return uuidsInQueue.contains(podcastUuid)
    }

    fun getSubscribingPodcastUuids(): Set<String> {
        return uuidsInQueue
    }

    private fun subscribeToExistingOrServerPodcast(podcastUuid: String, sync: Boolean, subscribed: Boolean): Single<Podcast> {
        // check if the podcast exists already
        val subscribedObservable = podcastDao.isSubscribedToPodcastRx(podcastUuid)
        return subscribedObservable.flatMap { isSubscribed ->
            // download the podcast json and add to the database if it doesn't exist
            if (isSubscribed) {
                subscribeToExistingPodcast(podcastUuid, sync)
            } else {
                subscribeToServerPodcast(podcastUuid, sync, subscribed)
            }
        }
    }

    private fun subscribeToExistingPodcast(podcastUuid: String, sync: Boolean): Single<Podcast> {
        // set subscribed to true and update the sync status
        val updateObservable = podcastDao.updateSubscribedRx(subscribed = true, uuid = podcastUuid)
            .andThen(podcastDao.updateSyncStatusRx(syncStatus = if (sync) Podcast.SYNC_STATUS_NOT_SYNCED else Podcast.SYNC_STATUS_SYNCED, uuid = podcastUuid))
            .andThen(Completable.fromAction { podcastDao.updateGrouping(PodcastGrouping.All.indexOf(settings.defaultPodcastGrouping()), podcastUuid) })
            .andThen(rxCompletable { podcastDao.updateShowArchived(podcastUuid, settings.defaultShowArchived()) })
        // return the final podcast
        val findObservable = podcastDao.findByUuidRx(podcastUuid)
        return updateObservable.andThen(findObservable.toSingle())
    }

    private fun subscribeToServerPodcast(podcastUuid: String, sync: Boolean, subscribed: Boolean): Single<Podcast> {
        // download the podcast
        val podcastObservable = downloadPodcast(podcastUuid)
            .doOnSuccess { podcast ->
                // mark sync status
                podcast.syncStatus = if (sync) Podcast.SYNC_STATUS_NOT_SYNCED else Podcast.SYNC_STATUS_SYNCED
                podcast.isSubscribed = subscribed
                podcast.grouping = PodcastGrouping.All.indexOf(settings.defaultPodcastGrouping())
                podcast.showArchived = settings.defaultShowArchived()
            }
        // add the podcast
        val insertPodcastObservable = podcastObservable.flatMap { podcast ->
            podcastDao.insertRx(podcast)
        }
        // insert episodes
        return insertPodcastObservable.flatMap { podcast -> subscribeInsertEpisodes(podcast).toSingle { podcast } }
    }

    private fun downloadPodcast(podcastUuid: String): Single<Podcast> {
        // download the podcast
        val serverPodcastObservable = podcastCacheServerManager.getPodcast(podcastUuid, episodeLimit = Settings.LIMIT_MAX_PODCAST_EPISODES)
            .subscribeOn(Schedulers.io())
            .doOnSuccess { Timber.i("Downloaded episodes success podcast $podcastUuid") }
        // download the colors
        val colorObservable = staticServerManager.getColorsSingle(podcastUuid)
            .subscribeOn(Schedulers.io())
            .doOnSuccess { Timber.i("Downloaded colors success podcast $podcastUuid") }
            .onErrorReturn { Optional.empty() }
        // find all podcasts from the database
        val allPodcastsObservable = podcastDao.findSubscribedRx().subscribeOn(Schedulers.io())
        // group the server podcast and all the existing podcasts to calculate the new podcast properties
        val cleanPodcastObservable = Single.zip(
            serverPodcastObservable, colorObservable, allPodcastsObservable,
            Function3<Podcast, Optional<ArtworkColors>, List<Podcast>, Podcast> { podcast, colors, allPodcasts ->
                cleanPodcast(podcast, colors, allPodcasts)
            }
        )
        // add sync information
        if (syncManager.isLoggedIn()) {
            val syncPodcastObservable = syncManager.getPodcastEpisodes(podcastUuid).subscribeOn(Schedulers.io())
            return Single.zip(cleanPodcastObservable, syncPodcastObservable, BiFunction<Podcast, PodcastEpisodesResponse, Podcast>(this::mergeSyncPodcast))
                .onErrorResumeNext(cleanPodcastObservable)
        } else {
            return cleanPodcastObservable
        }
    }

    private fun subscribeInsertEpisodes(podcast: Podcast): Completable {
        // insert the episodes
        return Completable.fromAction { episodeDao.insertAll(podcast.episodes) }
            // make sure the podcast has the latest episode uuid
            .andThen(updateLatestEpisodeUuid(podcast.uuid))
    }

    private fun cleanPodcast(podcast: Podcast, colors: Optional<ArtworkColors>, allPodcasts: List<Podcast>): Podcast {
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
        // give the podcasts and episodes the same added date so we can tell which are new episodes or added with podcast when calculating notifications
        podcast.addedDate = Date()
        // copy colors
        colors.ifPresent { it.copyToPodcast(podcast) }

        for (episode in podcast.episodes) {
            cleanEpisode(episode, podcast)
        }

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
    private fun updateLatestEpisodeUuid(podcastUuid: String): Completable {
        return episodeDao.findLatestRx(podcastUuid)
            .flatMapCompletable { episode -> podcastDao.updateLatestEpisodeRx(episode.uuid, episode.publishedDate, podcastUuid) }
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
            episode.setPlayingStatusInt(syncEpisode.playingStatus ?: 1)
            val duration = syncEpisode.duration ?: 0
            if (duration > 0) {
                episode.duration = duration.toDouble()
            }
        }
        return podcast
    }
}

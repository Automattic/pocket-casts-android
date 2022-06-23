package au.com.shiftyjelly.pocketcasts.repositories.sync

import android.annotation.SuppressLint
import android.app.job.JobInfo
import android.app.job.JobParameters
import android.app.job.JobScheduler
import android.app.job.JobService
import android.content.ComponentName
import android.content.Context
import android.os.SystemClock
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.models.db.dao.UpNextChangeDao
import au.com.shiftyjelly.pocketcasts.models.db.helper.UserEpisodePodcastSubstitute
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.models.entity.Playable
import au.com.shiftyjelly.pocketcasts.models.entity.UpNextChange
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.jobs.JobIds
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServerManagerImpl
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncServerManager
import au.com.shiftyjelly.pocketcasts.servers.sync.UpNextSyncRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.UpNextSyncResponse
import au.com.shiftyjelly.pocketcasts.utils.extensions.parseIsoDate
import au.com.shiftyjelly.pocketcasts.utils.extensions.splitIgnoreEmpty
import au.com.shiftyjelly.pocketcasts.utils.extensions.switchInvalidForNow
import au.com.shiftyjelly.pocketcasts.utils.extensions.toIsoString
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.android.AndroidEntryPoint
import io.reactivex.Completable
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.subscribeBy
import io.reactivex.schedulers.Schedulers
import kotlinx.coroutines.runBlocking
import retrofit2.HttpException
import javax.inject.Inject

@AndroidEntryPoint
@SuppressLint("SpecifyJobSchedulerIdRange")
class UpNextSyncJob : JobService() {

    @Inject lateinit var settings: Settings
    @Inject lateinit var serverManager: SyncServerManager
    @Inject lateinit var appDatabase: AppDatabase
    @Inject lateinit var upNextQueue: UpNextQueue
    @Inject lateinit var playbackManager: PlaybackManager
    @Inject lateinit var podcastManager: PodcastManager
    @Inject lateinit var episodeManager: EpisodeManager
    @Inject lateinit var downloadManager: DownloadManager
    @Inject lateinit var podcastCacheServerManager: PodcastCacheServerManagerImpl
    @Inject lateinit var userEpisodeManager: UserEpisodeManager

    private val disposables = CompositeDisposable()

    companion object {
        @JvmStatic
        fun run(settings: Settings, context: Context) {
            // Don't run the job if Up Next syncing is turned off
            if (!settings.isLoggedIn()) {
                return
            }
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "UpNextSyncJob - scheduled")
            val scheduler = context.getSystemService(Context.JOB_SCHEDULER_SERVICE) as JobScheduler
            val jobId = JobIds.UP_NEXT_SYNC_JOB_ID
            scheduler.cancel(jobId)
            val builder = JobInfo.Builder(jobId, ComponentName(context, UpNextSyncJob::class.java)).setRequiredNetworkType(JobInfo.NETWORK_TYPE_ANY)
            scheduler.schedule(builder.build())
        }
    }

    override fun onStartJob(jobParameters: JobParameters): Boolean {
        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "UpNextSyncJob - onStartJob")
        performSync(jobParameters)
        return true
    }

    override fun onStopJob(jobParameters: JobParameters): Boolean {
        LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "UpNextSyncJob - onStopJob")
        disposables.clear()
        return true
    }

    private fun performSync(jobParameters: JobParameters) {
        val startTime = SystemClock.elapsedRealtime()
        val upNextChangeDao = appDatabase.upNextChangeDao()
        upNextChangeDao
            .findAllRx()
            .map { changes -> buildRequest(changes) }
            .flatMapCompletable { request ->
                serverManager.upNextSync(request)
                    .flatMapCompletable { response -> readResponse(response) }
                    .andThen(clearSyncedData(request, upNextChangeDao))
                    .onErrorComplete { it is HttpException && it.code() == 304 }
            }
            .subscribeOn(Schedulers.io())
            .observeOn(AndroidSchedulers.mainThread())
            .subscribeBy(
                onError = { throwable ->
                    LogBuffer.e(LogBuffer.TAG_BACKGROUND_TASKS, throwable, "UpNextSyncJob - failed - ${String.format("%d ms", SystemClock.elapsedRealtime() - startTime)}")
                    jobFinished(jobParameters, false)
                },
                onComplete = {
                    LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "UpNextSyncJob - jobFinished - ${String.format("%d ms", SystemClock.elapsedRealtime() - startTime)}")
                    jobFinished(jobParameters, false)
                }
            )
            .addTo(disposables)
    }

    private fun clearSyncedData(upNextSyncRequest: UpNextSyncRequest, upNextChangeDao: UpNextChangeDao): Completable {
        val latestChange: UpNextSyncRequest.Change? = upNextSyncRequest.upNext.changes.maxByOrNull { it.modified }
        return if (latestChange == null) {
            Completable.complete()
        } else {
            val latestActionTime = latestChange.modified
            upNextChangeDao.deleteChangesOlderOrEqualToRx(latestActionTime)
        }
    }

    private fun buildRequest(changes: List<UpNextChange>): UpNextSyncRequest {
        val requestChanges = mutableListOf<UpNextSyncRequest.Change>()
        for (change in changes) {
            requestChanges.add(buildChangeRequest(change))
        }

        val serverModified = settings.getUpNextServerModified()
        val upNext = UpNextSyncRequest.UpNext(serverModified, requestChanges)
        val deviceTime = System.currentTimeMillis()
        val version = Settings.SYNC_API_VERSION.toString()
        return UpNextSyncRequest(deviceTime, version, upNext)
    }

    private fun buildChangeRequest(change: UpNextChange): UpNextSyncRequest.Change {
        // replace action
        if (change.type == UpNextChange.ACTION_REPLACE) {
            val uuids = change.uuids?.splitIgnoreEmpty(",") ?: listOf()
            val episodes = uuids.map { uuid ->
                val episode = runBlocking { episodeManager.findPlayableByUuid(uuid) }
                val podcastUuid = if (episode is Episode) episode.podcastUuid else UserEpisodePodcastSubstitute.uuid
                UpNextSyncRequest.ChangeEpisode(
                    uuid,
                    episode?.title,
                    episode?.downloadUrl,
                    podcastUuid,
                    episode?.publishedDate?.toIsoString()
                )
            }
            return UpNextSyncRequest.Change(
                UpNextChange.ACTION_REPLACE,
                change.modified,
                episodes = episodes
            )
        }
        // any other action
        else {
            val uuid = change.uuid
            val episode = if (uuid == null) null else runBlocking { episodeManager.findPlayableByUuid(uuid) }
            val publishedDate = episode?.publishedDate?.switchInvalidForNow()?.toIsoString()
            val podcastUuid = if (episode is Episode) episode.podcastUuid else UserEpisodePodcastSubstitute.uuid
            return UpNextSyncRequest.Change(
                action = change.type,
                modified = change.modified,
                uuid = change.uuid,
                title = episode?.title,
                url = episode?.downloadUrl,
                published = publishedDate,
                podcast = podcastUuid
            )
        }
    }

    private fun readResponse(response: UpNextSyncResponse): Completable {
        if (settings.getUpNextServerModified() == 0L && response.episodes.isNullOrEmpty() && playbackManager.getCurrentEpisode() != null) {
            // Server sent empty up next for first log in and we have an up next list already, we should keep the local copy
            upNextQueue.changeList(playbackManager.upNextQueue.queueEpisodes) // Change list will automatically include the current episode
            return Completable.complete()
        }

        if (!response.hasChanged(settings.getUpNextServerModified())) {
            return Completable.complete()
        }

        // import missing podcasts
        val podcastUuids: List<String> = response.episodes?.mapNotNull { it.podcast }?.filter { it != UserEpisodePodcastSubstitute.uuid } ?: emptyList()
        val addMissingPodcast: Completable = Observable.fromIterable(podcastUuids).flatMapCompletable { podcastUuid ->
            podcastManager.findOrDownloadPodcastRx(podcastUuid = podcastUuid).ignoreElement()
        }

        // import missing episodes
        val findOrDownloadEpisodes: Observable<Playable> = Observable.fromIterable(response.episodes ?: emptyList()).concatMap { responseEpisode ->
            val episodeUuid = responseEpisode.uuid
            val podcastUuid = responseEpisode.podcast
            if (podcastUuid == null) {
                Observable.empty()
            } else {
                if (podcastUuid == UserEpisodePodcastSubstitute.uuid) {
                    userEpisodeManager.downloadMissingUserEpisode(episodeUuid, placeholderTitle = responseEpisode.title, placeholderPublished = responseEpisode.published?.parseIsoDate()).toObservable()
                } else {
                    val skeletonEpisode = responseEpisode.toSkeletonEpisode(podcastUuid)
                    episodeManager.downloadMissingEpisode(episodeUuid, podcastUuid, skeletonEpisode, podcastManager, false).toObservable()
                }
            }
        }

        return addMissingPodcast
            .andThen(findOrDownloadEpisodes).toList()
            // import the server Up Next into the database
            .flatMapCompletable { episodes -> upNextQueue.importServerChanges(episodes, playbackManager, downloadManager) }
            // check the current episode it correct
            .andThen(playbackManager.loadQueueRx())
            // save the server Up Next modified so we only apply changes
            .andThen(saveServerModified(response))
    }

    private fun saveServerModified(response: UpNextSyncResponse): Completable {
        return Completable.fromAction { settings.setUpNextServerModified(response.serverModified) }
    }
}

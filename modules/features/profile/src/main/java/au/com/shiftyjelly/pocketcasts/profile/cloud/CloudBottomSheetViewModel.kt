package au.com.shiftyjelly.pocketcasts.profile.cloud

import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.toLiveData
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeAnalytics
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadQueue
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadType
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.containsUuid
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import au.com.shiftyjelly.pocketcasts.views.helper.CloudDeleteHelper
import au.com.shiftyjelly.pocketcasts.views.helper.DeleteState
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.UploadedFileDetailModalOptionType
import com.automattic.eventhorizon.UploadedFilePlayOptionType
import com.automattic.eventhorizon.UserFileDeletedEvent
import com.automattic.eventhorizon.UserFileDetailOptionTappedEvent
import com.automattic.eventhorizon.UserFilePlayPauseButtonTappedEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import io.reactivex.BackpressureStrategy
import io.reactivex.rxkotlin.Flowables
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@HiltViewModel
class CloudBottomSheetViewModel @Inject constructor(
    private val userEpisodeManager: UserEpisodeManager,
    private val playbackManager: PlaybackManager,
    private val episodeManager: EpisodeManager,
    private val downloadQueue: DownloadQueue,
    private val podcastManager: PodcastManager,
    private val eventHorizon: EventHorizon,
    private val episodeAnalytics: EpisodeAnalytics,
    @ApplicationScope private val applicationScope: CoroutineScope,
    userManager: UserManager,
) : ViewModel() {
    lateinit var state: LiveData<BottomSheetState>
    var signInState = userManager.getSignInState().toLiveData()
    private val source = SourceView.FILES

    fun setup(uuid: String) {
        val isPlayingFlowable = playbackManager.playbackStateRelay.filter { it.episodeUuid == uuid }.map { it.isPlaying }.startWith(false).toFlowable(BackpressureStrategy.LATEST)
        val inUpNextFlowable = playbackManager.upNextQueue.changesObservable.containsUuid(uuid).toFlowable(BackpressureStrategy.LATEST)
        val episodeFlowable = userEpisodeManager.episodeRxFlowable(uuid)
        val combined = Flowables.combineLatest(episodeFlowable, inUpNextFlowable, isPlayingFlowable) { episode, inUpNext, isPlaying ->
            BottomSheetState(episode, inUpNext, isPlaying)
        }
        state = combined.toLiveData()
    }

    fun getDeleteStateOnDeleteClick(episode: UserEpisode): DeleteState {
        trackOptionTapped(UploadedFileDetailModalOptionType.Delete)
        return CloudDeleteHelper.getDeleteState(episode)
    }

    fun deleteEpisode(episode: UserEpisode, deleteState: DeleteState) {
        CloudDeleteHelper.deleteEpisode(
            episode = episode,
            deleteState = deleteState,
            sourceView = source,
            downloadQueue = downloadQueue,
            playbackManager = playbackManager,
            userEpisodeManager = userEpisodeManager,
            applicationScope = applicationScope,
        )
        eventHorizon.track(UserFileDeletedEvent)
        if (deleteState == DeleteState.Cloud) {
            episodeAnalytics.trackEvent(
                event = AnalyticsEvent.EPISODE_DELETED_FROM_CLOUD,
                source = source,
                uuid = episode.uuid,
            )
        }
        viewModelScope.launch {
            episodeManager.disableAutoDownload(episode)
        }
    }

    fun uploadEpisode(episode: UserEpisode) {
        userEpisodeManager.uploadToServer(episode, waitForWifi = false)
        episodeAnalytics.trackEvent(AnalyticsEvent.EPISODE_UPLOAD_QUEUED, source = source, uuid = episode.uuid)
    }

    fun removeEpisode(episode: UserEpisode) {
        userEpisodeManager.removeFromCloud(episode)
        trackOptionTapped(UploadedFileDetailModalOptionType.DeleteFromCloud)
        episodeAnalytics.trackEvent(AnalyticsEvent.EPISODE_DELETED_FROM_CLOUD, source = source, uuid = episode.uuid)
    }

    fun cancelUpload(episode: UserEpisode) {
        userEpisodeManager.cancelUpload(episode)
        trackOptionTapped(UploadedFileDetailModalOptionType.CancelUpload)
        episodeAnalytics.trackEvent(AnalyticsEvent.EPISODE_UPLOAD_CANCELLED, source = source, uuid = episode.uuid)
    }

    fun cancelDownload(episode: UserEpisode) {
        downloadQueue.cancel(episode.uuid, source)
        trackOptionTapped(UploadedFileDetailModalOptionType.CancelDownload)
    }

    fun download(episode: UserEpisode) {
        downloadQueue.enqueue(episode.uuid, DownloadType.UserTriggered(waitForWifi = false), source)
    }

    fun removeFromUpNext(episode: UserEpisode) {
        playbackManager.removeEpisode(episodeToRemove = episode, source = source)
        trackOptionTapped(UploadedFileDetailModalOptionType.UpNextDelete)
    }

    fun playNext(episode: UserEpisode) {
        applicationScope.launch(Dispatchers.Default) {
            playbackManager.playNext(episode = episode, source = source)
            trackOptionTapped(UploadedFileDetailModalOptionType.UpNextAddTop)
        }
    }

    fun playLast(episode: UserEpisode) {
        applicationScope.launch(Dispatchers.Default) {
            playbackManager.playLast(episode = episode, source = source)
            trackOptionTapped(UploadedFileDetailModalOptionType.UpNextAddBottom)
        }
    }

    fun markAsPlayed(episode: UserEpisode) {
        viewModelScope.launch(Dispatchers.Default) {
            episodeManager.markAsPlayedBlocking(episode, playbackManager, podcastManager)
            episodeAnalytics.trackEvent(AnalyticsEvent.EPISODE_MARKED_AS_PLAYED, source, episode.uuid)
            trackOptionTapped(UploadedFileDetailModalOptionType.MarkPlayed)
        }
    }

    fun markAsUnplayed(episode: UserEpisode) {
        viewModelScope.launch(Dispatchers.Default) {
            episodeManager.markAsNotPlayedBlocking(episode)
            episodeAnalytics.trackEvent(AnalyticsEvent.EPISODE_MARKED_AS_UNPLAYED, source, episode.uuid)
            trackOptionTapped(UploadedFileDetailModalOptionType.MarkUnplayed)
        }
    }

    fun playNow(episode: UserEpisode, forceStream: Boolean) {
        playbackManager.playNow(episode = episode, forceStream = forceStream, sourceView = source)
        eventHorizon.track(
            UserFilePlayPauseButtonTappedEvent(
                option = UploadedFilePlayOptionType.Play,
            ),
        )
    }

    fun pause() {
        playbackManager.pause(sourceView = source)
        eventHorizon.track(
            UserFilePlayPauseButtonTappedEvent(
                option = UploadedFilePlayOptionType.Pause,
            ),
        )
    }

    fun trackOptionTapped(option: UploadedFileDetailModalOptionType) {
        eventHorizon.track(
            UserFileDetailOptionTappedEvent(
                option = option,
            ),
        )
    }
}

data class BottomSheetState(
    val episode: UserEpisode,
    val inUpNext: Boolean,
    val isPlaying: Boolean,
)

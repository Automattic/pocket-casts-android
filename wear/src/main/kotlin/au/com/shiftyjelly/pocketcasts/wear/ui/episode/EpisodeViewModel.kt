package au.com.shiftyjelly.pocketcasts.wear.ui.episode

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsSource
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeAnalytics
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.profile.cloud.AddFileActivity
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextPosition
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.servers.ServerShowNotesManager
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
import au.com.shiftyjelly.pocketcasts.utils.extensions.combine6
import au.com.shiftyjelly.pocketcasts.wear.ui.player.StreamingConfirmationScreen
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import timber.log.Timber
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class EpisodeViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val downloadManager: DownloadManager,
    private val episodeAnalytics: EpisodeAnalytics,
    private val episodeManager: EpisodeManager,
    private val playbackManager: PlaybackManager,
    private val podcastManager: PodcastManager,
    private val showNotesManager: ServerShowNotesManager,
    theme: Theme,
) : ViewModel() {

    private val analyticsSource = AnalyticsSource.WATCH_EPISODE_DETAILS

    sealed class State {
        data class Loaded(
            val episode: BaseEpisode,
            val podcast: Podcast?,
            val isPlayingEpisode: Boolean,
            val inUpNext: Boolean,
            val tintColor: Color?,
            val downloadProgress: Float? = null,
            val showNotes: String? = null,
        ) : State()

        object Empty : State()
    }

    data class UpNextOption(
        @DrawableRes val iconRes: Int,
        @StringRes val titleRes: Int,
        val onClick: () -> Unit,
    )

    val upNextOptions = listOf(
        UpNextOption(
            iconRes = IR.drawable.ic_upnext_playnext,
            titleRes = LR.string.play_next,
            onClick = { addToUpNext(UpNextPosition.NEXT) },
        ),
        UpNextOption(
            iconRes = IR.drawable.ic_upnext_playlast,
            titleRes = LR.string.play_last,
            onClick = { addToUpNext(UpNextPosition.LAST) },
        ),
    )

    private val _stateFlow = MutableStateFlow<State>(State.Empty)
    val stateFlow = _stateFlow.asStateFlow()

    init {
        val episodeUuid = savedStateHandle.get<String>(EpisodeScreenFlow.episodeUuidArgument)
            ?: throw IllegalStateException("EpisodeViewModel must have an episode uuid in the SavedStateHandle")

        val episodeFlow = episodeManager
            .observeEpisodeByUuid(episodeUuid)

        val podcastFlow = episodeFlow
            .filterIsInstance<PodcastEpisode>()
            .map {
                podcastManager.findPodcastByUuidSuspend(it.podcastUuid)
            }

        val isPlayingEpisodeFlow = playbackManager.playbackStateRelay.asFlow()
            .filter { it.episodeUuid == episodeUuid }
            .map { it.isPlaying }

        val inUpNextFlow = playbackManager.upNextQueue.changesObservable.asFlow()

        viewModelScope.launch {

            val downloadProgressFlow = combine(
                episodeFlow,
                downloadManager.progressUpdateRelay.asFlow()
            ) { episode, downloadProgressUpdate ->
                (episode to downloadProgressUpdate)
            }.filter { (episode, downloadProgressUpdate) ->
                episode.uuid == downloadProgressUpdate.episodeUuid
            }.map { (_, downloadProgressUpdate) ->
                downloadProgressUpdate.downloadProgress
            }

            val showNotesFlow = flow {
                emit(showNotesManager.loadShowNotes(episodeUuid))
            }

            combine6(
                episodeFlow,
                // Emitting a value "onStart" for the flows that don't need to block the UI
                podcastFlow.onStart { emit(null) },
                isPlayingEpisodeFlow.onStart { emit(false) },
                inUpNextFlow,
                downloadProgressFlow.onStart<Float?> { emit(null) },
                showNotesFlow.onStart { emit(null) }
            ) { episode, podcast, isPlayingEpisode, upNext, downloadProgress, showNotes ->

                val inUpNext = (upNext is UpNextQueue.State.Loaded) &&
                    (upNext.queue + upNext.episode)
                        .map { it.uuid }
                        .contains(episode.uuid)

                if (podcast != null) {
                    val podcastTint = podcast.getTintColor(theme.isDarkTheme)

                    val tint = ThemeColor.podcastIcon02(theme.activeTheme, podcastTint)
                    val tintColor = Color(tint)

                    State.Loaded(
                        episode = episode,
                        podcast = podcast,
                        isPlayingEpisode = isPlayingEpisode,
                        downloadProgress = downloadProgress,
                        inUpNext = inUpNext,
                        tintColor = tintColor,
                        showNotes = showNotes,
                    )
                } else {

                    val tintColor = AddFileActivity.darkThemeColors().find {
                        (episode as? UserEpisode)?.tintColorIndex == it.tintColorIndex
                    }?.let {
                        Color(it.color)
                    }

                    State.Loaded(
                        episode = episode,
                        podcast = null,
                        isPlayingEpisode = isPlayingEpisode,
                        downloadProgress = downloadProgress,
                        inUpNext = inUpNext,
                        tintColor = tintColor,
                        showNotes = showNotes,
                    )
                }
            }.collect {
                _stateFlow.value = it
            }
        }
    }

    fun downloadEpisode() {
        val episode = (stateFlow.value as? State.Loaded)?.episode ?: return
        viewModelScope.launch {
            val fromString = "wear episode screen"

            if (episode.downloadTaskId != null) {
                when (episode) {
                    is PodcastEpisode -> {
                        episodeManager.stopDownloadAndCleanUp(episode, fromString)
                    }
                    is UserEpisode -> {
                        downloadManager.removeEpisodeFromQueue(episode, fromString)
                    }
                }

                episodeAnalytics.trackEvent(
                    event = AnalyticsEvent.EPISODE_DOWNLOAD_CANCELLED,
                    source = analyticsSource,
                    uuid = episode.uuid
                )
            } else if (!episode.isDownloaded) {
                episode.autoDownloadStatus = PodcastEpisode.AUTO_DOWNLOAD_STATUS_MANUAL_OVERRIDE_WIFI
                downloadManager.addEpisodeToQueue(episode, fromString, true)

                episodeAnalytics.trackEvent(
                    event = AnalyticsEvent.EPISODE_DOWNLOAD_QUEUED,
                    source = analyticsSource,
                    uuid = episode.uuid
                )
            }
            episodeManager.clearPlaybackError(episode)
        }
    }

    fun deleteDownloadedEpisode() {
        val episode = (stateFlow.value as? State.Loaded)?.episode ?: return
        viewModelScope.launch(Dispatchers.IO) {
            episodeManager.deleteEpisodeFile(episode, playbackManager, disableAutoDownload = true, removeFromUpNext = true)
            episodeAnalytics.trackEvent(
                event = AnalyticsEvent.EPISODE_DOWNLOAD_DELETED,
                source = analyticsSource,
                uuid = episode.uuid,
            )
        }
    }

    fun onPlayClicked(showStreamingConfirmation: () -> Unit) {
        if (playbackManager.shouldWarnAboutPlayback()) {
            showStreamingConfirmation()
        } else {
            play()
        }
    }

    fun onStreamingConfirmationResult(result: StreamingConfirmationScreen.Result) {
        val confirmedStreaming = result == StreamingConfirmationScreen.Result.CONFIRMED
        if (confirmedStreaming && !playbackManager.isPlaying()) {
            play()
        }
    }

    private fun play() {
        val episode = (stateFlow.value as? State.Loaded)?.episode
            ?: return
        viewModelScope.launch {
            playbackManager.playNowSync(
                episode = episode,
                playbackSource = analyticsSource,
            )
        }
    }

    fun onPauseClicked() {
        if ((stateFlow.value as? State.Loaded)?.isPlayingEpisode != true) {
            Timber.e("Attempted to pause when not playing")
            return
        }
        viewModelScope.launch {
            playbackManager.pause(playbackSource = analyticsSource)
        }
    }

    fun addToUpNext(upNextPosition: UpNextPosition) {
        val state = stateFlow.value as? State.Loaded ?: return
        viewModelScope.launch {
            playbackManager.play(
                upNextPosition = upNextPosition,
                episode = state.episode,
                source = analyticsSource
            )
        }
    }

    private fun removeFromUpNext() {
        val state = stateFlow.value as? State.Loaded ?: return
        playbackManager.removeEpisode(episodeToRemove = state.episode, source = AnalyticsSource.WATCH_EPISODE_DETAILS)
    }

    fun onUpNextClicked(
        onRemoveFromUpNext: () -> Unit,
        navigateToUpNextOptions: () -> Unit
    ) {
        val state = stateFlow.value as? State.Loaded ?: return

        val wasInUpNext = state.inUpNext

        if (wasInUpNext) {
            removeFromUpNext()
            onRemoveFromUpNext()
        } else if (playbackManager.upNextQueue.queueEpisodes.isNotEmpty()) {
            navigateToUpNextOptions()
        } else {
            // If the Up Next queue is empty, it doesn't matter where we add the episode
            addToUpNext(UpNextPosition.NEXT)
        }
    }

    fun onArchiveClicked() {
        val episode = (stateFlow.value as? State.Loaded)?.episode ?: return
        if (episode !is PodcastEpisode) {
            Timber.e("Attempted to archive a non-podcast episode")
            return
        }
        viewModelScope.launch(Dispatchers.IO) {
            if (episode.isArchived) {
                episodeManager.unarchive(episode)
                episodeAnalytics.trackEvent(AnalyticsEvent.EPISODE_UNARCHIVED, analyticsSource, episode.uuid)
            } else {
                episodeManager.archive(episode, playbackManager)
                episodeAnalytics.trackEvent(AnalyticsEvent.EPISODE_ARCHIVED, analyticsSource, episode.uuid)
            }
        }
    }

    fun onStarClicked() {
        (stateFlow.value as? State.Loaded)?.episode?.let { episode ->
            if (episode !is PodcastEpisode) {
                Timber.e("Attempted to star a non-podcast episode")
                return
            }

            episodeManager.toggleStarEpisodeAsync(episode)
            val event = if (episode.isStarred) AnalyticsEvent.EPISODE_UNSTARRED else AnalyticsEvent.EPISODE_STARRED
            episodeAnalytics.trackEvent(event, analyticsSource, episode.uuid)
        }
    }

    fun onMarkAsPlayedClicked() {
        viewModelScope.launch(Dispatchers.IO) {
            (stateFlow.value as? State.Loaded)?.episode?.let { episode ->
                val event = if (episode.playingStatus == EpisodePlayingStatus.COMPLETED) {
                    episodeManager.markAsNotPlayed(episode)
                    AnalyticsEvent.EPISODE_MARKED_AS_UNPLAYED
                } else {
                    episodeManager.markAsPlayed(episode, playbackManager, podcastManager)
                    AnalyticsEvent.EPISODE_MARKED_AS_PLAYED
                }
                episodeAnalytics.trackEvent(event, analyticsSource, episode.uuid)
            }
        }
    }
}

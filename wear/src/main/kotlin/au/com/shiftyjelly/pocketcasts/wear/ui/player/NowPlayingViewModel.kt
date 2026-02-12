package au.com.shiftyjelly.pocketcasts.wear.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.wear.networking.ConnectivityStateManager
import com.google.android.horologist.media.ui.components.controls.SeekButtonIncrement
import com.google.android.horologist.media.ui.state.model.TrackPositionUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.time.DurationUnit
import kotlin.time.toDuration
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val playbackManager: PlaybackManager,
    settings: Settings,
    private val theme: Theme,
    @ApplicationScope private val coroutineScope: CoroutineScope,
    private val audioOutputSelectorHelper: AudioOutputSelectorHelper,
    private val connectivityStateManager: ConnectivityStateManager,
) : ViewModel() {
    private var playAttempt: Job? = null

    sealed class State {
        data class Loaded(
            val title: String,
            val subtitle: String?,
            val tintColor: Int?,
            val playing: Boolean,
            val episodeUuid: String,
            val theme: Theme,
            val seekBackwardIncrement: SeekButtonIncrement,
            val seekForwardIncrement: SeekButtonIncrement,
            val trackPositionUiModel: TrackPositionUiModel.Actual,
            val error: Boolean = false,
            val showConnectivityBanner: Boolean = false,
        ) : State()
        object Loading : State()
        object Empty : State()
    }

    val state: StateFlow<State> =
        combine(
            playbackManager.playbackStateRelay.asFlow(),
            settings.skipBackInSecs.flow,
            settings.skipForwardInSecs.flow,
            connectivityStateManager.isConnected,
        ) { playbackState, skipBackwardSecs, skipForwardSecs, isConnected ->

            if (playbackState.isEmpty) {
                State.Empty
            } else {

                val trackPositionUiModel = TrackPositionUiModel.Actual(
                    percent = with(playbackState) {
                        if (durationMs != 0) {
                            positionMs.toFloat() / durationMs
                        } else {
                            0f
                        }
                    },
                    duration = playbackState.durationMs.toDuration(DurationUnit.MILLISECONDS),
                    position = playbackState.positionMs.toDuration(DurationUnit.MILLISECONDS),
                    shouldAnimate = true,
                )

                val currentEpisode = playbackManager.getCurrentEpisode()
                val isEpisodeDownloaded = currentEpisode?.isDownloaded == true
                val showConnectivityBanner = !isConnected && !isEpisodeDownloaded

                State.Loaded(
                    title = playbackState.title,
                    subtitle = currentEpisode?.let { episode ->
                        val podcast = playbackState.podcast
                        episode.displaySubtitle(podcast)
                    },
                    tintColor = playbackState.podcast?.getTintColor(theme.isDarkTheme),
                    episodeUuid = playbackState.episodeUuid,
                    playing = playbackState.isPlaying,
                    theme = theme,
                    seekBackwardIncrement = SeekButtonIncrement.Known(skipBackwardSecs),
                    seekForwardIncrement = SeekButtonIncrement.Known(skipForwardSecs),
                    trackPositionUiModel = trackPositionUiModel,
                    error = playbackState.isError,
                    showConnectivityBanner = showConnectivityBanner,
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = State.Loading,
        )

    fun onPlayButtonClick(showStreamingConfirmation: () -> Unit) {
        val currentEpisode = playbackManager.getCurrentEpisode()
        val isEpisodeDownloaded = currentEpisode?.isDownloaded == true
        val isConnected = connectivityStateManager.isConnected.value
        val needsStreamingButDisconnected = !isEpisodeDownloaded && !isConnected

        if (needsStreamingButDisconnected || playbackManager.shouldWarnAboutPlayback()) {
            showStreamingConfirmation()
        } else {
            playAttempt?.cancel()
            playAttempt = coroutineScope.launch { audioOutputSelectorHelper.attemptPlay(::play) }
        }
    }

    fun onPauseButtonClick() {
        playAttempt?.cancel()

        playbackManager.pause(sourceView = SourceView.PLAYER)
    }

    fun onSeekBackButtonClick() {
        playbackManager.skipBackward(SourceView.PLAYER)
    }

    fun onSeekForwardButtonClick() {
        playbackManager.skipForward(SourceView.PLAYER)
    }

    fun onStreamingConfirmationResult(result: StreamingConfirmationScreen.Result) {
        val confirmedStreaming = result == StreamingConfirmationScreen.Result.CONFIRMED
        if (confirmedStreaming && !playbackManager.isPlaying()) {
            playAttempt?.cancel()
            playAttempt = coroutineScope.launch { audioOutputSelectorHelper.attemptPlay(::play) }
        }
    }

    private fun play() {
        playbackManager.playQueue(SourceView.PLAYER)
    }
}

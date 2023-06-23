package au.com.shiftyjelly.pocketcasts.wear.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsSource
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.wear.di.ForApplicationScope
import com.google.android.horologist.media.ui.components.controls.SeekButtonIncrement
import com.google.android.horologist.media.ui.state.model.TrackPositionUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.rx2.asFlow
import javax.inject.Inject
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val playbackManager: PlaybackManager,
    settings: Settings,
    private val theme: Theme,
    @ForApplicationScope private val coroutineScope: CoroutineScope,
    private val audioOutputSelectorHelper: AudioOutputSelectorHelper,
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
        ) : State()
        object Loading : State()
        object Empty : State()
    }

    val state: StateFlow<State> =
        combine(
            playbackManager.playbackStateRelay.asFlow(),
            settings.skipBackwardInSecsObservable.asFlow(),
            settings.skipForwardInSecsObservable.asFlow(),
        ) { playbackState, skipBackwardSecs, skipForwardSecs ->

            if (playbackState.isEmpty) {
                State.Empty
            } else {

                val trackPositionUiModel = TrackPositionUiModel.Actual(
                    percent = with(playbackState) {
                        if (durationMs != 0) {
                            positionMs.toFloat() / durationMs
                        } else 0f
                    },
                    duration = playbackState.durationMs.toDuration(DurationUnit.MILLISECONDS),
                    position = playbackState.positionMs.toDuration(DurationUnit.MILLISECONDS),
                    shouldAnimate = true
                )

                State.Loaded(
                    title = playbackState.title,
                    subtitle = playbackManager.getCurrentEpisode()?.let { episode ->
                        val podcast = playbackState.podcast
                        episode.displaySubtitle(podcast)
                    },
                    tintColor = playbackState.podcast?.getPlayerTintColor(theme.isDarkTheme),
                    episodeUuid = playbackState.episodeUuid,
                    playing = playbackState.isPlaying,
                    theme = theme,
                    seekBackwardIncrement = SeekButtonIncrement.Known(skipBackwardSecs),
                    seekForwardIncrement = SeekButtonIncrement.Known(skipForwardSecs),
                    trackPositionUiModel = trackPositionUiModel,
                )
            }
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = State.Loading
        )

    fun onPlayButtonClick(showStreamingConfirmation: () -> Unit) {
        if (playbackManager.shouldWarnAboutPlayback()) {
            showStreamingConfirmation()
        } else {
            playAttempt?.cancel()

            playAttempt = coroutineScope.launch { audioOutputSelectorHelper.attemptPlay(::play) }
        }
    }

    fun onPauseButtonClick() {
        playAttempt?.cancel()

        playbackManager.pause(playbackSource = AnalyticsSource.PLAYER)
    }

    fun onSeekBackButtonClick() {
        playbackManager.skipBackward(AnalyticsSource.PLAYER)
    }

    fun onSeekForwardButtonClick() {
        playbackManager.skipForward(AnalyticsSource.PLAYER)
    }

    fun onStreamingConfirmationResult(result: StreamingConfirmationScreen.Result) {
        val confirmedStreaming = result == StreamingConfirmationScreen.Result.CONFIRMED
        if (confirmedStreaming && !playbackManager.isPlaying()) {
            playAttempt?.cancel()

            playAttempt = coroutineScope.launch { audioOutputSelectorHelper.attemptPlay(::play) }
        }
    }

    private fun play() {
        playbackManager.playQueue(AnalyticsSource.PLAYER)
    }
}

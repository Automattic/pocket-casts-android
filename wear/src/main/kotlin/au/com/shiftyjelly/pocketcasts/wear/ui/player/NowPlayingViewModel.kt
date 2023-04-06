package au.com.shiftyjelly.pocketcasts.wear.ui.player

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsSource
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import com.google.android.horologist.media.ui.components.controls.SeekButtonIncrement
import com.google.android.horologist.media.ui.state.model.TrackPositionUiModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.rx2.asFlow
import javax.inject.Inject
import kotlin.time.DurationUnit
import kotlin.time.toDuration

@HiltViewModel
class NowPlayingViewModel @Inject constructor(
    private val playbackManager: PlaybackManager,
    settings: Settings,
) : ViewModel() {

    sealed class State {
        data class Loaded(
            val title: String,
            val subtitle: String?,
            val playing: Boolean,
            val episodeUuid: String,
            val seekBackwardIncrement: SeekButtonIncrement,
            val seekForwardIncrement: SeekButtonIncrement,
            val trackPositionUiModel: TrackPositionUiModel.Actual,
        ) : State()
        object Loading : State()
    }

    val state: StateFlow<State> =
        combine(
            playbackManager.playbackStateRelay.asFlow(),
            settings.skipBackwardInSecsObservable.asFlow(),
            settings.skipForwardInSecsObservable.asFlow(),
        ) { playbackState, skipBackwardSecs, skipForwardSecs ->

            val trackPositionUiModel = TrackPositionUiModel.Actual(
                percent = with(playbackState) { positionMs.toFloat() / durationMs },
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
                episodeUuid = playbackState.episodeUuid,
                playing = playbackState.isPlaying,
                seekBackwardIncrement = SeekButtonIncrement.Known(skipBackwardSecs),
                seekForwardIncrement = SeekButtonIncrement.Known(skipForwardSecs),
                trackPositionUiModel = trackPositionUiModel,
            )
        }.stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
            initialValue = State.Loading
        )

    fun onPlayButtonClick(showStreamingConfirmation: () -> Unit) {
        if (playbackManager.shouldWarnAboutPlayback()) {
            showStreamingConfirmation()
        } else {
            play()
        }
    }

    fun onPauseButtonClick() {
        playbackManager.pause(playbackSource = AnalyticsSource.WATCH_PLAYER)
    }

    fun onSeekBackButtonClick() {
        playbackManager.skipBackward(AnalyticsSource.WATCH_PLAYER)
    }

    fun onSeekForwardButtonClick() {
        playbackManager.skipForward(AnalyticsSource.WATCH_PLAYER)
    }

    fun onStreamingConfirmationResult(result: StreamingConfirmationScreen.Result) {
        val confirmedStreaming = result == StreamingConfirmationScreen.Result.CONFIRMED
        if (confirmedStreaming && !playbackManager.isPlaying()) {
            play()
        }
    }

    private fun play() {
        playbackManager.playQueue(AnalyticsSource.WATCH_PLAYER)
    }
}

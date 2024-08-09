package au.com.shiftyjelly.pocketcasts.sharing.clip

import app.cash.turbine.Turbine
import app.cash.turbine.plusAssign
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.update

class FakeClipPlayer : ClipPlayer {
    override val playbackState = MutableStateFlow(ClipPlayer.PlaybackState(isPlaying = false, isLoading = false))
    override val errors = MutableSharedFlow<Exception>()
    override val playbackProgress = MutableStateFlow(0.seconds)

    val clips = Turbine<Clip>()
    val playbackStates = Turbine<PlaybackState>()
    val pollingPeriods = Turbine<Duration>()

    override fun play(clip: Clip): Boolean {
        if (!playbackState.value.allowPlaying) {
            return false
        }
        clips += clip
        playbackState.update { it.copy(isPlaying = true) }
        playbackStates += PlaybackState.Playing
        return true
    }

    override fun stop(): Boolean {
        if (!playbackState.value.allowPausing) {
            return false
        }
        playbackState.update { it.copy(isPlaying = false) }
        playbackStates += PlaybackState.Stopped
        return true
    }

    override fun pause(): Boolean {
        if (!playbackState.value.allowPausing) {
            return false
        }
        playbackState.update { it.copy(isPlaying = false) }
        playbackStates += PlaybackState.Paused
        return true
    }

    override fun seekTo(duration: Duration) {
        playbackProgress.value = duration
    }

    override fun setPlaybackPollingPeriod(idleDuration: Duration) {
        pollingPeriods += idleDuration
    }

    override fun release() = Unit

    enum class PlaybackState { Stopped, Paused, Playing }
}

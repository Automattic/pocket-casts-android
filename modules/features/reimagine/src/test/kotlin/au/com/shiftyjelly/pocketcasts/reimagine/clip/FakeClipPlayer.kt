package au.com.shiftyjelly.pocketcasts.reimagine.clip

import app.cash.turbine.Turbine
import app.cash.turbine.plusAssign
import au.com.shiftyjelly.pocketcasts.sharing.Clip
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeClipPlayer : ClipPlayer {
    override val isPlayingState = MutableStateFlow(false)
    override val errors = MutableSharedFlow<Exception>()
    override val playbackProgress = MutableStateFlow(0.seconds)

    val clips = Turbine<Clip>()
    val playbackStates = Turbine<PlaybackState>()
    val pollingPeriods = Turbine<Duration>()

    override fun play(clip: Clip): Boolean {
        if (isPlayingState.value) {
            return false
        }
        clips += clip
        isPlayingState.value = true
        playbackStates += PlaybackState.Playing
        return true
    }

    override fun stop(): Boolean {
        if (!isPlayingState.value) {
            return false
        }
        isPlayingState.value = false
        playbackStates += PlaybackState.Stopped
        return true
    }

    override fun pause(): Boolean {
        if (!isPlayingState.value) {
            return false
        }
        isPlayingState.value = false
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

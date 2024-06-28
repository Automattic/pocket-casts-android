package au.com.shiftyjelly.pocketcasts.clip

import app.cash.turbine.Turbine
import app.cash.turbine.plusAssign
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeClipPlayer : ClipPlayer {
    override val isPlayingState = MutableStateFlow(false)
    override val errors = MutableSharedFlow<Exception>()

    val clips = Turbine<Clip>()

    override fun play(clip: Clip): Boolean {
        if (isPlayingState.value) {
            return false
        }
        clips += clip
        isPlayingState.value = true
        return true
    }

    override fun stop(): Boolean {
        if (!isPlayingState.value) {
            return false
        }
        isPlayingState.value = false
        return true
    }

    override fun release() = Unit
}

package au.com.shiftyjelly.pocketcasts.clip

import app.cash.turbine.Turbine
import app.cash.turbine.plusAssign
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeClipPlayer : ClipPlayer {
    override val isPlayingState = MutableStateFlow(false)
    override val errors = MutableSharedFlow<Exception>()

    val clips = Turbine<Clip>()

    override fun play(clip: Clip) {
        clips += clip
        isPlayingState.value = true
    }

    override fun stop() {
        isPlayingState.value = false
    }

    override fun release() = Unit
}

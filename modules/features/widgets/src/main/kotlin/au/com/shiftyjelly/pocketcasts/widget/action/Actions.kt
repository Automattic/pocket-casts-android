package au.com.shiftyjelly.pocketcasts.widget.action

import androidx.glance.action.Action
import au.com.shiftyjelly.pocketcasts.widget.data.PlayerWidgetState

internal fun controlPlaybackAction(state: PlayerWidgetState): Action = when {
    state.currentEpisode == null -> OpenPocketCastsAction.action()
    state.isPlaying -> PausePlaybackAction.action()
    else -> ResumePlaybackAction.action()
}

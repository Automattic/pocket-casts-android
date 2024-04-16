package au.com.shiftyjelly.pocketcasts.widget.action

import androidx.glance.action.Action
import au.com.shiftyjelly.pocketcasts.analytics.SourceView

internal fun controlPlaybackAction(
    isPlaying: Boolean,
    source: SourceView,
): Action = if (isPlaying) {
    PausePlaybackAction.action(source)
} else {
    ResumePlaybackAction.action(source)
}

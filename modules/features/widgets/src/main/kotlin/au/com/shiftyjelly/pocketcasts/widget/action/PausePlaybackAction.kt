package au.com.shiftyjelly.pocketcasts.widget.action

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.widget.di.widgetEntryPoint

internal class PausePlaybackAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val sourceView = SourceView.entries.find { it.ordinal == parameters[SourceKey] } ?: SourceView.UNKNOWN
        val playbackManager = context.widgetEntryPoint().playbackManager()

        playbackManager.pauseSuspend(sourceView = sourceView)
    }

    companion object {
        private val SourceKey = ActionParameters.Key<Int>("Source")

        fun action() = actionRunCallback<PausePlaybackAction>(
            actionParametersOf(SourceKey to SourceView.WIDGET_PLAYER_SMALL.ordinal),
        )
    }
}

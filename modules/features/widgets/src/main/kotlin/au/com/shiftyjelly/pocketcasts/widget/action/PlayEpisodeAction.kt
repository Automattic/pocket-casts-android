package au.com.shiftyjelly.pocketcasts.widget.action

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.widget.di.widgetEntryPoint
import timber.log.Timber

internal class PlayEpisodeAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val sourceView = SourceView.entries.find { it.ordinal == parameters[SourceKey] } ?: SourceView.UNKNOWN
        val playbackManager = context.widgetEntryPoint().playbackManager()

        try {
            val episodeId = requireNotNull(parameters[EpisodeIdKey]) {
                "Missing episode id"
            }
            playbackManager.playNowSuspend(episodeUuid = episodeId, sourceView = sourceView)
        } catch (e: Throwable) {
            Timber.e(e, "Failed to play episode")
        }
    }

    companion object {
        private val EpisodeIdKey = ActionParameters.Key<String>("EpisodeId")
        private val SourceKey = ActionParameters.Key<Int>("Source")

        fun action(
            episodeId: String,
            source: SourceView,
        ) = actionRunCallback<PlayEpisodeAction>(
            actionParametersOf(
                EpisodeIdKey to episodeId,
                SourceKey to source.ordinal,
            ),
        )
    }
}

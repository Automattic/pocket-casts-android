package au.com.shiftyjelly.pocketcasts.widget.action

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import au.com.shiftyjelly.pocketcasts.preferences.Settings.Companion.INTENT_OPEN_APP_EPISODE_UUID
import timber.log.Timber

internal class OpenEpisodeDetailsAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val packageName = context.packageName
        try {
            val episodeId = requireNotNull(parameters[EpisodeIdKey]) {
                "Missing episode id"
            }
            val intent = requireNotNull(context.packageManager.getLaunchIntentForPackage(packageName)) {
                "Missing default activity for $packageName"
            }
            intent.putExtra(INTENT_OPEN_APP_EPISODE_UUID, episodeId)
            context.startActivity(intent)
        } catch (e: Throwable) {
            Timber.e(e, "Failed to launch default intent for: $packageName")
        }
    }

    companion object {
        private val EpisodeIdKey = ActionParameters.Key<String>("EpisodeId")

        fun action(episodeId: String) = actionRunCallback<OpenEpisodeDetailsAction>(
            actionParametersOf(EpisodeIdKey to episodeId),
        )
    }
}

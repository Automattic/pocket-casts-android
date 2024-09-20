package au.com.shiftyjelly.pocketcasts.widget.action

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.action.ActionParameters
import androidx.glance.appwidget.action.ActionCallback
import androidx.glance.appwidget.action.actionRunCallback
import timber.log.Timber

internal class OpenPocketCastsAction : ActionCallback {
    override suspend fun onAction(context: Context, glanceId: GlanceId, parameters: ActionParameters) {
        val packageName = context.packageName
        try {
            val intent = requireNotNull(context.packageManager.getLaunchIntentForPackage(packageName)) {
                "Missing default activity for $packageName"
            }
            context.startActivity(intent)
        } catch (e: Throwable) {
            Timber.e(e, "Failed to launch default intent for: $packageName")
        }
    }

    companion object {
        fun action() = actionRunCallback<OpenPocketCastsAction>()
    }
}

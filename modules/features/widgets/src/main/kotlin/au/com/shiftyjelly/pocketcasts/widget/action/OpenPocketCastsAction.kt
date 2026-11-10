package au.com.shiftyjelly.pocketcasts.widget.action

import androidx.compose.runtime.Composable
import androidx.glance.LocalContext
import androidx.glance.action.Action
import androidx.glance.appwidget.action.actionStartActivity
import au.com.shiftyjelly.pocketcasts.utils.extensions.getLaunchIntent

internal object OpenPocketCastsAction {
    // Open the app through an activity PendingIntent that the launcher fires on our behalf.
    // Starting the activity from an actionRunCallback counts as a background activity launch,
    // which Android blocks silently for apps targeting API 35 and above.
    @Composable
    fun action(): Action {
        val context = LocalContext.current
        val intent = requireNotNull(context.getLaunchIntent()) {
            "Missing launcher intent for ${context.packageName}"
        }
        return actionStartActivity(intent)
    }
}

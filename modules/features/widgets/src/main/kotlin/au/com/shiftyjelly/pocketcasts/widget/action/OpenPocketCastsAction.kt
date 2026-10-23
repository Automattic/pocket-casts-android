package au.com.shiftyjelly.pocketcasts.widget.action

import android.content.Intent
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
        val intent = context.getLaunchIntent()
            ?: Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setPackage(context.packageName)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return actionStartActivity(intent)
    }
}

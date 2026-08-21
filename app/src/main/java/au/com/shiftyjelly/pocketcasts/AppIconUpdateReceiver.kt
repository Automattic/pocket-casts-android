package au.com.shiftyjelly.pocketcasts

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import au.com.shiftyjelly.pocketcasts.ui.helper.AppIcon
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Re-syncs the launcher activity aliases right after an app update, without waiting for the user
 * to open the app. [PocketCastsApplication] performs the same sync on every app start, so this
 * receiver's main job is waking the process after MY_PACKAGE_REPLACED; keep both in place.
 */
@AndroidEntryPoint
class AppIconUpdateReceiver : BroadcastReceiver() {

    @Inject lateinit var appIcon: AppIcon

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            appIcon.enableSelectedAlias(appIcon.activeAppIcon)
        }
    }
}

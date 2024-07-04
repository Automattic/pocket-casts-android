package au.com.shiftyjelly.pocketcasts.deeplink

import android.content.Context
import android.content.Intent
import au.com.shiftyjelly.pocketcasts.deeplink.DeepLink.Companion.ACTION_OPEN_DOWNLOADS

sealed interface DeepLink {
    fun toIntent(context: Context): Intent

    companion object {
        const val ACTION_OPEN_DOWNLOADS = "INTENT_OPEN_APP_DOWNLOADING"
    }
}

data object DownloadsDeepLink : DeepLink {
    override fun toIntent(context: Context) = context.launcherIntent
        .setAction(ACTION_OPEN_DOWNLOADS)
}

private val Context.launcherIntent get() = requireNotNull(packageManager.getLaunchIntentForPackage(packageName)) {
    "Missing launcher intent for $packageName"
}

package au.com.shiftyjelly.pocketcasts.deeplink

import android.content.Context
import android.content.Intent

sealed interface DeepLink {
    fun toIntent(context: Context): Intent
}

data object GoToDownloadedEpisodes : DeepLink {
    private const val INTENT_ACTION = "INTENT_OPEN_APP_DOWNLOADING"

    override fun toIntent(context: Context) = context.launcherIntent.setAction(INTENT_ACTION)

    internal val Adapter = DeepLinkAdapter { intent ->
        if (intent.action == INTENT_ACTION) GoToDownloadedEpisodes else null
    }
}

private val Context.launcherIntent get() = requireNotNull(packageManager.getLaunchIntentForPackage(packageName)) {
    "Missing launcher intent for $packageName"
}

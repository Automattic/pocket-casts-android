package au.com.shiftyjelly.pocketcasts.deeplink

import android.content.Intent
import au.com.shiftyjelly.pocketcasts.deeplink.DeepLink.Companion.ACTION_OPEN_DOWNLOADS

internal interface DeepLinkAdapter {
    fun create(intent: Intent): DeepLink?
}

internal class DownloadsAdapter : DeepLinkAdapter {
    override fun create(intent: Intent) = if (intent.action == ACTION_OPEN_DOWNLOADS) {
        DownloadsDeepLink
    } else {
        null
    }
}

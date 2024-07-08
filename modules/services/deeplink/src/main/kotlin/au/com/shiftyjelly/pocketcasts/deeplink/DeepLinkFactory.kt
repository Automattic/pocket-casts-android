package au.com.shiftyjelly.pocketcasts.deeplink

import android.content.Intent
import au.com.shiftyjelly.pocketcasts.deeplink.DeepLink.Companion.ACTION_OPEN_ADD_BOOKMARK
import au.com.shiftyjelly.pocketcasts.deeplink.DeepLink.Companion.ACTION_OPEN_DOWNLOADS
import timber.log.Timber

class DeepLinkFactory {
    private val adapters = listOf(
        DownloadsAdapter(),
        AddBookmarkAdapter(),
    )

    fun create(intent: Intent): DeepLink? {
        Timber.tag(TAG).i("Deep linking to: $intent")
        val deepLinks = adapters.mapNotNull { it.create(intent) }
        return when (deepLinks.size) {
            1 -> {
                val deepLink = deepLinks.first()
                Timber.tag(TAG).d("Found a matching deep link: $deepLink")
                deepLink
            }
            0 -> {
                Timber.tag(TAG).w("No matching deep links found")
                null
            }
            else -> {
                Timber.tag(TAG).w("Found multiple matching deep links: $deepLinks")
                deepLinks.first()
            }
        }
    }

    private companion object {
        val TAG = "DeepLinking"
    }
}

private interface DeepLinkAdapter {
    fun create(intent: Intent): DeepLink?
}

private class DownloadsAdapter : DeepLinkAdapter {
    override fun create(intent: Intent) = if (intent.action == ACTION_OPEN_DOWNLOADS) {
        DownloadsDeepLink
    } else {
        null
    }
}

private class AddBookmarkAdapter : DeepLinkAdapter {
    override fun create(intent: Intent) = if (intent.action == ACTION_OPEN_ADD_BOOKMARK) {
        AddBookmarkDeepLink
    } else {
        null
    }
}

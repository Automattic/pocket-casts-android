package au.com.shiftyjelly.pocketcasts.deeplink

import android.content.Intent
import timber.log.Timber

class DeepLinkFactory {
    private val adapters = listOf<DeepLinkAdapter>()

    fun createDeepLink(intent: Intent): DeepLink? {
        Timber.tag(TAG).i("Deep linking using intent '$intent'")
        val deepLinks = adapters.mapNotNull { it.create(intent) }
        return when {
            deepLinks.size == 1 -> {
                val deepLink = deepLinks.first()
                Timber.tag(TAG).w("Found a matching deep link: $deepLink")
                deepLink
            }
            deepLinks.size == 0 -> {
                Timber.tag(TAG).w("No matching deep links found.")
                null
            }
            else -> {
                Timber.tag(TAG).w("Found multiple matching deep links: '$deepLinks'")
                deepLinks.first()
            }
        }
    }

    private companion object {
        val TAG = "DeepLinking"
    }
}

package au.com.shiftyjelly.pocketcasts.deeplink

import android.content.Intent

internal fun interface DeepLinkAdapter {
    fun create(intent: Intent): DeepLink?
}

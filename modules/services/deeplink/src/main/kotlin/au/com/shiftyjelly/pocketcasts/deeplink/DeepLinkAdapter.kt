package au.com.shiftyjelly.pocketcasts.deeplink

import android.content.Intent

internal interface DeepLinkAdapter {
    fun create(intent: Intent): DeepLink?
}

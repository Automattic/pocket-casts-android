package au.com.shiftyjelly.pocketcasts.settings.util

import android.content.Context
import android.widget.ImageView
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import coil.load

object SettingsHelper {
    private fun getHeaderImageUrl(context: Context): String {
        val density = context.resources.displayMetrics.density
        val size = when {
            density <= 2 -> 640
            density <= 3 -> 960
            else -> 1280
        }
        return String.format("%s/trending/%s/trending_bg.webp", Settings.SERVER_STATIC_URL, size)
    }

    fun loadHeaderImageInto(imageView: ImageView) {
        val url = getHeaderImageUrl(imageView.context)
        imageView.load(url)
    }
}

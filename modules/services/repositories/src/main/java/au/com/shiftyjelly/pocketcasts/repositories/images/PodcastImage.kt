package au.com.shiftyjelly.pocketcasts.repositories.images

import android.content.Context
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.utils.Util

object PodcastImage {

    private const val STATIC_ARTWORK_URL = "%s/discover/images/webp/%s/%s.webp"
    private const val STATIC_ARTWORK_JPG_URL = "%s/discover/images/%s/%s.jpg"

    fun getArtworkUrl(size: Int, uuid: String): String {
        val realSize = when {
            size > 480 -> 960
            size > 200 -> 480
            else -> 200
        }
        return String.format(STATIC_ARTWORK_URL, Settings.SERVER_STATIC_URL, realSize, uuid)
    }

    fun getArtworkUrls(uuid: String): List<String> {
        return listOf(
            getArtworkUrl(size = 960, uuid = uuid),
            getArtworkUrl(size = 480, uuid = uuid),
            getArtworkUrl(size = 200, uuid = uuid)
        )
    }

    /**
     * Jpg image sizes: 130,140,200,210,280,340,400,420,680,960
     */
    fun getArtworkJpgUrl(size: Int, uuid: String): String {
        return String.format(STATIC_ARTWORK_JPG_URL, Settings.SERVER_STATIC_URL, size, uuid)
    }

    fun getLargeArtworkUrl(uuid: String, context: Context): String {
        // The watch's smaller screen does not need such large images
        val size = if (Util.isWearOs(context)) 480 else 960
        return getArtworkUrl(size, uuid)
    }
}

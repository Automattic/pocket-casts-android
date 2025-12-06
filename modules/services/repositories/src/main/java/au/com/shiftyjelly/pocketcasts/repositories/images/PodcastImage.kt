package au.com.shiftyjelly.pocketcasts.repositories.images

import au.com.shiftyjelly.pocketcasts.preferences.Settings

object PodcastImage {
    private const val STATIC_ARTWORK_URL = "%s/discover/images/webp/%s/%s.webp"
    private const val LARGE_SIZE = 960
    private const val MEDIUM_SIZE = 480
    private const val SMALL_SIZE = 200

    fun getMediumArtworkUrl(uuid: String): String {
        return getArtworkUrl(size = MEDIUM_SIZE, uuid = uuid, isWearOS = false)
    }

    fun getArtworkUrl(size: Int?, uuid: String, isWearOS: Boolean): String {
        val maxSize = if (isWearOS) MEDIUM_SIZE else LARGE_SIZE
        val realSize = when {
            size == null -> maxSize
            size > MEDIUM_SIZE -> maxSize
            size > SMALL_SIZE -> MEDIUM_SIZE
            else -> SMALL_SIZE
        }
        return String.format(STATIC_ARTWORK_URL, Settings.SERVER_STATIC_URL, realSize, uuid)
    }

    fun getArtworkUrls(uuid: String, isWearOS: Boolean): List<String> {
        return buildList {
            if (!isWearOS) {
                add(getArtworkUrl(size = LARGE_SIZE, uuid = uuid, isWearOS = false))
            }
            add(getArtworkUrl(size = MEDIUM_SIZE, uuid = uuid, isWearOS = isWearOS))
            add(getArtworkUrl(size = SMALL_SIZE, uuid = uuid, isWearOS = isWearOS))
        }
    }
}

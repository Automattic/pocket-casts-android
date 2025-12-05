package au.com.shiftyjelly.pocketcasts.ui.images

import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import coil3.ImageLoader
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CoilManager @Inject constructor(val imageLoader: ImageLoader) {

    fun clearCache(uuid: String) {
        val urls = PodcastImage.getArtworkUrls(uuid = uuid, isWearOS = false)
        for (url in urls) {
            imageLoader.diskCache?.remove(url)
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Removing $url from image cache.")
        }
        // clear the whole image memory cache, as clearing individual images didn't work
        imageLoader.memoryCache?.clear()
    }

    fun clearAll() {
        imageLoader.memoryCache?.clear()
        imageLoader.diskCache?.clear()
    }
}

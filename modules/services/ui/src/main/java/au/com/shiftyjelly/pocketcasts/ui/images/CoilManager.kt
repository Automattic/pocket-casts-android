package au.com.shiftyjelly.pocketcasts.ui.images

import android.widget.ImageView
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import coil.ImageLoader
import coil.annotation.ExperimentalCoilApi
import coil.size.Size
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.max

@Singleton
class CoilManager @Inject constructor(val imageLoader: ImageLoader) {

    @OptIn(ExperimentalCoilApi::class)
    fun clearCache(uuid: String) {
        val urls = PodcastImage.getArtworkUrls(uuid = uuid)
        for (url in urls) {
            imageLoader.diskCache?.remove(url)
            LogBuffer.i(LogBuffer.TAG_BACKGROUND_TASKS, "Removing $url from image cache.")
        }
        // clear the whole image memory cache, as clearing individual images didn't work
        imageLoader.memoryCache?.clear()
    }

    @OptIn(ExperimentalCoilApi::class)
    fun clearAll() {
        imageLoader.memoryCache?.clear()
        imageLoader.diskCache?.clear()
    }

    companion object {
        private const val MAX_IMAGE_SIZE_IN_PX = 2500

        fun getMaxImageSize(imageView: ImageView) = Size(
            max(imageView.width, MAX_IMAGE_SIZE_IN_PX),
            max(imageView.height, MAX_IMAGE_SIZE_IN_PX)
        )
    }
}

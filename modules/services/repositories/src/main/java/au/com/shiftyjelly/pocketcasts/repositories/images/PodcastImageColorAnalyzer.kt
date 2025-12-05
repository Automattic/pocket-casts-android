package au.com.shiftyjelly.pocketcasts.repositories.images

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.palette.graphics.Palette
import coil3.ImageLoader
import coil3.request.ErrorResult
import coil3.request.SuccessResult
import coil3.toBitmap
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class PodcastImageColorAnalyzer @Inject constructor(
    @ApplicationContext context: Context,
    private val imageLoader: ImageLoader,
) {
    private val requestFactory = PocketCastsImageRequestFactory(context)

    suspend fun getArtworkDominantColor(uuid: String): Color? {
        val request = requestFactory.createForPodcast(uuid)
        val bitmap = when (val result = imageLoader.execute(request)) {
            is SuccessResult -> result.memoryCacheKey?.let { key ->
                imageLoader.memoryCache?.get(key)?.image?.toBitmap()
            }

            is ErrorResult -> null
        }
        val palette = withContext(Dispatchers.IO) {
            bitmap?.let(Palette::from)
                ?.clearFilters()
                ?.generate()
        }
        return palette?.dominantSwatch?.rgb?.let(::Color)
    }
}

package au.com.shiftyjelly.pocketcasts.repositories.playback.auto

import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * The disk cache behind [AlbumArtContentProvider]. The car caches artwork by link, so a link to an episode image
 * that isn't downloaded yet is marked as pending, and the link changes once the image is downloaded. Otherwise
 * the podcast artwork the provider served while the image was slow would stay on screen after it arrived.
 */
@Singleton
class AlbumArtCache @Inject constructor(
    private val imageLoader: ImageLoader,
) {
    private val lateDownloadEvents = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    /**
     * Emits after images finish downloading once the provider stopped waiting for them, so the car can reload
     * what it shows and pick up their new links. Images that arrive close together emit once.
     */
    @OptIn(FlowPreview::class)
    val lateDownloads: Flow<Unit> = lateDownloadEvents.debounce(LATE_DOWNLOAD_GROUPING)

    /**
     * Whether the provider can serve the image at [url] from the disk cache without downloading it.
     */
    @OptIn(ExperimentalCoilApi::class)
    fun isDownloaded(url: String): Boolean {
        val httpUrl = url.toHttpUrlOrNull() ?: return false
        val diskCache = imageLoader.diskCache ?: return false
        val snapshot = runCatching { diskCache.openSnapshot(httpUrl.toString()) }.getOrNull() ?: return false
        snapshot.close()
        return true
    }

    internal fun onLateDownload() {
        lateDownloadEvents.tryEmit(Unit)
    }

    internal companion object {
        val LATE_DOWNLOAD_GROUPING = 1.seconds
    }
}

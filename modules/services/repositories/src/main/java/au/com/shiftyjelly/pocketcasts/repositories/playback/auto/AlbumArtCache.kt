package au.com.shiftyjelly.pocketcasts.repositories.playback.auto

import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.request.ErrorResult
import coil3.request.SuccessResult
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.launch
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

/**
 * The disk cache behind [AlbumArtContentProvider]. The car caches artwork by link, so a link to an episode image
 * that isn't downloaded yet is marked as pending, and the link changes once the image is downloaded. Otherwise
 * the podcast artwork the provider served while the image was slow would stay on screen after it arrived.
 */
@Singleton
class AlbumArtCache @Inject constructor(
    private val imageLoader: ImageLoader,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    private val lateDownloadEvents = MutableSharedFlow<Unit>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )

    private val retryingUrls: MutableSet<String> = ConcurrentHashMap.newKeySet()

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

    /**
     * Downloads the image again after [failure], so a pending link can still change to it once a weak connection
     * recovers. Tries after each of [DOWNLOAD_RETRY_DELAYS] and stops at the first success. Only network errors
     * are retried, since an image the host refused, or one that can't be decoded, would fail again.
     */
    internal fun retryFailedDownload(failure: ErrorResult) {
        val request = failure.request
        val url = request.data.toString()
        if (!failure.isNetworkError() || !retryingUrls.add(url)) {
            return
        }
        applicationScope.launch {
            try {
                for (retryDelay in DOWNLOAD_RETRY_DELAYS) {
                    delay(retryDelay)
                    when (val result = imageLoader.execute(request)) {
                        is SuccessResult -> {
                            onLateDownload()
                            break
                        }

                        is ErrorResult -> if (!result.isNetworkError()) break
                    }
                }
            } finally {
                retryingUrls.remove(url)
            }
        }
    }

    private fun ErrorResult.isNetworkError() = throwable is IOException

    internal companion object {
        val LATE_DOWNLOAD_GROUPING = 1.seconds
        val DOWNLOAD_RETRY_DELAYS = listOf(30.seconds, 2.minutes)
    }
}

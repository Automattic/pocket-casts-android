package au.com.shiftyjelly.pocketcasts.repositories.playback.auto

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import au.com.shiftyjelly.pocketcasts.coroutines.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.request.CachePolicy
import coil3.request.ErrorResult
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.io.File
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import timber.log.Timber

class AlbumArtContentProvider : ContentProvider() {

    companion object {
        private const val AUTHORITY = "%s.media.library.provider"

        // The provider serves the cached file, not the bitmap, so a thumbnail decode is enough to finish the request.
        internal const val ARTWORK_DECODE_SIZE_PX = 64

        // With podcast artwork to fall back on, don't hold the car's binder thread for a slow episode artwork host.
        internal val ARTWORK_WITH_FALLBACK_TIMEOUT = 5.seconds

        fun getAuthority(context: Context): String = String.format(AUTHORITY, context.packageName)

        internal fun artworkFileRequest(context: Context, url: String): ImageRequest = ImageRequest.Builder(context)
            .data(url)
            .size(ARTWORK_DECODE_SIZE_PX)
            .memoryCachePolicy(CachePolicy.DISABLED)
            .build()
    }

    override fun onCreate() = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val result = runCatching {
            Timber.tag("AlbumArtProvider").d("Content uri received ${uri.lastPathSegment}")

            val applicationContext = context?.applicationContext ?: return@runCatching null

            val fallback = uri.getQueryParameter(ALBUM_ART_FALLBACK_PARAM)
            val artworkTimeout = if (fallback.isNullOrBlank()) null else ARTWORK_WITH_FALLBACK_TIMEOUT
            val artworkFile = findArtworkFile(uri.lastPathSegment, applicationContext, artworkTimeout)
                ?: findArtworkFile(fallback, applicationContext, timeout = null)

            artworkFile?.let { ParcelFileDescriptor.open(it, ParcelFileDescriptor.MODE_READ_ONLY) }
        }
        return result
            .onFailure { exception ->
                val message = "Failed to provide artwork file descriptor for $uri"
                Timber.tag("AlbumArtProvider").e(exception, message)
                LogBuffer.e("AlbumArtProvider", exception, message)
            }
            .getOrNull()
    }

    private fun getEntryPoint(context: Context): AlbumArtEntryPoint {
        return EntryPointAccessors.fromApplication(context, AlbumArtEntryPoint::class.java)
    }

    private fun findArtworkFile(urlOrPath: String?, context: Context, timeout: Duration?): File? {
        if (urlOrPath.isNullOrBlank()) return null
        val imageUrl = urlOrPath.toHttpUrlOrNull()
        return if (imageUrl != null) {
            getEntryPoint(context)
                .getArtworkFile(imageUrl, context, timeout)
                ?.takeIf(File::exists)
        } else {
            // The provider is exported, so only serve local files from our own private storage
            // to stop a crafted uri from traversing to and leaking other files owned by our uid.
            urlOrPath.toArtworkFileInAppStorage(context)
        }
    }

    /**
     * Resolves the path to a local artwork file only if it stays within the app's private storage.
     * Canonicalising first collapses `..` and symlinks, so paths that escape our storage are rejected.
     */
    private fun String.toArtworkFileInAppStorage(context: Context): File? {
        val canonicalFile = runCatching { File(this).canonicalFile }.getOrNull() ?: return null
        val isWithinAppStorage = appStorageRoots(context).any { root -> canonicalFile.isWithin(root) }
        return canonicalFile.takeIf { isWithinAppStorage && it.isFile }
    }

    private fun appStorageRoots(context: Context): List<File> = listOfNotNull(
        context.cacheDir,
        context.filesDir,
        context.externalCacheDir,
        context.getExternalFilesDir(null),
    )

    private fun File.isWithin(directory: File): Boolean {
        val canonicalDir = runCatching { directory.canonicalPath }.getOrNull() ?: return false
        return path == canonicalDir || path.startsWith(canonicalDir + File.separator)
    }

    private fun AlbumArtEntryPoint.getArtworkFile(url: HttpUrl, context: Context, timeout: Duration?): File? {
        val imageLoader = imageLoader()
        val request = artworkFileRequest(context, url.toString())
        val result = runBlocking {
            if (timeout == null) {
                imageLoader.execute(request)
            } else {
                // Download outside this call, so an image that misses the timeout still reaches the disk cache.
                val download = applicationScope().async(Dispatchers.IO) { imageLoader.execute(request) }
                withTimeoutOrNull(timeout) { download.await() }
            }
        }
        return when (result) {
            null -> {
                Timber.tag("AlbumArtProvider").w("Timed out loading artwork for url: $url")
                null
            }

            is SuccessResult -> result.diskCacheKey?.let { key -> imageLoader.getCachedArtworkFile(key) }

            is ErrorResult -> {
                LogBuffer.e(LogBuffer.TAG_INVALID_STATE, result.throwable, "Failed to load artwork for url: $url")
                null
            }
        }
    }

    @OptIn(ExperimentalCoilApi::class)
    private fun ImageLoader.getCachedArtworkFile(key: String): File? {
        return diskCache?.openSnapshot(key)?.use { snapshot -> snapshot.data.toFile() }
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?,
    ): Cursor? = null

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?,
    ) = 0

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?) = 0

    override fun getType(uri: Uri): String? = null

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface AlbumArtEntryPoint {
        fun imageLoader(): ImageLoader

        @ApplicationScope
        fun applicationScope(): CoroutineScope
    }
}

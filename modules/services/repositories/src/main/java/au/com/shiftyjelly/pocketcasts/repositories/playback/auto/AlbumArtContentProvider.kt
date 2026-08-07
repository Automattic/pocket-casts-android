package au.com.shiftyjelly.pocketcasts.repositories.playback.auto

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import coil3.ImageLoader
import coil3.annotation.ExperimentalCoilApi
import coil3.request.ErrorResult
import coil3.request.SuccessResult
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.io.File
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import timber.log.Timber

class AlbumArtContentProvider : ContentProvider() {

    companion object {
        private const val AUTHORITY = "%s.media.library.provider"

        fun getAuthority(context: Context): String = String.format(AUTHORITY, context.packageName)
    }

    override fun onCreate() = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val result = runCatching {
            Timber.tag("AlbumArtProvider").d("Content uri received ${uri.lastPathSegment}")

            val applicationContext = context?.applicationContext ?: return@runCatching null

            val imageUrl = uri.toImageUrl()
            val artworkFile = if (imageUrl != null) {
                getEntryPoint(applicationContext).imageLoader()
                    .getArtworkFile(imageUrl, applicationContext)
                    ?.takeIf(File::exists)
            } else {
                // The provider is exported, so only serve local files from our own private storage
                // to stop a crafted uri from traversing to and leaking other files owned by our uid.
                uri.toArtworkFileInAppStorage(applicationContext)
            }

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

    private fun Uri.toImageUrl() = lastPathSegment?.toHttpUrlOrNull()

    /**
     * Resolves the uri to a local artwork file only if it stays within the app's private storage.
     * Canonicalising first collapses `..` and symlinks, so paths that escape our storage are rejected.
     */
    private fun Uri.toArtworkFileInAppStorage(context: Context): File? {
        val path = lastPathSegment ?: return null
        val canonicalFile = runCatching { File(path).canonicalFile }.getOrNull() ?: return null
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

    private fun ImageLoader.getArtworkFile(url: HttpUrl, context: Context): File? {
        val requestFactory = PocketCastsImageRequestFactory(context)
        val request = requestFactory.createForFileOrUrl(url.toString())
        return when (val result = runBlocking { execute(request) }) {
            is SuccessResult -> result.diskCacheKey?.let { key -> getCachedArtworkFile(key) }

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
    }
}

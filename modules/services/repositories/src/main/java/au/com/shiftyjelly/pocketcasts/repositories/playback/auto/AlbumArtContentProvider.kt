package au.com.shiftyjelly.pocketcasts.repositories.playback.auto

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import au.com.shiftyjelly.pocketcasts.servers.di.Downloads
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.io.File
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.buffer
import okio.sink
import okio.use
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
            val entryPoint = getEntryPoint(applicationContext)

            val imageUrl = uri.toImageUrl()
            val artworkFile = if (imageUrl != null) {
                val cacheFile = imageUrl.toCachedArtworkFile(applicationContext)
                if (cacheFile.exists()) {
                    cacheFile
                } else {
                    entryPoint.okHttpClient().fetchAndSaveArtwork(imageUrl, cacheFile)
                }
            } else {
                uri.path?.let(::File)
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

    private fun HttpUrl.toCachedArtworkFile(context: Context): File {
        val fileName = "${host}$encodedPath".replace(oldChar = '/', newChar = ':')
        return context.cacheDir.resolve(fileName)
    }

    private fun OkHttpClient.fetchAndSaveArtwork(imageUrl: HttpUrl, file: File): File? {
        Timber.tag("AlbumArtProvider").d("Executing call for $imageUrl")
        val request = Request.Builder().url(imageUrl).build()
        val response = newCall(request).execute()
        return response.use { it.writeSuccessBody(file) }
    }

    private fun Response.writeSuccessBody(file: File): File? {
        val responseBody = body
        return if (isSuccessful && responseBody != null) {
            file.sink().buffer().use { sink ->
                sink.writeAll(responseBody.source())
            }
            file
        } else {
            null
        }
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
        @Downloads
        fun okHttpClient(): OkHttpClient
    }
}

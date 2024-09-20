package au.com.shiftyjelly.pocketcasts.repositories.playback.auto

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import au.com.shiftyjelly.pocketcasts.servers.di.Downloads
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.io.IOException
import okhttp3.OkHttpClient
import okhttp3.Request
import okio.sink
import timber.log.Timber

class AlbumArtContentProvider : ContentProvider() {

    companion object {
        private const val AUTHORITY = "%s.media.library.provider"

        fun getAuthority(context: Context): String = String.format(AUTHORITY, context.packageName)
    }

    override fun onCreate() = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val context = this.context ?: return null
        Timber.tag("AlbumArtProvider").d("Content uri received ${uri.lastPathSegment}")

        // Extract the image uri e.g. https://static.pocketcasts.net/discover/images/webp/480/220e7cc0-d53e-0133-2e9f-6dc413d6d41d.webp
        val imageUri = Uri.parse(uri.lastPathSegment)
        // Create the cache file name e.g. static.pocketcasts.net:discover:images:webp:480:220e7cc0-d53e-0133-2e9f-6dc413d6d41d.webp
        val cachePath = "${imageUri.host}${imageUri.encodedPath}".replace(oldChar = '/', newChar = ':')
        // Create the cache file path e.g. /data/user/0/au.com.shiftyjelly.pocketcasts.debug/cache/static.pocketcasts.net:discover:images:webp:480:220e7cc0-d53e-0133-2e9f-6dc413d6d41d.webp
        val file = File(context.cacheDir, cachePath)
        // Cache the image
        if (!file.exists()) {
            Timber.tag("AlbumArtProvider").d("Executing call for $imageUri")
            val appContext = context.applicationContext
            val entryPoint = EntryPointAccessors.fromApplication(appContext, AlbumArtEntryPoint::class.java)
            val client = entryPoint.okHttpClient()
            val request = Request.Builder().url(imageUri.toString()).build()
            try {
                client.newCall(request).execute().use { response ->
                    if (response.isSuccessful) {
                        file.sink().use { sink ->
                            response.body?.source()?.readAll(sink)
                        }
                    }
                }
            } catch (e: IOException) {
                Timber.tag("AlbumArtProvider").d(e, "Failed to load image $imageUri")
            }
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
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

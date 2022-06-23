package au.com.shiftyjelly.pocketcasts.repositories.playback.auto

import android.content.ContentProvider
import android.content.ContentValues
import android.content.Context
import android.database.Cursor
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import au.com.shiftyjelly.pocketcasts.repositories.playback.auto.AutoConverter.autoImageLoaderListener
import com.bumptech.glide.Glide
import java.io.File

class AlbumArtContentProvider : ContentProvider() {

    companion object {
        private const val AUTHORITY = "%s.media.library.provider"

        fun getAuthority(context: Context): String = String.format(AUTHORITY, context.packageName)
    }

    override fun onCreate() = true

    override fun openFile(uri: Uri, mode: String): ParcelFileDescriptor? {
        val context = this.context ?: return null
        Log.d("AUTO_IMAGE", "Content uri received: ${uri.lastPathSegment}")

        // Extract the image uri e.g. https://static.pocketcasts.net/discover/images/webp/480/220e7cc0-d53e-0133-2e9f-6dc413d6d41d.webp
        val imageUri = Uri.parse(uri.lastPathSegment)
        // Create the cache file name e.g. static.pocketcasts.net:discover:images:webp:480:220e7cc0-d53e-0133-2e9f-6dc413d6d41d.webp
        val cachePath = "${imageUri.host}${imageUri.encodedPath}".replace(oldChar = '/', newChar = ':')
        // Create the cache file path e.g. /data/user/0/au.com.shiftyjelly.pocketcasts.debug/cache/static.pocketcasts.net:discover:images:webp:480:220e7cc0-d53e-0133-2e9f-6dc413d6d41d.webp
        val file = File(context.cacheDir, cachePath)
        // Cache the image
        if (!file.exists()) {
            val cacheFile = Glide.with(context)
                .downloadOnly()
                .addListener(autoImageLoaderListener)
                .load(imageUri)
                .submit()
                .get()

            cacheFile.renameTo(file)
        }
        return ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
    }

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun query(
        uri: Uri,
        projection: Array<String>?,
        selection: String?,
        selectionArgs: Array<String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<String>?
    ) = 0

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<String>?) = 0

    override fun getType(uri: Uri): String? = null
}

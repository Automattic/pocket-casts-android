package au.com.shiftyjelly.pocketcasts.repositories.playback.auto

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import java.io.File

fun File.asAlbumArtContentUri(context: Context): Uri {
    return Uri.Builder()
        .scheme(ContentResolver.SCHEME_CONTENT)
        .authority(AlbumArtContentProvider.getAuthority(context))
        .appendPath(this.path)
        .build()
}

fun Uri.asAlbumArtContentUri(context: Context): Uri {
    return Uri.Builder()
        .scheme(ContentResolver.SCHEME_CONTENT)
        .authority(AlbumArtContentProvider.getAuthority(context))
        .appendPath(this.toString())
        .build()
}

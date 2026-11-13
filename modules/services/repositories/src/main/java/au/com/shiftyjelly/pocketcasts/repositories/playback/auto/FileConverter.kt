package au.com.shiftyjelly.pocketcasts.repositories.playback.auto

import android.content.ContentResolver
import android.content.Context
import android.net.Uri

internal const val ALBUM_ART_FALLBACK_PARAM = "fallback"

/**
 * Wraps an artwork URL or local file path in a content uri served by [AlbumArtContentProvider].
 *
 * @param fallback Artwork the provider serves instead when this one can't be loaded, e.g. the podcast's
 *   artwork for an episode image whose host is down.
 */
fun Uri.asAlbumArtContentUri(context: Context, fallback: Uri? = null): Uri {
    val artwork = this
    return Uri.Builder()
        .scheme(ContentResolver.SCHEME_CONTENT)
        .authority(AlbumArtContentProvider.getAuthority(context))
        .appendPath(artwork.toString())
        .apply {
            if (fallback != null && fallback != artwork) {
                appendQueryParameter(ALBUM_ART_FALLBACK_PARAM, fallback.toString())
            }
        }
        .build()
}

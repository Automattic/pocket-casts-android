package au.com.shiftyjelly.pocketcasts.repositories.playback.auto

import android.content.ContentResolver
import android.content.Context
import android.net.Uri

internal const val ALBUM_ART_FALLBACK_PARAM = "fallback"
internal const val ALBUM_ART_PENDING_PARAM = "pending"

/**
 * Wraps an artwork URL or local file path in a content uri served by [AlbumArtContentProvider].
 *
 * @param fallback Artwork the provider serves instead when this one can't be loaded, e.g. the podcast's
 *   artwork for an episode image whose host is down.
 * @param isPending Whether the artwork isn't downloaded yet. The car caches artwork by link, so marking the link
 *   until the image is downloaded means the car loads it again then, instead of keeping a fallback served meanwhile.
 */
fun Uri.asAlbumArtContentUri(context: Context, fallback: Uri? = null, isPending: Boolean = false): Uri {
    val artwork = this
    return Uri.Builder()
        .scheme(ContentResolver.SCHEME_CONTENT)
        .authority(AlbumArtContentProvider.getAuthority(context))
        .appendPath(artwork.toString())
        .apply {
            if (fallback != null && fallback != artwork) {
                appendQueryParameter(ALBUM_ART_FALLBACK_PARAM, fallback.toString())
                // Only a link with a fallback can show other artwork while the image is slow.
                if (isPending) {
                    appendQueryParameter(ALBUM_ART_PENDING_PARAM, "true")
                }
            }
        }
        .build()
}

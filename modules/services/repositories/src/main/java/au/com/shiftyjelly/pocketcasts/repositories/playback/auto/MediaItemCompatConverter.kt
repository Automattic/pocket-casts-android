package au.com.shiftyjelly.pocketcasts.repositories.playback.auto

import android.support.v4.media.MediaBrowserCompat
import android.support.v4.media.MediaDescriptionCompat
import androidx.media3.common.MediaItem

object MediaItemCompatConverter {

    fun toCompat(item: MediaItem): MediaBrowserCompat.MediaItem {
        val metadata = item.mediaMetadata
        val descBuilder = MediaDescriptionCompat.Builder()
            .setMediaId(item.mediaId)
            .setTitle(metadata.title)
            .setSubtitle(metadata.artist)
            .setDescription(metadata.description)
            .setIconUri(metadata.artworkUri)
        metadata.extras?.let { descBuilder.setExtras(it) }

        val flags = (if (metadata.isBrowsable == true) MediaBrowserCompat.MediaItem.FLAG_BROWSABLE else 0) or
            (if (metadata.isPlayable == true) MediaBrowserCompat.MediaItem.FLAG_PLAYABLE else 0)

        return MediaBrowserCompat.MediaItem(descBuilder.build(), flags)
    }

    fun toCompatList(items: List<MediaItem>): List<MediaBrowserCompat.MediaItem> {
        return items.map { toCompat(it) }
    }
}

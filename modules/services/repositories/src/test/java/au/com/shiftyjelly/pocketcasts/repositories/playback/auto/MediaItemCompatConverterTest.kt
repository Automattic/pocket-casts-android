package au.com.shiftyjelly.pocketcasts.repositories.playback.auto

import android.support.v4.media.MediaBrowserCompat
import androidx.core.net.toUri
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class MediaItemCompatConverterTest {

    @Test
    fun `converts browsable item`() {
        val mediaItem = MediaItem.Builder()
            .setMediaId("podcast-123")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Test Podcast")
                    .setArtist("Test Author")
                    .setDescription("A description")
                    .setArtworkUri("https://example.com/art.jpg".toUri())
                    .setIsBrowsable(true)
                    .setIsPlayable(false)
                    .build(),
            )
            .build()

        val compat = MediaItemCompatConverter.toCompat(mediaItem)

        assertEquals("podcast-123", compat.mediaId)
        assertEquals("Test Podcast", compat.description.title)
        assertEquals("Test Author", compat.description.subtitle)
        assertEquals("A description", compat.description.description)
        assertEquals("https://example.com/art.jpg".toUri(), compat.description.iconUri)
        assertTrue(compat.isBrowsable)
        assertEquals(0, compat.flags and MediaBrowserCompat.MediaItem.FLAG_PLAYABLE)
    }

    @Test
    fun `converts playable item`() {
        val mediaItem = MediaItem.Builder()
            .setMediaId("episode-456")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Test Episode")
                    .setArtist("Test Podcast")
                    .setIsBrowsable(false)
                    .setIsPlayable(true)
                    .build(),
            )
            .build()

        val compat = MediaItemCompatConverter.toCompat(mediaItem)

        assertEquals("episode-456", compat.mediaId)
        assertEquals("Test Episode", compat.description.title)
        assertTrue(compat.isPlayable)
        assertEquals(0, compat.flags and MediaBrowserCompat.MediaItem.FLAG_BROWSABLE)
    }

    @Test
    fun `converts list of items`() {
        val items = listOf(
            MediaItem.Builder()
                .setMediaId("1")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("First")
                        .setIsBrowsable(true)
                        .setIsPlayable(false)
                        .build(),
                )
                .build(),
            MediaItem.Builder()
                .setMediaId("2")
                .setMediaMetadata(
                    MediaMetadata.Builder()
                        .setTitle("Second")
                        .setIsBrowsable(false)
                        .setIsPlayable(true)
                        .build(),
                )
                .build(),
        )

        val compatList = MediaItemCompatConverter.toCompatList(items)

        assertEquals(2, compatList.size)
        assertEquals("1", compatList[0].mediaId)
        assertEquals("2", compatList[1].mediaId)
    }

    @Test
    fun `handles item with no metadata flags`() {
        val mediaItem = MediaItem.Builder()
            .setMediaId("bare-item")
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle("Bare Item")
                    .build(),
            )
            .build()

        val compat = MediaItemCompatConverter.toCompat(mediaItem)

        assertEquals("bare-item", compat.mediaId)
        assertEquals(0, compat.flags)
    }
}

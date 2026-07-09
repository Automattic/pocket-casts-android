package au.com.shiftyjelly.pocketcasts.repositories.playback

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ResolveThumbnailStatusTest {

    @Test
    fun `returns null when episode artwork is disabled`() {
        assertNull(resolveThumbnailStatus(embeddedArtworkPath = null, useEpisodeArtwork = false))
        assertNull(resolveThumbnailStatus(embeddedArtworkPath = "/cache/artwork.jpg", useEpisodeArtwork = false))
    }

    @Test
    fun `returns available when artwork was extracted`() {
        assertEquals(
            PodcastEpisode.THUMBNAIL_STATUS_EMBEDDED_AVAILABLE,
            resolveThumbnailStatus(embeddedArtworkPath = "/cache/artwork.jpg", useEpisodeArtwork = true),
        )
    }

    @Test
    fun `returns not available when extraction was attempted but found no artwork`() {
        assertEquals(
            PodcastEpisode.THUMBNAIL_STATUS_EMBEDDED_NOT_AVAILABLE,
            resolveThumbnailStatus(embeddedArtworkPath = null, useEpisodeArtwork = true),
        )
    }
}

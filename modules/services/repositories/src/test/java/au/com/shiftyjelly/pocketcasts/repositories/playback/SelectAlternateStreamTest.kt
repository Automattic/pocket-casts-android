package au.com.shiftyjelly.pocketcasts.repositories.playback

import androidx.media3.common.MimeTypes
import au.com.shiftyjelly.pocketcasts.models.entity.AlternateEnclosureSource
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeAlternateEnclosure
import au.com.shiftyjelly.pocketcasts.models.type.MediaKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SelectAlternateStreamTest {

    @Test
    fun `prefers hls over a progressive video rendition`() {
        val stream = select(listOf(videoEnclosure("video/mp4", MP4_URL), videoEnclosure(MimeTypes.APPLICATION_M3U8, HLS_URL)))

        assertEquals(HLS_URL, stream?.url)
        assertTrue(stream!!.isHls)
    }

    @Test
    fun `falls back to the progressive video rendition when hls is disabled`() {
        val enclosures = listOf(videoEnclosure("video/mp4", MP4_URL), videoEnclosure(MimeTypes.APPLICATION_M3U8, HLS_URL))

        val stream = select(enclosures, isHlsEnabled = false)

        assertEquals(MP4_URL, stream?.url)
        assertEquals("video/mp4", stream?.contentType)
    }

    @Test
    fun `selects the progressive video rendition when there is no hls`() {
        assertEquals(MP4_URL, select(listOf(videoEnclosure("video/mp4", MP4_URL)))?.url)
    }

    @Test
    fun `does not select a progressive video rendition when its flag is off`() {
        assertNull(select(listOf(videoEnclosure("video/mp4", MP4_URL)), isVideoEnclosureEnabled = false))
    }

    @Test
    fun `selects a disabled rendition when the episode has no progressive enclosure to fall back on`() {
        val enclosures = listOf(videoEnclosure("video/mp4", MP4_URL))

        val stream = select(enclosures, isVideoEnclosureEnabled = false, hasProgressiveEnclosure = false)

        assertEquals(MP4_URL, stream?.url)
    }

    @Test
    fun `selects nothing when no enclosure offers video`() {
        assertNull(select(listOf(enclosure("audio/mp3", "https://example.com/episode.mp3"))))
        assertNull(select(emptyList()))
    }

    private fun select(
        enclosures: List<EpisodeAlternateEnclosure>,
        isHlsEnabled: Boolean = true,
        isVideoEnclosureEnabled: Boolean = true,
        hasProgressiveEnclosure: Boolean = true,
    ) = selectAlternateStream(
        enclosures = enclosures,
        isHlsEnabled = isHlsEnabled,
        isVideoEnclosureEnabled = isVideoEnclosureEnabled,
        hasProgressiveEnclosure = hasProgressiveEnclosure,
    )

    private fun enclosure(type: String, uri: String) = EpisodeAlternateEnclosure(
        episodeUuid = "episode-uuid",
        position = 0,
        type = type,
        sources = listOf(AlternateEnclosureSource(uri = uri)),
    )

    private fun videoEnclosure(type: String, uri: String) = enclosure(type, uri).copy(mediaKind = MediaKind.Video)

    private companion object {
        const val HLS_URL = "https://example.com/master.m3u8"
        const val MP4_URL = "https://example.com/episode.mp4"
    }
}

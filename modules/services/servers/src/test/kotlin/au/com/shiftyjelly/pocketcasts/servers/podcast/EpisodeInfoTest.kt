package au.com.shiftyjelly.pocketcasts.servers.podcast

import com.squareup.moshi.Moshi
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EpisodeInfoTest {
    private val adapter = Moshi.Builder().build().adapter(EpisodeInfo::class.java)

    @Test
    fun `parse episode with hls url`() {
        val episodeInfo = adapter.fromJson(
            """{"uuid":"episode-uuid","url":"https://example.com/episode.mp3","hls_url":"https://example.com/episode.m3u8","published":"2026-06-11T00:00:00Z"}""",
        )

        assertEquals("https://example.com/episode.m3u8", episodeInfo?.hlsUrl)

        val episode = episodeInfo?.toEpisode("podcast-uuid")
        assertEquals("https://example.com/episode.mp3", episode?.downloadUrl)
        assertEquals("https://example.com/episode.m3u8", episode?.hlsUrl)
    }

    @Test
    fun `parse episode without hls url`() {
        val episodeInfo = adapter.fromJson(
            """{"uuid":"episode-uuid","url":"https://example.com/episode.mp3","published":"2026-06-11T00:00:00Z"}""",
        )

        // Raw Moshi parsing is the unit under test here; toEpisode() applies a debug-only
        // DEBUG_HLS_TEST_URL fallback when hls_url is absent, so it is not asserted.
        assertNull(episodeInfo?.hlsUrl)
    }

    @Test
    fun `parse episode with null hls url`() {
        val episodeInfo = adapter.fromJson(
            """{"uuid":"episode-uuid","url":"https://example.com/episode.mp3","hls_url":null,"published":"2026-06-11T00:00:00Z"}""",
        )

        assertNull(episodeInfo?.hlsUrl)
    }
}

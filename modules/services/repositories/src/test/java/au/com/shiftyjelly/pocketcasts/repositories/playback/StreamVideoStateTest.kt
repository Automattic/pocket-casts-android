package au.com.shiftyjelly.pocketcasts.repositories.playback

import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Test

class StreamVideoStateTest {

    @Test
    fun `audio episode starts not video`() {
        val episode = createEpisode(fileType = "audio/mpeg")

        assertEquals(StreamVideoState.NotVideo, StreamVideoState.initialFor(episode, audioOnly = false))
    }

    @Test
    fun `audio episode ignores audio only`() {
        val episode = createEpisode(fileType = "audio/mpeg")

        assertEquals(StreamVideoState.NotVideo, StreamVideoState.initialFor(episode, audioOnly = true))
    }

    @Test
    fun `video episode starts not video so its own flag decides`() {
        val episode = createEpisode(fileType = "video/mp4")

        assertEquals(StreamVideoState.NotVideo, StreamVideoState.initialFor(episode, audioOnly = false))
    }

    @Test
    fun `audio only forces a video episode to audio`() {
        val episode = createEpisode(fileType = "video/mp4")

        assertEquals(StreamVideoState.AudioOnly, StreamVideoState.initialFor(episode, audioOnly = true))
    }

    @Test
    fun `hls stream starts unknown`() {
        val episode = createHlsEpisode()

        assertEquals(StreamVideoState.Unknown, StreamVideoState.initialFor(episode, audioOnly = false))
    }

    @Test
    fun `audio only forces an hls stream to audio`() {
        val episode = createHlsEpisode()

        assertEquals(StreamVideoState.AudioOnly, StreamVideoState.initialFor(episode, audioOnly = true))
    }

    private fun createEpisode(fileType: String) = PodcastEpisode(
        uuid = "episode-uuid",
        publishedDate = Date(),
        downloadUrl = "https://example.com/episode.mp3",
        fileType = fileType,
    )

    private fun createHlsEpisode() = createEpisode(fileType = "audio/mpeg").apply {
        overrideStreamUrl = "https://example.com/episode.m3u8"
        overrideStreamContentType = "application/x-mpegURL"
    }
}

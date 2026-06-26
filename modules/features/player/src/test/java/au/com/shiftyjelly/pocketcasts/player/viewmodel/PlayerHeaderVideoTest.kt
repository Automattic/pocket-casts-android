package au.com.shiftyjelly.pocketcasts.player.viewmodel

import au.com.shiftyjelly.pocketcasts.models.converter.SafeDate
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel.PlayerHeader
import au.com.shiftyjelly.pocketcasts.repositories.playback.SelectedStream
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerHeaderVideoTest {

    private val audioEpisode = PodcastEpisode(uuid = "uuid", publishedDate = SafeDate(), fileType = "audio/mpeg")
    private val videoEpisode = PodcastEpisode(uuid = "uuid", publishedDate = SafeDate(), fileType = "video/mp4")

    @Test
    fun `a stream found to carry video is shown on the video surface`() {
        val header = PlayerHeader(episode = audioEpisode, streamHasVideo = true)
        assertTrue(header.isVideo)
        assertTrue(header.isVideoVisible())
    }

    @Test
    fun `a selected video stream is video even before its tracks are known`() {
        val header = PlayerHeader(
            episode = audioEpisode,
            selectedStream = SelectedStream("https://example.com/video.mp4", "video/mp4"),
        )
        assertTrue(header.isVideo)
    }

    @Test
    fun `a selected audio stream over a video episode is not video`() {
        val header = PlayerHeader(
            episode = videoEpisode,
            selectedStream = SelectedStream("https://example.com/audio.mp3", "audio/mpeg"),
            streamHasVideo = false,
        )
        assertFalse(header.isVideo)
    }

    @Test
    fun `a video file with no selected stream is video`() {
        assertTrue(PlayerHeader(episode = videoEpisode).isVideo)
    }

    @Test
    fun `an audio file with no selected stream is not video`() {
        assertFalse(PlayerHeader(episode = audioEpisode).isVideo)
    }

    @Test
    fun `video is never shown on the surface during remote playback`() {
        val header = PlayerHeader(episode = audioEpisode, streamHasVideo = true, isPlaybackRemote = true)
        assertTrue(header.isVideo)
        assertFalse(header.isVideoVisible())
    }
}

package au.com.shiftyjelly.pocketcasts.player.viewmodel

import au.com.shiftyjelly.pocketcasts.models.converter.SafeDate
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel.PlayerHeader
import au.com.shiftyjelly.pocketcasts.repositories.playback.StreamVideoState
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PlayerHeaderVideoTest {

    private val audioEpisode = PodcastEpisode(uuid = "uuid", publishedDate = SafeDate(), fileType = "audio/mpeg")
    private val videoEpisode = PodcastEpisode(uuid = "uuid", publishedDate = SafeDate(), fileType = "video/mp4")

    @Test
    fun `a stream found to carry video is shown on the video surface`() {
        val header = PlayerHeader(episode = audioEpisode, streamVideoState = StreamVideoState.HasVideo)
        assertTrue(header.isVideo)
        assertTrue(header.isVideoVisible())
    }

    @Test
    fun `an unresolved hls stream stays on artwork until a video track is detected`() {
        val header = PlayerHeader(episode = audioEpisode, streamVideoState = StreamVideoState.Unknown)
        assertFalse(header.isVideo)
    }

    @Test
    fun `a stream resolved to audio-only falls back to artwork`() {
        val header = PlayerHeader(episode = audioEpisode, streamVideoState = StreamVideoState.AudioOnly)
        assertFalse(header.isVideo)
    }

    @Test
    fun `a video file with no stream override is video`() {
        assertTrue(PlayerHeader(episode = videoEpisode).isVideo)
    }

    @Test
    fun `an audio file with no stream override is not video`() {
        assertFalse(PlayerHeader(episode = audioEpisode).isVideo)
    }

    @Test
    fun `video is never shown on the surface during remote playback`() {
        val header = PlayerHeader(episode = audioEpisode, streamVideoState = StreamVideoState.HasVideo, isPlaybackRemote = true)
        assertTrue(header.isVideo)
        assertFalse(header.isVideoVisible())
    }
}

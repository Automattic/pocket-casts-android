package au.com.shiftyjelly.pocketcasts.repositories.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoTrackSelectionTest {

    @Test
    fun `disables video without a surface for progressive playback`() {
        assertTrue(shouldDisableVideoTrack(audioOnly = false, hasVideoSurface = false, isHlsStream = false))
    }

    @Test
    fun `keeps video when a surface is attached`() {
        assertFalse(shouldDisableVideoTrack(audioOnly = false, hasVideoSurface = true, isHlsStream = false))
    }

    @Test
    fun `audio only setting always disables video`() {
        assertTrue(shouldDisableVideoTrack(audioOnly = true, hasVideoSurface = true, isHlsStream = false))
        assertTrue(shouldDisableVideoTrack(audioOnly = true, hasVideoSurface = false, isHlsStream = true))
    }

    @Test
    fun `keeps video for hls streams without a surface`() {
        assertFalse(shouldDisableVideoTrack(audioOnly = false, hasVideoSurface = false, isHlsStream = true))
    }
}

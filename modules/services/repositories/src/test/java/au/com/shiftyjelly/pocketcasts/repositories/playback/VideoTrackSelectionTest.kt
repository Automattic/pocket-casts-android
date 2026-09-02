package au.com.shiftyjelly.pocketcasts.repositories.playback

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoTrackSelectionTest {

    @Test
    fun `disables video without a surface for progressive playback`() {
        assertTrue(shouldDisableVideoTrack(audioOnly = false, hasVideoSurface = false, isVideoStream = false))
    }

    @Test
    fun `keeps video when a surface is attached`() {
        assertFalse(shouldDisableVideoTrack(audioOnly = false, hasVideoSurface = true, isVideoStream = false))
    }

    @Test
    fun `audio only setting always disables video`() {
        assertTrue(shouldDisableVideoTrack(audioOnly = true, hasVideoSurface = true, isVideoStream = false))
        assertTrue(shouldDisableVideoTrack(audioOnly = true, hasVideoSurface = false, isVideoStream = true))
    }

    @Test
    fun `keeps video for a resolved video stream without a surface so it can be detected`() {
        assertFalse(shouldDisableVideoTrack(audioOnly = false, hasVideoSurface = false, isVideoStream = true))
    }
}

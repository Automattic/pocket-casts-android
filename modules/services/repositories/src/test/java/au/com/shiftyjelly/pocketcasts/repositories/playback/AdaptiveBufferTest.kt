package au.com.shiftyjelly.pocketcasts.repositories.playback

import androidx.media3.exoplayer.DefaultLoadControl
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class AdaptiveBufferTest {

    @Test
    fun `downloaded episode returns ExoPlayer default regardless of flag and network`() {
        val result = computeMaxBufferMs(
            isStreaming = false,
            useReducedBuffer = false,
            isAdaptiveBufferEnabled = true,
            isUnmeteredNetwork = true,
        )
        assertEquals(DefaultLoadControl.DEFAULT_MAX_BUFFER_MS, result)
    }

    @Test
    fun `streaming with reduced buffer returns 2 minutes regardless of flag and network`() {
        val result = computeMaxBufferMs(
            isStreaming = true,
            useReducedBuffer = true,
            isAdaptiveBufferEnabled = true,
            isUnmeteredNetwork = true,
        )
        assertEquals(TimeUnit.MINUTES.toMillis(2).toInt(), result)
    }

    @Test
    fun `streaming with flag disabled and unmetered network returns 4 minutes`() {
        val result = computeMaxBufferMs(
            isStreaming = true,
            useReducedBuffer = false,
            isAdaptiveBufferEnabled = false,
            isUnmeteredNetwork = true,
        )
        assertEquals(TimeUnit.MINUTES.toMillis(4).toInt(), result)
    }

    @Test
    fun `streaming with flag enabled and metered network returns 4 minutes`() {
        val result = computeMaxBufferMs(
            isStreaming = true,
            useReducedBuffer = false,
            isAdaptiveBufferEnabled = true,
            isUnmeteredNetwork = false,
        )
        assertEquals(TimeUnit.MINUTES.toMillis(4).toInt(), result)
    }

    @Test
    fun `streaming with flag enabled and unmetered network returns 5 minutes`() {
        val result = computeMaxBufferMs(
            isStreaming = true,
            useReducedBuffer = false,
            isAdaptiveBufferEnabled = true,
            isUnmeteredNetwork = true,
        )
        assertEquals(TimeUnit.MINUTES.toMillis(5).toInt(), result)
    }

    @Test
    fun `streaming with flag disabled and metered network returns 4 minutes`() {
        val result = computeMaxBufferMs(
            isStreaming = true,
            useReducedBuffer = false,
            isAdaptiveBufferEnabled = false,
            isUnmeteredNetwork = false,
        )
        assertEquals(TimeUnit.MINUTES.toMillis(4).toInt(), result)
    }
}

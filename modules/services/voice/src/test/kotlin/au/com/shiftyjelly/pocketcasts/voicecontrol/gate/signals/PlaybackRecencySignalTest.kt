package au.com.shiftyjelly.pocketcasts.voicecontrol.gate.signals

import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.voicecontrol.playback.PlaybackContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class PlaybackRecencySignalTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @Test
    fun `initially not recent`() {
        val contextFlow = MutableStateFlow<PlaybackContext>(PlaybackContext.Inactive)
        val signal = PlaybackRecencySignal(contextFlow, PlaybackRecencySignal.RECENCY_TIMEOUT_MS)
        assertFalse(signal.isRecent.value)
    }

    @Test
    fun `recent when actively playing`() {
        val contextFlow = MutableStateFlow<PlaybackContext>(PlaybackContext.Inactive)
        val signal = PlaybackRecencySignal(contextFlow, PlaybackRecencySignal.RECENCY_TIMEOUT_MS)
        contextFlow.value = PlaybackContext.Active(
            currentEpisodeUuid = "ep1",
            isPlaying = true,
        )
        assertTrue(signal.isRecent.value)
    }

    @Test
    fun `recent within timeout after pausing`() = runTest {
        val contextFlow = MutableStateFlow<PlaybackContext>(PlaybackContext.Inactive)
        val signal = PlaybackRecencySignal(contextFlow, timeoutMs = 100L)
        contextFlow.value = PlaybackContext.Active(
            currentEpisodeUuid = "ep1",
            isPlaying = true,
        )
        assertTrue(signal.isRecent.value)
        contextFlow.value = PlaybackContext.Active(
            currentEpisodeUuid = "ep1",
            isPlaying = false,
        )
        delay(50L)
        assertTrue(signal.isRecent.value)
    }

    @Test
    fun `not recent after timeout when paused`() = runTest {
        val contextFlow = MutableStateFlow<PlaybackContext>(PlaybackContext.Inactive)
        val signal = PlaybackRecencySignal(contextFlow, timeoutMs = 100L)
        contextFlow.value = PlaybackContext.Active(
            currentEpisodeUuid = "ep1",
            isPlaying = true,
        )
        assertTrue(signal.isRecent.value)
        contextFlow.value = PlaybackContext.Active(
            currentEpisodeUuid = "ep1",
            isPlaying = false,
        )
        delay(150L)
        assertFalse(signal.isRecent.value)
    }

    @Test
    fun `recent within timeout after becoming inactive`() = runTest {
        val contextFlow = MutableStateFlow<PlaybackContext>(PlaybackContext.Inactive)
        val signal = PlaybackRecencySignal(contextFlow, timeoutMs = 100L)
        contextFlow.value = PlaybackContext.Active(
            currentEpisodeUuid = "ep1",
            isPlaying = true,
        )
        assertTrue(signal.isRecent.value)
        contextFlow.value = PlaybackContext.Inactive
        delay(50L)
        assertTrue(signal.isRecent.value)
    }

    @Test
    fun `not recent after timeout when inactive`() = runTest {
        val contextFlow = MutableStateFlow<PlaybackContext>(PlaybackContext.Inactive)
        val signal = PlaybackRecencySignal(contextFlow, timeoutMs = 100L)
        contextFlow.value = PlaybackContext.Active(
            currentEpisodeUuid = "ep1",
            isPlaying = true,
        )
        assertTrue(signal.isRecent.value)
        contextFlow.value = PlaybackContext.Inactive
        delay(150L)
        assertFalse(signal.isRecent.value)
    }

    @Test
    fun `replaying resets timer`() = runTest {
        val contextFlow = MutableStateFlow<PlaybackContext>(PlaybackContext.Inactive)
        val signal = PlaybackRecencySignal(contextFlow, timeoutMs = 200L)
        contextFlow.value = PlaybackContext.Active(
            currentEpisodeUuid = "ep1",
            isPlaying = true,
        )
        assertTrue(signal.isRecent.value)

        // Pause
        contextFlow.value = PlaybackContext.Active(
            currentEpisodeUuid = "ep1",
            isPlaying = false,
        )
        delay(150L)
        assertTrue(signal.isRecent.value) // still within 200ms window

        // Resume playback — timer should reset
        contextFlow.value = PlaybackContext.Active(
            currentEpisodeUuid = "ep1",
            isPlaying = true,
        )
        assertTrue(signal.isRecent.value)

        // Pause again — timer restarts from now
        contextFlow.value = PlaybackContext.Active(
            currentEpisodeUuid = "ep1",
            isPlaying = false,
        )
        delay(150L)
        assertTrue(signal.isRecent.value) // within 200ms of second pause
        delay(60L)
        assertFalse(signal.isRecent.value) // 210ms after second pause
    }
}

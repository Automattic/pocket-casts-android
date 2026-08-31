package au.com.shiftyjelly.pocketcasts.voicecontrol.gate.signals

import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class GracePeriodSignalTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @Test
    fun `initially inactive`() {
        val signal = GracePeriodSignal()
        assertFalse(signal.isActive.value)
    }

    @Test
    fun `active after recognized command`() = runTest {
        val signal = GracePeriodSignal(timeoutMs = 100L)
        signal.onCommandRecognized()
        assertTrue(signal.isActive.value)
    }

    @Test
    fun `expires after timeout`() = runTest {
        val signal = GracePeriodSignal(timeoutMs = 100L)
        signal.onCommandRecognized()
        assertTrue(signal.isActive.value)
        delay(150L)
        assertFalse(signal.isActive.value)
    }

    @Test
    fun `command resets timer`() = runTest {
        val signal = GracePeriodSignal(timeoutMs = 200L)
        signal.onCommandRecognized()
        delay(150L)
        signal.onCommandRecognized() // reset
        delay(150L)
        assertTrue(signal.isActive.value) // still active
        delay(60L)
        assertFalse(signal.isActive.value) // 210ms after last command
    }

    @Test
    fun `active after wake word detection`() = runTest {
        val signal = GracePeriodSignal(timeoutMs = 100L)
        signal.onWakeWordDetected()
        assertTrue(signal.isActive.value)
    }

    @Test
    fun `wake word detection expires after timeout`() = runTest {
        val signal = GracePeriodSignal(timeoutMs = 100L)
        signal.onWakeWordDetected()
        assertTrue(signal.isActive.value)
        delay(150L)
        assertFalse(signal.isActive.value)
    }

    @Test
    fun `wake word resets the same timer as command`() = runTest {
        val signal = GracePeriodSignal(timeoutMs = 200L)
        signal.onCommandRecognized()
        delay(150L)
        signal.onWakeWordDetected() // resets the shared timer
        delay(150L)
        assertTrue(signal.isActive.value) // still active
        delay(60L)
        assertFalse(signal.isActive.value) // 210ms after wake word
    }

    @Test
    fun `route change breaks grace period`() = runTest {
        val signal = GracePeriodSignal(timeoutMs = 200L)
        signal.onCommandRecognized()
        assertTrue(signal.isActive.value)
        signal.onAudioRouteChanged()
        assertFalse(signal.isActive.value)
    }

    @Test
    fun `app switch breaks grace period`() = runTest {
        val signal = GracePeriodSignal(timeoutMs = 200L)
        signal.onCommandRecognized()
        assertTrue(signal.isActive.value)
        signal.onAppBackgrounded()
        assertFalse(signal.isActive.value)
    }
}

package au.com.shiftyjelly.pocketcasts.voicecontrol.gate.signals

import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class AttendedSignalTest {

    @get:Rule
    val mainCoroutineRule = MainCoroutineRule()

    @Test
    fun `initially unattended`() {
        val signal = AttendedSignal(timeoutMs = 30_000L)
        assertFalse(signal.isAttended.value)
    }

    @Test
    fun `attended after touch`() = runTest {
        val signal = AttendedSignal(timeoutMs = 30_000L)
        signal.onUserInteraction()
        assertTrue(signal.isAttended.value)
    }

    @Test
    fun `unattended after timeout expires`() = runTest {
        val signal = AttendedSignal(timeoutMs = 100L)
        signal.onUserInteraction()
        assertTrue(signal.isAttended.value)
        delay(150L)
        assertFalse(signal.isAttended.value)
    }

    @Test
    fun `touch resets timer`() = runTest {
        val signal = AttendedSignal(timeoutMs = 200L)
        signal.onUserInteraction()
        assertTrue(signal.isAttended.value)
        delay(150L)
        signal.onUserInteraction() // reset
        delay(150L)
        assertTrue(signal.isAttended.value) // still within window
        delay(60L)
        assertFalse(signal.isAttended.value) // 210ms after last touch
    }
}

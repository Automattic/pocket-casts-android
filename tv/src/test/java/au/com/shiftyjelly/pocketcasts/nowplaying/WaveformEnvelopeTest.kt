package au.com.shiftyjelly.pocketcasts.nowplaying

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WaveformEnvelopeTest {

    private val envelope = WaveformEnvelope()

    @Test
    fun `starts at zero and settled`() {
        assertEquals(0f, envelope.valueAt(0), 0f)
        assertTrue(envelope.isSettledAt(0))
    }

    @Test
    fun `fades in with an ease in out curve`() {
        envelope.fadeTo(1f, 0)
        assertEquals(0f, envelope.valueAt(0), 0f)
        assertEquals(0.125f, envelope.valueAt(500.msNanos), 1e-4f)
        assertEquals(0.5f, envelope.valueAt(1_000.msNanos), 1e-4f)
        assertEquals(0.875f, envelope.valueAt(1_500.msNanos), 1e-4f)
        assertEquals(1f, envelope.valueAt(2_000.msNanos), 0f)
    }

    @Test
    fun `holds the target value after the fade completes`() {
        envelope.fadeTo(1f, 0)
        assertEquals(1f, envelope.valueAt(10_000.msNanos), 0f)
    }

    @Test
    fun `retargets mid-fade without jumping`() {
        envelope.fadeTo(1f, 0)
        envelope.fadeTo(0f, 1_000.msNanos)
        assertEquals(0.5f, envelope.valueAt(1_000.msNanos), 1e-4f)
        assertEquals(0.25f, envelope.valueAt(2_000.msNanos), 1e-4f)
        assertEquals(0f, envelope.valueAt(3_000.msNanos), 1e-4f)
    }

    @Test
    fun `snaps to the target immediately and settled`() {
        envelope.fadeTo(1f, 0)
        envelope.snapTo(0f, 1_000.msNanos)
        assertEquals(0f, envelope.valueAt(1_000.msNanos), 0f)
        assertTrue(envelope.isSettledAt(1_000.msNanos))
    }

    @Test
    fun `settles only after the fade duration plus margin`() {
        envelope.fadeTo(1f, 0)
        assertFalse(envelope.isSettledAt(2_000.msNanos))
        assertTrue(envelope.isSettledAt(2_100.msNanos))
    }

    private val Int.msNanos: Long get() = this * 1_000_000L
}

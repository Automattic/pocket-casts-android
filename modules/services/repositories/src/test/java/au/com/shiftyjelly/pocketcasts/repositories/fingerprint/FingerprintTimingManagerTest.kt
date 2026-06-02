package au.com.shiftyjelly.pocketcasts.repositories.fingerprint

import au.com.shiftyjelly.pocketcasts.repositories.fingerprint.FingerprintTimingManager.TimeMappingEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class FingerprintTimingManagerTest {

    @Test
    fun `interpolate returns null for empty list`() {
        val result = FingerprintTimingManager.interpolate(
            time = 5.0,
            entries = emptyList(),
            keySelector = { it.playbackTime },
            valueSelector = { it.referenceTime },
        )
        assertNull(result)
    }

    @Test
    fun `interpolate extrapolates before first entry`() {
        val entries = listOf(
            TimeMappingEntry(playbackTime = 10.0, referenceTime = 20.0),
        )
        val result = FingerprintTimingManager.interpolate(
            time = 5.0,
            entries = entries,
            keySelector = { it.playbackTime },
            valueSelector = { it.referenceTime },
        )
        assertEquals(15.0, result!!, 0.001)
    }

    @Test
    fun `interpolate extrapolates after last entry`() {
        val entries = listOf(
            TimeMappingEntry(playbackTime = 10.0, referenceTime = 20.0),
        )
        val result = FingerprintTimingManager.interpolate(
            time = 15.0,
            entries = entries,
            keySelector = { it.playbackTime },
            valueSelector = { it.referenceTime },
        )
        assertEquals(25.0, result!!, 0.001)
    }

    @Test
    fun `interpolate returns exact value at entry`() {
        val entries = listOf(
            TimeMappingEntry(playbackTime = 10.0, referenceTime = 20.0),
            TimeMappingEntry(playbackTime = 20.0, referenceTime = 40.0),
        )
        val result = FingerprintTimingManager.interpolate(
            time = 10.0,
            entries = entries,
            keySelector = { it.playbackTime },
            valueSelector = { it.referenceTime },
        )
        assertEquals(20.0, result!!, 0.001)
    }

    @Test
    fun `interpolate linearly between two entries`() {
        val entries = listOf(
            TimeMappingEntry(playbackTime = 10.0, referenceTime = 20.0),
            TimeMappingEntry(playbackTime = 20.0, referenceTime = 40.0),
        )
        val result = FingerprintTimingManager.interpolate(
            time = 15.0,
            entries = entries,
            keySelector = { it.playbackTime },
            valueSelector = { it.referenceTime },
        )
        assertEquals(30.0, result!!, 0.001)
    }

    @Test
    fun `interpolate handles single entry`() {
        val entries = listOf(
            TimeMappingEntry(playbackTime = 10.0, referenceTime = 20.0),
        )
        val result = FingerprintTimingManager.interpolate(
            time = 10.0,
            entries = entries,
            keySelector = { it.playbackTime },
            valueSelector = { it.referenceTime },
        )
        assertEquals(20.0, result!!, 0.001)
    }

    @Test
    fun `interpolate uses binary search for many entries`() {
        val entries = (0..100).map {
            TimeMappingEntry(playbackTime = it.toDouble(), referenceTime = it * 2.0)
        }
        val result = FingerprintTimingManager.interpolate(
            time = 50.5,
            entries = entries,
            keySelector = { it.playbackTime },
            valueSelector = { it.referenceTime },
        )
        assertEquals(101.0, result!!, 0.001)
    }

    @Test
    fun `alignToWindowGrid aligns to stride boundary`() {
        val aligned = FingerprintTimingManager.alignToWindowGrid(5.7)
        assertEquals(4.0, aligned, 0.001)
    }

    @Test
    fun `alignToWindowGrid clamps negative to zero`() {
        val aligned = FingerprintTimingManager.alignToWindowGrid(-1.0)
        assertEquals(0.0, aligned, 0.001)
    }

    @Test
    fun `alignToWindowGrid zero stays zero`() {
        val aligned = FingerprintTimingManager.alignToWindowGrid(0.0)
        assertEquals(0.0, aligned, 0.001)
    }
}

package au.com.shiftyjelly.pocketcasts.repositories.fingerprint

import au.com.shiftyjelly.pocketcasts.repositories.fingerprint.FingerprintTimingManager.TimeMappingEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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
    fun `computeEager runs for downloaded episode with generated chapters`() {
        val eager = FingerprintTimingManager.computeEager(
            hasGeneratedChapters = true,
            isDownloaded = true,
            isUnmetered = { false },
        )
        assertTrue(eager)
    }

    @Test
    fun `computeEager runs for streaming episode on unmetered network`() {
        val eager = FingerprintTimingManager.computeEager(
            hasGeneratedChapters = true,
            isDownloaded = false,
            isUnmetered = { true },
        )
        assertTrue(eager)
    }

    @Test
    fun `computeEager skips streaming episode on metered network`() {
        val eager = FingerprintTimingManager.computeEager(
            hasGeneratedChapters = true,
            isDownloaded = false,
            isUnmetered = { false },
        )
        assertFalse(eager)
    }

    @Test
    fun `computeEager skips episode without generated chapters`() {
        val eager = FingerprintTimingManager.computeEager(
            hasGeneratedChapters = false,
            isDownloaded = true,
            isUnmetered = { true },
        )
        assertFalse(eager)
    }

    @Test
    fun `computeEager does not query network for downloaded episode`() {
        var queriedNetwork = false
        FingerprintTimingManager.computeEager(
            hasGeneratedChapters = true,
            isDownloaded = true,
            isUnmetered = { queriedNetwork = true; true },
        )
        assertFalse(queriedNetwork)
    }

    @Test
    fun `alignToWindowGrid aligns to stride boundary`() {
        val aligned = FingerprintTimingManager.alignToWindowGrid(5.7)
        assertEquals(5.0, aligned, 0.001)
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

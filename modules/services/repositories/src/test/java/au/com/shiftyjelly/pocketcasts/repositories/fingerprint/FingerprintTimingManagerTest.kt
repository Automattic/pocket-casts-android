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
        )
        assertTrue(eager)
    }

    @Test
    fun `computeEager skips streaming episode`() {
        // Streaming episodes never eager-decode the whole file; the PCM tap plus the bounded
        // on-demand resolver cover them without pulling the whole file.
        val eager = FingerprintTimingManager.computeEager(
            hasGeneratedChapters = true,
            isDownloaded = false,
        )
        assertFalse(eager)
    }

    @Test
    fun `computeEager skips episode without generated chapters`() {
        val eager = FingerprintTimingManager.computeEager(
            hasGeneratedChapters = false,
            isDownloaded = true,
        )
        assertFalse(eager)
    }

    @Test
    fun `shouldBlockOnDemandResolve never blocks downloaded episodes`() {
        val blocked = FingerprintTimingManager.shouldBlockOnDemandResolve(
            isDownloaded = true,
            warnOnMeteredNetwork = true,
            isUnmetered = { error("network should not be queried") },
        )
        assertFalse(blocked)
    }

    @Test
    fun `shouldBlockOnDemandResolve allows streaming on metered network by default`() {
        val blocked = FingerprintTimingManager.shouldBlockOnDemandResolve(
            isDownloaded = false,
            warnOnMeteredNetwork = false,
            isUnmetered = { false },
        )
        assertFalse(blocked)
    }

    @Test
    fun `shouldBlockOnDemandResolve blocks streaming on metered network when user warns on data use`() {
        val blocked = FingerprintTimingManager.shouldBlockOnDemandResolve(
            isDownloaded = false,
            warnOnMeteredNetwork = true,
            isUnmetered = { false },
        )
        assertTrue(blocked)
    }

    @Test
    fun `shouldBlockOnDemandResolve never blocks on unmetered network`() {
        val blocked = FingerprintTimingManager.shouldBlockOnDemandResolve(
            isDownloaded = false,
            warnOnMeteredNetwork = true,
            isUnmetered = { true },
        )
        assertFalse(blocked)
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

    @Test
    fun `searchWindow cold searches forward from reference time`() {
        val window = FingerprintTimingManager.searchWindow(referenceTimeSec = 600.0, estimatedPlaybackSec = null)
        assertEquals(600.0, window.startSec, 0.001)
        assertEquals(600.0 + FingerprintConstants.ON_DEMAND_COLD_BUDGET_SECONDS, window.endSec, 0.001)
    }

    @Test
    fun `searchWindow warm brackets the estimate within budgets`() {
        val window = FingerprintTimingManager.searchWindow(referenceTimeSec = 600.0, estimatedPlaybackSec = 900.0)
        assertEquals(900.0 - FingerprintConstants.ON_DEMAND_BACKWARD_MAX_SECONDS, window.startSec, 0.001)
        assertEquals(900.0 + FingerprintConstants.ON_DEMAND_FORWARD_BUDGET_SECONDS, window.endSec, 0.001)
    }

    @Test
    fun `searchWindow warm never starts below reference time`() {
        val window = FingerprintTimingManager.searchWindow(referenceTimeSec = 600.0, estimatedPlaybackSec = 650.0)
        assertEquals(600.0, window.startSec, 0.001)
        assertEquals(650.0 + FingerprintConstants.ON_DEMAND_FORWARD_BUDGET_SECONDS, window.endSec, 0.001)
    }

    @Test
    fun `searchWindow warm keeps forward budget when estimate is below reference time`() {
        val window = FingerprintTimingManager.searchWindow(referenceTimeSec = 600.0, estimatedPlaybackSec = 300.0)
        assertEquals(600.0, window.startSec, 0.001)
        assertEquals(600.0 + FingerprintConstants.ON_DEMAND_FORWARD_BUDGET_SECONDS, window.endSec, 0.001)
    }

    @Test
    fun `isResolveTargetCovered requires minimum anchors`() {
        val acc = MappingAccumulator()
        acc.insert(TimeMappingEntry(playbackTime = 650.0, referenceTime = 620.0))
        assertFalse(FingerprintTimingManager.isResolveTargetCovered(acc, targetReferenceSec = 600.0))
    }

    @Test
    fun `isResolveTargetCovered requires reference past target with margin`() {
        val acc = MappingAccumulator()
        acc.insert(TimeMappingEntry(playbackTime = 620.0, referenceTime = 590.0))
        acc.insert(TimeMappingEntry(playbackTime = 632.0, referenceTime = 602.0))
        assertFalse(FingerprintTimingManager.isResolveTargetCovered(acc, targetReferenceSec = 600.0))
    }

    @Test
    fun `isResolveTargetCovered stops once anchors bracket the target`() {
        val acc = MappingAccumulator()
        acc.insert(TimeMappingEntry(playbackTime = 620.0, referenceTime = 590.0))
        acc.insert(TimeMappingEntry(playbackTime = 636.0, referenceTime = 606.0))
        assertTrue(FingerprintTimingManager.isResolveTargetCovered(acc, targetReferenceSec = 600.0))
    }

    @Test
    fun `densePlaybackSec interpolates between close anchors`() {
        val entries = listOf(
            TimeMappingEntry(playbackTime = 130.0, referenceTime = 100.0),
            TimeMappingEntry(playbackTime = 134.0, referenceTime = 104.0),
        )
        val result = FingerprintTimingManager.densePlaybackSec(referenceTimeSec = 102.0, entries = entries)
        assertEquals(132.0, result!!, 0.001)
    }

    @Test
    fun `densePlaybackSec returns null outside the mapped range`() {
        val entries = listOf(
            TimeMappingEntry(playbackTime = 130.0, referenceTime = 100.0),
            TimeMappingEntry(playbackTime = 134.0, referenceTime = 104.0),
        )
        assertNull(FingerprintTimingManager.densePlaybackSec(referenceTimeSec = 99.0, entries = entries))
        assertNull(FingerprintTimingManager.densePlaybackSec(referenceTimeSec = 104.0 + FingerprintConstants.TAP_TRAILING_GRACE_SECONDS + 1.0, entries = entries))
    }

    @Test
    fun `densePlaybackSec extrapolates within the trailing grace past the last anchor`() {
        val entries = listOf(
            TimeMappingEntry(playbackTime = 130.0, referenceTime = 100.0),
            TimeMappingEntry(playbackTime = 134.0, referenceTime = 104.0),
        )
        val result = FingerprintTimingManager.densePlaybackSec(referenceTimeSec = 110.0, entries = entries)
        assertEquals(140.0, result!!, 0.001)
    }

    @Test
    fun `densePlaybackSec returns null when reference gap is too wide`() {
        val entries = listOf(
            TimeMappingEntry(playbackTime = 130.0, referenceTime = 100.0),
            TimeMappingEntry(playbackTime = 150.0, referenceTime = 120.0),
        )
        assertNull(FingerprintTimingManager.densePlaybackSec(referenceTimeSec = 110.0, entries = entries))
    }

    @Test
    fun `densePlaybackSec returns null when playback gap spans an ad boundary`() {
        val entries = listOf(
            TimeMappingEntry(playbackTime = 130.0, referenceTime = 100.0),
            TimeMappingEntry(playbackTime = 190.0, referenceTime = 104.0),
        )
        assertNull(FingerprintTimingManager.densePlaybackSec(referenceTimeSec = 102.0, entries = entries))
    }
}

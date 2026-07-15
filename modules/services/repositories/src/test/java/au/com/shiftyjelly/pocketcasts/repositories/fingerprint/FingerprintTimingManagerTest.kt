package au.com.shiftyjelly.pocketcasts.repositories.fingerprint

import au.com.shiftyjelly.pocketcasts.repositories.fingerprint.FingerprintTimingManager.PrepareTrigger
import au.com.shiftyjelly.pocketcasts.repositories.fingerprint.FingerprintTimingManager.ProgressDecision
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
            isUnmetered = {
                queriedNetwork = true
                true
            },
        )
        assertFalse(queriedNetwork)
    }

    @Test
    fun `shouldBlockRemoteFingerprinting never blocks downloaded episodes`() {
        val blocked = FingerprintTimingManager.shouldBlockRemoteFingerprinting(
            isDownloaded = true,
            trigger = PrepareTrigger.PLAYBACK,
            warnOnMeteredNetwork = true,
            isUnmetered = { false },
        )
        assertFalse(blocked)
    }

    @Test
    fun `shouldBlockRemoteFingerprinting does not query network for downloaded episodes`() {
        var queriedNetwork = false
        FingerprintTimingManager.shouldBlockRemoteFingerprinting(
            isDownloaded = true,
            trigger = PrepareTrigger.PLAYBACK,
            warnOnMeteredNetwork = false,
            isUnmetered = {
                queriedNetwork = true
                true
            },
        )
        assertFalse(queriedNetwork)
    }

    @Test
    fun `shouldBlockRemoteFingerprinting never blocks on unmetered network`() {
        val blocked = FingerprintTimingManager.shouldBlockRemoteFingerprinting(
            isDownloaded = false,
            trigger = PrepareTrigger.PLAYBACK,
            warnOnMeteredNetwork = true,
            isUnmetered = { true },
        )
        assertFalse(blocked)
    }

    @Test
    fun `shouldBlockRemoteFingerprinting blocks playback trigger on metered network`() {
        val blocked = FingerprintTimingManager.shouldBlockRemoteFingerprinting(
            isDownloaded = false,
            trigger = PrepareTrigger.PLAYBACK,
            warnOnMeteredNetwork = false,
            isUnmetered = { false },
        )
        assertTrue(blocked)
    }

    @Test
    fun `shouldBlockRemoteFingerprinting blocks bookmark trigger on metered network`() {
        val blocked = FingerprintTimingManager.shouldBlockRemoteFingerprinting(
            isDownloaded = false,
            trigger = PrepareTrigger.BOOKMARK,
            warnOnMeteredNetwork = false,
            isUnmetered = { false },
        )
        assertTrue(blocked)
    }

    @Test
    fun `shouldBlockRemoteFingerprinting allows transcript view on metered network`() {
        val blocked = FingerprintTimingManager.shouldBlockRemoteFingerprinting(
            isDownloaded = false,
            trigger = PrepareTrigger.TRANSCRIPT_VIEW,
            warnOnMeteredNetwork = false,
            isUnmetered = { false },
        )
        assertFalse(blocked)
    }

    @Test
    fun `shouldBlockRemoteFingerprinting blocks transcript view when user warns on data use`() {
        val blocked = FingerprintTimingManager.shouldBlockRemoteFingerprinting(
            isDownloaded = false,
            trigger = PrepareTrigger.TRANSCRIPT_VIEW,
            warnOnMeteredNetwork = true,
            isUnmetered = { false },
        )
        assertTrue(blocked)
    }

    @Test
    fun `decideOnProgress ignores ticks in eager mode`() {
        val decision = decideOnProgress(positionSec = 100.0, lastPositionSec = 10.0, isEager = true)
        assertEquals(ProgressDecision.None, decision)
    }

    @Test
    fun `decideOnProgress small delta does nothing`() {
        val decision = decideOnProgress(positionSec = 15.0, lastPositionSec = 10.0)
        assertEquals(ProgressDecision.None, decision)
    }

    @Test
    fun `decideOnProgress first tick is never a jump`() {
        val decision = decideOnProgress(positionSec = 500.0, lastPositionSec = null)
        assertEquals(ProgressDecision.None, decision)
    }

    @Test
    fun `decideOnProgress jump within mapped run does not restart`() {
        val decision = decideOnProgress(positionSec = 100.0, lastPositionSec = 10.0, mappedRunEndSec = 120.0)
        assertEquals(ProgressDecision.None, decision)
    }

    @Test
    fun `decideOnProgress jump within run cancels decode when streaming and mapped far ahead`() {
        val decision = decideOnProgress(
            positionSec = 100.0,
            lastPositionSec = 10.0,
            mappedRunEndSec = 200.0,
            isDecodeActive = true,
            isStreaming = true,
        )
        assertEquals(ProgressDecision.CancelDecode, decision)
    }

    @Test
    fun `decideOnProgress jump within run keeps decode for local files`() {
        val decision = decideOnProgress(
            positionSec = 100.0,
            lastPositionSec = 10.0,
            mappedRunEndSec = 200.0,
            isDecodeActive = true,
            isStreaming = false,
        )
        assertEquals(ProgressDecision.None, decision)
    }

    @Test
    fun `decideOnProgress jump within run without active decode does not cancel`() {
        val decision = decideOnProgress(
            positionSec = 100.0,
            lastPositionSec = 10.0,
            mappedRunEndSec = 200.0,
            isDecodeActive = false,
            isStreaming = true,
        )
        assertEquals(ProgressDecision.None, decision)
    }

    @Test
    fun `decideOnProgress jump outside mapped range schedules debounced restart`() {
        val decision = decideOnProgress(positionSec = 100.0, lastPositionSec = 10.0)
        assertEquals(ProgressDecision.ScheduleDebouncedRestart, decision)
    }

    @Test
    fun `decideOnProgress jump into mapping gap schedules debounced restart`() {
        val decision = decideOnProgress(
            positionSec = 300.0,
            lastPositionSec = 50.0,
            mappedRunEndSec = null,
            hasAnyMapping = true,
        )
        assertEquals(ProgressDecision.ScheduleDebouncedRestart, decision)
    }

    @Test
    fun `decideOnProgress jump covered by active decode does not restart`() {
        val decision = decideOnProgress(
            positionSec = 300.0,
            lastPositionSec = 50.0,
            hasAnyMapping = true,
            isDecodeActive = true,
            isCoveredByActiveDecode = true,
        )
        assertEquals(ProgressDecision.None, decision)
    }

    @Test
    fun `decideOnProgress pending restart suppresses outside range restart`() {
        val decision = decideOnProgress(
            positionSec = 100.0,
            lastPositionSec = 99.0,
            hasAnyMapping = true,
            hasPendingRestart = true,
        )
        assertEquals(ProgressDecision.None, decision)
    }

    @Test
    fun `decideOnProgress outside mapped range respects bootstrap cooldown`() {
        val decision = decideOnProgress(
            positionSec = 100.0,
            lastPositionSec = 99.0,
            hasAnyMapping = true,
            msSinceStreamStart = FingerprintConstants.STREAM_BOOTSTRAP_COOLDOWN_MS - 1,
        )
        assertEquals(ProgressDecision.None, decision)
    }

    @Test
    fun `decideOnProgress outside mapped range restarts after cooldown`() {
        val decision = decideOnProgress(
            positionSec = 100.0,
            lastPositionSec = 99.0,
            hasAnyMapping = true,
            msSinceStreamStart = FingerprintConstants.STREAM_BOOTSTRAP_COOLDOWN_MS,
        )
        assertEquals(ProgressDecision.RestartOutsideRange, decision)
    }

    @Test
    fun `decideOnProgress outside range covered by active decode does not restart`() {
        val decision = decideOnProgress(
            positionSec = 100.0,
            lastPositionSec = 99.0,
            hasAnyMapping = true,
            isDecodeActive = true,
            isCoveredByActiveDecode = true,
            msSinceStreamStart = FingerprintConstants.STREAM_BOOTSTRAP_COOLDOWN_MS,
        )
        assertEquals(ProgressDecision.None, decision)
    }

    @Test
    fun `decideOnProgress outside range with decode elsewhere still restarts`() {
        val decision = decideOnProgress(
            positionSec = 100.0,
            lastPositionSec = 99.0,
            hasAnyMapping = true,
            isDecodeActive = true,
            isCoveredByActiveDecode = false,
            msSinceStreamStart = FingerprintConstants.STREAM_BOOTSTRAP_COOLDOWN_MS,
        )
        assertEquals(ProgressDecision.RestartOutsideRange, decision)
    }

    @Test
    fun `decideOnProgress resumes from run end when the mapping is about to run out`() {
        val decision = decideOnProgress(
            positionSec = 100.0,
            lastPositionSec = 99.0,
            mappedRunEndSec = 130.0,
        )
        assertEquals(ProgressDecision.RestartFromRunEnd(130.0), decision)
    }

    @Test
    fun `decideOnProgress does not resume while the run end is far ahead`() {
        val decision = decideOnProgress(
            positionSec = 100.0,
            lastPositionSec = 99.0,
            mappedRunEndSec = 100.0 + FingerprintConstants.LOOKAHEAD_SECONDS,
        )
        assertEquals(ProgressDecision.None, decision)
    }

    @Test
    fun `decideOnProgress does not resume when a decode already covers the run end`() {
        val decision = decideOnProgress(
            positionSec = 100.0,
            lastPositionSec = 99.0,
            mappedRunEndSec = 130.0,
            isDecodeActive = true,
            isRunEndCoveredByActiveDecode = true,
        )
        assertEquals(ProgressDecision.None, decision)
    }

    @Test
    fun `decideOnProgress does not resume when the run end is near the episode end`() {
        val decision = decideOnProgress(
            positionSec = 100.0,
            lastPositionSec = 99.0,
            mappedRunEndSec = 130.0,
            isRunEndNearEpisodeEnd = true,
        )
        assertEquals(ProgressDecision.None, decision)
    }

    @Test
    fun `decideOnProgress run end resume respects bootstrap cooldown`() {
        val decision = decideOnProgress(
            positionSec = 100.0,
            lastPositionSec = 99.0,
            mappedRunEndSec = 130.0,
            msSinceStreamStart = FingerprintConstants.STREAM_BOOTSTRAP_COOLDOWN_MS - 1,
        )
        assertEquals(ProgressDecision.None, decision)
    }

    @Test
    fun `decideOnProgress relocates a decode replaying mapped audio to the run end`() {
        val decision = decideOnProgress(
            positionSec = 100.0,
            lastPositionSec = 99.0,
            mappedRunEndSec = 130.0,
            isDecodeActive = true,
            isRunEndCoveredByActiveDecode = false,
            isStreaming = true,
        )
        assertEquals(ProgressDecision.RestartFromRunEnd(130.0), decision)
    }

    @Test
    fun `decideOnProgress empty mapping does not restart before the stream ever ran`() {
        val decision = decideOnProgress(
            positionSec = 100.0,
            lastPositionSec = 99.0,
        )
        assertEquals(ProgressDecision.None, decision)
    }

    @Test
    fun `decideOnProgress empty mapping does not restart while the decode is running`() {
        val decision = decideOnProgress(
            positionSec = 100.0,
            lastPositionSec = 99.0,
            isDecodeActive = true,
            isCoveredByActiveDecode = true,
        )
        assertEquals(ProgressDecision.None, decision)
    }

    @Test
    fun `decideOnProgress empty mapping restarts after the stream died without producing one`() {
        val decision = decideOnProgress(
            positionSec = 100.0,
            lastPositionSec = 99.0,
            hasStreamStarted = true,
        )
        assertEquals(ProgressDecision.RestartOutsideRange, decision)
    }

    @Test
    fun `mappedRunEnd empty mapping returns null`() {
        assertNull(FingerprintTimingManager.mappedRunEnd(50.0, emptyList()))
    }

    @Test
    fun `mappedRunEnd inside contiguous run returns run end`() {
        assertEquals(100.0, FingerprintTimingManager.mappedRunEnd(35.0, entriesAt(0.0, 100.0, step = 10.0))!!, 0.0)
    }

    @Test
    fun `mappedRunEnd on exact anchor returns run end`() {
        assertEquals(100.0, FingerprintTimingManager.mappedRunEnd(40.0, entriesAt(0.0, 100.0, step = 10.0))!!, 0.0)
    }

    @Test
    fun `mappedRunEnd in gap between islands returns null`() {
        val entries = entriesAt(0.0, 100.0, step = 10.0) + entriesAt(500.0, 600.0, step = 10.0)
        assertNull(FingerprintTimingManager.mappedRunEnd(300.0, entries))
    }

    @Test
    fun `mappedRunEnd exactly on a mid-list island end gets no slack`() {
        val entries = entriesAt(0.0, 100.0, step = 10.0) + entriesAt(500.0, 600.0, step = 10.0)
        assertNull(FingerprintTimingManager.mappedRunEnd(100.0, entries))
    }

    @Test
    fun `mappedRunEnd just past island end with far next island returns null`() {
        val entries = entriesAt(0.0, 100.0, step = 10.0) + entriesAt(500.0, 600.0, step = 10.0)
        assertNull(FingerprintTimingManager.mappedRunEnd(110.0, entries))
    }

    @Test
    fun `mappedRunEnd inside first island stops at gap`() {
        val entries = entriesAt(0.0, 100.0, step = 10.0) + entriesAt(500.0, 600.0, step = 10.0)
        assertEquals(100.0, FingerprintTimingManager.mappedRunEnd(50.0, entries)!!, 0.0)
    }

    @Test
    fun `mappedRunEnd within margin after last entry returns last entry`() {
        assertEquals(100.0, FingerprintTimingManager.mappedRunEnd(120.0, entriesAt(0.0, 100.0, step = 10.0))!!, 0.0)
    }

    @Test
    fun `mappedRunEnd beyond margin after last entry returns null`() {
        assertNull(FingerprintTimingManager.mappedRunEnd(131.0, entriesAt(0.0, 100.0, step = 10.0)))
    }

    @Test
    fun `mappedRunEnd within margin before first entry returns run end`() {
        assertEquals(200.0, FingerprintTimingManager.mappedRunEnd(80.0, entriesAt(100.0, 200.0, step = 10.0))!!, 0.0)
    }

    @Test
    fun `mappedRunEnd beyond margin before first entry returns null`() {
        assertNull(FingerprintTimingManager.mappedRunEnd(60.0, entriesAt(100.0, 200.0, step = 10.0)))
    }

    private fun entriesAt(from: Double, to: Double, step: Double): List<TimeMappingEntry> {
        val entries = mutableListOf<TimeMappingEntry>()
        var time = from
        while (time <= to) {
            entries.add(TimeMappingEntry(playbackTime = time, referenceTime = time))
            time += step
        }
        return entries
    }

    private fun decideOnProgress(
        positionSec: Double,
        lastPositionSec: Double?,
        isEager: Boolean = false,
        mappedRunEndSec: Double? = null,
        hasAnyMapping: Boolean = mappedRunEndSec != null,
        isDecodeActive: Boolean = false,
        isCoveredByActiveDecode: Boolean = false,
        isRunEndCoveredByActiveDecode: Boolean = false,
        isRunEndNearEpisodeEnd: Boolean = false,
        hasPendingRestart: Boolean = false,
        hasStreamStarted: Boolean = hasAnyMapping || isDecodeActive,
        msSinceStreamStart: Long = FingerprintConstants.STREAM_BOOTSTRAP_COOLDOWN_MS,
        isStreaming: Boolean = false,
    ): ProgressDecision = FingerprintTimingManager.decideOnProgress(
        positionSec = positionSec,
        lastPositionSec = lastPositionSec,
        isEager = isEager,
        mappedRunEndSec = mappedRunEndSec,
        hasAnyMapping = hasAnyMapping,
        isDecodeActive = isDecodeActive,
        isCoveredByActiveDecode = isCoveredByActiveDecode,
        isRunEndCoveredByActiveDecode = isRunEndCoveredByActiveDecode,
        isRunEndNearEpisodeEnd = isRunEndNearEpisodeEnd,
        hasPendingRestart = hasPendingRestart,
        hasStreamStarted = hasStreamStarted,
        msSinceStreamStart = msSinceStreamStart,
        isStreaming = isStreaming,
    )

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

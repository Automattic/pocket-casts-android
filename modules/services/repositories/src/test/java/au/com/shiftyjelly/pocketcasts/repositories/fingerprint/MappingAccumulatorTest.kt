package au.com.shiftyjelly.pocketcasts.repositories.fingerprint

import au.com.shiftyjelly.pocketcasts.repositories.fingerprint.FingerprintTimingManager.TimeMappingEntry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MappingAccumulatorTest {

    @Test
    fun `replaceAll keeps both lookup directions sorted`() {
        val acc = MappingAccumulator()
        acc.replaceAll(
            listOf(
                TimeMappingEntry(playbackTime = 15.0, referenceTime = 5.0),
                TimeMappingEntry(playbackTime = 5.0, referenceTime = 15.0),
                TimeMappingEntry(playbackTime = 10.0, referenceTime = 10.0),
            ),
        )

        assertEquals(listOf(5.0, 10.0, 15.0), acc.playbackToReference.map { it.playbackTime })
        assertEquals(listOf(5.0, 10.0, 15.0), acc.referenceToPlayback.map { it.referenceTime })
    }

    @Test
    fun `scratch accumulator does not disturb another instance`() {
        val main = MappingAccumulator()
        val bootstrap = listOf(
            TimeMappingEntry(playbackTime = 10.0, referenceTime = 10.0),
            TimeMappingEntry(playbackTime = 12.0, referenceTime = 12.0),
            TimeMappingEntry(playbackTime = 14.0, referenceTime = 14.0),
        )
        bootstrap.forEach { main.consider(it) {} }

        val scratch = MappingAccumulator()
        scratch.consider(TimeMappingEntry(playbackTime = 500.0, referenceTime = 900.0)) {}
        scratch.reset()

        assertEquals(3, main.playbackToReference.size)
        assertEquals(bootstrap, main.playbackToReference)
        assertEquals(bootstrap.last(), main.lastTrusted)
    }

    @Test
    fun `reset clears mapping and filter state`() {
        val acc = MappingAccumulator()
        acc.consider(TimeMappingEntry(playbackTime = 10.0, referenceTime = 10.0)) {}
        acc.replaceAll(listOf(TimeMappingEntry(playbackTime = 1.0, referenceTime = 1.0)))
        acc.lastTrusted = TimeMappingEntry(playbackTime = 1.0, referenceTime = 1.0)

        acc.reset()

        assertTrue(acc.playbackToReference.isEmpty())
        assertTrue(acc.referenceToPlayback.isEmpty())
        assertTrue(acc.candidatePool.isEmpty())
        assertEquals(null, acc.lastTrusted)
    }
}

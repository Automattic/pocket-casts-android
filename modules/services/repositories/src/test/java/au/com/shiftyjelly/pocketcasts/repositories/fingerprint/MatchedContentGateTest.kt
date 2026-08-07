package au.com.shiftyjelly.pocketcasts.repositories.fingerprint

import au.com.shiftyjelly.pocketcasts.repositories.fingerprint.FingerprintTimingManager.TimeMappingEntry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class MatchedContentGateTest {

    private fun entry(playback: Double, reference: Double = playback) = TimeMappingEntry(playbackTime = playback, referenceTime = reference)

    @Test
    fun `matched on densely mapped content`() {
        val entries = (0..30).map { entry(it * 2.0) }
        assertTrue(FingerprintTimingManager.isWithinMatchedContent(30.0, entries))
    }

    @Test
    fun `matched bridges quick gap between anchors`() {
        val entries = listOf(entry(20.0), entry(26.0))
        assertTrue(FingerprintTimingManager.isWithinMatchedContent(23.0, entries))
    }

    @Test
    fun `not matched in wide gap (ad break)`() {
        val entries = listOf(entry(20.0), entry(50.0, reference = 21.0))
        assertFalse(FingerprintTimingManager.isWithinMatchedContent(35.0, entries))
    }

    @Test
    fun `not matched far past last anchor`() {
        val entries = listOf(entry(18.0), entry(20.0))
        assertFalse(FingerprintTimingManager.isWithinMatchedContent(40.0, entries))
    }

    @Test
    fun `matched within trailing grace past last anchor`() {
        val entries = listOf(entry(18.0), entry(20.0))
        assertTrue(FingerprintTimingManager.isWithinMatchedContent(31.0, entries))
    }

    @Test
    fun `not matched beyond trailing grace past last anchor`() {
        val entries = listOf(entry(18.0), entry(20.0))
        assertFalse(FingerprintTimingManager.isWithinMatchedContent(32.1, entries))
    }

    @Test
    fun `not matched within trailing grace when the live edge is unmatched`() {
        val entries = listOf(entry(18.0), entry(20.0))
        assertFalse(FingerprintTimingManager.isWithinMatchedContent(31.0, entries, allowTrailingGrace = false))
    }

    @Test
    fun `interior anchors still match when the trailing grace is dropped`() {
        val entries = listOf(entry(18.0), entry(20.0))
        assertTrue(FingerprintTimingManager.isWithinMatchedContent(19.0, entries, allowTrailingGrace = false))
    }

    @Test
    fun `not matched before first anchor`() {
        val entries = listOf(entry(18.0), entry(20.0))
        assertFalse(FingerprintTimingManager.isWithinMatchedContent(5.0, entries))
    }

    @Test
    fun `not matched with empty mapping`() {
        assertFalse(FingerprintTimingManager.isWithinMatchedContent(10.0, emptyList()))
    }

    @Test
    fun `flips immediately at last anchor before ad`() {
        val entries = listOf(
            entry(12.0),
            entry(14.0),
            entry(44.0, reference = 15.0),
            entry(45.0, reference = 16.0),
        )
        assertTrue(FingerprintTimingManager.isWithinMatchedContent(13.9, entries))
        assertFalse(FingerprintTimingManager.isWithinMatchedContent(14.1, entries))
    }

    @Test
    fun `gap exactly at threshold is matched`() {
        val entries = listOf(entry(10.0), entry(18.0))
        assertTrue(FingerprintTimingManager.isWithinMatchedContent(14.0, entries))
    }

    @Test
    fun `gap just over threshold is not matched`() {
        val entries = listOf(entry(10.0), entry(18.1))
        assertFalse(FingerprintTimingManager.isWithinMatchedContent(14.0, entries))
    }

    @Test
    fun `single anchor matches within the trailing grace`() {
        val entries = listOf(entry(20.0))
        assertTrue(FingerprintTimingManager.isWithinMatchedContent(20.0, entries))
        assertFalse(FingerprintTimingManager.isWithinMatchedContent(32.1, entries))
    }

    @Test
    fun `single anchor before first is not matched`() {
        val entries = listOf(entry(20.0))
        assertFalse(FingerprintTimingManager.isWithinMatchedContent(10.0, entries))
    }
}

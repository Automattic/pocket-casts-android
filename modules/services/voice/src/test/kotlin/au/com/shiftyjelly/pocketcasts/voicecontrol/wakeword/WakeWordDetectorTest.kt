package au.com.shiftyjelly.pocketcasts.voicecontrol.wakeword

import kotlin.math.PI
import kotlin.math.sin
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeWordDetectorTest {

    // ── CommandWindow tests ──

    @Test
    fun `CommandWindow opens on wake word detection`() {
        val window = CommandWindow()
        assertFalse(window.isActive)
        window.onWakeWord()
        assertTrue(window.isActive)
    }

    @Test
    fun `CommandWindow stays open across follow-up utterances`() {
        val window = CommandWindow()
        window.onWakeWord()
        assertTrue(window.isActive)
        window.onActivity()
        assertTrue(window.isActive)
        window.onActivity()
        assertTrue(window.isActive)
    }

    @Test
    fun `CommandWindow closes after silence exceeding conversation timeout`() {
        val shortTimeoutMs = 50L
        val window = CommandWindow(conversationTimeoutMs = shortTimeoutMs)
        window.onWakeWord()
        assertTrue(window.isActive)
        Thread.sleep(shortTimeoutMs + 20)
        assertFalse(window.isActive)
    }

    @Test
    fun `CommandWindow onActivity refreshes the window without re-opening`() {
        val shortTimeoutMs = 100L
        val window = CommandWindow(conversationTimeoutMs = shortTimeoutMs)
        window.onWakeWord()
        assertTrue(window.isActive)
        Thread.sleep(shortTimeoutMs / 2)
        window.onActivity()
        assertTrue(window.isActive)
        Thread.sleep(shortTimeoutMs / 2 + 20)
        assertTrue(window.isActive)
        Thread.sleep(shortTimeoutMs)
        assertFalse(window.isActive)
    }

    @Test
    fun `CommandWindow close immediately sets inactive`() {
        val window = CommandWindow()
        window.onWakeWord()
        assertTrue(window.isActive)
        window.close()
        assertFalse(window.isActive)
    }

    @Test
    fun `CommandWindow reset clears state`() {
        val window = CommandWindow()
        window.onWakeWord()
        window.reset()
        assertFalse(window.isActive)
    }

    @Test
    fun `CommandWindow second onWakeWord while open refreshes window`() {
        val shortTimeoutMs = 100L
        val window = CommandWindow(conversationTimeoutMs = shortTimeoutMs)
        window.onWakeWord()
        Thread.sleep(shortTimeoutMs / 2)
        val result = window.onWakeWord()
        assertTrue(result)
        assertTrue(window.isActive)
        Thread.sleep(shortTimeoutMs / 2 + 20)
        assertTrue(window.isActive)
    }

    // ── extractRemainder silence-gap tests ──

    @Test
    fun `extractRemainder with silence gap returns command portion`() {
        val sampleRate = 16000
        val wakeWordSamples = 32000 // 2s
        val silenceSamples = 4800 // 300ms
        val commandSamples = 24000 // 1.5s
        val totalSamples = wakeWordSamples + silenceSamples + commandSamples

        val segment = FloatArray(totalSamples)
        // Wake word: 440 Hz tone at 0.5 amplitude
        for (i in 0 until wakeWordSamples) {
            segment[i] = 0.5f * sin(2.0 * PI * 440.0 * i / sampleRate).toFloat()
        }
        // Silence: near-zero amplitude
        for (i in wakeWordSamples until wakeWordSamples + silenceSamples) {
            segment[i] = 0.001f
        }
        // Command: 880 Hz tone at 0.3 amplitude
        for (i in wakeWordSamples + silenceSamples until totalSamples) {
            segment[i] = 0.3f * sin(2.0 * PI * 880.0 * i / sampleRate).toFloat()
        }

        // maxOffsetSample=0, scanFrom=32000, silence starts at 32000
        val remainder = OpenWakeWordDetector.extractRemainder(segment, 0)

        assertNotNull("Should extract remainder when silence gap exists", remainder)
        // cutPoint = gapStart(32000) + MIN_SILENCE_SAMPLES(3200) = 35200
        // remainder = segment[35200..totalSamples)
        val expectedCut = wakeWordSamples + 3200 // 32000 + 3200 = 35200
        assertEquals(totalSamples - expectedCut, remainder!!.size)
        // Verify the remainder has energy (is the command audio, not silence)
        val remainderRms = kotlin.math.sqrt(remainder.map { it * it }.average())
        assertTrue("Remainder should contain command audio (RMS > 0.01)", remainderRms > 0.01f)
    }

    @Test
    fun `extractRemainder without gap returns null`() {
        val sampleRate = 16000
        val totalSamples = 80000 // 5s

        val segment = FloatArray(totalSamples)
        // Continuous audio from start to finish — no silence gap
        for (i in 0 until totalSamples) {
            segment[i] = 0.3f * sin(2.0 * PI * 440.0 * i / sampleRate).toFloat()
        }

        val remainder = OpenWakeWordDetector.extractRemainder(segment, 0)

        assertNull("Should return null when no silence gap found", remainder)
    }

    @Test
    fun `extractRemainder with too-short remainder after cut returns null`() {
        val sampleRate = 16000
        val wakeWordSamples = 32000 // 2s
        val silenceSamples = 4800 // 300ms
        val commandSamples = 4000 // 250ms — too short (< 500ms minimum)
        val totalSamples = wakeWordSamples + silenceSamples + commandSamples

        val segment = FloatArray(totalSamples)
        for (i in 0 until wakeWordSamples) {
            segment[i] = 0.5f * sin(2.0 * PI * 440.0 * i / sampleRate).toFloat()
        }
        for (i in wakeWordSamples until wakeWordSamples + silenceSamples) {
            segment[i] = 0.001f
        }
        for (i in wakeWordSamples + silenceSamples until totalSamples) {
            segment[i] = 0.3f * sin(2.0 * PI * 880.0 * i / sampleRate).toFloat()
        }

        val remainder = OpenWakeWordDetector.extractRemainder(segment, 0)

        assertNull("Should return null when remainder < 500ms", remainder)
    }

    @Test
    fun `extractRemainder with negative offset returns null`() {
        val segment = FloatArray(64000) { 0.3f * sin(2.0 * PI * 440.0 * it / 16000).toFloat() }

        val remainder = OpenWakeWordDetector.extractRemainder(segment, -1)

        assertNull("Should return null for negative offset", remainder)
    }

    @Test
    fun `extractRemainder with offset near end falls back to segment-half scan`() {
        val sampleRate = 16000
        // Segment layout:
        //   [0..15999]          wake word (1s)
        //   [16000..31999]      intermediate speech (1s)
        //   [32000..35999]      silence gap (250ms)
        //   [36000..63999]      command (1.75s)
        val totalSamples = 64000
        val silenceStart = 32000
        val silenceSamples = 4000
        val commandStart = silenceStart + silenceSamples

        val segment = FloatArray(totalSamples)
        // Wake word: 440 Hz tone
        for (i in 0 until 16000) {
            segment[i] = 0.5f * sin(2.0 * PI * 440.0 * i / sampleRate).toFloat()
        }
        // Intermediate speech: 880 Hz tone (between wake word and silence gap)
        for (i in 16000 until silenceStart) {
            segment[i] = 0.3f * sin(2.0 * PI * 880.0 * i / sampleRate).toFloat()
        }
        // Silence gap
        for (i in silenceStart until commandStart) {
            segment[i] = 0.001f
        }
        // Command: 1320 Hz tone
        for (i in commandStart until totalSamples) {
            segment[i] = 0.3f * sin(2.0 * PI * 1320.0 * i / sampleRate).toFloat()
        }

        // maxOffsetSample=28000, so scanFrom=60000 which is >= 57600 (90% of 64000)
        // Falls back to totalSamples/2 = 32000, which is exactly where silence starts
        val remainder = OpenWakeWordDetector.extractRemainder(segment, 28000)

        assertNotNull("Should find gap with fallback scan position", remainder)
        // cutPoint = silenceStart + MIN_SILENCE_SAMPLES = 32000 + 3200 = 35200
        val expectedCut = silenceStart + 3200
        assertEquals(totalSamples - expectedCut, remainder!!.size)
        assertTrue("Remainder should contain command audio", remainder.isNotEmpty())
    }
}

package au.com.shiftyjelly.pocketcasts.voicecontrol.tts

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TtsEngineTest {
    private lateinit var engine: FakeTtsEngine

    @Before
    fun setUp() {
        engine = FakeTtsEngine()
    }

    @Test
    fun `warmUp completes without error`() = runTest {
        engine.warmUp("en")
        assertTrue(engine.isWarm)
    }

    @Test
    fun `speak returns after utterance completes`() = runTest {
        engine.warmUp("en")
        engine.speak("1.5x speed", "en")
        assertEquals("1.5x speed", engine.lastSpokenText)
        assertEquals("en", engine.lastSpokenLanguage)
    }

    @Test
    fun `release prevents further speak`() = runTest {
        engine.release()
        try {
            runTest { engine.speak("test", "en") }
        } catch (_: IllegalStateException) {
            // Expected
        }
        // Must not crash
    }
}

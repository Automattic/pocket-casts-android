package au.com.shiftyjelly.pocketcasts.voicecontrol.feedback

import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.VoiceResponse
import au.com.shiftyjelly.pocketcasts.voicecontrol.tts.FakeTtsEngine
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class AudioFeedbackRendererTest {
    private lateinit var earconPlayer: EarconPlayer
    private lateinit var ttsEngine: FakeTtsEngine
    private lateinit var renderer: AudioFeedbackRenderer

    @Before
    fun setUp() {
        earconPlayer = mock()
        ttsEngine = FakeTtsEngine()
        renderer = AudioFeedbackRenderer(earconPlayer, ttsEngine)
    }

    @Test
    fun `Silent response does nothing`() = runTest {
        renderer.render(VoiceResponse.Silent)
        verifyNoInteractions(earconPlayer)
    }

    @Test
    fun `Earcon response plays via EarconPlayer`() = runTest {
        whenever(earconPlayer.play(EarconId.SUCCESS)).thenReturn(true)
        renderer.render(VoiceResponse.Earcon(EarconId.SUCCESS))
        verify(earconPlayer).play(EarconId.SUCCESS)
    }

    @Test
    fun `Spoken response routes to TtsEngine`() = runTest {
        ttsEngine.warmUp("en")
        renderer.render(VoiceResponse.Spoken("1.5x speed"), language = "en")
        assertEquals("1.5x speed", ttsEngine.lastSpokenText)
        assertEquals("en", ttsEngine.lastSpokenLanguage)
    }

    @Test
    fun `Combined response plays earcon then speaks`() = runTest {
        ttsEngine.warmUp("en")
        whenever(earconPlayer.play(EarconId.SUCCESS)).thenReturn(true)
        renderer.render(VoiceResponse.Combined(EarconId.SUCCESS, "1.5x speed"), language = "en")
        verify(earconPlayer).play(EarconId.SUCCESS)
        assertEquals("1.5x speed", ttsEngine.lastSpokenText)
    }

    @Test
    fun `playEarcon delegates to EarconPlayer`() {
        whenever(earconPlayer.play(EarconId.WAKE_WORD)).thenReturn(true)
        renderer.playEarcon(EarconId.WAKE_WORD)
        verify(earconPlayer).play(EarconId.WAKE_WORD)
    }

    @Test
    fun `sequential render calls result in last-spoken text`() = runTest {
        ttsEngine.warmUp("en")
        renderer.render(VoiceResponse.Spoken("first"), language = "en")
        renderer.render(VoiceResponse.Spoken("second"), language = "en")
        assertEquals("second", ttsEngine.lastSpokenText)
    }

    @Test
    fun `release disposes both earcon and TTS resources`() {
        renderer.release()
        verify(earconPlayer).release()
    }
}

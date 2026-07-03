package au.com.shiftyjelly.pocketcasts.voicecontrol.tts

import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.conditions.ModelsReadyCondition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TtsPreWarmerTest {
    private lateinit var ttsEngine: FakeTtsEngine
    private lateinit var modelsReady: ModelsReadyCondition

    @Before
    fun setUp() {
        ttsEngine = FakeTtsEngine()
        modelsReady = ModelsReadyCondition(isReady = false)
    }

    @Test
    fun `TTS engine is warmed up when models become ready`() = runTest {
        val preWarmer = TtsPreWarmer(ttsEngine, modelsReady, CoroutineScope(coroutineContext), "en")

        modelsReady.update(isReady = true)
        preWarmer.onModelsStateChanged()
        runCurrent()

        assertTrue(ttsEngine.isWarm)
    }

    @Test
    fun `TTS engine is NOT warmed up before models ready`() = runTest {
        val preWarmer = TtsPreWarmer(ttsEngine, modelsReady, CoroutineScope(coroutineContext), "en")

        modelsReady.update(isReady = false)
        preWarmer.onModelsStateChanged()

        assertFalse(ttsEngine.isWarm)
    }
}

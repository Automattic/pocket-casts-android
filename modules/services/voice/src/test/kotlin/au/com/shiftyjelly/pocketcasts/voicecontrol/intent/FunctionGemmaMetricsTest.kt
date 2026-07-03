package au.com.shiftyjelly.pocketcasts.voicecontrol.intent

import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.runtime.FunctionGemmaBackend
import org.junit.Assert.assertEquals
import org.junit.Test

class FunctionGemmaMetricsTest {
    @Test
    fun `preparation and inference timings remain separate`() {
        val recorder = RecordingFunctionGemmaMetrics()

        recorder.prepared(
            backend = FunctionGemmaBackend.GPU,
            modelRelease = "release-1",
            engineInitMs = 100,
            sessionCreateMs = 20,
            staticPrefillMs = 700,
        )
        recorder.inference(
            FunctionGemmaInferenceMetrics(
                backend = FunctionGemmaBackend.GPU,
                modelRelease = "release-1",
                sessionWaitMs = 0,
                requestPrefillMs = 30,
                decodeMs = 120,
                parseResolveMs = 2,
                totalMs = 152,
                inputCharacters = 55,
                outputCharacters = 76,
                fallbackReason = null,
                conversationReused = true,
                reuseCount = 1,
                conversationRotated = false,
                rotationCause = null,
            ),
        )

        assertEquals(700, recorder.preparations.single().staticPrefillMs)
        assertEquals(30, recorder.inferences.single().requestPrefillMs)
        assertEquals(120, recorder.inferences.single().decodeMs)
    }
}

internal data class FunctionGemmaPreparationRecord(
    val backend: FunctionGemmaBackend,
    val modelRelease: String,
    val engineInitMs: Long,
    val sessionCreateMs: Long,
    val staticPrefillMs: Long,
)

internal class RecordingFunctionGemmaMetrics : FunctionGemmaMetrics {
    val preparations = mutableListOf<FunctionGemmaPreparationRecord>()
    val inferences = mutableListOf<FunctionGemmaInferenceMetrics>()
    val fallbacks = mutableListOf<String>()

    override fun prepared(
        backend: FunctionGemmaBackend,
        modelRelease: String,
        engineInitMs: Long,
        sessionCreateMs: Long,
        staticPrefillMs: Long,
    ) {
        preparations += FunctionGemmaPreparationRecord(
            backend = backend,
            modelRelease = modelRelease,
            engineInitMs = engineInitMs,
            sessionCreateMs = sessionCreateMs,
            staticPrefillMs = staticPrefillMs,
        )
    }

    override fun inference(metrics: FunctionGemmaInferenceMetrics) {
        inferences += metrics
    }

    override fun backendFallback(reason: String, error: Throwable) {
        fallbacks += reason
    }
}

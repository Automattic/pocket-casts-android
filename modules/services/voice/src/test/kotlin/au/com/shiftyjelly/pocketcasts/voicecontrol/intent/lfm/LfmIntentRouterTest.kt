package au.com.shiftyjelly.pocketcasts.voicecontrol.intent.lfm

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import au.com.shiftyjelly.pocketcasts.voicecontrol.dialog.VoiceDialogManager
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.ToolCallMapper
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.VoiceIntent
import au.com.shiftyjelly.pocketcasts.voicecontrol.mode.ListeningMode
import au.com.shiftyjelly.pocketcasts.voicecontrol.model.ModelManager
import au.com.shiftyjelly.pocketcasts.voicecontrol.model.VoiceRecognitionContext
import au.com.shiftyjelly.pocketcasts.voicecontrol.route.MicExposure
import java.io.File
import java.security.MessageDigest
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LfmIntentRouterTest {
    @get:Rule val tempDir = TemporaryFolder()

    @Test
    fun noMatch_returnsNull() = runTest {
        val inference = FakeLfmInference().apply {
            classifyLabel = "no_match:"
        }
        val router = createRouter(inference)

        router.ensureReady().getOrThrow()
        assertNull(router.recognize("hello", RECOGNITION_CONTEXT))
        assertEquals(1, inference.resetCount)
    }

    @Test
    fun dialogControl_routesThroughDialogManager() = runTest {
        val inference = FakeLfmInference().apply {
            classifyLabel = "dialog_control:begin"
            generateResult =
                "<|tool_call_start|>[dialog_control(action='begin', target_tool='bookmark', target_action='rename')]<|tool_call_end|>"
        }
        val router = createRouter(inference)

        router.ensureReady().getOrThrow()
        assertNull(router.recognize("rename my bookmark", RECOGNITION_CONTEXT))
    }

    @Test
    fun spanFailure_returnsNullWithoutGuessingTool() = runTest {
        val inference = FakeLfmInference().apply {
            tokenizeThrows = IllegalArgumentException("user utterance tokens not found in prompt")
        }
        val router = createRouter(inference)

        router.ensureReady().getOrThrow()
        assertNull(router.recognize("pause", RECOGNITION_CONTEXT))
        assertEquals(0, inference.classifyCount)
    }

    @Test
    fun decodeFailure_returnsNullWithoutGuessingTool() = runTest {
        val inference = FakeLfmInference().apply {
            classifyLabel = "playback:pause"
            generateResult = null
        }
        val router = createRouter(inference)

        router.ensureReady().getOrThrow()
        assertNull(router.recognize("pause", RECOGNITION_CONTEXT))
    }

    @Test
    fun ensureReady_failsWhenNativeLoadFails() = runTest {
        val inference = FakeLfmInference().apply {
            loadResult = false
            lastErrorMessage = "invalid classifier.bin magic"
        }
        val router = createRouter(inference)

        assertFalse(router.ensureReady().isSuccess)
    }

    @Test
    fun pauseCommand_mapsToPlaybackPause() = runTest {
        val inference = FakeLfmInference().apply {
            classifyLabel = "playback:pause"
            generateResult =
                "<|tool_call_start|>[playback(action='pause')]<|tool_call_end|>"
        }
        val router = createRouter(inference)

        router.ensureReady().getOrThrow()
        assertEquals(VoiceIntent.Playback.Pause, router.recognize("pause", RECOGNITION_CONTEXT))
    }

    @Test
    fun seekRelative_mapsDeltaSecondsThroughSlotRepair() = runTest {
        val inference = FakeLfmInference().apply {
            classifyLabel = "playback:seek_relative"
            generateResult =
                "<|tool_call_start|>[playback(action='seek_relative', minutes=1)]<|tool_call_end|>"
        }
        val router = createRouter(inference)

        router.ensureReady().getOrThrow()
        assertEquals(
            VoiceIntent.Playback.SeekRelative(-60_000),
            router.recognize("go back a minute", RECOGNITION_CONTEXT),
        )
    }

    private fun createRouter(inference: FakeLfmInference): LfmIntentRouter {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = ModelManager(context).apply {
            filesDir = tempDir.root
        }
        seedLfmAssets(manager)
        return LfmIntentRouter(
            dialogManager = VoiceDialogManager(ToolCallMapper()),
            modelManager = manager,
            inference = inference,
        )
    }

    private fun seedLfmAssets(manager: ModelManager) {
        val modelDir = File(manager.filesDir, "function-call").apply { mkdirs() }
        File(modelDir, "model.gguf").writeText("gguf")
        File(modelDir, "classifier.bin").writeText("cls")
        File(modelDir, "label_map.json").writeText("""{"labels":["playback:pause"]}""")
        File(modelDir, "manifest.json").writeText(
            """
            {
              "version": "2026-06-21-143005",
              "assets": {
                "model.gguf": {
                  "bytes": 4,
                  "sha256": "${sha256("gguf")}",
                  "url": "https://example.test/model.gguf"
                },
                "classifier.bin": {
                  "bytes": 3,
                  "sha256": "${sha256("cls")}",
                  "url": "https://example.test/classifier.bin"
                },
                "label_map.json": {
                  "bytes": ${"""{"labels":["playback:pause"]}""".length},
                  "sha256": "${sha256("""{"labels":["playback:pause"]}""")}",
                  "url": "https://example.test/label_map.json"
                }
              }
            }
            """.trimIndent(),
        )
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray())
        .joinToString("") { "%02x".format(it) }

    private companion object {
        val RECOGNITION_CONTEXT = VoiceRecognitionContext(
            listeningMode = ListeningMode.Continuous,
            micExposure = MicExposure.Exposed,
        )
    }
}

internal class FakeLfmInference : LfmInference {
    var loadResult = true
    var lastErrorMessage = ""
    var classifyLabel: String? = "playback:pause"
    var generateResult: String? =
        "<|tool_call_start|>[playback(action='pause')]<|tool_call_end|>"
    var tokenizeThrows: Throwable? = null
    var classifyCount = 0
    var resetCount = 0

    override fun lastError(): String = lastErrorMessage

    override fun load(
        modelPath: String,
        classifierPath: String,
        labelMapPath: String,
        nCtx: Int,
    ): Boolean = loadResult

    override fun tokenize(text: String, addBos: Boolean): IntArray? {
        tokenizeThrows?.let { throw it }
        return when (text) {
            "pause", "go back a minute" -> intArrayOf(10)
            else -> intArrayOf(1, 10, 2)
        }
    }

    override fun classify(promptTokenIds: IntArray, poolStart: Int, poolEnd: Int): String? {
        classifyCount++
        return classifyLabel
    }

    override fun generate(prefill: String, nPredict: Int): String? = generateResult

    override fun reset() {
        resetCount++
    }

    override fun release() = Unit
}

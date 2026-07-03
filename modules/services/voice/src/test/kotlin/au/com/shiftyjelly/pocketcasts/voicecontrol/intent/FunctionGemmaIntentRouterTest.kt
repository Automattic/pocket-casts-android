package au.com.shiftyjelly.pocketcasts.voicecontrol.intent

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import au.com.shiftyjelly.pocketcasts.voicecontrol.dialog.VoiceDialogManager
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.runtime.FunctionGemmaBackend
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.runtime.FunctionGemmaRuntime
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.runtime.FunctionGemmaRuntimeFactory
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.runtime.FunctionGemmaSession
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.runtime.MonotonicClock
import au.com.shiftyjelly.pocketcasts.voicecontrol.mode.ListeningMode
import au.com.shiftyjelly.pocketcasts.voicecontrol.model.ModelManager
import au.com.shiftyjelly.pocketcasts.voicecontrol.model.VoiceRecognitionContext
import au.com.shiftyjelly.pocketcasts.voicecontrol.route.MicExposure
import java.io.File
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doThrow
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class FunctionGemmaIntentRouterTest {
    @get:Rule val tempDir = TemporaryFolder()

    @Test
    fun `ensureReady prepares a GPU session with the static prefix`() = runTest {
        val factory = FakeRuntimeFactory()
        val router = createRouter(factory)

        assertTrue(router.ensureReady().isSuccess)

        assertEquals(listOf(FunctionGemmaBackend.GPU), factory.createdBackends)
        assertEquals(FunctionGemmaPrompt.staticPrefix, factory.runtimes.single().sessions.single().prefills.single())
    }

    @Test
    fun `GPU initialization failure prepares CPU once`() = runTest {
        val factory = FakeRuntimeFactory(
            createFailures = mutableMapOf(
                FunctionGemmaBackend.GPU to IllegalStateException("unsupported"),
            ),
        )
        val router = createRouter(factory)

        assertTrue(router.ensureReady().isSuccess)

        assertEquals(listOf(FunctionGemmaBackend.GPU, FunctionGemmaBackend.CPU), factory.createdBackends)
    }

    @Test
    fun `GPU linkage failure prepares CPU once`() = runTest {
        val factory = FakeRuntimeFactory(
            createFailures = mutableMapOf(
                FunctionGemmaBackend.GPU to UnsatisfiedLinkError("missing GPU native library"),
            ),
        )
        val router = createRouter(factory)

        assertTrue(router.ensureReady().isSuccess)

        assertEquals(listOf(FunctionGemmaBackend.GPU, FunctionGemmaBackend.CPU), factory.createdBackends)
    }

    @Test
    fun `GPU static prefix failure closes GPU and prepares CPU once`() = runTest {
        val factory = FakeRuntimeFactory(
            staticPrefillFailures = mutableMapOf(
                FunctionGemmaBackend.GPU to IllegalStateException("gpu prefill failed"),
            ),
        )
        val router = createRouter(factory)

        assertTrue(router.ensureReady().isSuccess)

        assertEquals(listOf(FunctionGemmaBackend.GPU, FunctionGemmaBackend.CPU), factory.createdBackends)
        assertEquals(1, factory.runtimes.first().closeCount)
        assertEquals(FunctionGemmaPrompt.staticPrefix, factory.runtimes.last().sessions.single().prefills.single())
    }

    @Test
    fun `GPU and CPU readiness failures leave no active pool`() = runTest {
        val factory = FakeRuntimeFactory(
            createFailures = mutableMapOf(
                FunctionGemmaBackend.GPU to IllegalStateException("gpu unsupported"),
                FunctionGemmaBackend.CPU to IllegalStateException("cpu unavailable"),
            ),
        )
        val router = createRouter(factory)

        assertTrue(router.ensureReady().isFailure)
        assertNull(router.recognize("Pause.", RECOGNITION_CONTEXT))
        assertEquals(listOf(FunctionGemmaBackend.GPU, FunctionGemmaBackend.CPU), factory.createdBackends)
    }

    @Test
    fun `prepared session receives only request suffix`() = runTest {
        val factory = FakeRuntimeFactory(gpuDecodes = queueOf(Result.success(PAUSE_CALL)))
        val router = createRouter(factory)
        router.ensureReady().getOrThrow()

        assertEquals(VoiceIntent.Playback.Pause, router.recognize("Pause.", RECOGNITION_CONTEXT))

        val request = factory.runtimes.single().sessions.first().prefills.last()
        assertEquals(FunctionGemmaPrompt.requestSuffix("Pause.", emptyList()), request)
        assertFalse(request.contains("<start_function_declaration>"))
    }

    @Test
    fun `GPU decode failure creates CPU once and retries utterance once`() = runTest {
        val factory = FakeRuntimeFactory(
            gpuDecodes = queueOf(Result.failure(IllegalStateException("gpu decode failed"))),
            cpuDecodes = queueOf(Result.success(PAUSE_CALL)),
        )
        val router = createRouter(factory)
        router.ensureReady().getOrThrow()

        val intent = router.recognize("Pause.", RECOGNITION_CONTEXT)

        assertEquals(VoiceIntent.Playback.Pause, intent)
        assertEquals(listOf(FunctionGemmaBackend.GPU, FunctionGemmaBackend.CPU), factory.createdBackends)
        assertEquals(
            FunctionGemmaPrompt.requestSuffix("Pause.", emptyList()),
            factory.runtimes.last().sessions.first().prefills.last(),
        )
    }

    @Test
    fun `CPU retry failure returns null without creating a third runtime`() = runTest {
        val factory = FakeRuntimeFactory(
            gpuDecodes = queueOf(Result.failure(IllegalStateException("gpu decode failed"))),
            cpuDecodes = queueOf(Result.failure(IllegalStateException("cpu decode failed"))),
        )
        val router = createRouter(factory)
        router.ensureReady().getOrThrow()

        assertNull(router.recognize("Pause.", RECOGNITION_CONTEXT))
        assertEquals(listOf(FunctionGemmaBackend.GPU, FunctionGemmaBackend.CPU), factory.createdBackends)
    }

    @Test
    fun `ensureReady waits for failed CPU pool invalidation before reporting ready`() = runTest {
        val invalidationStarted = CountDownLatch(1)
        val releaseInvalidation = CountDownLatch(1)
        val factory = FakeRuntimeFactory(
            gpuDecodes = queueOf(Result.failure(IllegalStateException("gpu decode failed"))),
            cpuDecodes = queueOf(
                Result.success(NO_MATCH_CALL),
                Result.failure(IllegalStateException("cpu decode failed")),
            ),
        )
        val router = createRouter(factory)
        router.ensureReady().getOrThrow()
        assertNull(router.recognize("First.", RECOGNITION_CONTEXT))
        router.beforePoolInvalidation = {
            invalidationStarted.countDown()
            check(releaseInvalidation.await(5, TimeUnit.SECONDS)) { "Timed out waiting to release invalidation" }
        }

        val failedRecognition = async { router.recognize("Second.", RECOGNITION_CONTEXT) }
        assertTrue(withContext(Dispatchers.IO) { invalidationStarted.await(5, TimeUnit.SECONDS) })
        val readiness = async { router.ensureReady() }
        yield()

        assertFalse(readiness.isCompleted)

        releaseInvalidation.countDown()
        assertNull(failedRecognition.await())
        assertTrue(readiness.await().isSuccess)
        assertEquals(
            listOf(FunctionGemmaBackend.GPU, FunctionGemmaBackend.CPU, FunctionGemmaBackend.GPU),
            factory.createdBackends,
        )
        assertEquals(1, factory.runtimes[1].closeCount)
    }

    @Test
    fun `parse failure returns null and replenishes prepared session`() = runTest {
        val factory = FakeRuntimeFactory(gpuDecodes = queueOf(Result.success("not a tool call")))
        val router = createRouter(factory)
        router.ensureReady().getOrThrow()

        assertNull(router.recognize("Pause.", RECOGNITION_CONTEXT))
        advanceUntilIdle()

        val runtime = factory.runtimes.single()
        // Session always replaced (conversation reused, not rotated)
        assertEquals(2, runtime.sessions.size)
        assertEquals(listOf(FunctionGemmaBackend.GPU), factory.createdBackends)
    }

    @Test
    fun `parser exception returns null without CPU fallback`() = runTest {
        val factory = FakeRuntimeFactory(gpuDecodes = queueOf(Result.success(THROWING_PARSE_OUTPUT)))
        val router = createRouter(factory)
        router.ensureReady().getOrThrow()

        assertNull(router.recognize("Pause.", RECOGNITION_CONTEXT))
        advanceUntilIdle()

        assertTrue(factory.createdBackends.contains(FunctionGemmaBackend.GPU))
        assertEquals(1, factory.createdBackends.count { it == FunctionGemmaBackend.GPU })
    }

    @Test
    fun `dialog resolution exception returns null without CPU fallback`() = runTest {
        val dialogManager = mock<VoiceDialogManager>()
        whenever(dialogManager.promptHistory()).thenReturn(emptyList())
        doThrow(IllegalArgumentException("invalid dialog transition"))
            .whenever(dialogManager)
            .resolve(any(), any(), any())
        val factory = FakeRuntimeFactory(gpuDecodes = queueOf(Result.success(PAUSE_CALL)))
        val router = createRouter(factory, dialogManager = dialogManager)
        router.ensureReady().getOrThrow()

        assertNull(router.recognize("Pause.", RECOGNITION_CONTEXT))
        advanceUntilIdle()

        assertTrue(factory.createdBackends.contains(FunctionGemmaBackend.GPU))
        assertEquals(1, factory.createdBackends.count { it == FunctionGemmaBackend.GPU })
    }

    @Test
    fun `release change closes old pool and prepares a fresh GPU pool`() = runTest {
        val factory = FakeRuntimeFactory()
        val modelManager = createModelManager("release-1")
        val router = createRouter(factory, modelManager)
        router.ensureReady().getOrThrow()

        installRelease(modelManager, "release-2")
        router.ensureReady().getOrThrow()

        assertEquals(listOf(FunctionGemmaBackend.GPU, FunctionGemmaBackend.GPU), factory.createdBackends)
        assertEquals(1, factory.runtimes.first().closeCount)
        assertEquals(FunctionGemmaPrompt.staticPrefix, factory.runtimes.last().sessions.single().prefills.single())
    }

    @Test
    fun `same release reuses prepared pool`() = runTest {
        val factory = FakeRuntimeFactory()
        val router = createRouter(factory)

        router.ensureReady().getOrThrow()
        router.ensureReady().getOrThrow()

        assertEquals(listOf(FunctionGemmaBackend.GPU), factory.createdBackends)
    }

    @Test
    fun `blank transcript does not consume prepared session`() = runTest {
        val factory = FakeRuntimeFactory()
        val router = createRouter(factory)
        router.ensureReady().getOrThrow()

        assertNull(router.recognize("   ", RECOGNITION_CONTEXT))

        val runtime = factory.runtimes.single()
        assertEquals(1, runtime.sessions.size)
        assertEquals(listOf(FunctionGemmaPrompt.staticPrefix), runtime.sessions.single().prefills)
    }

    @Test
    fun `successful inference records preparation and separated request timings`() = runTest {
        val metrics = RecordingFunctionGemmaMetrics()
        val factory = FakeRuntimeFactory(gpuDecodes = queueOf(Result.success(PAUSE_CALL)))
        val router = createRouter(factory, metrics = metrics)
        router.ensureReady().getOrThrow()

        assertEquals(VoiceIntent.Playback.Pause, router.recognize("Pause.", RECOGNITION_CONTEXT))

        assertEquals(1, metrics.preparations.size)
        assertEquals(FunctionGemmaBackend.GPU, metrics.preparations.single().backend)
        assertEquals("release-1", metrics.preparations.single().modelRelease)
        assertEquals(1, metrics.inferences.size)
        val inference = metrics.inferences.single()
        assertEquals(FunctionGemmaBackend.GPU, inference.backend)
        assertEquals("release-1", inference.modelRelease)
        assertEquals(FunctionGemmaPrompt.requestSuffix("Pause.", emptyList()).length, inference.inputCharacters)
        assertEquals(PAUSE_CALL.length, inference.outputCharacters)
        assertEquals(null, inference.fallbackReason)
        assertTrue(inference.conversationReused)
        assertEquals(1, inference.reuseCount)
        assertFalse(inference.conversationRotated)
        assertEquals(null, inference.rotationCause)
    }

    @Test
    fun `GPU inference fallback is recorded on CPU retry`() = runTest {
        val metrics = RecordingFunctionGemmaMetrics()
        val factory = FakeRuntimeFactory(
            gpuDecodes = queueOf(Result.failure(IllegalStateException("gpu decode failed"))),
            cpuDecodes = queueOf(Result.success(PAUSE_CALL)),
        )
        val router = createRouter(factory, metrics = metrics)
        router.ensureReady().getOrThrow()

        assertEquals(VoiceIntent.Playback.Pause, router.recognize("Pause.", RECOGNITION_CONTEXT))

        assertEquals(listOf("gpu_inference"), metrics.fallbacks)
        assertEquals("gpu_inference", metrics.inferences.single().fallbackReason)
        assertEquals(FunctionGemmaBackend.CPU, metrics.inferences.single().backend)
    }

    @Test
    fun `release closes active pool and later readiness prepares a fresh pool`() = runTest {
        val factory = FakeRuntimeFactory()
        val router = createRouter(factory)
        router.ensureReady().getOrThrow()

        router.release()
        router.ensureReady().getOrThrow()

        assertEquals(2, factory.runtimes.size)
        assertEquals(1, factory.runtimes.first().closeCount)
        assertEquals(FunctionGemmaPrompt.staticPrefix, factory.runtimes.last().sessions.single().prefills.single())
    }

    @Test
    fun `concurrent recognition builds second suffix after first dialog resolution`() = runTest {
        val firstDecodeStarted = CountDownLatch(1)
        val releaseFirstDecode = CountDownLatch(1)
        val factory = FakeRuntimeFactory(
            gpuDecodes = queueOf(
                Result.success(BEGIN_DIALOG_CALL),
                Result.success(NO_MATCH_CALL),
            ),
            firstDecodeStarted = firstDecodeStarted,
            releaseFirstDecode = releaseFirstDecode,
        )
        val router = createRouter(factory)
        router.ensureReady().getOrThrow()

        val first = async { router.recognize("Rename a bookmark.", RECOGNITION_CONTEXT) }
        assertTrue(withContext(Dispatchers.IO) { firstDecodeStarted.await(5, TimeUnit.SECONDS) })
        val second = async { router.recognize("The second one.", RECOGNITION_CONTEXT) }
        yield()
        releaseFirstDecode.countDown()
        first.await()
        second.await()
        advanceUntilIdle()

        val requestPrefills = factory.runtimes.single().sessions
            .flatMap { it.prefills }
            .filterNot { it == FunctionGemmaPrompt.staticPrefix }
        assertEquals(2, requestPrefills.size)
        assertEquals(FunctionGemmaPrompt.requestSuffix("Rename a bookmark.", emptyList()), requestPrefills.first())
        assertTrue(requestPrefills.last().contains("Rename a bookmark."))
        assertTrue(requestPrefills.last().contains(BEGIN_DIALOG_CALL))
    }

    private fun kotlinx.coroutines.test.TestScope.createRouter(
        factory: FakeRuntimeFactory,
        modelManager: ModelManager = createModelManager("release-1"),
        dialogManager: VoiceDialogManager = VoiceDialogManager(ToolCallMapper()),
        metrics: FunctionGemmaMetrics = RecordingFunctionGemmaMetrics(),
    ): FunctionGemmaIntentRouter {
        val elapsed = AtomicLong()
        return FunctionGemmaIntentRouter(
            dialogManager = dialogManager,
            modelManager = modelManager,
            runtimeFactory = factory,
            applicationScope = backgroundScope,
            clock = MonotonicClock { elapsed.getAndIncrement() },
            metrics = metrics,
        )
    }

    private fun createModelManager(version: String): ModelManager {
        val context = ApplicationProvider.getApplicationContext<Context>()
        return ModelManager(context).apply {
            filesDir = tempDir.root
            installRelease(this, version)
        }
    }

    private fun installRelease(
        manager: ModelManager,
        version: String,
    ) {
        manager.functionGemmaDir.mkdirs()
        File(manager.functionGemmaDir, "model.litertlm").writeText("model")
        File(manager.functionGemmaDir, "model.litertlm.xnnpack_cache_test").writeText("cache")
        File(manager.functionGemmaDir, "manifest.json").writeText(
            """
            {
              "version": "$version",
              "assets": {
                "model.litertlm": {
                  "bytes": 5,
                  "sha256": "unused",
                  "url": "https://example.test/model.litertlm"
                },
                "model.litertlm.xnnpack_cache_test": {
                  "bytes": 5,
                  "sha256": "unused",
                  "url": "https://example.test/model.litertlm.xnnpack_cache_test"
                }
              }
            }
            """.trimIndent(),
        )
    }

    private class FakeRuntimeFactory(
        private val createFailures: MutableMap<FunctionGemmaBackend, Throwable> = mutableMapOf(),
        private val staticPrefillFailures: MutableMap<FunctionGemmaBackend, Throwable> = mutableMapOf(),
        private val gpuDecodes: ConcurrentLinkedQueue<Result<String>> = queueOf(),
        private val cpuDecodes: ConcurrentLinkedQueue<Result<String>> = queueOf(),
        private val firstDecodeStarted: CountDownLatch? = null,
        private val releaseFirstDecode: CountDownLatch? = null,
    ) : FunctionGemmaRuntimeFactory {
        val createdBackends = mutableListOf<FunctionGemmaBackend>()
        val runtimes = mutableListOf<FakeRuntime>()

        override fun create(
            modelPath: String,
            cacheDir: String,
            backend: FunctionGemmaBackend,
        ): FunctionGemmaRuntime {
            createdBackends += backend
            createFailures.remove(backend)?.let { throw it }
            return FakeRuntime(
                backend = backend,
                decodes = if (backend == FunctionGemmaBackend.GPU) gpuDecodes else cpuDecodes,
                staticPrefillFailure = staticPrefillFailures.remove(backend),
                firstDecodeStarted = firstDecodeStarted,
                releaseFirstDecode = releaseFirstDecode,
            ).also(runtimes::add)
        }
    }

    private class FakeRuntime(
        override val backend: FunctionGemmaBackend,
        private val decodes: ConcurrentLinkedQueue<Result<String>>,
        private val staticPrefillFailure: Throwable?,
        private val firstDecodeStarted: CountDownLatch?,
        private val releaseFirstDecode: CountDownLatch?,
    ) : FunctionGemmaRuntime {
        val sessions = mutableListOf<FakeSession>()
        private val decodeCount = AtomicInteger()
        var closeCount = 0

        override fun createSession(): FunctionGemmaSession {
            return FakeSession(
                decodes = decodes,
                staticPrefillFailure = if (sessions.isEmpty()) staticPrefillFailure else null,
                decodeCount = decodeCount,
                firstDecodeStarted = firstDecodeStarted,
                releaseFirstDecode = releaseFirstDecode,
            ).also(sessions::add)
        }

        override fun createSessionWithNewConversation(systemInstruction: String): FunctionGemmaSession {
            return createSession() // tests don't differentiate
        }

        override fun close() {
            closeCount++
        }
    }

    private class FakeSession(
        private val decodes: ConcurrentLinkedQueue<Result<String>>,
        private val staticPrefillFailure: Throwable?,
        private val decodeCount: AtomicInteger,
        private val firstDecodeStarted: CountDownLatch?,
        private val releaseFirstDecode: CountDownLatch?,
    ) : FunctionGemmaSession {
        val prefills = mutableListOf<String>()

        override val tokenCount: Int
            get() = prefills.size * 100

        override fun prefill(text: String) {
            prefills += text
            if (text == FunctionGemmaPrompt.staticPrefix) {
                staticPrefillFailure?.let { throw it }
            }
        }

        override fun decode(): String {
            if (decodeCount.getAndIncrement() == 0 && firstDecodeStarted != null && releaseFirstDecode != null) {
                firstDecodeStarted.countDown()
                check(releaseFirstDecode.await(5, TimeUnit.SECONDS)) { "Timed out waiting to release first decode" }
            }
            return (decodes.poll() ?: Result.success(NO_MATCH_CALL)).getOrThrow()
        }

        override fun close() = Unit
    }

    private companion object {
        val RECOGNITION_CONTEXT = VoiceRecognitionContext(
            listeningMode = ListeningMode.Continuous,
            micExposure = MicExposure.Isolated,
        )

        const val PAUSE_CALL =
            "<start_function_call>call:playback{action:<escape>pause</escape>}<end_function_call>"
        const val BEGIN_DIALOG_CALL =
            "<start_function_call>call:dialog_control{action:<escape>begin</escape>," +
                "target_tool:<escape>bookmark</escape>,target_action:<escape>rename</escape>}<end_function_call>"
        const val NO_MATCH_CALL = "<start_function_call>call:no_match{}<end_function_call>"
        const val THROWING_PARSE_OUTPUT =
            "<start_function_call>call:playback{action:<escape>pause</escape><end_function_call>"

        fun queueOf(): ConcurrentLinkedQueue<Result<String>> = ConcurrentLinkedQueue()

        fun queueOf(value: Result<String>): ConcurrentLinkedQueue<Result<String>> {
            return ConcurrentLinkedQueue(listOf(value))
        }

        fun queueOf(
            first: Result<String>,
            second: Result<String>,
        ): ConcurrentLinkedQueue<Result<String>> {
            return ConcurrentLinkedQueue(listOf(first, second))
        }
    }
}

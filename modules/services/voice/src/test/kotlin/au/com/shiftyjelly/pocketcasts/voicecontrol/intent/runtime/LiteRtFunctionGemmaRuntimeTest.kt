package au.com.shiftyjelly.pocketcasts.voicecontrol.intent.runtime

import au.com.shiftyjelly.pocketcasts.voicecontrol.BuildConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class LiteRtFunctionGemmaRuntimeTest {
    @Test
    fun `build exposes the pinned LiteRT-LM version`() {
        assertEquals("0.13.1", BuildConfig.LITERTLM_VERSION)
    }

    @Test
    fun `factory maps GPU engine configuration and restores benchmark flag`() {
        val benchmarkFlags = FakeLiteRtBenchmarkFlags(enabled = false)
        val engine = FakeLiteRtEngine(isBenchmarkEnabled = { benchmarkFlags.enabled })
        val engineFactory = CapturingLiteRtEngineFactory(engine)
        val conversation = FakeLiteRtConversation()
        val factory = LiteRtFunctionGemmaRuntimeFactory(
            engineFactory,
            benchmarkFlags,
            FakeConversationFactory(conversation),
        )

        val runtime = factory.create(
            modelPath = "/models/function-gemma.litertlm",
            cacheDir = "/models/cache",
            backend = FunctionGemmaBackend.GPU,
        )

        assertEquals(FunctionGemmaBackend.GPU, runtime.backend)
        assertEquals("/models/function-gemma.litertlm", engineFactory.config?.modelPath)
        assertEquals("/models/cache", engineFactory.config?.cacheDir)
        assertEquals(2048, engineFactory.config?.maxNumTokens)
        assertEquals(LiteRtBackend.GPU, engineFactory.config?.backend)
        assertEquals(LiteRtActivationDataType.FP32, engineFactory.config?.activationDataType)
        assertTrue(engine.benchmarkEnabledDuringInitialize)
        assertFalse(benchmarkFlags.enabled)
        assertTrue(
            "system instruction must contain developer turn",
            conversation.systemInstruction.contains("<start_of_turn>developer"),
        )
    }

    @Test
    fun `factory maps CPU backend without activation override`() {
        val engineFactory = CapturingLiteRtEngineFactory(FakeLiteRtEngine())
        val factory = LiteRtFunctionGemmaRuntimeFactory(
            engineFactory = engineFactory,
            benchmarkFlags = FakeLiteRtBenchmarkFlags(),
            conversationFactory = FakeConversationFactory(FakeLiteRtConversation()),
        )

        factory.create("model", "cache", FunctionGemmaBackend.CPU)

        assertEquals(LiteRtBackend.CPU, engineFactory.config?.backend)
        assertEquals(null, engineFactory.config?.activationDataType)
    }

    @Test
    fun `factory restores benchmark flag when initialization fails`() {
        val benchmarkFlags = FakeLiteRtBenchmarkFlags(enabled = false)
        val engine = FakeLiteRtEngine(
            isBenchmarkEnabled = { benchmarkFlags.enabled },
            initializeFailure = IllegalStateException("initialization failed"),
        )
        val factory = LiteRtFunctionGemmaRuntimeFactory(
            engineFactory = CapturingLiteRtEngineFactory(engine),
            benchmarkFlags = benchmarkFlags,
            conversationFactory = FakeConversationFactory(FakeLiteRtConversation()),
        )

        try {
            factory.create("model", "cache", FunctionGemmaBackend.GPU)
            fail("Expected initialization to fail")
        } catch (_: IllegalStateException) {
            assertTrue(engine.benchmarkEnabledDuringInitialize)
            assertFalse(benchmarkFlags.enabled)
        }
    }

    @Test
    fun `session extracts user transcript from formatted suffix and delegates to conversation`() {
        val conversation = FakeLiteRtConversation(response = "<start_function_call>call:playback{action:<escape>pause</escape>}<end_function_call>")
        val factory = LiteRtFunctionGemmaRuntimeFactory(
            engineFactory = CapturingLiteRtEngineFactory(FakeLiteRtEngine()),
            benchmarkFlags = FakeLiteRtBenchmarkFlags(),
            conversationFactory = FakeConversationFactory(conversation),
        )
        val runtime = factory.create("model", "cache", FunctionGemmaBackend.GPU)

        val session = runtime.createSession()
        // This is what FunctionGemmaPrompt.requestSuffix produces for "Pause." with no history
        session.prefill("\n<start_of_turn>user\nPause.<end_of_turn>\n<start_of_turn>model\n")

        assertEquals(
            "<start_function_call>call:playback{action:<escape>pause</escape>}<end_function_call>",
            session.decode(),
        )
        // Verify the conversation received just the transcript, not the formatted suffix
        assertEquals("Pause.", conversation.lastUserText)
        assertEquals(1, conversation.sendMessageCount)
    }

    @Test
    fun `session handles prefill without user turn tags as fallback`() {
        val conversation = FakeLiteRtConversation(response = "fallback_response")
        val factory = LiteRtFunctionGemmaRuntimeFactory(
            engineFactory = CapturingLiteRtEngineFactory(FakeLiteRtEngine()),
            benchmarkFlags = FakeLiteRtBenchmarkFlags(),
            conversationFactory = FakeConversationFactory(conversation),
        )
        val runtime = factory.create("model", "cache", FunctionGemmaBackend.CPU)

        val session = runtime.createSession()
        session.prefill("plain text without turn markers")

        assertEquals("fallback_response", session.decode())
        assertEquals("plain text without turn markers", conversation.lastUserText)
    }

    @Test
    fun `decode throws when prefill not called`() {
        val factory = LiteRtFunctionGemmaRuntimeFactory(
            engineFactory = CapturingLiteRtEngineFactory(FakeLiteRtEngine()),
            benchmarkFlags = FakeLiteRtBenchmarkFlags(),
            conversationFactory = FakeConversationFactory(FakeLiteRtConversation()),
        )
        val runtime = factory.create("model", "cache", FunctionGemmaBackend.CPU)
        val session = runtime.createSession()

        try {
            session.decode()
            fail("Expected IllegalStateException")
        } catch (_: IllegalStateException) {
            // expected
        }
    }

    @Test
    fun `runtime close is idempotent`() {
        val engine = FakeLiteRtEngine()
        val conversation = FakeLiteRtConversation()
        val runtime = LiteRtFunctionGemmaRuntimeFactory(
            engineFactory = CapturingLiteRtEngineFactory(engine),
            benchmarkFlags = FakeLiteRtBenchmarkFlags(),
            conversationFactory = FakeConversationFactory(conversation),
        ).create("model", "cache", FunctionGemmaBackend.CPU)

        runtime.close()
        runtime.close()

        assertEquals(1, engine.closeCount)
        assertEquals(1, conversation.closeCount)
    }

    @Test
    fun `session close is idempotent`() {
        val runtime = LiteRtFunctionGemmaRuntimeFactory(
            engineFactory = CapturingLiteRtEngineFactory(FakeLiteRtEngine()),
            benchmarkFlags = FakeLiteRtBenchmarkFlags(),
            conversationFactory = FakeConversationFactory(FakeLiteRtConversation()),
        ).create("model", "cache", FunctionGemmaBackend.CPU)

        val session = runtime.createSession()
        session.close()
        session.close()
        // no native session close — just verifying no crash
    }

    // -- Fakes --

    private class CapturingLiteRtEngineFactory(
        private val engine: FakeLiteRtEngine,
    ) : LiteRtEngineFactory {
        var config: LiteRtEngineConfig? = null

        override fun create(config: LiteRtEngineConfig): LiteRtEngine {
            this.config = config
            return engine
        }
    }

    private class FakeLiteRtBenchmarkFlags(
        override var enabled: Boolean = false,
    ) : LiteRtBenchmarkFlags

    private class FakeConversationFactory(
        private val conversation: FakeLiteRtConversation,
    ) : LiteRtConversationFactory {
        override fun create(engine: LiteRtEngine, systemInstruction: String): LiteRtConversation {
            conversation.systemInstruction = systemInstruction
            return conversation
        }
    }

    private class FakeLiteRtEngine(
        private val session: FakeLiteRtSession = FakeLiteRtSession(),
        private val isBenchmarkEnabled: () -> Boolean = { false },
        private val initializeFailure: RuntimeException? = null,
    ) : LiteRtEngine {
        var benchmarkEnabledDuringInitialize = false
        var sessionConfig: LiteRtSessionConfig? = null
        var closeCount = 0

        override fun initialize() {
            benchmarkEnabledDuringInitialize = isBenchmarkEnabled()
            initializeFailure?.let { throw it }
        }

        override fun createSession(config: LiteRtSessionConfig): LiteRtSession {
            sessionConfig = config
            return session
        }

        override fun close() {
            closeCount++
        }
    }

    private class FakeLiteRtSession(
        private val decodeResult: String = "",
    ) : LiteRtSession {
        var prefilledText: String? = null
        var closeCount = 0

        override fun prefill(text: String) {
            prefilledText = text
        }

        override fun decode(): String = decodeResult

        override fun close() {
            closeCount++
        }
    }

    private class FakeLiteRtConversation(
        private val response: String = "",
    ) : LiteRtConversation {
        var systemInstruction: String = ""
        var lastUserText: String? = null
        var sendMessageCount = 0
        var closeCount = 0
        var cancelProcessCount = 0

        override fun sendMessage(userText: String): String {
            lastUserText = userText
            sendMessageCount++
            return response
        }

        override val tokenCount: Int
            get() = sendMessageCount * 100

        override fun cancelProcess() {
            cancelProcessCount++
        }

        override fun close() {
            closeCount++
        }
    }
}

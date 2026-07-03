package au.com.shiftyjelly.pocketcasts.voicecontrol.intent.runtime

import com.google.ai.edge.litertlm.Backend
import com.google.ai.edge.litertlm.Content
import com.google.ai.edge.litertlm.Contents
import com.google.ai.edge.litertlm.Conversation
import com.google.ai.edge.litertlm.ConversationConfig
import com.google.ai.edge.litertlm.Engine
import com.google.ai.edge.litertlm.EngineConfig
import com.google.ai.edge.litertlm.ExperimentalApi
import com.google.ai.edge.litertlm.ExperimentalFlags
import com.google.ai.edge.litertlm.SamplerConfig
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class LiteRtFunctionGemmaRuntimeFactory internal constructor(
    private val engineFactory: LiteRtEngineFactory,
    private val benchmarkFlags: LiteRtBenchmarkFlags,
    private val conversationFactory: LiteRtConversationFactory,
) : FunctionGemmaRuntimeFactory {
    @Inject
    constructor() : this(
        engineFactory = ProductionLiteRtEngineFactory,
        benchmarkFlags = ProductionLiteRtBenchmarkFlags,
        conversationFactory = ProductionLiteRtConversationFactory,
    )

    override fun create(
        modelPath: String,
        cacheDir: String,
        backend: FunctionGemmaBackend,
    ): FunctionGemmaRuntime {
        val engine = synchronized(BENCHMARK_FLAG_LOCK) {
            val previousBenchmarkFlag = benchmarkFlags.enabled
            try {
                benchmarkFlags.enabled = true
                engineFactory.create(
                    LiteRtEngineConfig(
                        modelPath = modelPath,
                        cacheDir = cacheDir,
                        backend = when (backend) {
                            FunctionGemmaBackend.GPU -> LiteRtBackend.GPU
                            FunctionGemmaBackend.CPU -> LiteRtBackend.CPU
                        },
                        maxNumTokens = MAX_CONTEXT_TOKENS,
                        activationDataType = when (backend) {
                            FunctionGemmaBackend.GPU -> LiteRtActivationDataType.FP32
                            FunctionGemmaBackend.CPU -> null
                        },
                    ),
                ).also(LiteRtEngine::initialize)
            } finally {
                benchmarkFlags.enabled = previousBenchmarkFlag
            }
        }
        val conversation = conversationFactory.create(
            engine = engine,
            systemInstruction = FUNCTION_GEMMA_SYSTEM_INSTRUCTION,
        )
        return LiteRtFunctionGemmaRuntime(engine, conversationFactory, conversation, backend)
    }

    private companion object {
        // Matches training config.yaml max_seq_length_cap.
        const val MAX_CONTEXT_TOKENS = 2048
        val BENCHMARK_FLAG_LOCK = Any()

        // LiteRT-LM Conversation API formats this as a system turn using our exact text,
        // preserving the <start_of_turn>developer format the model was fine-tuned on.
        val FUNCTION_GEMMA_SYSTEM_INSTRUCTION: String by lazy {
            au.com.shiftyjelly.pocketcasts.voicecontrol.intent.FunctionGemmaPrompt.staticPrefix
        }
    }
}

internal enum class LiteRtBackend {
    GPU,
    CPU,
}

/**
 * Activation data type for the GPU delegate.
 *
 * The GPU delegate defaults to FP16, which produces all-`<pad>` output with the
 * FunctionGemma model (confirmed on macOS Metal; applies to Android OpenCL as well).
 * Force FP32 so the GPU delegate produces correct output.
 *
 * When litertlm-android EngineConfig exposes set_activation_data_type, wire
 * [LiteRtEngineConfig.activationDataType] through in ProductionLiteRtEngineFactory.
 */
internal enum class LiteRtActivationDataType {
    FP32,
    FP16,
}

internal data class LiteRtEngineConfig(
    val modelPath: String,
    val cacheDir: String,
    val backend: LiteRtBackend,
    val maxNumTokens: Int,
    val activationDataType: LiteRtActivationDataType? = null,
)

internal data class LiteRtSessionConfig(
    val topK: Int,
    val topP: Double,
    val temperature: Double,
    val seed: Int,
)

internal interface LiteRtBenchmarkFlags {
    var enabled: Boolean
}

internal fun interface LiteRtEngineFactory {
    fun create(config: LiteRtEngineConfig): LiteRtEngine
}

internal interface LiteRtEngine : AutoCloseable {
    fun initialize()

    fun createSession(config: LiteRtSessionConfig): LiteRtSession
}

internal interface LiteRtSession : AutoCloseable {
    fun prefill(text: String)

    fun decode(): String
}

// -- Conversation API abstractions (for testability) --

internal interface LiteRtConversationFactory {
    fun create(engine: LiteRtEngine, systemInstruction: String): LiteRtConversation
}

internal interface LiteRtConversation : AutoCloseable {
    fun sendMessage(userText: String): String
    val tokenCount: Int
    fun cancelProcess()
}

// -- Production implementations --

@OptIn(ExperimentalApi::class)
private object ProductionLiteRtBenchmarkFlags : LiteRtBenchmarkFlags {
    override var enabled: Boolean
        get() = ExperimentalFlags.enableBenchmark
        set(value) {
            ExperimentalFlags.enableBenchmark = value
        }
}

private object ProductionLiteRtEngineFactory : LiteRtEngineFactory {
    override fun create(config: LiteRtEngineConfig): LiteRtEngine {
        val engineConfig = EngineConfig(
            modelPath = config.modelPath,
            backend = when (config.backend) {
                LiteRtBackend.GPU -> Backend.GPU()
                LiteRtBackend.CPU -> Backend.CPU()
            },
            maxNumTokens = config.maxNumTokens,
            cacheDir = config.cacheDir,
        )
        // TODO: When litertlm-android EngineConfig adds set_activation_data_type,
        // apply config.activationDataType here to prevent all-<pad> GPU output.
        val engine = Engine(engineConfig)
        return ProductionLiteRtEngine(engine)
    }
}

internal class ProductionLiteRtEngine(
    val engine: Engine,
) : LiteRtEngine {
    override fun initialize() = engine.initialize()

    override fun createSession(config: LiteRtSessionConfig): LiteRtSession {
        val sampler = SamplerConfig(
            topK = config.topK,
            topP = config.topP,
            temperature = config.temperature,
            seed = config.seed,
        )
        return ProductionLiteRtSession(
            engine.createSession(
                com.google.ai.edge.litertlm.SessionConfig(samplerConfig = sampler),
            ),
        )
    }

    override fun close() = engine.close()
}

private class ProductionLiteRtSession(
    private val session: com.google.ai.edge.litertlm.Session,
) : LiteRtSession {
    override fun prefill(text: String) {
        session.runPrefill(listOf(com.google.ai.edge.litertlm.InputData.Text(text)))
    }

    override fun decode(): String = session.runDecode()

    override fun close() = session.close()
}

@OptIn(ExperimentalApi::class)
private object ProductionLiteRtConversationFactory : LiteRtConversationFactory {
    override fun create(engine: LiteRtEngine, systemInstruction: String): LiteRtConversation {
        val realEngine = (engine as ProductionLiteRtEngine).engine

        // Enable automatic context-window management so the conversation evicts old
        // turns instead of growing unbounded and eventually hanging the GPU backend.
        ExperimentalFlags.enableConversationConstrainedDecoding = true
        ExperimentalFlags.filterChannelContentFromKvCache = true

        val config = ConversationConfig(
            systemInstruction = Contents.Companion.of(systemInstruction),
            samplerConfig = SamplerConfig(
                topK = 1,
                topP = 1.0,
                temperature = 0.0,
                seed = 0,
            ),
            automaticToolCalling = false,
        )
        val conversation = realEngine.createConversation(config)
        return ProductionLiteRtConversation(conversation)
    }
}

private class ProductionLiteRtConversation(
    private val conversation: Conversation,
) : LiteRtConversation {
    override fun sendMessage(userText: String): String {
        val response = conversation.sendMessage(userText, emptyMap())
        return response.contents.contents
            .filterIsInstance<Content.Text>()
            .joinToString("") { it.text }
    }

    override val tokenCount: Int
        get() = conversation.getTokenCount()

    override fun cancelProcess() = conversation.cancelProcess()

    override fun close() = conversation.close()
}

// -- Runtime wrapping the Conversation API --

private class LiteRtFunctionGemmaRuntime(
    private val engine: LiteRtEngine,
    private val conversationFactory: LiteRtConversationFactory,
    private var conversation: LiteRtConversation,
    override val backend: FunctionGemmaBackend,
) : FunctionGemmaRuntime {
    private val isClosed = AtomicBoolean()

    override fun createSession(): FunctionGemmaSession = ConversationFunctionGemmaSession(conversation)

    override fun createSessionWithNewConversation(systemInstruction: String): FunctionGemmaSession {
        conversation.close()
        conversation = conversationFactory.create(engine, systemInstruction)
        return ConversationFunctionGemmaSession(conversation)
    }

    override fun close() {
        if (isClosed.compareAndSet(false, true)) {
            conversation.close()
            engine.close()
        }
    }
}

/**
 * Session wrapper that bridges the [FunctionGemmaSession] prefill/decode contract
 * to the LiteRT-LM Conversation API.
 *
 * [prefill] stores the request suffix text; [decode] extracts the user transcript,
 * sends it via [LiteRtConversation.sendMessage], and returns the model response.
 *
 * The conversation maintains the system instruction KV cache across all sessions,
 * eliminating the per-request static-prefix cost.
 */
private class ConversationFunctionGemmaSession(
    private val conversation: LiteRtConversation,
) : FunctionGemmaSession {
    private var pendingText: String? = null
    private val isClosed = AtomicBoolean()

    override fun prefill(text: String) {
        pendingText = text
    }

    override fun decode(): String {
        val text = checkNotNull(pendingText) { "decode called before prefill" }
        pendingText = null

        // Extract user transcript from the formatted suffix.
        // Format: ...<start_of_turn>user\n{transcript}<end_of_turn>\n<start_of_turn>model\n
        val userStart = text.lastIndexOf("<start_of_turn>user\n")
        val transcript = if (userStart >= 0) {
            val contentStart = userStart + "<start_of_turn>user\n".length
            val endTurn = text.indexOf("<end_of_turn>", contentStart)
            if (endTurn > contentStart) {
                text.substring(contentStart, endTurn)
            } else {
                text
            }
        } else {
            text
        }

        val tokenCountBefore = conversation.tokenCount
        val sendMessageTask = SEND_MESSAGE_EXECUTOR.submit<String> {
            conversation.sendMessage(transcript)
        }
        try {
            return sendMessageTask.get(SEND_MESSAGE_TIMEOUT_MS, TimeUnit.MILLISECONDS)
                .also { result ->
                    val tokenCountAfter = conversation.tokenCount
                    Timber.d("FunctionGemma sendMessage done: tokenCount=$tokenCountBefore→$tokenCountAfter delta=${tokenCountAfter - tokenCountBefore} outputLen=${result.length}")
                }
        } catch (e: TimeoutException) {
            val tokenCountAfter = runCatching { conversation.tokenCount }.getOrDefault(-1)
            Timber.w(e, "FunctionGemma sendMessage timed out; tokenCount=$tokenCountBefore→$tokenCountAfter")
            Thread { runCatching { conversation.cancelProcess() } }.start()
            sendMessageTask.cancel(true)
            throw SendMessageTimeoutException(
                "sendMessage timed out after ${SEND_MESSAGE_TIMEOUT_MS}ms (tokens: $tokenCountBefore→$tokenCountAfter)",
            )
        } catch (e: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IllegalStateException("sendMessage interrupted", e)
        }
    }

    override fun close() {
        if (isClosed.compareAndSet(false, true)) {
            pendingText = null
        }
    }

    override val tokenCount: Int
        get() = conversation.tokenCount

    private companion object {
        private val SEND_MESSAGE_EXECUTOR = Executors.newCachedThreadPool { runnable ->
            Thread(runnable, "fg-send-message").apply { isDaemon = true }
        }

        const val SEND_MESSAGE_TIMEOUT_MS = 30_000L
    }
}

internal class SendMessageTimeoutException(message: String) : IllegalStateException(message)

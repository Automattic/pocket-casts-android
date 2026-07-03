package au.com.shiftyjelly.pocketcasts.voicecontrol.intent.runtime

enum class FunctionGemmaBackend {
    GPU,
    CPU,
}

fun interface MonotonicClock {
    fun elapsedRealtimeMs(): Long
}

interface FunctionGemmaRuntimeFactory {
    /**
     * Initializes the model runtime and blocks the calling thread. Call from a worker thread.
     */
    fun create(
        modelPath: String,
        cacheDir: String,
        backend: FunctionGemmaBackend,
    ): FunctionGemmaRuntime
}

interface FunctionGemmaRuntime : AutoCloseable {
    val backend: FunctionGemmaBackend

    fun createSession(): FunctionGemmaSession

    /**
     * Creates a new conversation-backed session with [systemInstruction] pre-filled.
     *
     * Call when the pool decides to rotate conversations (long gap, approaching token limit).
     * Implementations that don't manage conversation state may return [createSession].
     */
    fun createSessionWithNewConversation(systemInstruction: String): FunctionGemmaSession
}

interface FunctionGemmaSession : AutoCloseable {
    fun prefill(text: String)

    fun decode(): String

    val tokenCount: Int
}

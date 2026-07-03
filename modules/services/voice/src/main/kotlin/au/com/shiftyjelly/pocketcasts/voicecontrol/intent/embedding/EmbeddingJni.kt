package au.com.shiftyjelly.pocketcasts.voicecontrol.intent.embedding

object EmbeddingJni {
    init {
        System.loadLibrary("onnxruntime")
        System.loadLibrary("pocketcasts_voice_capture")
    }

    /** Load the ONNX model from raw bytes. Returns true on success. */
    external fun nativeInit(modelData: ByteArray): Boolean

    /** Last error message from the native layer. */
    external fun nativeError(): String

    /** Embedding dimension (384 for multilingual-e5-small). */
    external fun nativeDim(): Int

    /** Run CLS-pooled, L2-normalized embedding on [tokenIds]. */
    external fun nativeEmbed(tokenIds: IntArray): FloatArray?

    /** Release the ORT session. */
    external fun nativeClose()
}

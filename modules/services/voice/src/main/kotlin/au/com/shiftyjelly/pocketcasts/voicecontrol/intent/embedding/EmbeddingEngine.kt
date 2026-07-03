package au.com.shiftyjelly.pocketcasts.voicecontrol.intent.embedding

/**
 * Runs embedding model inference and returns a normalized vector.
 * Separated as an interface to allow swapping between JNI ONNX Runtime
 * and Java ONNX Runtime API implementations.
 */
interface EmbeddingEngine {
    /** Load the ONNX model. Called once at startup. */
    fun load(modelPath: String): Boolean

    /**
     * Run inference on [tokenIds] and return an L2-normalized embedding vector.
     * The implementation handles mean pooling over the last hidden state
     * and normalization. E5 models use the CLS token representation.
     */
    fun embed(tokenIds: IntArray): FloatArray

    /** Dimension of the output embedding vector. */
    val embeddingDim: Int
}

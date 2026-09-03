package au.com.shiftyjelly.pocketcasts.voicecontrol.intent.lfm

object LfmNative {
    init {
        System.loadLibrary("pocketcasts_voice_llm")
    }

    fun lastError(): String = lastErrorNative()

    fun loadClassifier(classifierPath: String, labelMapPath: String, expectedHiddenSize: Int = -1): Boolean = nativeLoadClassifier(classifierPath, labelMapPath, expectedHiddenSize)

    fun classifyEmbedding(embedding: FloatArray): String? = nativeClassifyEmbedding(embedding)

    fun releaseClassifier() = nativeReleaseClassifier()

    fun load(
        modelPath: String,
        classifierPath: String,
        labelMapPath: String,
        nCtx: Int = 2048,
    ): Boolean = nativeLoad(modelPath, classifierPath, labelMapPath, nCtx)

    fun classify(promptTokenIds: IntArray, poolStart: Int, poolEnd: Int): String? = nativeClassify(promptTokenIds, poolStart, poolEnd)

    fun generate(prefill: String, nPredict: Int = 64): String? = nativeGenerate(prefill, nPredict)

    fun tokenize(text: String, addBos: Boolean = false): IntArray? = nativeTokenize(text, addBos)

    fun reset() = nativeReset()

    fun release() = nativeRelease()

    private external fun lastErrorNative(): String

    private external fun nativeLoadClassifier(
        classifierPath: String,
        labelMapPath: String,
        expectedHiddenSize: Int,
    ): Boolean

    private external fun nativeClassifyEmbedding(embedding: FloatArray): String?

    private external fun nativeReleaseClassifier()

    private external fun nativeLoad(
        modelPath: String,
        classifierPath: String,
        labelMapPath: String,
        nCtx: Int,
    ): Boolean

    private external fun nativeClassify(
        promptTokenIds: IntArray,
        poolStart: Int,
        poolEnd: Int,
    ): String?

    private external fun nativeGenerate(prefill: String, nPredict: Int): String?

    private external fun nativeTokenize(text: String, addBos: Boolean): IntArray?

    private external fun nativeReset()

    private external fun nativeRelease()
}

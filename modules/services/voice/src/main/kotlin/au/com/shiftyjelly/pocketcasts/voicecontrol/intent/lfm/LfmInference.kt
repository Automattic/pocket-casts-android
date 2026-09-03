package au.com.shiftyjelly.pocketcasts.voicecontrol.intent.lfm

internal interface LfmInference {
    fun lastError(): String

    fun load(
        modelPath: String,
        classifierPath: String,
        labelMapPath: String,
        nCtx: Int = 2048,
    ): Boolean

    fun classify(promptTokenIds: IntArray, poolStart: Int, poolEnd: Int): String?

    fun generate(prefill: String, nPredict: Int = 64): String?

    fun tokenize(text: String, addBos: Boolean = false): IntArray?

    fun reset()

    fun release()
}

internal object LfmNativeInference : LfmInference {
    override fun lastError(): String = LfmNative.lastError()

    override fun load(
        modelPath: String,
        classifierPath: String,
        labelMapPath: String,
        nCtx: Int,
    ): Boolean = LfmNative.load(modelPath, classifierPath, labelMapPath, nCtx)

    override fun classify(promptTokenIds: IntArray, poolStart: Int, poolEnd: Int): String? = LfmNative.classify(promptTokenIds, poolStart, poolEnd)

    override fun generate(prefill: String, nPredict: Int): String? = LfmNative.generate(prefill, nPredict)

    override fun tokenize(text: String, addBos: Boolean): IntArray? = LfmNative.tokenize(text, addBos)

    override fun reset() = LfmNative.reset()

    override fun release() = LfmNative.release()
}

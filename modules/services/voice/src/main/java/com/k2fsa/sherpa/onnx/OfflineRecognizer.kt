package com.k2fsa.sherpa.onnx

import android.content.res.AssetManager

data class OfflineSenseVoiceModelConfig(
    var model: String = "",
)

data class OfflineModelConfig(
    var senseVoice: OfflineSenseVoiceModelConfig = OfflineSenseVoiceModelConfig(),
    var tokens: String = "",
    var numThreads: Int = 1,
    var debug: Boolean = false,
    var provider: String = "cpu",
)

data class OfflineRecognizerConfig(
    var featConfig: FeatureConfig = FeatureConfig(),
    var modelConfig: OfflineModelConfig = OfflineModelConfig(),
)

data class OfflineRecognizerResult(
    val text: String,
)

class OfflineRecognizer(
    assetManager: AssetManager? = null,
    val config: OfflineRecognizerConfig,
) {
    private var ptr: Long

    init {
        ptr = if (assetManager != null) {
            newFromAsset(assetManager, config)
        } else {
            newFromFile(config)
        }
    }

    protected fun finalize() {
        if (ptr != 0L) {
            delete(ptr)
            ptr = 0
        }
    }

    fun release() = finalize()

    fun createStream(hotwords: String = ""): OfflineStream {
        val p = if (hotwords.isEmpty()) {
            createStream(ptr)
        } else {
            createStreamWithHotwords(ptr, hotwords)
        }
        return OfflineStream(p)
    }

    fun decode(stream: OfflineStream) = decode(ptr, stream.ptr)

    fun decodeStreams(streams: Array<OfflineStream>) {
        val ptrs = LongArray(streams.size) { i -> streams[i].ptr }
        decodeStreams(ptr, ptrs)
    }

    fun getResult(stream: OfflineStream): OfflineRecognizerResult = getResult(ptr, stream.ptr)

    private external fun delete(ptr: Long)
    private external fun newFromAsset(assetManager: AssetManager, config: OfflineRecognizerConfig): Long
    private external fun newFromFile(config: OfflineRecognizerConfig): Long
    private external fun createStream(ptr: Long): Long
    private external fun createStreamWithHotwords(ptr: Long, hotwords: String): Long
    private external fun decode(ptr: Long, streamPtr: Long): Unit
    private external fun decodeStreams(ptr: Long, streamPtrs: LongArray): Unit
    private external fun getResult(ptr: Long, streamPtr: Long): OfflineRecognizerResult

    companion object {
        init {
            System.loadLibrary("sherpa-onnx-jni")
        }
    }
}

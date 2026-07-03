package au.com.shiftyjelly.pocketcasts.voicecontrol.intent.embedding

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import timber.log.Timber

@Singleton
class JniEmbeddingEngine @Inject constructor() : EmbeddingEngine {

    override var embeddingDim = 0
        private set

    private var loaded = false

    override fun load(modelPath: String): Boolean {
        return try {
            val modelFile = File(modelPath)
            if (!modelFile.exists()) {
                Timber.e("Model file not found: %s", modelPath)
                return false
            }
            val modelData = modelFile.readBytes()
            Timber.i("Read %d bytes from model file", modelData.size)

            if (!EmbeddingJni.nativeInit(modelData)) {
                Timber.e("EmbeddingJni.nativeInit failed: %s", EmbeddingJni.nativeError())
                return false
            }
            embeddingDim = EmbeddingJni.nativeDim()
            loaded = true
            Timber.i("JniEmbeddingEngine loaded (dim=%d)", embeddingDim)
            true
        } catch (e: Exception) {
            Timber.e(e, "Failed to load embedding model")
            false
        }
    }

    override fun embed(tokenIds: IntArray): FloatArray {
        if (!loaded) return FloatArray(384)
        return EmbeddingJni.nativeEmbed(tokenIds) ?: FloatArray(384)
    }
}

package au.com.shiftyjelly.pocketcasts.voicecontrol.asr

import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineSenseVoiceModelConfig
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import timber.log.Timber

@Singleton
class SenseVoiceBackend @Inject constructor() : AsrBackend {

    private var recognizer: OfflineRecognizer? = null
    private var modelDir: File? = null

    override suspend fun ensureReady(): Result<Unit> = withContext(Dispatchers.IO) {
        val dir = modelDir
        if (dir == null || !dir.exists()) {
            return@withContext Result.failure(IllegalStateException("SenseVoice model directory not set"))
        }
        val modelFile = File(dir, SENSEVOICE_MODEL_FILENAME)
        val tokensFile = File(dir, SENSEVOICE_TOKENS_FILENAME)
        if (!modelFile.exists() || !tokensFile.exists()) {
            return@withContext Result.failure(IllegalStateException("SenseVoice model files missing"))
        }
        try {
            val config = OfflineRecognizerConfig(
                featConfig = FeatureConfig(sampleRate = 16000, featureDim = 80),
                modelConfig = OfflineModelConfig(
                    senseVoice = OfflineSenseVoiceModelConfig(model = modelFile.absolutePath),
                    tokens = tokensFile.absolutePath,
                    numThreads = 4,
                    provider = "cpu",
                ),
            )
            recognizer?.release()
            recognizer = OfflineRecognizer(config = config)
            Timber.i("SenseVoiceBackend ready")
            Result.success(Unit)
        } catch (e: Exception) {
            Timber.e(e, "SenseVoice initialization failed")
            Result.failure(e)
        }
    }

    override suspend fun transcribe(samples: FloatArray, sampleRateHz: Int): AsrResult = withContext(Dispatchers.IO) {
        val rec = recognizer
        if (rec == null) {
            return@withContext AsrResult(text = "", detectedLanguage = null)
        }
        try {
            val stream = rec.createStream()
            try {
                stream.acceptWaveform(samples, sampleRateHz)
                rec.decode(stream)
                val result = rec.getResult(stream)
                val trimmed = result.text.trim()
                if (trimmed.isEmpty()) {
                    AsrResult(text = "", detectedLanguage = null)
                } else {
                    // SenseVoice auto-LID prefixes text like "<|zh|>...", strip and detect
                    val lang = detectLanguage(trimmed)
                    val cleanText = stripLanguageTag(trimmed)
                    AsrResult(text = cleanText, detectedLanguage = lang)
                }
            } finally {
                stream.release()
            }
        } catch (e: Exception) {
            Timber.e(e, "SenseVoice transcription failed")
            AsrResult(text = "", detectedLanguage = null)
        }
    }

    /**
     * Sets the model directory path. Called by
     * [au.com.shiftyjelly.pocketcasts.voicecontrol.model.ModelManager] after download.
     */
    fun setModelDir(dir: File) {
        modelDir = dir
    }

    override val requiredModel: ModelSpec = ModelSpec(
        files = listOf(
            ModelFile(
                url = "$SENSEVOICE_BASE_URL/$SENSEVOICE_MODEL_FILENAME",
                filename = SENSEVOICE_MODEL_FILENAME,
                sha256 = "",
            ),
            ModelFile(
                url = "$SENSEVOICE_BASE_URL/$SENSEVOICE_TOKENS_FILENAME",
                filename = SENSEVOICE_TOKENS_FILENAME,
                sha256 = "",
            ),
        ),
        targetDir = "sensevoice-model",
    )

    override val capabilities: AsrCapabilities = AsrCapabilities(
        supportedLanguages = setOf("zh", "en", "ja", "ko", "yue"),
        canTranslateToEnglish = false,
        requiresSnapdragon = false,
    )

    override fun release() {
        recognizer?.release()
        recognizer = null
    }

    companion object {
        private const val SENSEVOICE_BASE_URL =
            "https://hf-mirror.com/csukuangfj/sherpa-onnx-sense-voice-zh-en-ja-ko-yue-2024-07-17/resolve/main"
        private const val SENSEVOICE_MODEL_FILENAME = "model.int8.onnx"
        private const val SENSEVOICE_TOKENS_FILENAME = "tokens.txt"

        private val LANG_PATTERN = Regex("^<\\|(\\w+)\\|>")

        private fun detectLanguage(text: String): String? {
            return LANG_PATTERN.find(text)?.groupValues?.get(1)
        }

        private fun stripLanguageTag(text: String): String {
            return text.replace(LANG_PATTERN, "").trim()
        }
    }
}

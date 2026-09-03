package au.com.shiftyjelly.pocketcasts.voicecontrol.asr

data class AsrToken(
    val text: String,
    val startMs: Int,
    val endMs: Int,
)

data class AsrResult(
    val text: String,
    val detectedLanguage: String? = null,
    val confidence: Float = 1.0f,
    val tokens: List<AsrToken>? = null,
)

data class AsrCapabilities(
    val supportedLanguages: Set<String>,
    val canTranslateToEnglish: Boolean = false,
    val requiresSnapdragon: Boolean = false,
)

data class ModelSpec(
    val files: List<ModelFile>,
    val targetDir: String,
)

data class ModelFile(
    val url: String,
    val filename: String,
    val sha256: String = "",
)

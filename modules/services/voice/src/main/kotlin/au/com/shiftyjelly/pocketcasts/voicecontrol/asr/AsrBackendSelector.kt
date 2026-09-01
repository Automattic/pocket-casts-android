package au.com.shiftyjelly.pocketcasts.voicecontrol.asr

import dagger.Lazy
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AsrBackendSelector @Inject constructor(
    private val whisperCppBackend: Lazy<WhisperCppBackend>,
    private val senseVoiceBackend: Lazy<SenseVoiceBackend>,
    private val canaryFlashBackend: Lazy<CanaryFlashBackend>,
    private val currentLocale: () -> Locale,
) {

    /** Manual override: force a specific backend. Set to "whisper-cpp", "sensevoice", or "canary-flash". */
    var manualOverride: String? = null

    fun select(): AsrBackend {
        val override = manualOverride
        if (override != null) {
            return selectByOverride(override)
        }
        return selectByMatrix()
    }

    private fun selectByOverride(override: String): AsrBackend {
        return when (override.lowercase()) {
            "whisper-cpp" -> whisperCppBackend.get()
            "sensevoice" -> senseVoiceBackend.get()
            "canary-flash" -> canaryFlashBackend.get()
            else -> error("Unknown backend override: $override")
        }
    }

    private fun selectByMatrix(): AsrBackend {
        // Matrix (by OS locale):
        // 1. zh/ja/ko/yue -> SenseVoiceBackend (native text -> ML Kit translation)
        // 2. de/es/fr      -> CanaryFlashBackend (native translate to English)
        // 3. en            -> SenseVoiceBackend (fast non-autoregressive path; no translation)
        // 4. otherwise     -> WhisperBackend (fallback, translate to English)
        val osLang = currentLocale().language
        return when (osLang) {
            in SENSEVOICE_LANGS -> senseVoiceBackend.get()
            in CANARY_LANGS -> canaryFlashBackend.get()
            "en" -> senseVoiceBackend.get()
            else -> whisperCppBackend.get()
        }
    }

    companion object {
        private val SENSEVOICE_LANGS = setOf("zh", "ja", "ko", "yue")
        private val CANARY_LANGS = setOf("de", "es", "fr")
    }
}

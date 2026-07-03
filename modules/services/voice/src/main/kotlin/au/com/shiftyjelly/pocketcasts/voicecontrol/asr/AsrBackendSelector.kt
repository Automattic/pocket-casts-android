package au.com.shiftyjelly.pocketcasts.voicecontrol.asr

import dagger.Lazy
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AsrBackendSelector @Inject constructor(
    private val deviceProbe: DeviceProbe,
    private val whisperCppBackend: Lazy<WhisperCppBackend>,
    private val senseVoiceBackend: Lazy<SenseVoiceBackend>,
) {

    /** Manual override: force a specific backend. Set to "whisper-cpp", "sensevoice", or "npu". */
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
            "npu" -> error("NPU backend is not yet implemented (Phase 3)")
            else -> error("Unknown backend override: $override")
        }
    }

    private fun selectByMatrix(): AsrBackend {
        // Matrix:
        // 1. Snapdragon + NPU available + NPU backend shipped -> WhisperNpuBackend (Phase 3)
        // 2. SenseVoice: OS language in {zh, ja, ko, yue} -> SenseVoiceBackend
        // 3. Default -> WhisperCppBackend
        val osLang = Locale.getDefault().language
        if (osLang in SENSEVOICE_LANGS) {
            return senseVoiceBackend.get()
        }
        return whisperCppBackend.get()
    }

    companion object {
        private val SENSEVOICE_LANGS = setOf("zh", "ja", "ko", "yue")
    }
}

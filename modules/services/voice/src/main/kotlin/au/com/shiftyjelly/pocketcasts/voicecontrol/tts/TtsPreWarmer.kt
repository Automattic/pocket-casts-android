package au.com.shiftyjelly.pocketcasts.voicecontrol.tts

import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.VoiceControlRuleState
import au.com.shiftyjelly.pocketcasts.voicecontrol.gate.conditions.ModelsReadyCondition
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class TtsPreWarmer(
    private val ttsEngine: TtsEngine,
    private val modelsReady: ModelsReadyCondition,
    private val scope: CoroutineScope,
    private val defaultLanguage: String = "en",
) {
    fun onModelsStateChanged() {
        if (modelsReady.state.value is VoiceControlRuleState.Allowed) {
            scope.launch {
                ttsEngine.warmUp(defaultLanguage)
            }
        }
    }
}

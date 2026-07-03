package au.com.shiftyjelly.pocketcasts.voicecontrol.dialog

import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.DialogPromptTurn

data class PendingVoiceDialog(
    val targetTool: String,
    val targetAction: String,
    val filledSlots: Map<String, String> = emptyMap(),
    val missingSlots: Set<String> = emptySet(),
    val lastQuestion: String = "",
    val requiresConfirmation: Boolean = false,
    val promptHistory: List<DialogPromptTurn> = emptyList(),
)

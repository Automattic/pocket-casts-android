package au.com.shiftyjelly.pocketcasts.voicecontrol.dialog

import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.DialogPromptTurn
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.ToolCall
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.ToolCallMapper
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.VoiceIntent
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceDialogManager @Inject constructor(
    private val mapper: ToolCallMapper,
) {
    @Volatile
    private var pending: PendingVoiceDialog? = null

    val isInProgress: Boolean get() = pending != null
    val pendingDialog: PendingVoiceDialog? get() = pending

    fun promptHistory(): List<DialogPromptTurn> = pending?.promptHistory.orEmpty()

    fun resolve(
        transcript: String,
        generated: String,
        call: ToolCall,
    ): VoiceIntent? {
        val previous = pending
        val result = resolve(call)
        val previousHistory = if (call.isReplacementBegin(previous)) {
            emptyList()
        } else {
            previous?.promptHistory.orEmpty()
        }
        pending = pending?.copy(
            promptHistory = (
                previousHistory +
                    DialogPromptTurn("user", transcript) +
                    DialogPromptTurn("assistant", generated)
                ).takeLast(MAX_PROMPT_TURNS),
        )
        return result
    }

    private fun ToolCall.isReplacementBegin(previous: PendingVoiceDialog?): Boolean {
        return name == "dialog_control" && action == "begin" && pending !== previous
    }

    /**
     * Process a raw tool call before intent mapping. Consumes `dialog_control` tool calls
     * for multi-turn flows. Returns a [VoiceIntent] only when the dialog is complete; returns
     * null while the dialog is still pending.
     */
    fun resolve(call: ToolCall): VoiceIntent? {
        if (call.name == "dialog_control") {
            return handleDialogControl(call)
        }
        // If there's a pending dialog and a new dispatchable tool call arrives, cancel the dialog
        pending = null
        return mapper.map(call)
    }

    private fun handleDialogControl(call: ToolCall): VoiceIntent? {
        return when (call.action) {
            "begin" -> {
                val targetTool = call.stringParam("target_tool") ?: return null
                val targetAction = call.stringParam("target_action") ?: return null
                val isDestructive = targetAction in DESTRUCTIVE_ACTIONS
                pending = PendingVoiceDialog(
                    targetTool = targetTool,
                    targetAction = targetAction,
                    requiresConfirmation = isDestructive,
                )
                null
            }

            "provide_slot" -> {
                val current = pending ?: return null
                val slot = call.stringParam("slot") ?: return null
                val value = call.stringParam("value") ?: return null
                val updated = current.copy(
                    filledSlots = current.filledSlots + (slot to value),
                    missingSlots = current.missingSlots - slot,
                )
                pending = updated
                if (updated.missingSlots.isEmpty() && !updated.requiresConfirmation) {
                    // All slots filled, no confirmation needed — build intent
                    pending = null
                    buildCompletedIntent(updated)
                } else {
                    null
                }
            }

            "confirm" -> {
                val current = pending ?: return null
                pending = null
                if (current.missingSlots.isEmpty()) {
                    buildCompletedIntent(current)
                } else {
                    null
                }
            }

            "deny" -> {
                pending = null
                null
            }

            "cancel" -> {
                pending = null
                null
            }

            "new_command" -> {
                pending = null
                val value = call.stringParam("value") ?: return null
                // Reprocess value as a fresh command — would go back through recognition
                // For now, return null to signal re-routing is needed
                null
            }

            else -> null
        }
    }

    private fun buildCompletedIntent(dialog: PendingVoiceDialog): VoiceIntent? {
        // Convert filled slots + target info to a ToolCall, then map through mapper
        val call = ToolCall(
            name = dialog.targetTool,
            action = dialog.targetAction,
            params = dialog.filledSlots,
        )
        return mapper.map(call)
    }

    companion object {
        private const val MAX_PROMPT_TURNS = 4

        private val DESTRUCTIVE_ACTIONS = setOf(
            "clear",
            "delete",
            "delete_all",
            "unsubscribe",
            "cancel_subscription",
        )
    }
}

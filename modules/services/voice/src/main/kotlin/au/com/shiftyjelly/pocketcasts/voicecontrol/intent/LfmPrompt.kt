package au.com.shiftyjelly.pocketcasts.voicecontrol.intent

object LfmPrompt {
    const val SYSTEM = "You map podcast voice commands to a tool call."

    private const val START_OF_TEXT = "<|startoftext|>"
    private const val IM_START = "<|im_start|>"
    private const val IM_END = "<|im_end|>"
    private const val MAX_HISTORY_TURNS = 4

    fun render(
        transcript: String,
        history: List<DialogPromptTurn> = emptyList(),
    ): String = buildString {
        append(START_OF_TEXT)
        append(IM_START)
        append("system\n")
        append(SYSTEM)
        append(IM_END)
        append('\n')
        history.takeLast(MAX_HISTORY_TURNS).forEach { turn ->
            append(IM_START)
            append(turn.role)
            append('\n')
            append(turn.content)
            append(IM_END)
            append('\n')
        }
        append(IM_START)
        append("user\n")
        append(transcript)
        append(IM_END)
        append('\n')
        append(IM_START)
        append("assistant\n")
    }
}

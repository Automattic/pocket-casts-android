package au.com.shiftyjelly.pocketcasts.voicecontrol.intent.lfm

object LfmCallPrefill {
    private const val TOOL_CALL_START = "<|tool_call_start|>"
    private const val TOOL_CALL_END = "<|tool_call_end|>"

    fun render(tool: String, action: String): String {
        val escapedAction = action.replace("'", "\\'")
        val inner = "$tool(action='$escapedAction')"
        val full = "$TOOL_CALL_START[$inner)]$TOOL_CALL_END"
        val suffix = ")]$TOOL_CALL_END"
        require(full.endsWith(suffix)) { "unexpected tool-call suffix" }
        return full.removeSuffix(suffix)
    }
}

object LfmLabel {
    fun parse(label: String): Pair<String, String> {
        val separator = label.indexOf(':')
        require(separator >= 0) { "invalid label $label" }
        val tool = label.substring(0, separator)
        val action = label.substring(separator + 1)
        return tool to action
    }
}

object LfmTokenSpan {
    fun lastUserTokenSpan(promptTokenIds: IntArray, userTokenIds: IntArray): Pair<Int, Int> {
        require(promptTokenIds.isNotEmpty()) { "prompt must not be empty" }
        require(userTokenIds.isNotEmpty()) { "user utterance must not be empty" }
        var start = -1
        val userList = userTokenIds.toList()
        for (index in 0..promptTokenIds.size - userTokenIds.size) {
            val slice = promptTokenIds.sliceArray(index until index + userTokenIds.size).toList()
            if (slice == userList) {
                start = index
            }
        }
        require(start >= 0) { "user utterance tokens not found in prompt" }
        return start to start + userTokenIds.size - 1
    }
}

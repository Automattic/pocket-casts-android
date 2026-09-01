package au.com.shiftyjelly.pocketcasts.voicecontrol.intent

object LfmToolCallParser {
    private const val TOOL_CALL_START = "<|tool_call_start|>"
    private const val TOOL_CALL_END = "<|tool_call_end|>"

    fun parse(response: String): ToolCall? {
        val callText = extractLastCallText(response) ?: return null
        return parsePythonicCall(callText)
    }

    private fun extractLastCallText(response: String): String? {
        var searchFrom = 0
        var lastCall: String? = null
        while (true) {
            val startIdx = response.indexOf(TOOL_CALL_START, searchFrom)
            if (startIdx == -1) break
            val openBracket = response.indexOf('[', startIdx + TOOL_CALL_START.length)
            if (openBracket == -1) break
            val closeBracket = response.indexOf(']', openBracket + 1)
            if (closeBracket == -1) break
            val endIdx = response.indexOf(TOOL_CALL_END, closeBracket)
            if (endIdx == -1) break
            lastCall = response.substring(openBracket + 1, closeBracket)
            searchFrom = endIdx + TOOL_CALL_END.length
        }
        return lastCall
    }

    private fun parsePythonicCall(callText: String): ToolCall? {
        val trimmed = callText.trim()
        val openParen = trimmed.indexOf('(')
        if (openParen == -1) return null
        if (!trimmed.endsWith(')')) return null

        val toolName = trimmed.substring(0, openParen).trim()
        if (toolName.isEmpty()) return null
        if (toolName == "no_match") return ToolCall("no_match", "", emptyMap())

        val argsText = trimmed.substring(openParen + 1, trimmed.length - 1).trim()
        if (argsText.isEmpty()) return ToolCall(toolName, "", emptyMap())

        val params = mutableMapOf<String, Any?>()
        var action: String? = null
        var pos = 0
        while (pos < argsText.length) {
            while (pos < argsText.length && argsText[pos].isWhitespace()) pos++
            if (pos >= argsText.length) break

            val keyEnd = argsText.indexOf('=', pos)
            if (keyEnd == -1) return null
            val key = argsText.substring(pos, keyEnd).trim()
            if (key.isEmpty()) return null
            pos = keyEnd + 1
            while (pos < argsText.length && argsText[pos].isWhitespace()) pos++
            if (pos >= argsText.length) return null

            val parsed = parseValue(argsText, pos) ?: return null
            when (key) {
                "action" -> action = parsed.value as? String
                else -> params[key] = parsed.value
            }
            pos = parsed.nextIndex
            while (pos < argsText.length && (argsText[pos].isWhitespace() || argsText[pos] == ',')) pos++
        }

        return ToolCall(toolName, action ?: "", params)
    }

    private data class ParsedValue(
        val value: Any?,
        val nextIndex: Int,
    )

    private fun parseValue(text: String, start: Int): ParsedValue? {
        if (start >= text.length) return null
        return when (text[start]) {
            '\'' -> parseQuotedString(text, start)
            else -> parseBareValue(text, start)
        }
    }

    private fun parseQuotedString(text: String, start: Int): ParsedValue? {
        if (text[start] != '\'') return null
        val builder = StringBuilder()
        var index = start + 1
        while (index < text.length) {
            when (val char = text[index]) {
                '\\' -> {
                    index++
                    if (index >= text.length) return null
                    builder.append(text[index])
                }

                '\'' -> return ParsedValue(builder.toString(), index + 1)

                else -> builder.append(char)
            }
            index++
        }
        return null
    }

    private fun parseBareValue(text: String, start: Int): ParsedValue? {
        val end = text.indexOf(',', start).let { if (it == -1) text.length else it }
        val raw = text.substring(start, end).trim()
        if (raw.isEmpty()) return null
        val value = when {
            raw.equals("True", ignoreCase = false) -> true
            raw.equals("False", ignoreCase = false) -> false
            raw.equals("None", ignoreCase = false) -> null
            raw.equals("true", ignoreCase = true) -> true
            raw.equals("false", ignoreCase = true) -> false
            raw.equals("null", ignoreCase = true) -> null
            raw.toIntOrNull() != null -> raw.toInt()
            raw.toDoubleOrNull() != null -> raw.toDouble()
            else -> raw
        }
        return ParsedValue(value, end)
    }
}

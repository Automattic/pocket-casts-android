package au.com.shiftyjelly.pocketcasts.voicecontrol.intent

import org.json.JSONObject

data class ToolCall(
    val name: String,
    val action: String,
    val params: Map<String, Any?>,
) {
    fun stringParam(key: String): String? = params[key] as? String
    fun intParam(key: String): Int? = (params[key] as? Number)?.toInt()
    fun doubleParam(key: String): Double? = (params[key] as? Number)?.toDouble()
    fun boolParam(key: String): Boolean? = params[key] as? Boolean

    companion object {
        private const val TOOL_CALL_START = "▎"
        private const val ACTION_KEY = "action"

        fun parse(response: String): ToolCall? {
            // Gemma function-calling format:
            // <start_function_call>call:tool{key:<escape>value</escape>}<end_function_call>
            parseGemmaFunctionCall(response)?.let { return it }

            // Legacy JSON fallback
            val jsonText = extractJsonObject(response) ?: return null
            val json = try {
                JSONObject(jsonText)
            } catch (_: Exception) {
                return null
            }

            val name = json.optString("name") ?: return null
            if (name == "no_match" || !json.has("name")) {
                return if (name == "no_match") ToolCall("no_match", "", emptyMap()) else null
            }

            val action = json.optString(ACTION_KEY) ?: return null
            val params = mutableMapOf<String, Any?>()
            val paramsObj = json.optJSONObject("parameters") ?: json.optJSONObject("params")
            if (paramsObj != null) {
                for (key in paramsObj.keys()) {
                    params[key] = when (val v = paramsObj.get(key)) {
                        is JSONObject -> v.toString()
                        else -> v
                    }
                }
            }

            return ToolCall(name, action, params)
        }

        private fun parseGemmaFunctionCall(response: String): ToolCall? {
            val startTag = "<start_function_call>"
            val endTag = "<end_function_call>"
            val startIdx = response.indexOf(startTag)
            if (startIdx == -1) return null
            val endIdx = response.indexOf(endTag, startIdx)
            if (endIdx == -1) return null

            val body = response.substring(startIdx + startTag.length, endIdx).trim()
            if (!body.startsWith("call:")) return null

            val braceIdx = body.indexOf('{')
            if (braceIdx == -1) return null

            val toolName = body.substring("call:".length, braceIdx).trim()
            if (toolName == "no_match") return ToolCall("no_match", "", emptyMap())

            val argsStr = body.substring(braceIdx + 1, body.lastIndexOf('}')).trim()
            if (argsStr.isEmpty()) return ToolCall(toolName, "", emptyMap())

            val params = mutableMapOf<String, Any?>()
            var action: String? = null
            var pos = 0
            while (pos < argsStr.length) {
                while (pos < argsStr.length && argsStr[pos].isWhitespace()) pos++
                if (pos >= argsStr.length) break

                val keyEnd = argsStr.indexOf(':', pos)
                if (keyEnd == -1) break
                val key = argsStr.substring(pos, keyEnd).trim()
                pos = keyEnd + 1
                while (pos < argsStr.length && argsStr[pos].isWhitespace()) pos++
                if (pos >= argsStr.length) break

                val value: String
                if (argsStr.startsWith("<escape>", pos)) {
                    val escapeEnd = argsStr.indexOf("</escape>", pos + "<escape>".length)
                    if (escapeEnd == -1) break
                    value = argsStr.substring(pos + "<escape>".length, escapeEnd)
                    pos = escapeEnd + "</escape>".length
                } else {
                    val commaIdx = argsStr.indexOf(',', pos)
                    val end = if (commaIdx == -1) argsStr.length else commaIdx
                    value = argsStr.substring(pos, end).trim()
                    pos = end
                }

                when (key) {
                    "action" -> action = value
                    else -> params[key] = parseParamValue(value)
                }
                while (pos < argsStr.length && (argsStr[pos].isWhitespace() || argsStr[pos] == ',')) pos++
            }

            return ToolCall(toolName, action ?: "", params)
        }

        private fun parseParamValue(raw: String): Any? {
            return when {
                raw.equals("true", ignoreCase = true) -> true
                raw.equals("false", ignoreCase = true) -> false
                raw.toIntOrNull() != null -> raw.toInt()
                raw.toDoubleOrNull() != null -> raw.toDouble()
                else -> raw
            }
        }

        private fun extractJsonObject(response: String): String? {
            val normalized = response
                .trim()
                .removePrefix(TOOL_CALL_START)
                .trim()
                .removeSurrounding("```json", "```")
                .removeSurrounding("```", "```")
                .trim()
            if (normalized.startsWith("{") && normalized.endsWith("}")) return normalized

            val start = normalized.indexOf('{')
            if (start == -1) return null
            var depth = 0
            var inString = false
            var escaped = false
            for (index in start until normalized.length) {
                val char = normalized[index]
                when {
                    escaped -> escaped = false

                    char == '\\' && inString -> escaped = true

                    char == '"' -> inString = !inString

                    !inString && char == '{' -> depth++

                    !inString && char == '}' -> {
                        depth--
                        if (depth == 0) return normalized.substring(start, index + 1)
                    }
                }
            }
            return null
        }
    }
}

object ToolSchema {
    val json: String = """
        [
          {
            "name": "playback",
            "description": "Basic playback controls: pause, resume, skip forward or backward, seek to a position, play next episode.",
            "parameters": {
              "action": {"type": "string", "enum": ["pause", "resume", "seek_relative", "seek_to", "next_episode"]},
              "delta_seconds": {"type": "integer", "description": "Signed seek delta. Use with seek_relative; positive=forward, negative=backward. Omit when the utterance has no explicit duration — SlotRepair fills a signed ±30s default from forward/back wording."},
              "position_seconds": {"type": "integer", "description": "Non-negative absolute episode position in seconds from the beginning. Use with seek_to; 0 means the beginning."}
            }
          },
          {
            "name": "effects",
            "description": "Playback effects: speed, trim silence, volume boost.",
            "parameters": {
              "action": {"type": "string", "enum": ["set_speed", "adjust_speed", "set_trim_mode", "set_volume_boost", "query_effects"]},
              "speed": {"type": "number", "description": "Playback speed (0.5-5.0)."},
              "delta": {"type": "number", "description": "Speed delta. Positive = faster, negative = slower."},
              "mode": {"type": "string", "enum": ["off", "low", "medium", "high"], "description": "Trim silence mode."},
              "enabled": {"type": "boolean", "description": "On/off for volume boost."}
            }
          },
          {
            "name": "volume",
            "description": "Control device volume.",
            "parameters": {
              "action": {"type": "string", "enum": ["set_volume", "adjust_volume", "query"]},
              "volume": {"type": "integer", "description": "Volume level (0-100)."},
              "delta": {"type": "integer", "description": "Volume delta. Positive = louder, negative = quieter."}
            }
          },
          {
            "name": "sleep",
            "description": "Sleep timer: set a timer, stop at end of episode or chapter, add time, cancel.",
            "parameters": {
              "action": {"type": "string", "enum": ["set", "end_of_episode", "end_of_chapter", "add_time", "cancel", "query"]},
              "minutes": {"type": "integer", "description": "Duration in minutes."}
            }
          },
          {
            "name": "chapter",
            "description": "Navigate and query episode chapters.",
            "parameters": {
              "action": {"type": "string", "enum": ["next", "previous", "by_index", "by_title", "open_link", "query_list", "query_current", "query_count", "query_next"]},
              "index": {"type": "integer", "description": "Chapter number (1-based)."},
              "query": {"type": "string", "description": "Chapter title search query."}
            }
          },
          {
            "name": "bookmark",
            "description": "Create, rename, play, delete, and query bookmarks.",
            "parameters": {
              "action": {"type": "string", "enum": ["add", "rename", "play", "delete", "delete_all", "query_list", "query_count", "query_nearby"]},
              "title": {"type": "string", "description": "Bookmark title."},
              "ref": {"type": "string", "description": "Bookmark reference: title, index, or 'latest'."}
            }
          },
          {
            "name": "queue",
            "description": "Manage the Up Next queue: add, remove, reorder, clear.",
            "parameters": {
              "action": {"type": "string", "enum": ["add_top", "add_bottom", "remove", "move_to_top", "move_to_bottom", "clear", "remove_by_podcast", "sort", "query_contents", "query_next", "query_length", "query_is_queued"]},
              "episode": {"type": "string", "description": "Episode title or description."},
              "podcast": {"type": "string", "description": "Podcast name."},
              "sort_order": {"type": "string", "enum": ["newest_first", "oldest_first"]}
            }
          },
          {
            "name": "playback_query",
            "description": "Query current playback state and episode info.",
            "parameters": {
              "action": {"type": "string", "enum": ["whats_playing", "position", "time_remaining", "current_podcast", "episode_duration", "publish_date", "episode_description", "download_status", "episode_title"]}
            }
          },
          {
            "name": "stats_query",
            "description": "Query listening statistics.",
            "parameters": {
              "action": {"type": "string", "enum": ["listening_time", "top_podcasts", "episodes_finished", "listening_streak", "subscription_count", "unplayed_total", "download_stats", "queue_total", "new_episodes", "time_since_last_listen"]},
              "period": {"type": "string", "description": "Time period."},
              "timeframe": {"type": "string", "description": "Time window."}
            }
          },
          {
            "name": "cloud_route",
            "description": "Route cross-podcast, cross-episode, web-backed, or cloud-enhanced assistant requests to the cloud assistant. Use when the request is broader than current episode local playback, metadata, queue, chapter, bookmark, or transcript tools. Preserve the full user request; do not locally decompose it into local actions.",
            "parameters": {
              "action": {"type": "string", "enum": ["route"]},
              "request": {"type": "string", "description": "The complete user utterance or resolved multi-turn request to send to cloud."},
              "tier": {"type": "string", "enum": ["free", "premium", "unknown"], "description": "Lowest cloud tier that appears to cover the request. Use unknown when tier cannot be determined locally."}
            }
          },
          {
            "name": "dialog_control",
            "description": "Router-only control for bounded multi-turn voice dialogs. Use only to start a supported clarification/confirmation flow, fill a pending slot, confirm, deny, cancel, or signal that the user started a new command. This tool never dispatches an app action directly.",
            "parameters": {
              "action": {"type": "string", "enum": ["begin", "provide_slot", "confirm", "deny", "cancel", "new_command"]},
              "target_tool": {"type": "string", "description": "Tool being clarified, such as bookmark or queue."},
              "target_action": {"type": "string", "description": "Action being clarified, such as rename or clear."},
              "slot": {"type": "string", "description": "Pending slot supplied by the current turn, such as ref or title."},
              "value": {"type": "string", "description": "Normalized slot value, or the replacement utterance when action is new_command."}
            }
          },
          {
            "name": "no_match",
            "description": "No command was recognized. Select this when the user is not issuing a voice command."
          }
        ]
    """.trimIndent()
}

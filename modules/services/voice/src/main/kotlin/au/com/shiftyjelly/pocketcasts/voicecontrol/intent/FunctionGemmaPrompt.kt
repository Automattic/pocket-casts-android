package au.com.shiftyjelly.pocketcasts.voicecontrol.intent

data class DialogPromptTurn(
    val role: String,
    val content: String,
)

object FunctionGemmaPrompt {
    val staticPrefix: String by lazy {
        buildString {
            append("<start_of_turn>developer\n")
            append("You are a model that can do function calling with the following functions")
            for (declaration in FUNCTION_DECLARATIONS) append(declaration)
            append("<end_of_turn>")
        }
    }

    fun requestSuffix(
        transcript: String,
        history: List<DialogPromptTurn>,
    ): String = buildString {
        history.forEach { turn ->
            append("\n<start_of_turn>")
            append(turn.role)
            append('\n')
            append(turn.content)
            append("<end_of_turn>")
        }
        append("\n<start_of_turn>user\n")
        append(transcript)
        append("<end_of_turn>\n<start_of_turn>model\n")
    }

    // Helpers for building tool declarations in <start_function_declaration> format.
    // Must precede FUNCTION_DECLARATIONS / staticPrefix — Kotlin init order.

    private class DeclarationBuilder {
        val properties = mutableListOf<Prop>()
        fun param(
            name: String,
            type: String,
            description: String = "",
            vararg enumValues: String,
            required: Boolean = false,
        ) {
            properties.add(Prop(name, type, description, enumValues.toList(), required))
        }
    }

    private data class Prop(
        val name: String,
        val type: String,
        val description: String,
        val enumValues: List<String>,
        val required: Boolean,
    )

    private fun declaration(
        name: String,
        description: String,
        params: DeclarationBuilder.() -> Unit,
    ): String {
        val builder = DeclarationBuilder()
        builder.params()
        val propParts = mutableListOf<String>()
        for (p in builder.properties) {
            val inner = mutableListOf<String>()
            if (p.description.isNotEmpty()) inner.add("description:<escape>${p.description}<escape>")
            if (p.enumValues.isNotEmpty()) {
                inner.add("enum:[${p.enumValues.joinToString(",") { "<escape>$it<escape>" }}]")
            }
            inner.add("type:<escape>${p.type}<escape>")
            propParts.add("${p.name}:{${inner.joinToString(",")}}")
        }
        val propertiesStr = propParts.joinToString(",")

        var paramsSection = ""
        if (propertiesStr.isNotEmpty()) {
            val hasExplicitRequired = builder.properties.any { it.required }
            val requiredNames = if (hasExplicitRequired) {
                builder.properties.filter { it.required }.map { it.name }
            } else if (builder.properties.any { it.name == "action" }) {
                listOf("action")
            } else {
                emptyList()
            }
            val requiredStr = if (requiredNames.isNotEmpty()) {
                ",required:[${requiredNames.joinToString(",") { "<escape>$it<escape>" }}]"
            } else {
                ""
            }
            paramsSection = ",parameters:{properties:{$propertiesStr}$requiredStr,type:<escape>OBJECT<escape>}"
        }

        return "<start_function_declaration>declaration:$name{" +
            "description:<escape>$description<escape>" +
            paramsSection +
            "}<end_function_declaration>"
    }

    /** Tool declarations in <start_function_declaration> format, matching _format_declaration in _data.py. */
    private val FUNCTION_DECLARATIONS: List<String> = listOf(
        declaration("playback", "Basic playback controls: pause, resume, skip forward or backward, seek to a position, play next episode.") {
            param("action", "STRING", "Action to perform", "pause", "resume", "seek_relative", "seek_to", "next_episode")
            param("seconds", "INTEGER", "Seconds. For seek_relative: signed delta (positive=forward, negative=backward). For seek_to: absolute position from 0.")
        },
        declaration("effects", "Playback effects: speed, trim silence, volume boost.") {
            param("action", "STRING", "", "set_speed", "adjust_speed", "set_trim_mode", "set_volume_boost", "query_effects")
            param("speed", "NUMBER", "Playback speed (0.5-5.0).")
            param("delta", "NUMBER", "Speed delta. Positive = faster, negative = slower.")
            param("mode", "STRING", "Trim silence mode.", "off", "low", "medium", "high")
            param("enabled", "BOOLEAN", "On/off for volume boost.")
        },
        declaration("volume", "Control device volume.") {
            param("action", "STRING", "", "set_volume", "adjust_volume", "query")
            param("volume", "INTEGER", "Volume level (0-100).")
            param("delta", "INTEGER", "Volume delta. Positive = louder, negative = quieter.")
        },
        declaration("sleep", "Sleep timer: set a timer, stop at end of episode or chapter, add time, cancel.") {
            param("action", "STRING", "", "set", "end_of_episode", "end_of_chapter", "add_time", "cancel", "query")
            param("minutes", "INTEGER", "Duration in minutes.")
        },
        declaration("chapter", "Navigate and query episode chapters.") {
            param("action", "STRING", "", "next", "previous", "by_index", "by_title", "open_link", "query_list", "query_current", "query_count", "query_next")
            param("index", "INTEGER", "Chapter number (1-based).")
            param("query", "STRING", "Chapter title search query.")
        },
        declaration("bookmark", "Create, rename, play, delete, and query bookmarks.") {
            param("action", "STRING", "", "add", "rename", "play", "delete", "delete_all", "query_list", "query_count", "query_nearby")
            param("title", "STRING", "Bookmark title.")
            param("ref", "STRING", "Bookmark reference: title, index, or 'latest'.")
        },
        declaration("queue", "Manage the Up Next queue: add, remove, reorder, clear.") {
            param("action", "STRING", "", "add_top", "add_bottom", "remove", "move_to_top", "move_to_bottom", "clear", "remove_by_podcast", "sort", "query_contents", "query_next", "query_length", "query_is_queued")
            param("episode", "STRING", "Episode title or description.")
            param("podcast", "STRING", "Podcast name.")
            param("sort_order", "STRING", "", "newest_first", "oldest_first")
        },
        declaration("playback_query", "Query current playback state and episode info.") {
            param("action", "STRING", "", "whats_playing", "position", "time_remaining", "current_podcast", "episode_duration", "publish_date", "episode_description", "download_status", "episode_title")
        },
        declaration("stats_query", "Query listening statistics.") {
            param("action", "STRING", "", "listening_time", "top_podcasts", "episodes_finished", "listening_streak", "subscription_count", "unplayed_total", "download_stats", "queue_total", "new_episodes", "time_since_last_listen")
            param("period", "STRING", "Time period.")
            param("timeframe", "STRING", "Time window.")
        },
        declaration("cloud_route", "Route cross-podcast, cross-episode, web-backed, or cloud-enhanced assistant requests to the cloud assistant. Use when the request is broader than current episode local playback, metadata, queue, chapter, bookmark, or transcript tools. Preserve the full user request; do not locally decompose it into local actions.") {
            param("action", "STRING", "", "route")
            param("request", "STRING", "The complete user utterance or resolved multi-turn request to send to cloud.")
            param("tier", "STRING", "Lowest cloud tier that appears to cover the request. Use unknown when tier cannot be determined locally.", "free", "premium", "unknown")
        },
        declaration("dialog_control", "Router-only control for bounded multi-turn voice dialogs. Use only to start a supported clarification/confirmation flow, fill a pending slot, confirm, deny, cancel, or signal that the user started a new command. This tool never dispatches an app action directly.") {
            param("action", "STRING", "", "begin", "provide_slot", "confirm", "deny", "cancel", "new_command")
            param("target_tool", "STRING", "Tool being clarified, such as bookmark or queue.")
            param("target_action", "STRING", "Action being clarified, such as rename or clear.")
            param("slot", "STRING", "Pending slot supplied by the current turn, such as ref or title.")
            param("value", "STRING", "Normalized slot value, or the replacement utterance when action is new_command.")
        },
        declaration("no_match", "No command was recognized. Select this when the user is not issuing a voice command.") { },
    )
}

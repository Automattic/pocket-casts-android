package au.com.shiftyjelly.pocketcasts.voicecontrol.intent

object SlotRepair {
    fun repair(
        raw: String,
        utterance: String,
        tool: String,
        action: String,
    ): ToolCall? {
        if (tool.isEmpty()) return null
        if (tool == "no_match") {
            return ToolCall("no_match", action, emptyMap())
        }

        val parsed = LfmToolCallParser.parse(raw)
        var params = parsed?.params?.toMutableMap() ?: mutableMapOf()
        params = sanitizeParams(tool, action, params).toMutableMap()
        params = dropNoneLike(params).toMutableMap()
        params = repairNumericParams(tool, action, params, utterance).toMutableMap()
        params = repairStringParams(tool, action, params, utterance).toMutableMap()
        params = sanitizeParams(tool, action, params).toMutableMap()
        params = dropNoneLike(params).toMutableMap()
        params = fillSeekRelativeDefault(tool, action, params, utterance).toMutableMap()
        return ToolCall(tool, action, params)
    }

    internal fun collapseRepetition(text: String): String {
        if (text.isEmpty()) return text
        var previous: String? = null
        var out = text
        while (previous != out) {
            previous = out
            out = WORD_REPEAT_REGEX.replace(out, "$1")
            out = THREE_WORD_REPEAT_REGEX.replace(out, "$1")
            out = TWO_WORD_REPEAT_REGEX.replace(out, "$1")
        }
        return out.replace(Regex("\\s+"), " ").trim(' ', ',')
    }

    private val WORD_REPEAT_REGEX = Regex("""\b(\w+)(?:[\s,]+\1){2,}\b""", RegexOption.IGNORE_CASE)
    private val THREE_WORD_REPEAT_REGEX =
        Regex("""\b(\w+\s+\w+\s+\w+)(?:[\s,]+\1)+\b""", RegexOption.IGNORE_CASE)
    private val TWO_WORD_REPEAT_REGEX =
        Regex("""\b(\w+\s+\w+)(?:[\s,]+\1)+\b""", RegexOption.IGNORE_CASE)

    private val STRING_KEYS = setOf(
        "episode",
        "podcast",
        "title",
        "ref",
        "query",
        "request",
        "value",
        "timeframe",
        "tier",
        "sort_order",
        "mode",
        "slot",
        "target_tool",
        "target_action",
        "period",
    )

    // Must cover every (tool, action) ToolCallMapper / ToolSchema can dispatch so
    // sanitizeParams does not strip legitimate generated slots.
    private val ACTION_PARAMS: Map<Pair<String, String>, Set<String>> = mapOf(
        "playback" to "pause" to emptySet(),
        "playback" to "resume" to emptySet(),
        "playback" to "seek_relative" to setOf("delta_seconds"),
        "playback" to "seek_to" to setOf("position_seconds"),
        "playback" to "next_episode" to emptySet(),
        "effects" to "set_speed" to setOf("speed"),
        "effects" to "adjust_speed" to setOf("delta"),
        "effects" to "set_trim_mode" to setOf("mode"),
        "effects" to "set_volume_boost" to setOf("enabled"),
        "effects" to "query_effects" to emptySet(),
        "volume" to "set_volume" to setOf("volume"),
        "volume" to "adjust_volume" to setOf("delta"),
        "volume" to "query" to emptySet(),
        "sleep" to "set" to setOf("minutes"),
        "sleep" to "end_of_episode" to emptySet(),
        "sleep" to "end_of_chapter" to emptySet(),
        "sleep" to "add_time" to setOf("minutes"),
        "sleep" to "cancel" to emptySet(),
        "sleep" to "query" to emptySet(),
        "chapter" to "next" to emptySet(),
        "chapter" to "previous" to emptySet(),
        "chapter" to "by_index" to setOf("index"),
        "chapter" to "by_title" to setOf("query"),
        "chapter" to "open_link" to setOf("index", "query"),
        "chapter" to "query_list" to emptySet(),
        "chapter" to "query_current" to emptySet(),
        "chapter" to "query_count" to emptySet(),
        "chapter" to "query_next" to emptySet(),
        "bookmark" to "add" to setOf("title"),
        "bookmark" to "rename" to setOf("ref", "title"),
        "bookmark" to "play" to setOf("ref"),
        "bookmark" to "delete" to setOf("ref"),
        "bookmark" to "delete_all" to emptySet(),
        "bookmark" to "query_list" to emptySet(),
        "bookmark" to "query_count" to emptySet(),
        "bookmark" to "query_nearby" to emptySet(),
        "queue" to "add_top" to setOf("episode"),
        "queue" to "add_bottom" to setOf("episode"),
        "queue" to "remove" to setOf("episode"),
        "queue" to "move_to_top" to setOf("episode"),
        "queue" to "move_to_bottom" to setOf("episode"),
        "queue" to "clear" to emptySet(),
        "queue" to "remove_by_podcast" to setOf("podcast"),
        "queue" to "sort" to setOf("sort_order"),
        "queue" to "query_contents" to emptySet(),
        "queue" to "query_next" to emptySet(),
        "queue" to "query_length" to emptySet(),
        "queue" to "query_is_queued" to setOf("episode"),
        "playback_query" to "whats_playing" to emptySet(),
        "playback_query" to "position" to emptySet(),
        "playback_query" to "time_remaining" to emptySet(),
        "playback_query" to "current_podcast" to emptySet(),
        "playback_query" to "episode_duration" to emptySet(),
        "playback_query" to "publish_date" to emptySet(),
        "playback_query" to "episode_description" to emptySet(),
        "playback_query" to "download_status" to emptySet(),
        "playback_query" to "episode_title" to emptySet(),
        "stats_query" to "listening_time" to setOf("period"),
        "stats_query" to "top_podcasts" to setOf("period"),
        "stats_query" to "episodes_finished" to setOf("period"),
        "stats_query" to "listening_streak" to emptySet(),
        "stats_query" to "subscription_count" to emptySet(),
        "stats_query" to "unplayed_total" to emptySet(),
        "stats_query" to "download_stats" to emptySet(),
        "stats_query" to "queue_total" to emptySet(),
        "stats_query" to "new_episodes" to setOf("timeframe"),
        "stats_query" to "time_since_last_listen" to emptySet(),
        "cloud_route" to "route" to setOf("request", "tier"),
        "dialog_control" to "begin" to setOf("target_tool", "target_action"),
        "dialog_control" to "provide_slot" to setOf("target_tool", "target_action", "slot", "value"),
        "dialog_control" to "confirm" to emptySet(),
        "dialog_control" to "deny" to emptySet(),
        "dialog_control" to "cancel" to emptySet(),
        "dialog_control" to "new_command" to setOf("value"),
        "no_match" to "" to emptySet(),
    )

    private fun allowedParams(tool: String, action: String): Set<String> = ACTION_PARAMS[tool to action] ?: emptySet()

    private fun sanitizeParams(tool: String, action: String, params: Map<String, Any?>): Map<String, Any?> {
        val allowed = allowedParams(tool, action)
        return params.filterKeys { it in allowed }
    }

    private fun dropNoneLike(params: Map<String, Any?>): Map<String, Any?> = params.filterValues { value ->
        when (value) {
            null -> false
            is String -> value.isNotBlank() && !value.equals("none", ignoreCase = true)
            else -> true
        }
    }

    private fun repairNumericParams(
        tool: String,
        action: String,
        params: Map<String, Any?>,
        utterance: String,
    ): Map<String, Any?> {
        val allowed = allowedParams(tool, action)
        val extracted = extractNumericSlots(tool, action, utterance)
        val out = params.toMutableMap()
        for ((key, value) in extracted) {
            if (key in allowed) {
                out[key] = value
            }
        }
        return out
    }

    private fun extractNumericSlots(tool: String, action: String, utterance: String): Map<String, Any?> {
        val allowed = allowedParams(tool, action)
        val out = mutableMapOf<String, Any?>()
        if ("delta_seconds" in allowed) {
            extractDeltaSeconds(utterance)?.let { out["delta_seconds"] = it }
        }
        return out
    }

    private fun extractDeltaSeconds(utterance: String): Int? {
        val lower = utterance.lowercase()
        val pairs = durationPairs(utterance)
        val seconds = if (pairs.isEmpty() && A_MINUTE_REGEX.containsMatchIn(lower)) {
            60
        } else {
            secondsFromPairs(pairs)
        } ?: return null
        return if (BACK_REGEX.containsMatchIn(lower)) -seconds else seconds
    }

    /** When the model omits delta_seconds, fill a signed ±30s default from wording. */
    private fun fillSeekRelativeDefault(
        tool: String,
        action: String,
        params: Map<String, Any?>,
        utterance: String,
    ): Map<String, Any?> {
        if (tool != "playback" || action != "seek_relative") return params
        if (params.containsKey("delta_seconds")) return params
        val signed = if (BACK_REGEX.containsMatchIn(utterance.lowercase())) -DEFAULT_SKIP_SECONDS else DEFAULT_SKIP_SECONDS
        return params + ("delta_seconds" to signed)
    }

    private val A_MINUTE_REGEX = Regex("""\ba\s+minute\b""")
    private val BACK_REGEX = Regex("""\b(back|rewind|behind)\b""")
    private const val DEFAULT_SKIP_SECONDS = 30

    private fun durationPairs(utterance: String): List<Pair<Number, String>> {
        val pairs = mutableListOf<Pair<Number, String>>()
        for (match in NUMBER_REGEX.findAll(utterance)) {
            val value = parseNumberPhrase(match.value) ?: continue
            val unitMatch = UNIT_REGEX.matchAt(utterance, match.range.last + 1) ?: continue
            pairs.add(value to unitMatch.groupValues[1].lowercase())
        }
        return pairs
    }

    private fun secondsFromPairs(pairs: List<Pair<Number, String>>): Int? {
        if (pairs.isEmpty()) return null
        var total = 0.0
        for ((value, unit) in pairs) {
            total += toSeconds(value, unit)
        }
        return total.toInt()
    }

    private fun toSeconds(value: Number, unit: String): Double {
        return when {
            unit.startsWith("hour") || unit in setOf("hr", "hrs") -> value.toDouble() * 3600
            unit.startsWith("min") -> value.toDouble() * 60
            else -> value.toDouble()
        }
    }

    private val NUMBER_REGEX = Regex(
        """(?<![A-Za-z])(?:\d+(?:\.\d+)?|(?:twenty|thirty|forty|fifty|sixty|seventy|eighty|ninety)(?:[-\s](?:one|two|three|four|five|six|seven|eight|nine))?|(?:one|two|three|four|five|six|seven|eight|nine|ten|eleven|twelve|thirteen|fourteen|fifteen|sixteen|seventeen|eighteen|nineteen|zero|oh))(?![A-Za-z])""",
        RegexOption.IGNORE_CASE,
    )
    private val UNIT_REGEX = Regex("""\s*(seconds?|secs?|minutes?|mins?|hours?|hrs?)\b""", RegexOption.IGNORE_CASE)

    private fun parseNumberPhrase(text: String): Number? {
        val raw = text.trim().lowercase().replace("-", " ")
        raw.toIntOrNull()?.let { return it }
        raw.toDoubleOrNull()?.let { return it }
        val parts = raw.split(Regex("\\s+"))
        if (parts.size == 2 && parts[0] in TENS && parts[1] in ONES_1_9) {
            return TENS.getValue(parts[0]) + ONES_1_9.getValue(parts[1])
        }
        return ONES[parts.singleOrNull() ?: return null]
    }

    private val ONES = mapOf(
        "zero" to 0, "oh" to 0, "one" to 1, "two" to 2, "three" to 3, "four" to 4,
        "five" to 5, "six" to 6, "seven" to 7, "eight" to 8, "nine" to 9, "ten" to 10,
        "eleven" to 11, "twelve" to 12, "thirteen" to 13, "fourteen" to 14, "fifteen" to 15,
        "sixteen" to 16, "seventeen" to 17, "eighteen" to 18, "nineteen" to 19,
    )
    private val ONES_1_9 = ONES.filterKeys { it in setOf("one", "two", "three", "four", "five", "six", "seven", "eight", "nine") }
    private val TENS = mapOf(
        "twenty" to 20,
        "thirty" to 30,
        "forty" to 40,
        "fifty" to 50,
        "sixty" to 60,
        "seventy" to 70,
        "eighty" to 80,
        "ninety" to 90,
    )

    private fun repairStringParams(
        tool: String,
        action: String,
        params: Map<String, Any?>,
        utterance: String,
    ): Map<String, Any?> {
        val quotes = quotedSpans(utterance)
        val allowed = allowedParams(tool, action)
        val out = params.toMutableMap()
        if (action == "provide_slot" && "value" in allowed) {
            repairProvideSlotValue(out, utterance, quotes)
        }
        for (key in out.keys.toList()) {
            if (key !in STRING_KEYS) continue
            val value = out[key] as? String ?: continue
            repairOneString(key, value, utterance, quotes)?.let { out[key] = it }
        }
        return out
    }

    private fun repairProvideSlotValue(
        params: MutableMap<String, Any?>,
        utterance: String,
        quotes: List<String>,
    ) {
        when (params["slot"]) {
            "title" -> {
                if (quotes.isNotEmpty()) {
                    params["value"] = quotes.last()
                } else {
                    val cleaned = utterance.trim().trimEnd('.', '?', '!', ' ')
                    if (cleaned.isNotEmpty()) {
                        params["value"] = cleaned
                    }
                }
            }

            "ref" -> {
                if (THIS_ONE_REGEX.containsMatchIn(utterance)) {
                    params["value"] = "this"
                }
            }
        }
    }

    private val THIS_ONE_REGEX = Regex("""\bthis one\b""", RegexOption.IGNORE_CASE)

    private fun repairOneString(
        key: String,
        value: String,
        utterance: String,
        quotes: List<String>,
    ): String? {
        val cleaned = collapseRepetition(value.trim())
        if (key == "title" && quotes.isNotEmpty()) {
            val quote = quotes.last()
            if (looksGarbled(cleaned) || isBrokenCopy(cleaned, quote)) {
                return quote
            }
        }
        if (key in setOf("title", "value", "ref") && quotes.isNotEmpty()) {
            val quote = quotes.singleOrNull() ?: quotes.last()
            if (isBrokenCopy(cleaned, quote)) {
                return quote
            }
        }
        return if (cleaned != value) cleaned else null
    }

    internal fun quotedSpans(utterance: String): List<String> {
        val spans = mutableListOf<String>()
        DOUBLE_QUOTE_REGEX.findAll(utterance).forEach { match ->
            spans.add(match.groupValues[1])
        }
        var index = 0
        while (index < utterance.length) {
            if (utterance[index] != '\'') {
                index++
                continue
            }
            val prev = utterance.getOrNull(index - 1)
            val next = utterance.getOrNull(index + 1)
            if (prev?.isLetter() == true && next?.isLetter() == true) {
                index++
                continue
            }
            var cursor = index + 1
            val buffer = StringBuilder()
            var closed = false
            while (cursor < utterance.length) {
                if (utterance.startsWith("'n'", cursor, ignoreCase = true)) {
                    buffer.append(utterance.substring(cursor, cursor + 3))
                    cursor += 3
                    continue
                }
                val char = utterance[cursor]
                if (char != '\'') {
                    buffer.append(char)
                    cursor++
                    continue
                }
                val prevChar = utterance.getOrNull(cursor - 1)
                val nextChar = utterance.getOrNull(cursor + 1)
                if (prevChar?.lowercaseChar() == 'n' && nextChar?.lowercaseChar() == 't') {
                    buffer.append('\'')
                    cursor++
                    continue
                }
                if (prevChar?.isLetter() == true && nextChar?.isLetter() == true) {
                    buffer.append('\'')
                    cursor++
                    continue
                }
                if (buffer.toString().trim().length >= 3) {
                    spans.add(buffer.toString())
                }
                index = cursor + 1
                closed = true
                break
            }
            if (!closed) break
        }
        return spans
    }

    private val DOUBLE_QUOTE_REGEX = Regex(""""([^"]{1,80})"""")

    internal fun looksGarbled(value: String?): Boolean {
        if (value.isNullOrBlank()) return true
        val text = value.trim()
        if (text.equals("none", ignoreCase = true) || text.equals("null", ignoreCase = true)) return true
        if (WAIT_LOOP_REGEX.containsMatchIn(text)) return true
        if (WORD_REPEAT_REGEX.containsMatchIn(text)) return true
        val collapsed = collapseRepetition(text)
        return text.length >= 20 && collapsed.length < text.length * 0.8
    }

    private val WAIT_LOOP_REGEX = Regex("""\bwait(?:[\s,]+wait){2,}\b""", RegexOption.IGNORE_CASE)

    internal fun isBrokenCopy(pred: String, target: String): Boolean {
        if (pred.isBlank() || target.isBlank()) return false
        val predLower = pred.trim().lowercase()
        val targetLower = target.trim().lowercase()
        if (predLower == targetLower) return false
        if (predLower.length >= 6 && targetLower.startsWith(predLower) && targetLower.length > predLower.length + 1) {
            return true
        }
        val ratio = sequenceSimilarity(predLower, targetLower)
        if (ratio >= 0.78) return true
        if (ratio >= 0.62 && shareContentToken(predLower, targetLower)) return true
        return editDistance(predLower, targetLower) <= maxOf(2, targetLower.length / 8)
    }

    private fun shareContentToken(left: String, right: String): Boolean {
        val stop = setOf(
            "the", "a", "an", "of", "and", "to", "in", "for", "on",
            "episode", "from", "that", "this",
        )
        val tokenRegex = Regex("[A-Za-z0-9']+")
        val leftTokens = tokenRegex.findAll(left).map { it.value.lowercase() }
            .filter { it !in stop && it.length >= 4 }.toSet()
        val rightTokens = tokenRegex.findAll(right).map { it.value.lowercase() }
            .filter { it !in stop && it.length >= 4 }.toSet()
        return leftTokens.intersect(rightTokens).isNotEmpty()
    }

    private fun sequenceSimilarity(left: String, right: String): Double {
        if (left == right) return 1.0
        val maxLen = maxOf(left.length, right.length)
        if (maxLen == 0) return 1.0
        return 1.0 - editDistance(left, right).toDouble() / maxLen
    }

    private fun editDistance(left: String, right: String): Int {
        if (left == right) return 0
        if (left.isEmpty()) return right.length
        if (right.isEmpty()) return left.length
        var prev = IntArray(right.length + 1) { it }
        for (i in left.indices) {
            val current = IntArray(right.length + 1)
            current[0] = i + 1
            for (j in right.indices) {
                val cost = if (left[i] == right[j]) 0 else 1
                current[j + 1] = minOf(
                    prev[j + 1] + 1,
                    current[j] + 1,
                    prev[j] + cost,
                )
            }
            prev = current
        }
        return prev[right.length]
    }
}

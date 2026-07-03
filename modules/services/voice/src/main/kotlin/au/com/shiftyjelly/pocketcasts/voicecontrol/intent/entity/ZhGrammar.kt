package au.com.shiftyjelly.pocketcasts.voicecontrol.intent.entity

import javax.inject.Inject
import javax.inject.Singleton

/**
 * Chinese (Mandarin) grammar for extracting duration, number, ordinal, trim mode,
 * and boolean entities from voice command transcripts.
 */
@Singleton
class ZhGrammar @Inject constructor() : LanguageGrammar {
    override val languageCode = "zh"

    override fun canParse(text: String): Boolean {
        val hanCount = text.count { isHan(it) }
        return hanCount > 0 && hanCount.toDouble() / text.length.coerceAtLeast(1) > 0.3
    }

    private fun isHan(c: Char): Boolean = c in '一'..'鿿' || c in '㐀'..'䶿'

    // -- Duration ----------------------------------------------------------

    // Chinese unit → seconds (all multi-character before single to avoid partial match)
    private val unitMultipliers = linkedMapOf(
        "小时" to 3600,
        "钟头" to 3600,
        "分钟" to 60,
        "秒钟" to 1,
        "秒钟" to 1,
        "秒" to 1,
        "分" to 60,
    )

    // Chinese numeral → value
    private val hanNumbers = linkedMapOf(
        "零" to 0, "一" to 1, "二" to 2, "两" to 2, "三" to 3,
        "四" to 4, "五" to 5, "六" to 6, "七" to 7, "八" to 8,
        "九" to 9, "十" to 10,
    )

    override fun extractDuration(text: String): List<Int> {
        val results = mutableListOf<Int>()

        // "半小时" / "半个钟头" = 1800s (30 min)
        if (Regex("""半小时|半个钟头""").containsMatchIn(text)) {
            results.add(1800)
        }

        // "半分钟" = 30s
        if (Regex("""半分钟""").containsMatchIn(text)) {
            results.add(30)
        }

        // "一分半" / "一分半钟" = 90s
        val oneAndHalfRegex = Regex("""(一|两|二|三|四|五|六|七|八|九|十)?(分|分钟|秒)半(钟)?""")
        oneAndHalfRegex.find(text)?.let { match ->
            val num = match.groupValues[1].let { hanNumbers[it] ?: 1 }
            val unit = match.groupValues[2]
            val multiplier = unitMultipliers[unit] ?: 1
            results.add(((num + 0.5) * multiplier).toInt())
        }

        // Compound: "X小时Y分钟Z秒" / "一小时二十分钟"
        val compoundRegex = Regex("""(\d+|[一二两三四五六七八九十]+)?(小时|钟头)?(\d+|[一二两三四五六七八九十]+)?(分钟|分)?(\d+|[一二两三四五六七八九十]+)?(秒)?""")
        compoundRegex.find(text)?.let { match ->
            val hRaw = match.groupValues[1]
            val hUnit = match.groupValues[2]
            val mRaw = match.groupValues[3]
            val mUnit = match.groupValues[4]
            val sRaw = match.groupValues[5]
            val sUnit = match.groupValues[6]

            var total = 0
            var hasMatch = false

            if (hRaw.isNotEmpty() && hUnit.isNotEmpty()) {
                total += (parseHanNumber(hRaw) ?: 0) * (unitMultipliers[hUnit] ?: 3600)
                hasMatch = true
            }
            if (mRaw.isNotEmpty() && (mUnit == "分钟" || mUnit == "分")) {
                total += (parseHanNumber(mRaw) ?: 0) * 60
                hasMatch = true
            }
            if (sRaw.isNotEmpty() && sUnit == "秒") {
                total += parseHanNumber(sRaw) ?: 0
                hasMatch = true
            }

            if (hasMatch && total > 0) {
                results.add(total)
            }
        }

        // Simple: "30秒" / "五分钟" / "两小时"
        val simpleRegex = Regex("""(\d+|[一二两三四五六七八九十]+)\s*(小时|钟头|分钟|分|秒)""")
        for (match in simpleRegex.findAll(text)) {
            val value = match.groupValues[1].toIntOrNull() ?: parseHanNumber(match.groupValues[1])
            val unit = match.groupValues[2]
            val multiplier = unitMultipliers[unit] ?: 1
            if (value != null) {
                results.add(value * multiplier)
            }
        }

        // "一秒" / "一分钟" — standalone with single-character units
        // Only match if the entire phrase is short (avoid false positives on "一分钟" vs complex text)
        val singleUnitRegex = Regex("""每?(一|两)\s*(秒|分)""")
        singleUnitRegex.find(text)?.let { match ->
            val unit = match.groupValues[2]
            val multiplier = unitMultipliers[unit] ?: 1
            results.add(multiplier)
        }

        return results.distinct()
    }

    // -- Number ------------------------------------------------------------

    // Digit-based Han numerals
    private val digitHanNumbers = mapOf(
        '零' to 0, '一' to 1, '二' to 2, '三' to 3, '四' to 4,
        '五' to 5, '六' to 6, '七' to 7, '八' to 8, '九' to 9,
    )

    private val largeMultipliers = linkedMapOf(
        '千' to 1000,
        '百' to 100,
        '十' to 10,
    )

    override fun extractNumber(text: String): List<Double> {
        val results = mutableListOf<Double>()

        // Arabic numerals: "1.5", "30", "100"
        Regex("""\b(\d+\.?\d*)\b""").findAll(text).forEach { match ->
            match.groupValues[1].toDoubleOrNull()?.let { results.add(it) }
        }

        // "X倍" → multiplier
        Regex("""(\d+(?:\.\d+)?|[一二两三四五六七八九十]+)倍""").find(text)?.let { match ->
            val num = match.groupValues[1].toDoubleOrNull()
                ?: parseHanNumber(match.groupValues[1])?.toDouble()
            if (num != null) results.add(num)
        }

        // Chinese numerals when no Arabic digits found
        if (results.isEmpty()) {
            val num = parseHanNumber(text)
            if (num != null) results.add(num.toDouble())
        }

        return results.distinct()
    }

    // -- Ordinal -----------------------------------------------------------

    override fun extractOrdinal(text: String): List<Int> {
        val results = mutableListOf<Int>()

        // "第X" → X-1 (0-based)
        Regex("""第\s*(\d+|[一二两三四五六七八九十]+)""").find(text)?.let { match ->
            val raw = match.groupValues[1]
            val num = raw.toIntOrNull() ?: parseHanNumber(raw)
            if (num != null) results.add(num - 1)
        }

        // "最后" → -1
        if (Regex("""最后""").containsMatchIn(text)) {
            results.add(-1)
        }

        return results.distinct()
    }

    // -- Trim mode ---------------------------------------------------------

    private val trimModeKeywords = linkedMapOf(
        "关闭" to "off",
        "关" to "off",
        "低" to "low",
        "中" to "medium",
        "中等" to "medium",
        "高" to "high",
    )

    override fun extractTrimMode(text: String): String? {
        for ((keyword, mode) in trimModeKeywords) {
            if (text.contains(keyword)) return mode
        }
        return null
    }

    // -- Boolean -----------------------------------------------------------

    override fun extractBoolean(text: String): Boolean? {
        if (Regex("""开|启用|打开|开启""").containsMatchIn(text)) return true
        if (Regex("""关|禁用|关闭|停用""").containsMatchIn(text)) return false
        return null
    }

    // -- Helpers -----------------------------------------------------------

    /**
     * Parse a Chinese numeral string to an integer.
     * Handles: "三十", "一百二十", "十五", "二百五" (250).
     */
    private fun parseHanNumber(raw: String): Int? {
        // Try direct digit mapping for single characters
        if (raw.length == 1) {
            return hanNumbers[raw]
        }

        var total = 0
        var current = 0
        var hasDigit = false

        for (ch in raw) {
            when {
                ch in digitHanNumbers -> {
                    current = digitHanNumbers[ch]!!
                    hasDigit = true
                }

                ch in largeMultipliers -> {
                    val multiplier = largeMultipliers[ch]!!
                    if (current == 0) current = 1
                    total += current * multiplier
                    current = 0
                }

                ch == '两' -> {
                    current = 2
                    hasDigit = true
                }
            }
        }
        total += current

        return if (hasDigit) total else null
    }
}

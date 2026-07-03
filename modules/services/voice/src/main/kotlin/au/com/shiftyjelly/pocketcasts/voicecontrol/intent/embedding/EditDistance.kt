package au.com.shiftyjelly.pocketcasts.voicecontrol.intent.embedding

/**
 * Levenshtein (edit) distance and related utilities for ASR error correction.
 */
object EditDistance {

    /**
     * Compute normalized Levenshtein distance between [a] and [b].
     * Returns a value in [0.0, 1.0] where 0.0 means identical strings.
     */
    fun normalized(a: String, b: String): Double {
        val dist = levenshtein(a, b)
        val maxLen = maxOf(a.length, b.length)
        if (maxLen == 0) return 0.0
        return dist.toDouble() / maxLen
    }

    /**
     * Compute raw Levenshtein distance (minimum number of single-character
     * edits — insertions, deletions, substitutions — to transform [a] into [b]).
     */
    fun levenshtein(a: String, b: String): Int {
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length

        // Use single-row DP to minimize allocation.
        val prev = IntArray(b.length + 1) { it }
        val curr = IntArray(b.length + 1)

        for (i in 1..a.length) {
            curr[0] = i
            for (j in 1..b.length) {
                val cost = if (a[i - 1] == b[j - 1]) 0 else 1
                curr[j] = minOf(
                    prev[j] + 1, // deletion
                    curr[j - 1] + 1, // insertion
                    prev[j - 1] + cost, // substitution
                )
            }
            // Swap rows
            for (j in 0..b.length) {
                prev[j] = curr[j]
            }
        }
        return prev[b.length]
    }
}

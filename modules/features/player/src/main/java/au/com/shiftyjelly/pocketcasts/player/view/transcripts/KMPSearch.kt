package au.com.shiftyjelly.pocketcasts.player.view.transcripts

import javax.inject.Inject

// An implementation of the Knuth-Morris-Pratt algorithm
class KMPSearch @Inject constructor() {
    private var pattern: CharArray = charArrayOf()
    private var lps: IntArray = intArrayOf()

    fun setPattern(pattern: String) {
        this.pattern = pattern.lowercase().toCharArray()
        lps = IntArray(pattern.length)
        computeLPSArray()
    }

    private fun computeLPSArray() {
        if (lps.isEmpty()) {
            return
        }

        // Length of the previous longest prefix suffix
        var length = 0
        // lps[0] is always 0
        lps[0] = 0
        var i = 1

        // Loop calculates the lps for i = 1 to M-1
        while (i < pattern.size) {
            if (pattern[i] == pattern[length]) {
                length++
                lps[i] = length
                i++
            } else {
                if (length != 0) {
                    length = lps[length - 1]
                } else {
                    lps[i] = 0
                    i++
                }
            }
        }
    }

    fun search(text: String): List<Int> {
        if (pattern.isEmpty()) {
            return emptyList()
        }

        val textArray = text.lowercase().toCharArray()
        val result = mutableListOf<Int>()
        var i = 0 // index for textArray
        var j = 0 // index for pattern

        while (i < textArray.size) {
            if (pattern[j] == textArray[i]) {
                i++
                j++
            }

            if (j == pattern.size) {
                result.add(i - j)
                j = lps[j - 1]
            } else if (i < textArray.size && pattern[j] != textArray[i]) {
                if (j != 0) {
                    j = lps[j - 1]
                } else {
                    i++
                }
            }
        }

        return result
    }
}

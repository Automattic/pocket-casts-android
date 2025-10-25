package au.com.shiftyjelly.pocketcasts.utils.search

import au.com.shiftyjelly.pocketcasts.utils.extensions.removeAccents

fun String.kmpSearch(pattern: String): List<Int> {
    return KmpSearch(pattern).search(this)
}

fun List<String>.kmpSearch(pattern: String): Map<Int, List<Int>> {
    val kmpSearch = KmpSearch(pattern)
    return buildMap {
        forEachIndexed { index, text ->
            val searchResult = kmpSearch.search(text)
            if (searchResult.isNotEmpty()) {
                put(index, searchResult)
            }
        }
    }
}

private class KmpSearch(
    pattern: String,
) {
    private val sanitizedPattern = pattern.sanitize()
    private val lps = IntArray(sanitizedPattern.size)

    init {
        computeLpsArray()
    }

    fun search(text: String): List<Int> {
        if (sanitizedPattern.isEmpty() || text.isEmpty()) {
            return emptyList()
        }

        val sanitizedText = text.sanitize()
        val result = mutableListOf<Int>()
        var i = 0
        var j = 0

        while (i < sanitizedText.size) {
            if (sanitizedPattern[j] == sanitizedText[i]) {
                i++
                j++
            }

            if (j == sanitizedPattern.size) {
                result.add(i - j)
                j = lps[j - 1]
            } else if (i < sanitizedText.size && sanitizedPattern[j] != sanitizedText[i]) {
                if (j != 0) {
                    j = lps[j - 1]
                } else {
                    i++
                }
            }
        }

        return result
    }

    private fun computeLpsArray() {
        if (lps.isNotEmpty()) {
            var length = 0
            lps[0] = 0
            var i = 1

            while (i < sanitizedPattern.size) {
                if (sanitizedPattern[i] == sanitizedPattern[length]) {
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
    }

    private fun String.sanitize() = removeAccents().lowercase().toCharArray()
}

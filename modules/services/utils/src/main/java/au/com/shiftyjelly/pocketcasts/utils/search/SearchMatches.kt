package au.com.shiftyjelly.pocketcasts.utils.search

data class SearchMatches(
    val selectedMatch: SearchCoordinates?,
    val lineMatches: Map<Int, List<Int>>,
) {
    init {
        if (selectedMatch != null) {
            val matchCoordinates = requireNotNull(lineMatches[selectedMatch.line]) {
                "Match result is missing for line ${selectedMatch.line}"
            }
            require(selectedMatch.match in matchCoordinates) {
                "Match result is missing for coordinates $selectedMatch"
            }
        }
    }

    val selectedMatchIndex = if (selectedMatch != null) {
        lineMatches.entries.sumOf { (line, matches) ->
            when {
                line < selectedMatch.line -> matches.size
                line == selectedMatch.line -> matches.indexOf(selectedMatch.match)
                else -> 0
            }
        }
    } else {
        0
    }

    val count = lineMatches.values.sumOf { it.size }

    fun next() = when {
        selectedMatch == null || count == 1 -> this

        selectedMatchIndex + 1 == count -> {
            val (firstLine, firstLineMatches) = lineMatches.entries.first()
            val firstMatch = firstLineMatches.first()
            copy(selectedMatch = SearchCoordinates(firstLine, firstMatch))
        }

        else -> {
            val lineMatches = lineMatches.getValue(selectedMatch.line)
            val nextSearchCoordaintes = if (selectedMatch.match == lineMatches.last()) {
                val lines = this.lineMatches.keys.toList()
                val nextLine = lines[lines.indexOf(selectedMatch.line) + 1]
                val nextLineMatch = this.lineMatches.getValue(nextLine).first()
                SearchCoordinates(nextLine, nextLineMatch)
            } else {
                val nextMatch = lineMatches[lineMatches.indexOf(selectedMatch.match) + 1]
                selectedMatch.copy(match = nextMatch)
            }
            copy(selectedMatch = nextSearchCoordaintes)
        }
    }

    fun previous() = when {
        selectedMatch == null || count == 1 -> this

        selectedMatchIndex == 0 -> {
            val (lastLine, lastLineMatches) = lineMatches.entries.last()
            val lastMatch = lastLineMatches.last()
            copy(selectedMatch = SearchCoordinates(lastLine, lastMatch))
        }

        else -> {
            val lineMatches = lineMatches.getValue(selectedMatch.line)
            val nextSearchCoordaintes = if (selectedMatch.match == lineMatches.first()) {
                val lines = this.lineMatches.keys.toList()
                val previousLine = lines[lines.lastIndexOf(selectedMatch.line) - 1]
                val previousLineMatch = this.lineMatches.getValue(previousLine).last()
                SearchCoordinates(previousLine, previousLineMatch)
            } else {
                val previousMatch = lineMatches[lineMatches.indexOf(selectedMatch.match) - 1]
                selectedMatch.copy(match = previousMatch)
            }
            copy(selectedMatch = nextSearchCoordaintes)
        }
    }
}

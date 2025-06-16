package au.com.shiftyjelly.pocketcasts.utils.search

data class SearchMatches(
    val selectedCoordinate: SearchCoordinates?,
    val matchingCoordinates: Map<Int, List<Int>>,
) {
    init {
        if (selectedCoordinate != null) {
            val matchCoordinates = requireNotNull(matchingCoordinates[selectedCoordinate.line]) {
                "Match result is missing for line ${selectedCoordinate.line}"
            }
            require(selectedCoordinate.match in matchCoordinates) {
                "Match result is missing for coordinates $selectedCoordinate"
            }
        }
    }

    val selectedMatchIndex = if (selectedCoordinate != null) {
        matchingCoordinates.entries.sumOf { (line, matches) ->
            when {
                line < selectedCoordinate.line -> matches.size
                line == selectedCoordinate.line -> matches.indexOf(selectedCoordinate.match)
                else -> 0
            }
        }
    } else {
        0
    }

    val count = matchingCoordinates.values.sumOf { it.size }

    fun next() = when {
        selectedCoordinate == null || count == 1 -> this

        selectedMatchIndex + 1 == count -> {
            val (firstLine, firstLineMatches) = matchingCoordinates.entries.first()
            val firstMatch = firstLineMatches.first()
            copy(selectedCoordinate = SearchCoordinates(firstLine, firstMatch))
        }

        else -> {
            val lineMatches = matchingCoordinates.getValue(selectedCoordinate.line)
            val nextSearchCoordaintes = if (selectedCoordinate.match == lineMatches.last()) {
                val lines = matchingCoordinates.keys.toList()
                val nextLine = lines[lines.indexOf(selectedCoordinate.line) + 1]
                val nextLineMatch = matchingCoordinates.getValue(nextLine).first()
                SearchCoordinates(nextLine, nextLineMatch)
            } else {
                val nextMatch = lineMatches[lineMatches.indexOf(selectedCoordinate.match) + 1]
                selectedCoordinate.copy(match = nextMatch)
            }
            copy(selectedCoordinate = nextSearchCoordaintes)
        }
    }

    fun previous() = when {
        selectedCoordinate == null || count == 1 -> this

        selectedMatchIndex == 0 -> {
            val (lastLine, lastLineMatches) = matchingCoordinates.entries.last()
            val lastMatch = lastLineMatches.last()
            copy(selectedCoordinate = SearchCoordinates(lastLine, lastMatch))
        }

        else -> {
            val lineMatches = matchingCoordinates.getValue(selectedCoordinate.line)
            val nextSearchCoordaintes = if (selectedCoordinate.match == lineMatches.first()) {
                val lines = matchingCoordinates.keys.toList()
                val previousLine = lines[lines.lastIndexOf(selectedCoordinate.line) - 1]
                val previousLineMatch = matchingCoordinates.getValue(previousLine).last()
                SearchCoordinates(previousLine, previousLineMatch)
            } else {
                val previousMatch = lineMatches[lineMatches.indexOf(selectedCoordinate.match) - 1]
                selectedCoordinate.copy(match = previousMatch)
            }
            copy(selectedCoordinate = nextSearchCoordaintes)
        }
    }
}

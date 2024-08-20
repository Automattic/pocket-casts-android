package au.com.shiftyjelly.pocketcasts.player.view.transcripts

import au.com.shiftyjelly.pocketcasts.repositories.podcast.TranscriptFormat
import java.util.regex.PatternSyntaxException

private val vttSpeakerRegex = "<v (.+?)>".toRegex()
private val srtSpeakerRegex = "^(.+?):".toRegex()

// TODO: [Transcript] Modify regex for non english languages
interface TranscriptFilter {
    fun filter(input: String): String
}

class TranscriptRegexFilters(private val filters: List<TranscriptFilter>) : TranscriptFilter {
    override fun filter(input: String): String {
        return filters.fold(input) { partialResult, filter ->
            filter.filter(partialResult)
        }
    }

    companion object {
        val transcriptFilters = TranscriptRegexFilters(
            filters = listOf(
                RegexFilters.vttTagsFilter,
                RegexFilters.speakerFilter,
                RegexFilters.notEndOfLineCharNewLineFilter,
                RegexFilters.endOfLineCharNewLineFilter,
                RegexFilters.endOfLineCharEndOfCueFilter,
            ),
        )

        val htmlFilters = TranscriptRegexFilters(
            filters = listOf(
                RegexFilters.breakLineFilter,
                RegexFilters.nbspFilter,
                RegexFilters.vttTagsFilter,
                RegexFilters.soundDescriptorFilter,
                RegexFilters.htmlSpeakerFilter,
                RegexFilters.htmlSpeakerNewlineFilter,
                RegexFilters.emptySpacesAtEndOfLinesFilter,
                RegexFilters.doubleOrMoreSpacesFilter,
                RegexFilters.tripleOrMoreEmptyLinesFilter,
            ),
        )

        fun extractSpeaker(cue: String, format: TranscriptFormat?) = when (format) {
            TranscriptFormat.VTT -> regexMatch(input = cue, regex = vttSpeakerRegex, position = 1)
            TranscriptFormat.SRT -> regexMatch(input = cue, regex = srtSpeakerRegex, position = 1)
            else -> null
        }

        private fun regexMatch(input: String, regex: Regex, position: Int = 0): String? {
            return try {
                regex.find(input)?.groupValues?.get(position)
            } catch (e: Exception) {
                null
            }
        }
    }
}

class RegexFilter(private val pattern: String, private val replacement: String) : TranscriptFilter {

    override fun filter(input: String): String {
        return regexSearchReplace(input, pattern, replacement)
    }

    private fun regexSearchReplace(input: String, pattern: String, replacement: String): String {
        return try {
            val regex = Regex(pattern)
            regex.replace(input, replacement)
        } catch (e: PatternSyntaxException) {
            input
        }
    }
}

object RegexFilters {
    // Remove VTT tags, for example: <Speaker 1> to ""
    val vttTagsFilter = RegexFilter("<[^>]*>", "")

    // Remove SRT tags, for example: "Speaker 1: " to ""
    val speakerFilter = RegexFilter("Speaker \\d?: *", "")

    // Ensure that any end of line character starts a new line
    val endOfLineCharNewLineFilter = RegexFilter("([.!?])\\s+", "$1\n\n")

    // End of line character at end of cue
    val endOfLineCharEndOfCueFilter = RegexFilter("([.!?])\\z", "$1\n\n")

    // Ensure that end of cues have a space when appended to the next cue
    val notEndOfLineCharNewLineFilter = RegexFilter("([^.!?$])\\z", "$1 ")

    // &nbsp filter
    val nbspFilter = RegexFilter("&nbsp;", " ")

    // <br> filter
    val breakLineFilter = RegexFilter("<br>|<BR>|<br/>|<BR/>|<BR />|<br />", "\n\n")

    // Sound descriptor filter. Ex: [laughs]
    val soundDescriptorFilter = RegexFilter("\\[[^\\]]*\\]", "")

    // Speaker names at start
    val htmlSpeakerFilter = RegexFilter("^[ ]*\\w+:\\s*", "")

    // Speaker names after newline
    val htmlSpeakerNewlineFilter = RegexFilter("\\n[ ]*\\w+:\\s*", "\n")

    // Empty spaces at the end of lines
    val emptySpacesAtEndOfLinesFilter = RegexFilter("[ ]*\\n", "\n\n")

    // Double or more spaces
    val doubleOrMoreSpacesFilter = RegexFilter("[ ]+", " ")

    // Double or more lines
    val tripleOrMoreEmptyLinesFilter = RegexFilter("[\\n]+", "\n\n")
}

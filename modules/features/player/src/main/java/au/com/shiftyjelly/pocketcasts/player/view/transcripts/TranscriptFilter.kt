package au.com.shiftyjelly.pocketcasts.player.view.transcripts

import au.com.shiftyjelly.pocketcasts.repositories.podcast.TranscriptFormat
import java.util.regex.PatternSyntaxException

private val vttSpeakerRegex = "<v (.+?)>".toRegex()
private val srtSpeakerRegex = "^(.+?):".toRegex()
private val vttTagsRegex = "<[^>]*>".toRegex()
private val srtTagsRegex = "Speaker \\d?: *".toRegex()
private val endOfLineChar = "([.!?])\\s+".toRegex()
private val endOfLineCharEndOfCue = "([.!?])\\z".toRegex()
private val notEndOfLineCharEndOfCue = "([^.!?$])\\z".toRegex()
private val nbspRegex = "&nbsp;".toRegex()
private val breakLineRegex = "<br>|<BR>|<br/>|<BR/>|<BR />|<br />".toRegex()
private val soundDescriptorRegex = "\\[[^]]*]".toRegex()
private val htmlSpeakerRegex = "^ *\\w+:\\s*".toRegex()
private val htmlSpeakerNewlineRegex = "\\n *\\w+:\\s*".toRegex()
private val emptySpacesAtEndOfLinesRegex = " *\\n".toRegex()
private val doubleOrMoreSpacesRegex = " +".toRegex()
private val tripleOrMoreEmptyLinesRegex = "\\n+".toRegex()

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
                RegexFilters.srtTagsFilter,
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

class RegexFilter(private val regex: Regex, private val replacement: String) : TranscriptFilter {

    override fun filter(input: String): String {
        return regexSearchReplace(input, regex, replacement)
    }

    private fun regexSearchReplace(input: String, regex: Regex, replacement: String): String {
        return try {
            regex.replace(input, replacement)
        } catch (e: PatternSyntaxException) {
            input
        }
    }
}

object RegexFilters {
    // Remove VTT tags, for example: <Speaker 1> to ""
    val vttTagsFilter = RegexFilter(vttTagsRegex, "")

    // Remove SRT tags, for example: "Speaker 1: " to ""
    val srtTagsFilter = RegexFilter(srtTagsRegex, "")

    // Ensure that any end of line character starts a new line
    val endOfLineCharNewLineFilter = RegexFilter(endOfLineChar, "$1\n\n")

    // End of line character at end of cue
    val endOfLineCharEndOfCueFilter = RegexFilter(endOfLineCharEndOfCue, "$1\n\n")

    // Ensure that end of cues have a space when appended to the next cue
    val notEndOfLineCharNewLineFilter = RegexFilter(notEndOfLineCharEndOfCue, "$1 ")

    // &nbsp filter
    val nbspFilter = RegexFilter(nbspRegex, " ")

    // <br> filter
    val breakLineFilter = RegexFilter(breakLineRegex, "\n\n")

    // Sound descriptor filter. Ex: [laughs]
    val soundDescriptorFilter = RegexFilter(soundDescriptorRegex, "")

    // Speaker names at start
    val htmlSpeakerFilter = RegexFilter(htmlSpeakerRegex, "")

    // Speaker names after newline
    val htmlSpeakerNewlineFilter = RegexFilter(htmlSpeakerNewlineRegex, "\n")

    // Empty spaces at the end of lines
    val emptySpacesAtEndOfLinesFilter = RegexFilter(emptySpacesAtEndOfLinesRegex, "\n\n")

    // Double or more spaces
    val doubleOrMoreSpacesFilter = RegexFilter(doubleOrMoreSpacesRegex, " ")

    // Double or more lines
    val tripleOrMoreEmptyLinesFilter = RegexFilter(tripleOrMoreEmptyLinesRegex, "\n\n")
}

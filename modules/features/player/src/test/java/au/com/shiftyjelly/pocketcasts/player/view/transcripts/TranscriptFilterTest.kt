package au.com.shiftyjelly.pocketcasts.player.view.transcripts

import au.com.shiftyjelly.pocketcasts.repositories.podcast.TranscriptFormat
import org.junit.Assert.assertEquals
import org.junit.Test

class TranscriptFilterTest {

    @Test
    fun `filter removes vtt tags from input`() {
        val input = "<v Speaker 1> Hello, world!"
        val expected = " Hello, world!"
        val filter = RegexFilters.vttTagsFilter
        val result = filter.filter(input)
        assertEquals(expected, result)
    }

    @Test
    fun `filter removes speaker tags from input`() {
        val input = "Speaker 1: Hello, world!"
        val expected = "Hello, world!"
        val filter = RegexFilters.srtTagsFilter
        val result = filter.filter(input)
        assertEquals(expected, result)
    }

    @Test
    fun `filter adds new lines after end of line character`() {
        val input = "Hello. World! How are you? Just curious."
        val expected = """
        Hello.
    
        World!
    
        How are you?
    
        Just curious.
        """.trimIndent()
        val filter = RegexFilters.endOfLineCharNewLineFilter
        val result = filter.filter(input)
        assertEquals(expected, result)
    }

    @Test
    fun `filter adds new line after end of line character at end of input`() {
        val input = listOf("Hello, world!", "Hello, world?", "Hello, world.")
        val expected = listOf("Hello, world!\n\n", "Hello, world?\n\n", "Hello, world.\n\n")
        val filter = RegexFilters.endOfLineCharEndOfCueFilter
        val result = input.map {
            filter.filter(it)
        }
        assertEquals(expected, result)
    }

    @Test
    fun `filter adds space at end of input if no end of line character`() {
        val input = "Hello, world"
        val expected = "Hello, world "
        val filter = RegexFilters.notEndOfLineCharNewLineFilter
        val result = filter.filter(input)
        assertEquals(expected, result)
    }

    @Test
    fun `filter replaces nbsp with space`() {
        val input = "Hello,&nbsp;world!"
        val expected = "Hello, world!"
        val filter = RegexFilters.nbspFilter
        val result = filter.filter(input)
        assertEquals(expected, result)
    }

    @Test
    fun `filter replaces break line with new line`() {
        val input = "Hello,<br>world!"
        val expected = """
        Hello,
    
        world!
        """.trimIndent()
        val filter = RegexFilters.breakLineFilter
        val result = filter.filter(input)
        assertEquals(expected, result)
    }

    @Test
    fun `filter removes sound descriptors`() {
        val input = "Hello, \\[laughs\\]"
        val expected = "Hello, "
        val filter = RegexFilters.soundDescriptorFilter
        val result = filter.filter(input)
        assertEquals(expected, result)
    }

    @Test
    fun `filter removes speaker names at start`() {
        val input = "John: Hello, world!"
        val expected = "Hello, world!"
        val filter = RegexFilters.htmlSpeakerFilter
        val result = filter.filter(input)
        assertEquals(expected, result)
    }

    @Test
    fun `filter removes speaker names after newline`() {
        val input = "\nJohn: Hello, world!"
        val expected = "\nHello, world!"
        val filter = RegexFilters.htmlSpeakerNewlineFilter
        val result = filter.filter(input)
        assertEquals(expected, result)
    }

    @Test
    fun `filter removes empty spaces at end of lines`() {
        val input = "Hello, world! \n"
        val expected = "Hello, world!\n\n"
        val filter = RegexFilters.emptySpacesAtEndOfLinesFilter
        val result = filter.filter(input)
        assertEquals(expected, result)
    }

    @Test
    fun `filter replaces double or more spaces with single space`() {
        val input = "Hello,  world!"
        val expected = "Hello, world!"
        val filter = RegexFilters.doubleOrMoreSpacesFilter
        val result = filter.filter(input)
        assertEquals(expected, result)
    }

    @Test
    fun `filter replaces triple or more new lines with double new line`() {
        val input = "Hello, world!\n\n\nHow are you?"
        val expected = "Hello, world!\n\nHow are you?"
        val filter = RegexFilters.tripleOrMoreEmptyLinesFilter
        val result = filter.filter(input)
        assertEquals(expected, result)
    }

    @Test
    fun `filter replaces input with multiple html entities with correct chars`() {
        val input = "Hello,&nbsp;world!&#160;This&quot;is&#34;a&apos;test&#39;with&lt;multiple&#60;html&gt;entities&#62;and&#38;more&amp;."
        val expected = "Hello, world! This\"is\"a'test'with<multiple<html>entities>and&more&."
        val filter = HTMLEntitiesFilter()
        val result = filter.filter(input)
        assertEquals(expected, result)
    }

    @Test
    fun `extractFilter extracts speaker from input for vtt format`() {
        val input = "<v Speaker 1> Hello, world!"
        val expected = "Speaker 1"
        val result = TranscriptRegexFilters.extractSpeaker(input, TranscriptFormat.VTT)
        assertEquals(expected, result)
    }

    @Test
    fun `extractFilter extracts speaker from input for srt format`() {
        val input = "Speaker 1: Hello, world!"
        val expected = "Speaker 1"
        val result = TranscriptRegexFilters.extractSpeaker(input, TranscriptFormat.SRT)
        assertEquals(expected, result)
    }
}

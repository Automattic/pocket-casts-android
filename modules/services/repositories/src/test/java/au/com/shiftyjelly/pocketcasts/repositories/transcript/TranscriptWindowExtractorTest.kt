package au.com.shiftyjelly.pocketcasts.repositories.transcript

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TranscriptWindowExtractorTest {

    private val sampleVtt = """
        |WEBVTT
        |
        |00:00:00.000 --> 00:00:05.000
        |Welcome to the show everyone.
        |
        |00:00:05.000 --> 00:00:10.000
        |Today we are going to discuss artificial intelligence.
        |
        |00:00:10.000 --> 00:00:20.000
        |Let me start by defining what AI actually means in practice.
        |
        |00:00:20.000 --> 00:00:30.000
        |AI is a broad field that includes machine learning, deep learning, and more.
        |
        |00:00:30.000 --> 00:00:40.000
        |The recent advances have been truly remarkable for the industry.
        |
        |00:00:40.000 --> 00:00:50.000
        |Companies are investing billions of dollars into AI research.
        |
        |00:01:00.000 --> 00:01:10.000
        |This is a much later segment about totally different things.
    """.trimMargin()

    @Test
    fun `extract window around middle of transcript`() {
        val result = TranscriptWindowExtractor.parseVttWindow(sampleVtt, timeSecs = 25, windowSecs = 15)

        assertEquals(
            "Let me start by defining what AI actually means in practice. " +
                "AI is a broad field that includes machine learning, deep learning, and more. " +
                "The recent advances have been truly remarkable for the industry.",
            result,
        )
    }

    @Test
    fun `extract window at start of transcript`() {
        val result = TranscriptWindowExtractor.parseVttWindow(sampleVtt, timeSecs = 0, windowSecs = 10)

        assertEquals(
            "Welcome to the show everyone. Today we are going to discuss artificial intelligence.",
            result,
        )
    }

    @Test
    fun `return null when window has too few words`() {
        val shortVtt = """
            |WEBVTT
            |
            |00:00:00.000 --> 00:00:05.000
            |Just a few words.
        """.trimMargin()

        val result = TranscriptWindowExtractor.parseVttWindow(shortVtt, timeSecs = 2, windowSecs = 30)

        assertNull(result)
    }

    @Test
    fun `return null when no cues in window`() {
        val result = TranscriptWindowExtractor.parseVttWindow(sampleVtt, timeSecs = 300, windowSecs = 10)

        assertNull(result)
    }

    @Test
    fun `strip html tags from cue text`() {
        val vttWithTags = """
            |WEBVTT
            |
            |00:00:00.000 --> 00:00:10.000
            |<v Alice>This is a sentence with enough words to pass the minimum threshold for extraction.
        """.trimMargin()

        val result = TranscriptWindowExtractor.parseVttWindow(vttWithTags, timeSecs = 5, windowSecs = 10)

        assertEquals(
            "This is a sentence with enough words to pass the minimum threshold for extraction.",
            result,
        )
    }
}

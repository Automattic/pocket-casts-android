package au.com.shiftyjelly.pocketcasts.repositories.chat

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatTimestampsTest {
    @Test
    fun `ranges are bracketed and shown as minutes and seconds`() {
        assertEquals(
            "Viability is first discussed at (10:32 - 10:39), then revisited at (13:07 - 14:02).",
            "Viability is first discussed at 00:10:32 to 00:10:39, then revisited at 00:13:07 to 00:14:02.".withChatTimestamps(),
        )
    }

    @Test
    fun `existing brackets are not doubled`() {
        assertEquals(
            "Wu-Tang is linked to Staten Island (24:15 - 24:21).",
            "Wu-Tang is linked to Staten Island (00:24:15–00:24:21).".withChatTimestamps(),
        )
    }

    @Test
    fun `dashes and spacing are normalised`() {
        listOf("33:05–33:07", "33:05 — 33:07", "33:05 - 33:07", "33:05-33:07", "( 33:05 - 33:07 )").forEach { value ->
            assertEquals(value, "(33:05 - 33:07)", value.withChatTimestamps())
        }
    }

    @Test
    fun `hours show only when needed`() {
        assertEquals("(1:02:03 - 1:02:09)", "01:02:03 - 01:02:09".withChatTimestamps())
        assertEquals("(0:05 - 0:12)", "00:00:05 - 00:00:12".withChatTimestamps())
    }

    @Test
    fun `a single time is bracketed`() {
        assertEquals("It comes up at (5:04).", "It comes up at 05:04.".withChatTimestamps())
    }

    @Test
    fun `milliseconds are dropped from the label`() {
        assertEquals("(46:01 - 46:05)", "46:01.845 - 46:05,100".withChatTimestamps())
    }

    @Test
    fun `text without times is unchanged`() {
        listOf(
            "Juniper was born at 23 weeks and 6 days.",
            "The meeting was at 9:30 am.",
            "Doors open at 7:15pm.",
            "The ratio was 3:1.",
            "Seconds can't be 10:75.",
            "Version 1:02:03:04 is not a time.",
        ).forEach { value ->
            assertEquals(value, value, value.withChatTimestamps())
        }
    }

    @Test
    fun `timestamps are found with their start time`() {
        val text = "Discussed at (10:32 - 10:39) and (1:02:03)."

        val timestamps = findChatTimestamps(text)

        assertEquals(
            listOf(
                ChatTimestamp(range = 13..27, startMs = 632_000),
                ChatTimestamp(range = 33..41, startMs = 3_723_000),
            ),
            timestamps,
        )
        assertEquals("(10:32 - 10:39)", text.substring(timestamps[0].range))
        assertEquals("(1:02:03)", text.substring(timestamps[1].range))
    }

    @Test
    fun `found timestamps keep milliseconds`() {
        assertEquals(2_761_845, findChatTimestamps("46:01.845").single().startMs)
    }

    @Test
    fun `quote labels use the same format`() {
        val quote = ChatMessage.Quote(
            text = "Staten Island is Wu-Tang",
            start = "00:24:15",
            end = "00:24:21",
            startMs = 1_455_000,
            endMs = 1_461_000,
        )

        assertEquals("24:15 - 24:21", quote.timestampLabel)
    }

    @Test
    fun `answers show canonical timestamps`() {
        val answer = ChatMessage.Assistant(text = "- First at 00:10:32 to 00:10:39\n- Then 13:07")

        assertEquals("•  First at (10:32 - 10:39)\n•  Then (13:07)", answer.displayText)
    }
}

package au.com.shiftyjelly.pocketcasts.voicecontrol.intent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LfmToolCallParserTest {
    @Test
    fun parse_sleepSet() {
        val call = LfmToolCallParser.parse(
            "<|tool_call_start|>[sleep(action='set', minutes=30)]<|tool_call_end|>",
        )
        assertNotNull(call)
        assertEquals("sleep", call!!.name)
        assertEquals("set", call.action)
        assertEquals(30, call.params["minutes"])
    }

    @Test
    fun parse_usesLastToolCallSpan() {
        val call = LfmToolCallParser.parse(
            "noise <|tool_call_start|>[playback(action='pause')]<|tool_call_end|> " +
                "<|tool_call_start|>[volume(action='set_volume', volume=50)]<|tool_call_end|>",
        )
        assertNotNull(call)
        assertEquals("volume", call!!.name)
        assertEquals("set_volume", call.action)
        assertEquals(50, call.params["volume"])
    }

    @Test
    fun parse_noMatch() {
        val call = LfmToolCallParser.parse(
            "<|tool_call_start|>[no_match(action='')]<|tool_call_end|>",
        )
        assertNotNull(call)
        assertEquals("no_match", call!!.name)
        assertEquals("", call.action)
        assertEquals(emptyMap<String, Any?>(), call.params)
    }

    @Test
    fun parse_invalidReturnsNull() {
        assertNull(LfmToolCallParser.parse("not a tool call"))
        assertNull(
            LfmToolCallParser.parse(
                "<|tool_call_start|>[broken(action=)]<|tool_call_end|>",
            ),
        )
    }
}

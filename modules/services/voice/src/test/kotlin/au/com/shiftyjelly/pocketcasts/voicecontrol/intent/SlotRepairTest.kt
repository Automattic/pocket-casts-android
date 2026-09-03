package au.com.shiftyjelly.pocketcasts.voicecontrol.intent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SlotRepairTest {
    @Test
    fun collapseRepetition_collapsesRepeatedSuffix() {
        assertEquals(
            "the turning point",
            SlotRepair.collapseRepetition("the turning point the turning point"),
        )
    }

    @Test
    fun repair_seekRelativeFromMinuteUtterance_overridesWrongModelSlots() {
        val fromMinutes = SlotRepair.repair(
            raw = "<|tool_call_start|>[playback(action='seek_relative', minutes=1)]<|tool_call_end|>",
            utterance = "Could you just go back a minute, please?",
            tool = "playback",
            action = "seek_relative",
        )
        assertNotNull(fromMinutes)
        assertEquals(-60, fromMinutes!!.params["delta_seconds"])

        val fromWrongDelta = SlotRepair.repair(
            raw = "<|tool_call_start|>[playback(action='seek_relative', delta_seconds=-1)]<|tool_call_end|>",
            utterance = "go back a minute",
            tool = "playback",
            action = "seek_relative",
        )
        assertNotNull(fromWrongDelta)
        assertEquals(-60, fromWrongDelta!!.params["delta_seconds"])
    }

    @Test
    fun repair_garbledTitle_restoredFromQuotedSpan() {
        val repaired = SlotRepair.repair(
            raw = "<|tool_call_start|>[dialog_control(action='provide_slot', target_tool='bookmark', target_action='rename', slot='title', value='Key Insuel')]<|tool_call_end|>",
            utterance = "Call it 'Key Insight'.",
            tool = "dialog_control",
            action = "provide_slot",
        )
        assertNotNull(repaired)
        assertEquals("Key Insight", repaired!!.params["value"])
    }

    @Test
    fun repair_noMatch_returnsEmptyParams() {
        val repaired = SlotRepair.repair(
            raw = "<|tool_call_start|>[no_match(action='')]<|tool_call_end|>",
            utterance = "hello there",
            tool = "no_match",
            action = "",
        )
        assertNotNull(repaired)
        assertEquals("no_match", repaired!!.name)
        assertEquals("", repaired.action)
        assertTrue(repaired.params.isEmpty())
    }

    @Test
    fun repair_neverChangesClassifierToolAndAction() {
        val repaired = SlotRepair.repair(
            raw = "<|tool_call_start|>[volume(action='set_volume', volume=50)]<|tool_call_end|>",
            utterance = "go back a minute",
            tool = "playback",
            action = "seek_relative",
        )
        assertNotNull(repaired)
        assertEquals("playback", repaired!!.name)
        assertEquals("seek_relative", repaired.action)
    }
}

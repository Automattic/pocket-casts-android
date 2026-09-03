package au.com.shiftyjelly.pocketcasts.voicecontrol.intent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LfmPromptTest {
    @Test
    fun systemPrompt_isExactShortLine() {
        assertEquals(
            "You map podcast voice commands to a tool call.",
            LfmPrompt.SYSTEM,
        )
    }

    @Test
    fun request_doesNotDumpTools() {
        val text = LfmPrompt.render(transcript = "pause", history = emptyList())
        assertFalse(text.contains("List of tools"))
        assertTrue(text.contains("<|im_start|>system\nYou map podcast voice commands to a tool call."))
        assertTrue(text.endsWith("<|im_start|>assistant\n"))
    }

    @Test
    fun request_includesHistoryUpToFourTurns() {
        val history = (1..5).map { index ->
            DialogPromptTurn(
                role = if (index % 2 == 1) "user" else "assistant",
                content = "turn-$index",
            )
        }
        val text = LfmPrompt.render(transcript = "pause", history = history)
        assertFalse(text.contains("turn-1"))
        assertTrue(text.contains("turn-2"))
        assertTrue(text.contains("turn-5"))
    }
}

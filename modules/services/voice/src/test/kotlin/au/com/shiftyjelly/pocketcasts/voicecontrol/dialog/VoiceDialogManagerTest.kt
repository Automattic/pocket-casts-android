package au.com.shiftyjelly.pocketcasts.voicecontrol.dialog

import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.DialogPromptTurn
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.ToolCall
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.ToolCallMapper
import au.com.shiftyjelly.pocketcasts.voicecontrol.intent.VoiceIntent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceDialogManagerTest {
    private val manager = VoiceDialogManager(ToolCallMapper())

    @Test
    fun `begin retains the initiating prompt pair while dialog is pending`() {
        val generated = nativeCall("begin")

        assertNull(manager.resolve("Rename a bookmark.", generated, beginCall()))

        assertTrue(manager.isInProgress)
        assertEquals(
            listOf(
                DialogPromptTurn("user", "Rename a bookmark."),
                DialogPromptTurn("assistant", generated),
            ),
            manager.promptHistory(),
        )
    }

    @Test
    fun `replacement begin resets history to the new initiating prompt pair`() {
        manager.resolve("Rename a bookmark.", nativeCall("begin"), beginCall())
        val replacementGenerated = nativeCall("begin replacement")

        manager.resolve("Clear the queue.", replacementGenerated, beginCall("queue", "clear"))

        assertEquals(
            listOf(
                DialogPromptTurn("user", "Clear the queue."),
                DialogPromptTurn("assistant", replacementGenerated),
            ),
            manager.promptHistory(),
        )
    }

    @Test
    fun `provide slot retains prompt history while confirmation remains pending`() {
        manager.resolve("Delete a bookmark.", nativeCall("begin"), beginCall("bookmark", "delete"))
        val generated = nativeCall("provide_slot")

        assertNull(
            manager.resolve(
                "The latest one.",
                generated,
                provideSlotCall("ref", "latest"),
            ),
        )

        assertTrue(manager.isInProgress)
        assertEquals(
            listOf(
                DialogPromptTurn("user", "Delete a bookmark."),
                DialogPromptTurn("assistant", nativeCall("begin")),
                DialogPromptTurn("user", "The latest one."),
                DialogPromptTurn("assistant", generated),
            ),
            manager.promptHistory(),
        )
    }

    @Test
    fun `prompt history is capped at four turns through valid dialog transitions`() {
        manager.resolve("Delete a bookmark.", nativeCall("begin"), beginCall("bookmark", "delete"))
        manager.resolve("The latest one.", nativeCall("provide ref"), provideSlotCall("ref", "latest"))
        manager.resolve("Call it highlight.", nativeCall("provide title"), provideSlotCall("title", "highlight"))

        assertEquals(
            listOf(
                DialogPromptTurn("user", "The latest one."),
                DialogPromptTurn("assistant", nativeCall("provide ref")),
                DialogPromptTurn("user", "Call it highlight."),
                DialogPromptTurn("assistant", nativeCall("provide title")),
            ),
            manager.promptHistory(),
        )
    }

    @Test
    fun `generated model payload is preserved byte for byte in history`() {
        val generated = " \n\t${nativeCall("begin")}\n  "

        manager.resolve("Rename a bookmark.", generated, beginCall())

        assertEquals(DialogPromptTurn("assistant", generated), manager.promptHistory().last())
    }

    @Test
    fun `completion clears pending dialog and prompt history`() {
        manager.resolve("Clear the queue.", nativeCall("begin"), beginCall("queue", "clear"))

        val result = manager.resolve("Yes.", nativeCall("confirm"), ToolCall("dialog_control", "confirm", emptyMap()))

        assertEquals(VoiceIntent.Queue.Clear, result)
        assertFalse(manager.isInProgress)
        assertTrue(manager.promptHistory().isEmpty())
    }

    @Test
    fun `cancel and deny clear pending dialog and prompt history`() {
        listOf("cancel", "deny").forEach { action ->
            manager.resolve("Rename a bookmark.", nativeCall("begin"), beginCall())

            assertNull(manager.resolve(action, nativeCall(action), ToolCall("dialog_control", action, emptyMap())))
            assertFalse(manager.isInProgress)
            assertTrue(manager.promptHistory().isEmpty())
        }
    }

    @Test
    fun `new command clears pending dialog and prompt history`() {
        manager.resolve("Rename a bookmark.", nativeCall("begin"), beginCall())

        assertNull(
            manager.resolve(
                "Actually pause.",
                nativeCall("new_command"),
                ToolCall("dialog_control", "new_command", mapOf("value" to "Pause.")),
            ),
        )

        assertFalse(manager.isInProgress)
        assertTrue(manager.promptHistory().isEmpty())
    }

    @Test
    fun `new dispatch clears pending dialog and maps the new intent`() {
        manager.resolve("Rename a bookmark.", nativeCall("begin"), beginCall())

        val result = manager.resolve(
            "Pause.",
            nativeCall("pause"),
            ToolCall("playback", "pause", emptyMap()),
        )

        assertEquals(VoiceIntent.Playback.Pause, result)
        assertFalse(manager.isInProgress)
        assertTrue(manager.promptHistory().isEmpty())
    }

    @Test
    fun `existing resolve completes destructive dialog`() {
        assertNull(manager.resolve(beginCall("queue", "clear")))

        val result = manager.resolve(ToolCall("dialog_control", "confirm", emptyMap()))

        assertEquals(VoiceIntent.Queue.Clear, result)
        assertFalse(manager.isInProgress)
    }

    private fun beginCall(
        targetTool: String = "bookmark",
        targetAction: String = "rename",
    ) = ToolCall(
        name = "dialog_control",
        action = "begin",
        params = mapOf(
            "target_tool" to targetTool,
            "target_action" to targetAction,
        ),
    )

    private fun provideSlotCall(
        slot: String,
        value: String,
    ) = ToolCall(
        name = "dialog_control",
        action = "provide_slot",
        params = mapOf(
            "slot" to slot,
            "value" to value,
        ),
    )

    private fun nativeCall(action: String): String {
        return "<start_function_call>call:dialog_control{action:<escape>$action<escape>}<end_function_call>"
    }
}

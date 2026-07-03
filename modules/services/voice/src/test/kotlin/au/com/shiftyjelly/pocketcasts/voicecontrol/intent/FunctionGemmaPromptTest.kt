package au.com.shiftyjelly.pocketcasts.voicecontrol.intent

import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FunctionGemmaPromptTest {
    @Test
    fun `static prefix contains declarations but no user turn`() {
        assertTrue(FunctionGemmaPrompt.staticPrefix.contains("<start_function_declaration>"))
        assertTrue(FunctionGemmaPrompt.staticPrefix.endsWith("<end_of_turn>"))
        assertFalse(FunctionGemmaPrompt.staticPrefix.contains("<start_of_turn>user"))
    }

    @Test
    fun `static prefix matches the training prompt and schema`() {
        val prefix = FunctionGemmaPrompt.staticPrefix
        val sha256 = MessageDigest.getInstance("SHA-256")
            .digest(prefix.toByteArray(Charsets.UTF_8))
            .joinToString("") { byte -> "%02x".format(byte) }

        // Changes require synchronization with the FunctionGemma training prompt and tool schema.
        assertEquals(EXPECTED_STATIC_PREFIX_LENGTH, prefix.length)
        assertEquals(EXPECTED_STATIC_PREFIX_SHA256, sha256)
    }

    @Test
    fun `request suffix preserves the trained single-turn template`() {
        assertEquals(
            "\n<start_of_turn>user\nPause.<end_of_turn>\n<start_of_turn>model\n",
            FunctionGemmaPrompt.requestSuffix("Pause.", emptyList()),
        )
    }

    @Test
    fun `request suffix places history in order before the current user turn`() {
        val history = listOf(
            DialogPromptTurn("user", "Rename the bookmark."),
            DialogPromptTurn(
                "model",
                "<start_function_call>call:dialog_control{action:<escape>begin<escape>," +
                    "target_tool:<escape>bookmark<escape>,target_action:<escape>rename<escape>}" +
                    "<end_function_call>",
            ),
        )

        assertEquals(
            "\n<start_of_turn>user\nRename the bookmark.<end_of_turn>" +
                "\n<start_of_turn>model\n${history[1].content}<end_of_turn>" +
                "\n<start_of_turn>user\nThe second one.<end_of_turn>" +
                "\n<start_of_turn>model\n",
            FunctionGemmaPrompt.requestSuffix("The second one.", history),
        )
    }

    private companion object {
        const val EXPECTED_STATIC_PREFIX_LENGTH = 8691
        const val EXPECTED_STATIC_PREFIX_SHA256 = "7f88fe443062a6cc2de02988dd69da39d55b54bc4dd667d5e0b2a5bf1767fabf"
    }
}

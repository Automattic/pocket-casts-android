package au.com.shiftyjelly.pocketcasts.repositories.chat

import org.junit.Assert.assertEquals
import org.junit.Test

class ChatMessageTest {
    @Test
    fun `display text replaces dash and asterisk bullet markers`() {
        val message = ChatMessage.Assistant(text = "- One\n* Two\n  - Three")

        assertEquals("•  One\n•  Two\n•  Three", message.displayText)
    }

    @Test
    fun `display text keeps blank line between paragraph and list`() {
        val message = ChatMessage.Assistant(text = "Intro:\n\n- One\n- Two")

        assertEquals("Intro:\n\n•  One\n•  Two", message.displayText)
    }

    @Test
    fun `display text does not join a bare marker with the next line`() {
        val message = ChatMessage.Assistant(text = "-\nNext")

        assertEquals("-\nNext", message.displayText)
    }

    @Test
    fun `display text leaves bold markers and hyphenated words alone`() {
        val message = ChatMessage.Assistant(text = "**Bold** start\nwell-known")

        assertEquals("**Bold** start\nwell-known", message.displayText)
    }
}

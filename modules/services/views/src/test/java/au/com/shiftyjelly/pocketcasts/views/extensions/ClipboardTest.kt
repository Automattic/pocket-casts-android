package au.com.shiftyjelly.pocketcasts.views.extensions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ClipboardTest {
    @Test
    fun `returns web link unchanged`() {
        assertEquals("https://pocketcasts.com", linkClipboardText("https://pocketcasts.com"))
    }

    @Test
    fun `removes mailto scheme from email link`() {
        assertEquals("lovetrappedpod@gmail.com", linkClipboardText("mailto:lovetrappedpod@gmail.com"))
    }

    @Test
    fun `removes mailto scheme ignoring case`() {
        assertEquals("lovetrappedpod@gmail.com", linkClipboardText("MAILTO:lovetrappedpod@gmail.com"))
    }

    @Test
    fun `ignores blank links`() {
        assertNull(linkClipboardText(""))
        assertNull(linkClipboardText(" "))
        assertNull(linkClipboardText("mailto:"))
    }
}

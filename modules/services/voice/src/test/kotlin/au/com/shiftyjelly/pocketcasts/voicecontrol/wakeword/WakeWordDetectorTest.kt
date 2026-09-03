package au.com.shiftyjelly.pocketcasts.voicecontrol.wakeword

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WakeWordDetectorTest {

    // ── CommandWindow tests ──

    @Test
    fun `CommandWindow opens on wake word detection`() {
        val window = CommandWindow()
        assertFalse(window.isActive)
        window.onWakeWord()
        assertTrue(window.isActive)
    }

    @Test
    fun `CommandWindow stays open across follow-up utterances`() {
        val window = CommandWindow()
        window.onWakeWord()
        assertTrue(window.isActive)
        window.onActivity()
        assertTrue(window.isActive)
        window.onActivity()
        assertTrue(window.isActive)
    }

    @Test
    fun `CommandWindow closes after silence exceeding conversation timeout`() {
        val shortTimeoutMs = 50L
        val window = CommandWindow(conversationTimeoutMs = shortTimeoutMs)
        window.onWakeWord()
        assertTrue(window.isActive)
        Thread.sleep(shortTimeoutMs + 20)
        assertFalse(window.isActive)
    }

    @Test
    fun `CommandWindow onActivity refreshes the window without re-opening`() {
        val shortTimeoutMs = 100L
        val window = CommandWindow(conversationTimeoutMs = shortTimeoutMs)
        window.onWakeWord()
        assertTrue(window.isActive)
        Thread.sleep(shortTimeoutMs / 2)
        window.onActivity()
        assertTrue(window.isActive)
        Thread.sleep(shortTimeoutMs / 2 + 20)
        assertTrue(window.isActive)
        Thread.sleep(shortTimeoutMs)
        assertFalse(window.isActive)
    }

    @Test
    fun `CommandWindow close immediately sets inactive`() {
        val window = CommandWindow()
        window.onWakeWord()
        assertTrue(window.isActive)
        window.close()
        assertFalse(window.isActive)
    }

    @Test
    fun `CommandWindow reset clears state`() {
        val window = CommandWindow()
        window.onWakeWord()
        window.reset()
        assertFalse(window.isActive)
    }

    @Test
    fun `CommandWindow second onWakeWord while open refreshes window`() {
        val shortTimeoutMs = 100L
        val window = CommandWindow(conversationTimeoutMs = shortTimeoutMs)
        window.onWakeWord()
        Thread.sleep(shortTimeoutMs / 2)
        val result = window.onWakeWord()
        assertTrue(result)
        assertTrue(window.isActive)
        Thread.sleep(shortTimeoutMs / 2 + 20)
        assertTrue(window.isActive)
    }
}

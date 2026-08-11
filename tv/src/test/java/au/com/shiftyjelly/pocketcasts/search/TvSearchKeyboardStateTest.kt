package au.com.shiftyjelly.pocketcasts.search

import android.view.KeyEvent.KEYCODE_DPAD_LEFT
import android.view.KeyEvent.KEYCODE_DPAD_RIGHT
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TvSearchKeyboardStateTest {

    private val state = TvSearchKeyboardState()

    @Test
    fun `initial state selects the first letter on the letters page`() {
        assertFalse(state.isSymbolsPage)
        assertEquals(TvSearchKey.Character('a'), state.selectedKey)
    }

    @Test
    fun `dpad right moves to the next key and consumes the event`() {
        val consumed = state.handleDpadDirection(KEYCODE_DPAD_RIGHT, isKeyDown = true)
        assertTrue(consumed)
        assertEquals(TvSearchKey.Character('b'), state.selectedKey)
    }

    @Test
    fun `dpad left walks onto the leading Space and Toggle keys then stops`() {
        state.handleDpadDirection(KEYCODE_DPAD_LEFT, isKeyDown = true)
        assertEquals(TvSearchKey.Space, state.selectedKey)
        state.handleDpadDirection(KEYCODE_DPAD_LEFT, isKeyDown = true)
        assertEquals(TvSearchKey.TogglePage, state.selectedKey)

        val consumedAtStart = state.handleDpadDirection(KEYCODE_DPAD_LEFT, isKeyDown = true)
        assertFalse(consumedAtStart)
        assertEquals(TvSearchKey.TogglePage, state.selectedKey)
    }

    @Test
    fun `dpad right stops on the trailing Delete key`() {
        repeat(40) { state.handleDpadDirection(KEYCODE_DPAD_RIGHT, isKeyDown = true) }
        assertEquals(TvSearchKey.Delete, state.selectedKey)

        val consumedAtEnd = state.handleDpadDirection(KEYCODE_DPAD_RIGHT, isKeyDown = true)
        assertFalse(consumedAtEnd)
        assertEquals(TvSearchKey.Delete, state.selectedKey)
    }

    @Test
    fun `key up returns the consumed state then resets`() {
        state.handleDpadDirection(KEYCODE_DPAD_RIGHT, isKeyDown = true)
        assertTrue(state.handleDpadDirection(KEYCODE_DPAD_RIGHT, isKeyDown = false))
        assertFalse(state.handleDpadDirection(KEYCODE_DPAD_RIGHT, isKeyDown = false))
    }

    @Test
    fun `toggle page swaps to the symbols page and back`() {
        state.togglePage()
        assertTrue(state.isSymbolsPage)
        assertEquals(TvSearchKey.Character('0'), state.selectedKey)

        state.togglePage()
        assertFalse(state.isSymbolsPage)
        assertEquals(TvSearchKey.Character('a'), state.selectedKey)
    }

    @Test
    fun `toggle page clamps a high selection onto the shorter symbols page`() {
        repeat(23) { state.handleDpadDirection(KEYCODE_DPAD_RIGHT, isKeyDown = true) }
        state.togglePage()
        assertTrue(state.isSymbolsPage)
        assertEquals(TvSearchKey.Delete, state.selectedKey)
    }

    @Test
    fun `isSelected only reports the selection while focused`() {
        assertFalse(state.isSelected(state.selectedIndex))
        state.onFocusChanged(true)
        assertTrue(state.isSelected(state.selectedIndex))
        assertFalse(state.isSelected(state.selectedIndex + 1))
    }
}

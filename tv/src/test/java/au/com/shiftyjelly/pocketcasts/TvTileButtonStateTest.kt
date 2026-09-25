package au.com.shiftyjelly.pocketcasts

import android.view.KeyEvent.KEYCODE_DPAD_LEFT
import android.view.KeyEvent.KEYCODE_DPAD_RIGHT
import android.view.KeyEvent.KEYCODE_DPAD_UP
import au.com.shiftyjelly.pocketcasts.component.TvTileButtonState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TvTileButtonStateTest {

    private val state = TvTileButtonState(buttonCount = 3)

    @Test
    fun `initial state has selectedIndex 0 and is not focused`() {
        assertEquals(0, state.selectedIndex)
        assertFalse(state.isFocused)
    }

    @Test
    fun `onFocusChanged sets isFocused to true`() {
        state.onFocusChanged(true)
        assertTrue(state.isFocused)
    }

    @Test
    fun `onFocusChanged sets isFocused to false`() {
        state.onFocusChanged(true)
        state.onFocusChanged(false)
        assertFalse(state.isFocused)
    }

    @Test
    fun `gaining focus resets selectedIndex to 0`() {
        state.onFocusChanged(true)
        state.handleDpadDirection(KEYCODE_DPAD_RIGHT, isKeyDown = true)
        assertEquals(1, state.selectedIndex)

        state.onFocusChanged(false)
        state.onFocusChanged(true)
        assertEquals(0, state.selectedIndex)
    }

    @Test
    fun `dpad right increments selectedIndex`() {
        val consumed = state.handleDpadDirection(KEYCODE_DPAD_RIGHT, isKeyDown = true)
        assertTrue(consumed)
        assertEquals(1, state.selectedIndex)
    }

    @Test
    fun `dpad left decrements selectedIndex`() {
        state.handleDpadDirection(KEYCODE_DPAD_RIGHT, isKeyDown = true)
        assertEquals(1, state.selectedIndex)

        val consumed = state.handleDpadDirection(KEYCODE_DPAD_LEFT, isKeyDown = true)
        assertTrue(consumed)
        assertEquals(0, state.selectedIndex)
    }

    @Test
    fun `dpad right at max index is not consumed`() {
        state.handleDpadDirection(KEYCODE_DPAD_RIGHT, isKeyDown = true)
        state.handleDpadDirection(KEYCODE_DPAD_RIGHT, isKeyDown = true)
        assertEquals(2, state.selectedIndex)

        val consumed = state.handleDpadDirection(KEYCODE_DPAD_RIGHT, isKeyDown = true)
        assertFalse(consumed)
        assertEquals(2, state.selectedIndex)
    }

    @Test
    fun `dpad left at index 0 is not consumed`() {
        val consumed = state.handleDpadDirection(KEYCODE_DPAD_LEFT, isKeyDown = true)
        assertFalse(consumed)
        assertEquals(0, state.selectedIndex)
    }

    @Test
    fun `key up after consumed key down is also consumed`() {
        state.handleDpadDirection(KEYCODE_DPAD_RIGHT, isKeyDown = true)
        val consumed = state.handleDpadDirection(KEYCODE_DPAD_RIGHT, isKeyDown = false)
        assertTrue(consumed)
    }

    @Test
    fun `key up after unconsumed key down is not consumed`() {
        val consumed = state.handleDpadDirection(KEYCODE_DPAD_LEFT, isKeyDown = false)
        assertFalse(consumed)
    }

    @Test
    fun `non dpad keys are not consumed`() {
        val consumed = state.handleDpadDirection(KEYCODE_DPAD_UP, isKeyDown = true)
        assertFalse(consumed)
    }

    @Test
    fun `isButtonSelected returns true only when focused and index matches`() {
        assertFalse(state.isButtonSelected(0))

        state.onFocusChanged(true)
        assertTrue(state.isButtonSelected(0))
        assertFalse(state.isButtonSelected(1))

        state.handleDpadDirection(KEYCODE_DPAD_RIGHT, isKeyDown = true)
        assertFalse(state.isButtonSelected(0))
        assertTrue(state.isButtonSelected(1))
    }

    @Test
    fun `isButtonSelected returns false when not focused`() {
        state.onFocusChanged(true)
        state.onFocusChanged(false)
        assertFalse(state.isButtonSelected(0))
    }
}

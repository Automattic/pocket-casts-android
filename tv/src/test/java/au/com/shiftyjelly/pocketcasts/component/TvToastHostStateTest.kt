package au.com.shiftyjelly.pocketcasts.component

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TvToastHostStateTest {

    private val state = TvToastHostState()

    @Test
    fun `initial state has no toast`() {
        assertNull(state.currentToast)
    }

    @Test
    fun `show sets the current toast message`() {
        state.show("Marked as played")
        assertEquals("Marked as played", state.currentToast?.message)
    }

    @Test
    fun `showing again replaces the current toast with a new id`() {
        state.show("Will play next")
        val first = state.currentToast

        state.show("Will play last")
        val second = state.currentToast

        assertEquals("Will play last", second?.message)
        assertNotEquals(first?.id, second?.id)
    }

    @Test
    fun `dismiss clears the current toast when the id matches`() {
        state.show("Archived")
        state.dismiss(state.currentToast!!.id)
        assertNull(state.currentToast)
    }

    @Test
    fun `dismiss with a stale id keeps the current toast`() {
        state.show("Archived")
        val staleId = state.currentToast!!.id
        state.show("Unarchived")

        state.dismiss(staleId)

        assertEquals("Unarchived", state.currentToast?.message)
    }
}

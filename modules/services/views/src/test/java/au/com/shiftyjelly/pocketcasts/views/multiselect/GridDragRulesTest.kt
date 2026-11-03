package au.com.shiftyjelly.pocketcasts.views.multiselect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class GridDragRulesTest {
    private val first = DragItemRect("first", 0, 0, 100, 100)
    private val second = DragItemRect("second", 110, 0, 210, 100)
    private val third = DragItemRect("third", 220, 0, 320, 100)
    private val below = DragItemRect("below", 0, 110, 100, 210)
    private val items = listOf(first, second, third, below)

    @Test
    fun `stays put until the cell moved half its size on either axis`() {
        assertNull(ItemTouchDragRules.gridDropTarget(first, items, draggedLeft = 49.9f, draggedTop = 49.9f))
    }

    @Test
    fun `swaps right once the cell passed the whole next cell`() {
        assertNull(ItemTouchDragRules.gridDropTarget(first, items, draggedLeft = 110f, draggedTop = 0f))
        assertEquals(second, ItemTouchDragRules.gridDropTarget(first, items, draggedLeft = 111f, draggedTop = 0f))
    }

    @Test
    fun `swaps left once the cell passed the whole previous cell`() {
        assertNull(ItemTouchDragRules.gridDropTarget(second, items, draggedLeft = 0f, draggedTop = 0f))
        assertEquals(first, ItemTouchDragRules.gridDropTarget(second, items, draggedLeft = -1f, draggedTop = 0f))
    }

    @Test
    fun `swaps down with the cell below`() {
        assertEquals(below, ItemTouchDragRules.gridDropTarget(first, items, draggedLeft = 0f, draggedTop = 111f))
    }

    @Test
    fun `ignores cells the dragged cell does not overlap`() {
        assertNull(ItemTouchDragRules.gridDropTarget(first, items, draggedLeft = 400f, draggedTop = 0f))
    }

    @Test
    fun `the cell passed by the most wins`() {
        assertEquals(second, ItemTouchDragRules.gridDropTarget(first, items, draggedLeft = 115f, draggedTop = 3f))
    }
}

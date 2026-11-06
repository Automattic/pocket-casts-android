package au.com.shiftyjelly.pocketcasts.views.multiselect

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ItemTouchDragRulesTest {
    private val header = DragItemBounds("header", 0, 120)
    private val first = DragItemBounds("first", 120, 288)
    private val second = DragItemBounds("second", 288, 456)
    private val hidden = DragItemBounds("hidden", 456, 576)
    private val third = DragItemBounds("third", 576, 744)
    private val items = listOf(header, first, second, hidden, third)

    @Test
    fun `stays put until the row moved half its height`() {
        assertNull(ItemTouchDragRules.dropTarget(first, items, draggedTop = 120f + 83.9f))
    }

    @Test
    fun `does not swap past the next row centre`() {
        assertNull(ItemTouchDragRules.dropTarget(first, items, draggedTop = 120f + 120f))
        assertNull(ItemTouchDragRules.dropTarget(first, items, draggedTop = 120f + 168f))
    }

    @Test
    fun `swaps once the row passed the whole next row`() {
        assertEquals(second, ItemTouchDragRules.dropTarget(first, items, draggedTop = 120f + 169f))
    }

    @Test
    fun `swaps up once the row passed the whole previous row`() {
        assertNull(ItemTouchDragRules.dropTarget(second, items, draggedTop = 120f))
        assertEquals(first, ItemTouchDragRules.dropTarget(second, items, draggedTop = 119f))
    }

    @Test
    fun `the row passed by the most wins`() {
        assertEquals(second, ItemTouchDragRules.dropTarget(first, items, draggedTop = 420f))
    }

    @Test
    fun `passing only the hidden header targets the header`() {
        assertEquals(hidden, ItemTouchDragRules.dropTarget(second, items, draggedTop = 410f))
        assertEquals(third, ItemTouchDragRules.dropTarget(second, items, draggedTop = 600f))
    }

    @Test
    fun `truncates the drag position like the view`() {
        assertNull(ItemTouchDragRules.dropTarget(first, items, draggedTop = 288.9f))
        assertEquals(second, ItemTouchDragRules.dropTarget(first, items, draggedTop = 289f))
    }

    @Test
    fun `out of bounds only counts in the drag direction`() {
        assertEquals(-20, ItemTouchDragRules.outOfBounds(draggedTop = 1f, dy = -5f, height = 168, paddingTop = 21, viewportHeight = 1000, paddingBottom = 84))
        assertEquals(0, ItemTouchDragRules.outOfBounds(draggedTop = 1f, dy = 5f, height = 168, paddingTop = 21, viewportHeight = 1000, paddingBottom = 84))
        assertEquals(10, ItemTouchDragRules.outOfBounds(draggedTop = 758f, dy = 5f, height = 168, paddingTop = 21, viewportHeight = 1000, paddingBottom = 84))
        assertEquals(0, ItemTouchDragRules.outOfBounds(draggedTop = 758f, dy = -5f, height = 168, paddingTop = 21, viewportHeight = 1000, paddingBottom = 84))
    }

    @Test
    fun `out of bounds scroll accelerates over two seconds`() {
        assertEquals(1, ItemTouchDragRules.outOfBoundsScroll(height = 168, outOfBounds = 84, msSinceStartScroll = 0, maxScroll = 53))
        assertEquals(-1, ItemTouchDragRules.outOfBoundsScroll(height = 168, outOfBounds = -84, msSinceStartScroll = 0, maxScroll = 53))
        assertEquals(1, ItemTouchDragRules.outOfBoundsScroll(height = 168, outOfBounds = 84, msSinceStartScroll = 1000, maxScroll = 53))
        assertEquals(51, ItemTouchDragRules.outOfBoundsScroll(height = 168, outOfBounds = 84, msSinceStartScroll = 2500, maxScroll = 53))
        assertEquals(53, ItemTouchDragRules.outOfBoundsScroll(height = 168, outOfBounds = 400, msSinceStartScroll = 2500, maxScroll = 53))
    }
}

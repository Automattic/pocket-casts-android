package au.com.shiftyjelly.pocketcasts.compose.swipe

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeRowAnchorsTest {
    @Test
    fun `no actions produce only a resting anchor`() {
        val anchors = anchors(leadingWidthPx = null, trailingWidthPx = null)

        assertEquals(1, anchors.size)
        assertEquals(0f, anchors.positionOf(SwipeRowAnchor.Resting))
    }

    @Test
    fun `a leading action adds a positive reveal anchor`() {
        val anchors = anchors(leadingWidthPx = 72f, trailingWidthPx = null)

        assertEquals(72f, anchors.positionOf(SwipeRowAnchor.Leading))
        assertFalse(anchors.hasPositionFor(SwipeRowAnchor.Trailing))
    }

    @Test
    fun `a trailing action adds a negative reveal anchor`() {
        val anchors = anchors(leadingWidthPx = null, trailingWidthPx = 72f)

        assertEquals(-72f, anchors.positionOf(SwipeRowAnchor.Trailing))
        assertFalse(anchors.hasPositionFor(SwipeRowAnchor.Leading))
    }

    @Test
    fun `both actions produce three anchors`() {
        val anchors = anchors(leadingWidthPx = 72f, trailingWidthPx = 72f)

        assertEquals(3, anchors.size)
        assertEquals(72f, anchors.positionOf(SwipeRowAnchor.Leading))
        assertEquals(0f, anchors.positionOf(SwipeRowAnchor.Resting))
        assertEquals(-72f, anchors.positionOf(SwipeRowAnchor.Trailing))
    }

    @Test
    fun `a full swipe action adds a committed anchor at twice the row width`() {
        val anchors = anchors(
            leadingWidthPx = null,
            trailingWidthPx = 72f,
            isTrailingFullSwipeEnabled = true,
        )

        assertEquals(-ROW_WIDTH * FULL_SWIPE_ANCHOR_MULTIPLIER, anchors.positionOf(SwipeRowAnchor.FullTrailing))
    }

    @Test
    fun `a full swipe anchor is omitted when the action does not allow it`() {
        val anchors = anchors(leadingWidthPx = 72f, trailingWidthPx = 72f)

        assertFalse(anchors.hasPositionFor(SwipeRowAnchor.FullLeading))
        assertFalse(anchors.hasPositionFor(SwipeRowAnchor.FullTrailing))
    }

    @Test
    fun `a zero row width produces only a resting anchor`() {
        val anchors = anchors(
            rowWidthPx = 0f,
            leadingWidthPx = 72f,
            trailingWidthPx = 72f,
            isTrailingFullSwipeEnabled = true,
        )

        assertEquals(1, anchors.size)
        assertTrue(anchors.hasPositionFor(SwipeRowAnchor.Resting))
    }

    private fun anchors(
        rowWidthPx: Float = ROW_WIDTH,
        leadingWidthPx: Float?,
        trailingWidthPx: Float?,
        isLeadingFullSwipeEnabled: Boolean = false,
        isTrailingFullSwipeEnabled: Boolean = false,
    ) = swipeRowAnchors(
        rowWidthPx = rowWidthPx,
        leadingWidthPx = leadingWidthPx,
        trailingWidthPx = trailingWidthPx,
        isLeadingFullSwipeEnabled = isLeadingFullSwipeEnabled,
        isTrailingFullSwipeEnabled = isTrailingFullSwipeEnabled,
    )

    private companion object {
        const val ROW_WIDTH = 1080f
    }
}

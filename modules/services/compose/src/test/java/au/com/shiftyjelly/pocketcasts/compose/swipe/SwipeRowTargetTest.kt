package au.com.shiftyjelly.pocketcasts.compose.swipe

import org.junit.Assert.assertEquals
import org.junit.Test

class SwipeRowTargetTest {
    @Test
    fun `release stops at the resting position before a third of the buttons`() {
        assertEquals(SwipeRowAnchor.Resting, target(offset = -70f))
    }

    @Test
    fun `release opens the row past a third of the buttons`() {
        assertEquals(SwipeRowAnchor.Trailing, target(offset = -73f))
    }

    @Test
    fun `moving back keeps the row open until two thirds of the buttons`() {
        assertEquals(SwipeRowAnchor.Trailing, target(offset = -150f, velocity = 1f))
        assertEquals(SwipeRowAnchor.Resting, target(offset = -140f, velocity = 1f))
    }

    @Test
    fun `a paused release keeps the direction of the last drag`() {
        assertEquals(SwipeRowAnchor.Resting, target(offset = -140f, lastDragDirection = 1f))
        assertEquals(SwipeRowAnchor.Trailing, target(offset = -140f, lastDragDirection = -1f))
    }

    @Test
    fun `release past the full swipe threshold commits a full swipe`() {
        assertEquals(SwipeRowAnchor.FullTrailing, target(offset = -240f))
        assertEquals(SwipeRowAnchor.FullTrailing, target(offset = -240f, velocity = 1f))
    }

    @Test
    fun `release past the threshold only opens the row without full swipe`() {
        assertEquals(SwipeRowAnchor.Leading, target(offset = 400f))
    }

    @Test
    fun `an opening fling opens the row without committing a full swipe`() {
        assertEquals(SwipeRowAnchor.Trailing, target(offset = -30f, velocity = -FLING))
        assertEquals(SwipeRowAnchor.Trailing, target(offset = -230f, velocity = -FLING))
    }

    @Test
    fun `an opening fling past the full swipe threshold commits a full swipe`() {
        assertEquals(SwipeRowAnchor.FullTrailing, target(offset = -240f, velocity = -FLING))
    }

    @Test
    fun `a closing fling closes the row`() {
        assertEquals(SwipeRowAnchor.Resting, target(offset = -200f, velocity = FLING))
        assertEquals(SwipeRowAnchor.Resting, target(offset = -300f, velocity = FLING))
    }

    @Test
    fun `a side without actions stays at rest`() {
        assertEquals(SwipeRowAnchor.Resting, target(offset = 100f, leading = SwipeRowSide(0f, null)))
    }

    private fun target(
        offset: Float,
        velocity: Float = 0f,
        lastDragDirection: Float = 0f,
        leading: SwipeRowSide = SwipeRowSide(revealedWidthPx = 144f, fullSwipeThresholdPx = null),
        trailing: SwipeRowSide = SwipeRowSide(revealedWidthPx = 216f, fullSwipeThresholdPx = 240f),
    ) = swipeRowTarget(offset, velocity, lastDragDirection, minFlingVelocity = FLING, leading, trailing)

    private companion object {
        const val FLING = 50f
    }
}

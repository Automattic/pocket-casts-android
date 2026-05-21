package au.com.shiftyjelly.pocketcasts.views.extensions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RecyclerViewTest {
    @Test
    fun `quickScrollSpeedPerPixel returns null for non-positive scrollRange`() {
        assertNull(quickScrollSpeedPerPixel(scrollOffset = 100, scrollRange = 0, densityDpi = 480))
        assertNull(quickScrollSpeedPerPixel(scrollOffset = 100, scrollRange = -1, densityDpi = 480))
    }

    @Test
    fun `quickScrollSpeedPerPixel returns null for non-positive scrollOffset`() {
        assertNull(quickScrollSpeedPerPixel(scrollOffset = 0, scrollRange = 1000, densityDpi = 480))
        assertNull(quickScrollSpeedPerPixel(scrollOffset = -1, scrollRange = 1000, densityDpi = 480))
    }

    @Test
    fun `quickScrollSpeedPerPixel returns positive speed for typical values`() {
        val speed = quickScrollSpeedPerPixel(scrollOffset = 500, scrollRange = 5000, densityDpi = 480)
        assertNotNull(speed)
        assertTrue("Speed should be positive but was $speed", speed!! > 0f)
    }

    @Test
    fun `quickScrollSpeedPerPixel stays positive when scrollRange times scrollOffset overflows Int`() {
        // The previous implementation computed scrollRange * scrollOffset as Int, which
        // overflows when the product exceeds Int.MAX_VALUE (~2.1B). Verify the new
        // implementation returns a positive speed even in that regime, since a
        // non-positive speed leads to LinearSmoothScroller throwing
        // IllegalStateException: "If you provide an interpolator, you must set a positive duration".
        val speed = quickScrollSpeedPerPixel(scrollOffset = 60_000, scrollRange = 100_000, densityDpi = 480)
        assertNotNull(speed)
        assertTrue("Speed should be positive but was $speed", speed!! > 0f)
    }

    @Test
    fun `quickScrollSpeedPerPixel caps speed at MAX_MILLIS_PER_INCH per densityDpi`() {
        // Small scrollOffset would otherwise produce a very large millis-per-pixel value,
        // but it should be capped at MAX_MILLIS_PER_INCH / densityDpi.
        val densityDpi = 480
        val speed = quickScrollSpeedPerPixel(scrollOffset = 1, scrollRange = 1000, densityDpi = densityDpi)
        assertEquals(50f / densityDpi, speed!!, 0.0001f)
    }
}

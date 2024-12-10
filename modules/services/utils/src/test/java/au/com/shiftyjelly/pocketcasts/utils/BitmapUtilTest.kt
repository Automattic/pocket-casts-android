package au.com.shiftyjelly.pocketcasts.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class BitmapUtilTest {
    @Test
    fun `fail for zero aspect ratio`() {
        try {
            calculateNearestSize(0, 0, aspectRatio = 0f)
        } catch (e: IllegalStateException) {
            assertEquals("Aspect ratio must be a positive number: 0.0", e.message)
        }
    }

    @Test
    fun `fail for negative aspect ratio`() {
        try {
            calculateNearestSize(0, 0, aspectRatio = -1f)
        } catch (e: IllegalStateException) {
            assertEquals("Aspect ratio must be a positive number: -1.0", e.message)
        }
    }

    @Test
    fun `calculate to fit horizontally`() {
        val size = calculateNearestSize(width = 100, height = 200, aspectRatio = 9f / 16)

        assertEquals(113 to 200, size)
    }

    @Test
    fun `calculate to fit vertically`() {
        val size = calculateNearestSize(width = 200, height = 100, aspectRatio = 9f / 16)

        assertEquals(200 to 356, size)
    }
}

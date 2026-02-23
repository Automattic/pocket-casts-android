package au.com.shiftyjelly.pocketcasts.views.helper

import android.view.View
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`

class PredictiveBackAnimatorTest {
    private lateinit var mockView: View

    @Before
    fun setup() {
        mockView = mock(View::class.java)
    }

    @Test
    fun `applyProgress at 0 progress maintains full scale and alpha`() {
        PredictiveBackAnimator.applyProgress(mockView, progress = 0f)

        verify(mockView).scaleX = 1f
        verify(mockView).scaleY = 1f
        verify(mockView).alpha = 1f
    }

    @Test
    fun `applyProgress at 0_5 progress reduces scale and alpha`() {
        // With default scale = 0.1 and alpha = 0.3
        // At progress 0.5: scale = 1 - (0.1 * 0.5) = 0.95
        // At progress 0.5: alpha = 1 - (0.3 * 0.5) = 0.85
        PredictiveBackAnimator.applyProgress(mockView, progress = 0.5f)

        verify(mockView).scaleX = 0.95f
        verify(mockView).scaleY = 0.95f
        verify(mockView).alpha = 0.85f
    }

    @Test
    fun `applyProgress at 1 progress scales to 0_9 and alpha to 0_7`() {
        PredictiveBackAnimator.applyProgress(mockView, progress = 1f)

        verify(mockView).scaleX = 0.9f
        verify(mockView).scaleY = 0.9f
        verify(mockView).alpha = 0.7f
    }

    @Test
    fun `applyProgress with custom amounts uses provided values`() {
        PredictiveBackAnimator.applyProgress(
            mockView,
            progress = 1f,
            scaleAmount = 0.2f,
            alphaAmount = 0.5f,
        )

        verify(mockView).scaleX = 0.8f
        verify(mockView).scaleY = 0.8f
        verify(mockView).alpha = 0.5f
    }

    @Test
    fun `applyProgressReverse at 0 progress starts at reduced scale and alpha`() {
        PredictiveBackAnimator.applyProgressReverse(mockView, progress = 0f)

        verify(mockView).scaleX = 0.95f
        verify(mockView).scaleY = 0.95f
        verify(mockView).alpha = 0.5f
    }

    @Test
    fun `applyProgressReverse at 1 progress reaches full scale and alpha`() {
        PredictiveBackAnimator.applyProgressReverse(mockView, progress = 1f)

        verify(mockView).scaleX = 1f
        verify(mockView).scaleY = 1f
        verify(mockView).alpha = 1f
    }

    @Test
    fun `applyProgressReverse with custom amounts uses provided values`() {
        PredictiveBackAnimator.applyProgressReverse(
            mockView,
            progress = 1f,
            scaleAmount = 0.1f,
            alphaAmount = 0.6f,
        )

        verify(mockView).scaleX = 1f
        verify(mockView).scaleY = 1f
        verify(mockView).alpha = 1f
    }

    @Test
    fun `reset restores view to full scale and alpha`() {
        PredictiveBackAnimator.reset(mockView)

        verify(mockView).scaleX = 1f
        verify(mockView).scaleY = 1f
        verify(mockView).alpha = 1f
    }

    @Test
    fun `default constants have expected values`() {
        assertEquals(0.1f, PredictiveBackAnimator.Defaults.SCALE_AMOUNT)
        assertEquals(0.3f, PredictiveBackAnimator.Defaults.ALPHA_AMOUNT)
        assertEquals(0.05f, PredictiveBackAnimator.Defaults.SCALE_AMOUNT_REVERSE)
        assertEquals(0.5f, PredictiveBackAnimator.Defaults.ALPHA_AMOUNT_REVERSE)
        assertEquals(0.85f, PredictiveBackAnimator.Defaults.TARGET_SCALE)
        assertEquals(0f, PredictiveBackAnimator.Defaults.TARGET_ALPHA)
        assertEquals(150L, PredictiveBackAnimator.Defaults.ANIMATION_DURATION_MS)
        assertEquals(100L, PredictiveBackAnimator.Defaults.SHORT_ANIMATION_DURATION_MS)
    }
}

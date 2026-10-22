package au.com.shiftyjelly.pocketcasts.component

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TvTopBarVisibilityTest {

    private val visibility = TvTopBarVisibility()

    @Test
    fun `top bar is visible initially`() {
        assertTrue(visibility.isVisible)
    }

    @Test
    fun `entering a detail hides the top bar`() {
        visibility.enterDetail()
        assertFalse(visibility.isVisible)
    }

    @Test
    fun `exiting the only detail shows the top bar again`() {
        visibility.enterDetail()
        visibility.exitDetail()
        assertTrue(visibility.isVisible)
    }

    @Test
    fun `nested details keep the bar hidden until the outer one exits`() {
        visibility.enterDetail()
        visibility.enterDetail()
        visibility.exitDetail()
        assertFalse(visibility.isVisible)

        visibility.exitDetail()
        assertTrue(visibility.isVisible)
    }

    @Test
    fun `an unbalanced exit cannot hide the bar permanently`() {
        visibility.exitDetail()
        assertTrue(visibility.isVisible)
    }
}

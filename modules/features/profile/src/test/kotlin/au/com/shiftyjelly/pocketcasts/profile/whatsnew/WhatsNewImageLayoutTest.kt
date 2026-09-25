package au.com.shiftyjelly.pocketcasts.profile.whatsnew

import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import org.junit.Assert.assertEquals
import org.junit.Test

class WhatsNewImageLayoutTest {
    @Test
    fun `a wide image fills the content width`() {
        val size = WhatsNewImageLayout.size(aspectRatio = 2f, contentWidth = 320.dp, pageHeight = 800.dp)

        assertEquals(DpSize(320.dp, 160.dp), size)
    }

    @Test
    fun `a tall image is capped at just over half the page height and keeps its shape`() {
        val size = WhatsNewImageLayout.size(aspectRatio = 0.5f, contentWidth = 320.dp, pageHeight = 800.dp)

        assertEquals(416f, size.height.value, 0.01f)
        assertEquals(208f, size.width.value, 0.01f)
    }

    @Test
    fun `an image of unknown shape is capped at the same height`() {
        assertEquals(416f, WhatsNewImageLayout.maximumHeight(800.dp).value, 0.01f)
    }
}

package au.com.shiftyjelly.pocketcasts.wear.ui.episode

import android.graphics.Bitmap
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import android.graphics.Color as AndroidColor

@RunWith(RobolectricTestRunner::class)
class LightVibrantColorTest {

    @Test
    fun `returns a light vibrant color for saturated light artwork`() {
        val bitmap = solidBitmap(AndroidColor.rgb(255, 128, 128))

        val color = bitmap.lightVibrantColor()

        assertNotNull(color)
        assertTrue(color!!.red > color.green && color.red > color.blue)
    }

    @Test
    fun `returns null for artwork without a light vibrant swatch`() {
        val bitmap = solidBitmap(AndroidColor.rgb(128, 128, 128))

        assertNull(bitmap.lightVibrantColor())
    }

    private fun solidBitmap(color: Int) = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888).apply {
        eraseColor(color)
    }
}

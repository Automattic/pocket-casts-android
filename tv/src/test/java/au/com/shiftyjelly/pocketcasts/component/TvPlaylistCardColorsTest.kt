package au.com.shiftyjelly.pocketcasts.component

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class TvPlaylistCardColorsTest {

    @Test
    fun `saturated tint is clamped to the card range`() {
        val color = TvPlaylistCardColors.cardColor(podcastTint = 0xFFFF0000.toInt(), seed = "seed")

        assertEquals(Color.hsv(hue = 0f, saturation = 0.85f, value = 0.45f), color)
    }

    @Test
    fun `mid tone tint keeps its hue`() {
        val color = TvPlaylistCardColors.cardColor(podcastTint = 0xFF406080.toInt(), seed = "seed")
        val expected = Color.hsv(hue = 210f, saturation = 0.5f, value = 0.45f)

        assertEquals(expected.red, color.red, 0.005f)
        assertEquals(expected.green, color.green, 0.005f)
        assertEquals(expected.blue, color.blue, 0.005f)
    }

    @Test
    fun `grey tint falls back to the seeded palette`() {
        val greyColor = TvPlaylistCardColors.cardColor(podcastTint = 0xFF808080.toInt(), seed = "seed")
        val seededColor = TvPlaylistCardColors.cardColor(podcastTint = null, seed = "seed")

        assertEquals(seededColor, greyColor)
    }

    @Test
    fun `near black tint falls back to the seeded palette`() {
        val darkColor = TvPlaylistCardColors.cardColor(podcastTint = 0xFF1E1F1E.toInt(), seed = "seed")
        val seededColor = TvPlaylistCardColors.cardColor(podcastTint = null, seed = "seed")

        assertEquals(seededColor, darkColor)
    }

    @Test
    fun `fallback color is stable for a seed`() {
        val first = TvPlaylistCardColors.cardColor(podcastTint = null, seed = "playlist-uuid")
        val second = TvPlaylistCardColors.cardColor(podcastTint = null, seed = "playlist-uuid")
        val other = TvPlaylistCardColors.cardColor(podcastTint = null, seed = "different-uuid")

        assertEquals(first, second)
        assertNotEquals(first, other)
    }
}

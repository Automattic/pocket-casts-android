package au.com.shiftyjelly.pocketcasts.compose

import androidx.compose.ui.graphics.Color
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class PodcastColorsTest {
    @Test
    fun `player tint stays opaque when the podcast has no extracted colors`() {
        val podcastColors = PodcastColors(Podcast(uuid = "uuid"))

        assertEquals(1f, podcastColors.playerTint.alpha, 0f)
        assertEquals(Color.White, podcastColors.playerTint)
    }

    @Test
    fun `player tint falls back to white when the podcast uses the server default dark tint`() {
        val serverDefaultDarkTint = 0xFFC62828.toInt()

        val podcastColors = PodcastColors(Podcast(uuid = "uuid", tintColorForDarkBg = serverDefaultDarkTint))

        assertEquals(Color.White, podcastColors.playerTint)
    }

    @Test
    fun `player tint uses the podcast dark tint when it is set`() {
        val tint = 0xFF3366FF.toInt()

        val podcastColors = PodcastColors(Podcast(uuid = "uuid", tintColorForDarkBg = tint))

        assertEquals(Color(tint), podcastColors.playerTint)
    }
}

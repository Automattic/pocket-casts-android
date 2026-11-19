package au.com.shiftyjelly.pocketcasts.player.view

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.performClick
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class LongPressOptionsPageTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private var markPlayedClicks = 0
    private var nextEpisodeClicks = 0

    private fun setContent(showNextEpisode: Boolean) {
        composeTestRule.setContent {
            AppTheme(Theme.ThemeType.DARK) {
                LongPressOptionsPage(
                    showNextEpisode = showNextEpisode,
                    onMarkPlayedClick = { markPlayedClicks++ },
                    onNextEpisodeClick = { nextEpisodeClicks++ },
                )
            }
        }
    }

    @Test
    fun `rows invoke their callbacks`() {
        setContent(showNextEpisode = true)

        composeTestRule.onNodeWithContentDescription("Mark played").performClick()
        composeTestRule.onNodeWithContentDescription("Next episode").performClick()

        assertEquals(1, markPlayedClicks)
        assertEquals(1, nextEpisodeClicks)
    }

    @Test
    fun `next episode row is hidden without a next episode`() {
        setContent(showNextEpisode = false)

        composeTestRule.onNodeWithContentDescription("Mark played").assertExists()
        composeTestRule.onNodeWithContentDescription("Next episode").assertDoesNotExist()
    }
}

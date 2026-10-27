package au.com.shiftyjelly.pocketcasts.profile.sonos

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import au.com.shiftyjelly.pocketcasts.images.R as IR

@RunWith(RobolectricTestRunner::class)
class SonosAppLinkPageTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows the title and state texts and forwards close and connect clicks`() {
        var closeClicks = 0
        var connectClicks = 0
        composeTestRule.setContent {
            AppTheme(Theme.ThemeType.LIGHT) {
                SonosAppLinkPage(
                    imageRes = IR.drawable.sonos_light,
                    explanation = "Explanation",
                    buttonText = "Setup Account",
                    textColors = SonosTextColors(title = Color.Black, explanation = Color.Black, button = Color.White),
                    onCloseClick = { closeClicks++ },
                    onConnectClick = { connectClicks++ },
                )
            }
        }

        composeTestRule.onNodeWithText("Connect To Sonos").assertExists()
        composeTestRule.onNodeWithText("Explanation").assertExists()
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.onNodeWithText("Setup Account").performClick()

        assertEquals(1, closeClicks)
        assertEquals(1, connectClicks)
    }
}

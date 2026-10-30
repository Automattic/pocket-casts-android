package au.com.shiftyjelly.pocketcasts.profile

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SubCancelledPageTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows the expiry hint and the cancellation notes and closes on done`() {
        var doneClicks = 0
        composeTestRule.setContent {
            AppTheme(Theme.ThemeType.LIGHT) {
                SubCancelledPage(
                    expiryHint = "Plus expires on 1 Jan 2027",
                    onDoneClick = { doneClicks++ },
                )
            }
        }

        composeTestRule.onNodeWithText("Sorry to see you go!").assertExists()
        composeTestRule.onNodeWithText("Plus expires on 1 Jan 2027").assertExists()
        composeTestRule.onNodeWithText("Access to Pocket Casts Plus features will be locked").assertExists()
        composeTestRule.onNodeWithText("Done").performClick()

        assertEquals(1, doneClicks)
    }
}

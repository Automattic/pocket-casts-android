package au.com.shiftyjelly.pocketcasts.account

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
import au.com.shiftyjelly.pocketcasts.images.R as IR

@RunWith(RobolectricTestRunner::class)
class PromoCodeUpgradedPageTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows the code description above the welcome text and dismisses on done`() {
        var doneClicks = 0
        composeTestRule.setContent {
            AppTheme(Theme.ThemeType.LIGHT) {
                PromoCodeUpgradedPage(
                    codeDescription = "Three months free",
                    logoRes = IR.drawable.plus_logo_vertical_white,
                    onDoneClick = { doneClicks++ },
                )
            }
        }

        composeTestRule.onNodeWithText("Your account has been upgraded").assertExists()
        composeTestRule.onNodeWithText("Three months free\n", substring = true).assertExists()
        composeTestRule.onNodeWithText("Done").performClick()

        assertEquals(1, doneClicks)
    }
}

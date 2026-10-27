package au.com.shiftyjelly.pocketcasts.compose.buttons

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
class RowButtonTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `clicking the button invokes the callback once`() {
        var clicks = 0
        composeTestRule.setContent {
            AppTheme(Theme.ThemeType.LIGHT) {
                RowButton(text = "Continue", onClick = { clicks++ })
            }
        }

        composeTestRule.onNodeWithText("Continue").performClick()

        assertEquals(1, clicks)
    }
}

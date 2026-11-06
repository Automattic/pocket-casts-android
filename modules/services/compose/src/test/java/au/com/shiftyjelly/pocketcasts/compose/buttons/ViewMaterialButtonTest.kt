package au.com.shiftyjelly.pocketcasts.compose.buttons

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
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
class ViewMaterialButtonTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `clicking the label invokes the callback once`() {
        var clicks = 0
        composeTestRule.setContent {
            AppTheme(Theme.ThemeType.LIGHT) {
                ViewMaterialButton(
                    text = "Done",
                    onClick = { clicks++ },
                    backgroundColor = Color.Blue,
                    textColor = Color.White,
                )
            }
        }

        composeTestRule.onNodeWithText("Done", useUnmergedTree = false)
            .assert(SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.Button))
            .performClick()

        assertEquals(1, clicks)
    }
}

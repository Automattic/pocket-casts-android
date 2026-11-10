package au.com.shiftyjelly.pocketcasts.views.compose

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.SettingRow
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SettingRowTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `shows primary and secondary texts`() {
        composeTestRule.setContent {
            AppTheme(Theme.ThemeType.DARK) {
                SettingRow(
                    primaryText = "Warn before using data",
                    secondaryText = "Only on Wi-Fi",
                )
            }
        }

        composeTestRule.onNodeWithText("Warn before using data").assertIsDisplayed()
        composeTestRule.onNodeWithText("Only on Wi-Fi").assertIsDisplayed()
    }
}

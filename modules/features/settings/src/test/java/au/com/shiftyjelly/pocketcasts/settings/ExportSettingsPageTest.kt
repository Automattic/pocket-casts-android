package au.com.shiftyjelly.pocketcasts.settings

import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ExportSettingsPageTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val clicks = mutableListOf<String>()

    @Test
    fun `shows the title and both sections`() {
        setContent()

        composeTestRule.onNodeWithText("Import & export OPML").assertExists()
        composeTestRule.onNodeWithText("Import podcasts").assertExists()
        composeTestRule.onNodeWithText("Export podcasts").assertExists()
    }

    @Test
    fun `passes each row tap`() {
        setContent()

        composeTestRule.onNodeWithText("Select file").performScrollTo().performClick()
        composeTestRule.onNodeWithText("By URL").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Send email").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Save file").performScrollTo().performClick()

        assertEquals(listOf("file", "url", "email", "save"), clicks)
    }

    @Test
    fun `navigates back from the toolbar`() {
        setContent()

        composeTestRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(listOf("back"), clicks)
    }

    private fun setContent() {
        composeTestRule.setContent {
            AppTheme(Theme.ThemeType.LIGHT) {
                ExportSettingsPage(
                    bottomInset = 0.dp,
                    onNavigationClick = { clicks += "back" },
                    onImportFileClick = { clicks += "file" },
                    onImportUrlClick = { clicks += "url" },
                    onExportEmailClick = { clicks += "email" },
                    onExportFileClick = { clicks += "save" },
                )
            }
        }
    }
}

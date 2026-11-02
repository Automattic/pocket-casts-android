package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
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

@RunWith(RobolectricTestRunner::class)
class BookmarksContainerTopBarTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private var navigationClicks = 0

    @Test
    fun `shows a back button when hosted as a screen`() {
        setContent(isDialog = false)

        composeTestRule.onNodeWithText("Bookmarks").assertExists()
        composeTestRule.onNodeWithContentDescription("Back").performClick()

        assertEquals(1, navigationClicks)
    }

    @Test
    fun `shows a close button when hosted as a dialog`() {
        setContent(isDialog = true)

        composeTestRule.onNodeWithContentDescription("Back").assertDoesNotExist()
        composeTestRule.onNodeWithContentDescription("Close").performClick()

        assertEquals(1, navigationClicks)
    }

    @Test
    fun `shows the multi-select top bar instead of the toolbar`() {
        setContent(isDialog = false, multiSelectTopBar = { Text("3") })

        composeTestRule.onNodeWithText("3").assertExists()
        composeTestRule.onNodeWithText("Bookmarks").assertDoesNotExist()
    }

    private fun setContent(isDialog: Boolean, multiSelectTopBar: (@Composable () -> Unit)? = null) {
        composeTestRule.setContent {
            AppTheme(Theme.ThemeType.LIGHT) {
                BookmarksContainerTopBar(
                    isDialog = isDialog,
                    onNavigationClick = { navigationClicks++ },
                    multiSelectTopBar = multiSelectTopBar,
                )
            }
        }
    }
}

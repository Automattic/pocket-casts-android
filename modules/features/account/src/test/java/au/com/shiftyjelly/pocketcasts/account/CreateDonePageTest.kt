package au.com.shiftyjelly.pocketcasts.account

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
class CreateDonePageTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val events = mutableListOf<String>()

    @Test
    fun `phone shows the account and handles newsletter, close and done`() {
        setContent(isCar = false)

        composeTestRule.onNodeWithText("Account Created").assertExists()
        composeTestRule.onNodeWithText("Welcome to Pocket Casts!").assertExists()
        composeTestRule.onNodeWithText("Pocket Casts newsletter").performClick()
        composeTestRule.onNodeWithContentDescription("Close").performClick()
        composeTestRule.onNodeWithText("Done").performClick()

        assertEquals(listOf("newsletter true", "close", "done"), events)
    }

    @Test
    fun `car has no close button`() {
        setContent(isCar = true)

        composeTestRule.onNodeWithContentDescription("Close").assertDoesNotExist()
        composeTestRule.onNodeWithText("Done").performClick()

        assertEquals(listOf("done"), events)
    }

    private fun setContent(isCar: Boolean) {
        composeTestRule.setContent {
            AppTheme(Theme.ThemeType.LIGHT) {
                CreateDonePage(
                    state = CreateDonePageState(
                        isCar = isCar,
                        imageRes = R.drawable.ic_created_free_account,
                        title = "Account Created",
                        detail = "Welcome to Pocket Casts!",
                        isNewsletterChecked = false,
                        isProgressVisible = false,
                    ),
                    onNewsletterChange = { events += "newsletter $it" },
                    onCloseClick = { events += "close" },
                    onDoneClick = { events += "done" },
                )
            }
        }
    }
}

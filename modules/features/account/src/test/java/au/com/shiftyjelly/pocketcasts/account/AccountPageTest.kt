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
class AccountPageTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val clicks = mutableListOf<String>()

    @Test
    fun `signed out shows create and sign in`() {
        setContent(isSignedIn = false)

        composeTestRule.onNodeWithText("Sign in or create account").assertExists()
        composeTestRule.onNodeWithText("Create account").performClick()
        composeTestRule.onNodeWithText("Sign in").performClick()
        composeTestRule.onNodeWithContentDescription("Cancel").performClick()

        assertEquals(listOf("create", "signIn", "close"), clicks)
    }

    @Test
    fun `signed in shows done only`() {
        setContent(isSignedIn = true)

        composeTestRule.onNodeWithText("Sign in").assertDoesNotExist()
        composeTestRule.onNodeWithText("Done").performClick()

        assertEquals(listOf("create"), clicks)
    }

    private fun setContent(isSignedIn: Boolean) {
        composeTestRule.setContent {
            AppTheme(Theme.ThemeType.LIGHT) {
                AccountPage(
                    state = AccountPageState(isSignedIn = isSignedIn, isCar = false, isLandscape = false),
                    onCloseClick = { clicks += "close" },
                    onCreateClick = { clicks += "create" },
                    onSignInClick = { clicks += "signIn" },
                )
            }
        }
    }
}

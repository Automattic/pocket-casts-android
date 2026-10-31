package au.com.shiftyjelly.pocketcasts.account

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
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
class SignInPageTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val clicks = mutableListOf<String>()

    @Test
    fun `valid form signs in, shows the error and opens forgot password`() {
        setContent(SignInPageState(isCar = false, isLandscape = false, isFormValid = true, isLoading = false, isProgressVisible = false, errorText = "Incorrect password"))

        composeTestRule.onNodeWithText("Incorrect password").assertExists()
        composeTestRule.onNodeWithText("Sign in").assertIsEnabled().performClick()
        composeTestRule.onNodeWithText("I forgot my password").performClick()

        assertEquals(listOf("signIn", "forgot"), clicks)
    }

    @Test
    fun `invalid form disables sign in and car shows the title`() {
        setContent(SignInPageState(isCar = true, isLandscape = true, isFormValid = false, isLoading = false, isProgressVisible = false, errorText = null))

        composeTestRule.onAllNodesWithText("Sign in").assertCountEquals(2)[1].assertIsNotEnabled().performClick()

        assertEquals(emptyList<String>(), clicks)
    }

    private fun setContent(state: SignInPageState) {
        composeTestRule.setContent {
            AppTheme(Theme.ThemeType.LIGHT) {
                SignInPage(state = state, onSignInClick = { clicks += "signIn" }, onForgotPasswordClick = { clicks += "forgot" })
            }
        }
    }
}

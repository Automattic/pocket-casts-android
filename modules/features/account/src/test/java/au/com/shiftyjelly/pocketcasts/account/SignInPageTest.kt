package au.com.shiftyjelly.pocketcasts.account

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasImeAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.text.input.ImeAction
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
        setContent(SignInPageState(isCar = false, isLandscape = false, email = "test@example.com", password = "abcdef12", isEmailValid = true, isFormValid = true, isLoading = false, isProgressVisible = false, errorText = "Incorrect password"))

        composeTestRule.onNodeWithText("Incorrect password").assertExists()
        composeTestRule.onNodeWithText("Sign in").assertIsEnabled().performClick()
        composeTestRule.onNodeWithText("I forgot my password").performClick()

        assertEquals(listOf("signIn", "forgot"), clicks)
    }

    @Test
    fun `typing updates the fields and the keyboard actions move focus and sign in`() {
        setContent(SignInPageState(isCar = false, isLandscape = false, email = "", password = "", isEmailValid = false, isFormValid = false, isLoading = false, isProgressVisible = false, errorText = null))

        composeTestRule.onNode(hasImeAction(ImeAction.Next)).performTextInput("test@example.com")
        composeTestRule.onNode(hasImeAction(ImeAction.Next)).performImeAction()
        composeTestRule.onNode(hasImeAction(ImeAction.Done)).assertIsFocused().performTextInput("abcdef12")
        composeTestRule.onNode(hasImeAction(ImeAction.Done)).performImeAction()

        assertEquals(listOf("email:test@example.com", "password:abcdef12", "signIn"), clicks)
    }

    @Test
    fun `invalid form disables sign in and car shows the title`() {
        setContent(SignInPageState(isCar = true, isLandscape = true, email = "", password = "", isEmailValid = false, isFormValid = false, isLoading = false, isProgressVisible = false, errorText = null))

        composeTestRule.onAllNodesWithText("Sign in").assertCountEquals(2)[1].assertIsNotEnabled().performClick()

        assertEquals(emptyList<String>(), clicks)
    }

    private fun setContent(state: SignInPageState) {
        composeTestRule.setContent {
            var pageState by remember { mutableStateOf(state) }
            AppTheme(Theme.ThemeType.LIGHT) {
                SignInPage(
                    state = pageState,
                    onEmailChange = {
                        clicks += "email:$it"
                        pageState = pageState.copy(email = it)
                    },
                    onPasswordChange = {
                        clicks += "password:$it"
                        pageState = pageState.copy(password = it)
                    },
                    onSignInClick = { clicks += "signIn" },
                    onForgotPasswordClick = { clicks += "forgot" },
                )
            }
        }
    }
}

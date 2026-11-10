package au.com.shiftyjelly.pocketcasts.account

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class CreateEmailPageTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private var clicks = 0
    private val emails = mutableListOf<String>()

    @Test
    fun `valid form goes next and shows the error`() {
        setContent(CreateEmailPageState(isCar = false, isLandscape = false, email = "test@example.com", password = "abcdef12", isEmailValid = true, isFormValid = true, isPasswordInvalid = false, isProgressVisible = false, errorText = "We couldn't set up that account, sorry."))

        composeTestRule.onNodeWithText("We couldn't set up that account, sorry.").assertExists()
        composeTestRule.onNodeWithText("Password must be at least 6 characters").assertExists()
        composeTestRule.onNodeWithText("Next").assertIsEnabled().performClick()
        composeTestRule.onNodeWithText("test@example.com").performTextInput("a")

        assertEquals(1, clicks)
        assertEquals(listOf("atest@example.com"), emails)
    }

    @Test
    fun `invalid form disables create account on car`() {
        setContent(CreateEmailPageState(isCar = true, isLandscape = true, email = "test", password = "abc", isEmailValid = false, isFormValid = false, isPasswordInvalid = true, isProgressVisible = true, errorText = ""))

        composeTestRule.onNodeWithText("Password must be at least 6 characters").assertExists()
        composeTestRule.onAllNodesWithText("Create Account").assertCountEquals(2)[1].assertIsNotEnabled().performClick()

        assertEquals(0, clicks)
    }

    private fun setContent(state: CreateEmailPageState) {
        composeTestRule.setContent {
            AppTheme(Theme.ThemeType.LIGHT) {
                CreateEmailPage(
                    state = state,
                    onEmailChange = { emails += it },
                    onPasswordChange = {},
                    onFocusChange = {},
                    onNextClick = { clicks++ },
                )
            }
        }
    }
}

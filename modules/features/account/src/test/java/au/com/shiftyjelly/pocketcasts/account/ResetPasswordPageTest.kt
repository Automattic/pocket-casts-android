package au.com.shiftyjelly.pocketcasts.account

import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.v2.createComposeRule
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
class ResetPasswordPageTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private var clicks = 0
    private val emails = mutableListOf<String>()

    @Test
    fun `valid email confirms and shows the error`() {
        setContent(ResetPasswordPageState(isCar = false, email = "test@example.com", isEmailValid = true, isProgressVisible = false, errorText = "Error sending email."))

        composeTestRule.onNodeWithText("Error sending email.").assertExists()
        composeTestRule.onNodeWithText("Confirm").assertIsEnabled().performClick()
        composeTestRule.onNodeWithText("test@example.com").performTextInput("a")

        assertEquals(1, clicks)
        assertEquals(listOf("atest@example.com"), emails)
    }

    @Test
    fun `invalid email disables confirm on car`() {
        setContent(ResetPasswordPageState(isCar = true, email = "test", isEmailValid = false, isProgressVisible = true, errorText = ""))

        composeTestRule.onNodeWithText("Confirm").assertIsNotEnabled().performClick()

        assertEquals(0, clicks)
    }

    private fun setContent(state: ResetPasswordPageState) {
        composeTestRule.setContent {
            AppTheme(Theme.ThemeType.LIGHT) {
                ResetPasswordPage(state = state, onEmailChange = { emails += it }, onConfirmClick = { clicks++ })
            }
        }
    }
}

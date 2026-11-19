package au.com.shiftyjelly.pocketcasts.account

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
class PromoCodePageTest {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val clicks = mutableListOf<String>()

    @Test
    fun `failed code offers account creation`() {
        setContent(state(title = "Promotion Expired or Invalid", doneText = "Done", createText = "Create a Pocket Casts Account", isSignInVisible = false))

        composeTestRule.onNodeWithText("Promotion Expired or Invalid").assertExists()
        composeTestRule.onNodeWithText("Sign in").assertDoesNotExist()
        composeTestRule.onNodeWithText("Create a Pocket Casts Account").performClick()
        composeTestRule.onNodeWithText("Done").performClick()

        assertEquals(listOf("create", "done"), clicks)
    }

    @Test
    fun `signed out code offers sign in`() {
        setContent(state(title = "Sign in or create an account", doneText = "Create account", createText = null, isSignInVisible = true))

        composeTestRule.onNodeWithText("Sign in").performClick()
        composeTestRule.onNodeWithText("Create account").performClick()

        assertEquals(listOf("signIn", "done"), clicks)
    }

    private fun state(title: String, doneText: String, createText: String?, isSignInVisible: Boolean) = PromoCodePageState(
        isProgressVisible = false,
        isLoaded = true,
        imageRes = R.drawable.ic_plus_account,
        overlayRes = null,
        title = title,
        detail = "Detail",
        doneText = doneText,
        createText = createText,
        isSignInVisible = isSignInVisible,
    )

    private fun setContent(state: PromoCodePageState) {
        composeTestRule.setContent {
            AppTheme(Theme.ThemeType.LIGHT) {
                PromoCodePage(
                    state = state,
                    onCloseClick = { clicks += "close" },
                    onDoneClick = { clicks += "done" },
                    onCreateClick = { clicks += "create" },
                    onSignInClick = { clicks += "signIn" },
                )
            }
        }
    }
}

package au.com.shiftyjelly.pocketcasts.account.onboarding.pageobject

import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput

class EmailLogInPage(
    private val rule: ComposeTestRule
) {
    fun enterEmail(email: String): EmailLogInPage {
        rule.onNode(hasText("Email Address")).performTextInput(email)
        return this
    }

    fun enterPassword(password: String): EmailLogInPage {
        rule.onNode(hasText("Password")).performTextInput(password)
        return this
    }

    fun tapCreateAccount(): EmailLogInPage {
        rule.onNode(hasText("Continue with email") and hasClickAction()).performClick()
        return this
    }
}

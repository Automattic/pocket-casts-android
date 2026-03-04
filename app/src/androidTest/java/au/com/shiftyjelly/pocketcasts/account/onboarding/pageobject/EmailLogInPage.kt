package au.com.shiftyjelly.pocketcasts.account.onboarding.pageobject

import android.content.Context
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import au.com.shiftyjelly.pocketcasts.account.onboarding.testutil.OnboardingTestConstants
import au.com.shiftyjelly.pocketcasts.localization.R as LR

class EmailLogInPage(
    private val rule: ComposeTestRule,
    context: Context
) {
    private val emailAddressText = context.getString(LR.string.profile_email_address)
    private val passwordText = context.getString(LR.string.profile_password)
    private val continueWithEmailText = context.getString(LR.string.onboarding_login_continue_with_email)

    @OptIn(ExperimentalTestApi::class)
    fun waitForVisible(): EmailLogInPage {
        rule.waitUntilAtLeastOneExists(hasText(emailAddressText), OnboardingTestConstants.UI_TIMEOUT_MS)
        rule.waitUntilAtLeastOneExists(hasText(passwordText), OnboardingTestConstants.UI_TIMEOUT_MS)
        rule.waitUntilAtLeastOneExists(hasText(continueWithEmailText), OnboardingTestConstants.UI_TIMEOUT_MS)
        return this
    }

    fun enterEmail(email: String): EmailLogInPage {
        rule.onNode(hasText(emailAddressText)).performTextInput(email)
        return this
    }

    fun enterPassword(password: String): EmailLogInPage {
        rule.onNode(hasText(passwordText)).performTextInput(password)
        return this
    }

    fun tapCreateAccount(): EmailLogInPage {
        rule.onNode(hasText(continueWithEmailText) and hasClickAction()).performClick()
        return this
    }
}

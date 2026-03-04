package au.com.shiftyjelly.pocketcasts.account.onboarding.pageobject

import android.content.Context
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import androidx.compose.ui.test.junit4.ComposeTestRule
import au.com.shiftyjelly.pocketcasts.account.onboarding.testutil.OnboardingTestConstants

class OnboardingWelcomePage(
    private val rule: ComposeTestRule,
    context: Context
) {
    private val getStartedText = context.getString(LR.string.onboarding_intro_get_started)
    private val getLogInText = context.getString(LR.string.onboarding_log_in)


    @OptIn(ExperimentalTestApi::class)
    fun waitForVisible(): OnboardingWelcomePage {
        rule.waitUntilAtLeastOneExists(hasText(getStartedText), OnboardingTestConstants.UI_TIMEOUT_MS)
        rule.onNodeWithText(getStartedText).assertIsDisplayed()
        return this
    }

    fun tapGetStarted(): OnboardingWelcomePage {
        rule.onNodeWithText(getStartedText).performClick()
        return this
    }

    @OptIn(ExperimentalTestApi::class)
    fun waitForVisibleLogIn(): OnboardingWelcomePage {
        rule.waitUntilAtLeastOneExists(hasText(getLogInText), OnboardingTestConstants.UI_TIMEOUT_MS)
        rule.onNodeWithText(getLogInText).assertIsDisplayed()
        return this
    }

    fun tapLogIn(): OnboardingWelcomePage {
        rule.onNodeWithText(getLogInText).performClick()
        return this
    }
}

class InterestsSelectionPage(
    private val rule: ComposeTestRule,
    context: Context
) {
    private val titleText = context.getString(LR.string.onboarding_interests_title)
    private val instructionText = context.getString(LR.string.onboarding_interests_select_at_least_label)
    private val continueText = context.getString(LR.string.navigation_continue)

    @OptIn(ExperimentalTestApi::class)
    fun waitForVisible(): InterestsSelectionPage {
        rule.waitUntilAtLeastOneExists(hasText(titleText), OnboardingTestConstants.UI_TIMEOUT_MS)
        rule.onNodeWithText(titleText).assertIsDisplayed()
        return this
    }

    fun assertInstructionVisible(): InterestsSelectionPage {
        rule.onNodeWithText(instructionText).assertIsDisplayed()
        return this
    }

    fun selectInterests(vararg interests: String): InterestsSelectionPage {
        interests.forEach { rule.onNodeWithText(it).assertIsDisplayed().performClick() }
        return this
    }

    fun tapContinue(): InterestsSelectionPage {
        rule.onNodeWithText(continueText).assertIsDisplayed().performClick()
        return this
    }
}

class SuggestionsPage(
    private val rule: ComposeTestRule,
    context: Context
) {
    private val titleText = context.getString(LR.string.onboarding_recommendations_title)
    private val continueText = context.getString(LR.string.navigation_continue)

    @OptIn(ExperimentalTestApi::class)
    fun waitForVisible(): SuggestionsPage {
        rule.waitUntilAtLeastOneExists(hasText(titleText), OnboardingTestConstants.UI_TIMEOUT_MS)
        rule.onNodeWithText(titleText).assertIsDisplayed()
        return this
    }

    fun tapContinue(): SuggestionsPage {
        rule.onNodeWithText(continueText).assertIsDisplayed().performClick()
        return this
    }
}

class SignUpOptionsPage(
    private val rule: ComposeTestRule,
    context: Context
) {
    private val signUpWithEmailText = context.getString(LR.string.onboarding_create_account_sign_up_email)

    @OptIn(ExperimentalTestApi::class)
    fun waitForVisible(): SignUpOptionsPage {
        rule.waitUntilAtLeastOneExists(hasText(signUpWithEmailText), OnboardingTestConstants.UI_TIMEOUT_MS)
        rule.onNodeWithText(signUpWithEmailText).assertIsDisplayed()
        return this
    }

    fun tapSignUpWithEmail(): SignUpOptionsPage {
        rule.onNodeWithText(signUpWithEmailText).performClick()
        return this
    }
}

class EmailSignUpPage(
    private val rule: ComposeTestRule,
    context: Context
) {
    private val emailAddressText = context.getString(LR.string.profile_email_address)
    private val passwordText = context.getString(LR.string.profile_password)
    private val createAccountText = context.getString(LR.string.create_account)
    private val passwordTooShortText = "• ${context.getString(LR.string.profile_create_password_requirements)}"

    @OptIn(ExperimentalTestApi::class)
    fun waitForVisible(): EmailSignUpPage {
        rule.waitUntilAtLeastOneExists(hasText(emailAddressText), OnboardingTestConstants.UI_TIMEOUT_MS)
        rule.waitUntilAtLeastOneExists(hasText(passwordText), OnboardingTestConstants.UI_TIMEOUT_MS)
        rule.waitUntilAtLeastOneExists(hasText(createAccountText), OnboardingTestConstants.UI_TIMEOUT_MS)
        return this
    }

    fun enterEmail(email: String): EmailSignUpPage {
        rule.onNode(hasText(emailAddressText)).performTextInput(email)
        return this
    }

    fun enterPassword(password: String): EmailSignUpPage {
        rule.onNode(hasText(passwordText)).performTextInput(password)
        return this
    }

    fun tapCreateAccount(): EmailSignUpPage {
        rule.onNode(hasText(createAccountText) and hasClickAction()).performClick()
        return this
    }

    fun assertPasswordTooShortVisible(): EmailSignUpPage {
        rule.onNodeWithText(passwordTooShortText).assertIsDisplayed()
        return this
    }
}

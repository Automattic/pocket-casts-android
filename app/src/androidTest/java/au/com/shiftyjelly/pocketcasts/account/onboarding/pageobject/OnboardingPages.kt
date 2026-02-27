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
    private val context: Context
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
    private val context: Context
) {
    private val titleText = context.getString(LR.string.onboarding_interests_title)

    @OptIn(ExperimentalTestApi::class)
    fun waitForVisible(): InterestsSelectionPage {
        rule.waitUntilAtLeastOneExists(hasText(titleText), OnboardingTestConstants.UI_TIMEOUT_MS)
        rule.onNodeWithText(titleText).assertIsDisplayed()
        return this
    }

    fun assertInstructionVisible(): InterestsSelectionPage {
        rule.onNodeWithText("Select at least 3").assertIsDisplayed()
        return this
    }

    fun selectInterests(vararg interests: String): InterestsSelectionPage {
        interests.forEach { rule.onNodeWithText(it).assertIsDisplayed().performClick() }
        return this
    }

    fun tapContinue(): InterestsSelectionPage {
        rule.onNodeWithText("Continue").assertIsDisplayed().performClick()
        return this
    }
}

class SuggestionsPage(
    private val rule: ComposeTestRule
) {
    fun waitForVisible(): SuggestionsPage {
        rule.onNodeWithText("We hope you love these suggestions!").assertIsDisplayed()
        return this
    }

    fun tapContinue(): SuggestionsPage {
        rule.onNodeWithText("Continue").assertIsDisplayed().performClick()
        return this
    }
}

class SignUpOptionsPage(
    private val rule: ComposeTestRule
) {
    fun waitForVisible(): SignUpOptionsPage {
        rule.onNodeWithText("Sign up with email").assertIsDisplayed()
        return this
    }

    fun tapSignUpWithEmail(): SignUpOptionsPage {
        rule.onNodeWithText("Sign up with email").performClick()
        return this
    }
}

class EmailSignUpPage(
    private val rule: ComposeTestRule
) {
    fun enterEmail(email: String): EmailSignUpPage {
        rule.onNode(hasText("Email Address")).performTextInput(email)
        return this
    }

    fun enterPassword(password: String): EmailSignUpPage {
        rule.onNode(hasText("Password")).performTextInput(password)
        return this
    }

    fun tapCreateAccount(): EmailSignUpPage {
        rule.onNode(hasText("Create account") and hasClickAction()).performClick()
        return this
    }

    fun assertPasswordTooShortVisible(): EmailSignUpPage {
        rule.onNodeWithText("• Password must be at least 6 characters").assertIsDisplayed()
        return this
    }
}




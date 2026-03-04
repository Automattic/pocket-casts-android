package au.com.shiftyjelly.pocketcasts.account.onboarding.pageobject

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.performClick
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import androidx.compose.ui.test.junit4.ComposeTestRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.ExperimentalTestApi
import au.com.shiftyjelly.pocketcasts.account.onboarding.testutil.OnboardingTestConstants

class SubscriptionBoardinghouse(
    private val rule: ComposeTestRule,
    context: Context,
) {
    private val closeText = context.getString(LR.string.close)
    private val subtitleText = context.getString(LR.string.onboarding_upgrade_generic_title)

    fun tapClose(): SubscriptionBoardinghouse {
        rule.onNodeWithContentDescription(closeText).assertIsDisplayed().performClick()
        return this
    }

    @OptIn(ExperimentalTestApi::class)
    fun waitForVisibleSubText(): SubscriptionBoardinghouse {
        rule.waitUntilAtLeastOneExists(hasText(subtitleText), OnboardingTestConstants.UI_TIMEOUT_MS)
        rule.onNodeWithText(subtitleText).assertIsDisplayed()
        return this
    }
}

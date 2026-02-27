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
    private val context: Context,
) {
    fun tapClose(): SubscriptionBoardinghouse {
        val closeText = context.getString(LR.string.close)
        rule.onNodeWithContentDescription(closeText).assertIsDisplayed().performClick()
        return this
    }

    @OptIn(ExperimentalTestApi::class)
    fun waitForVisibleSubText(): SubscriptionBoardinghouse {
        rule.waitUntilAtLeastOneExists(hasText("Superpowers for your podcasts"), OnboardingTestConstants.UI_TIMEOUT_MS)
        rule.onNodeWithText("Superpowers for your podcasts").assertIsDisplayed()
        return this
    }
}

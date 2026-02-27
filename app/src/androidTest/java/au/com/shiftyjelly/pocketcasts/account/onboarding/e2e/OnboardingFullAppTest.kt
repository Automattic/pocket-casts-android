package au.com.shiftyjelly.pocketcasts.account.onboarding.e2e

import android.content.Context
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import au.com.shiftyjelly.pocketcasts.account.onboarding.testutil.OnboardingTestConstants
import au.com.shiftyjelly.pocketcasts.account.onboarding.testutil.OnboardingTestCredentials
import au.com.shiftyjelly.pocketcasts.account.onboarding.pageobject.EmailSignUpPage
import au.com.shiftyjelly.pocketcasts.account.onboarding.pageobject.InterestsSelectionPage
import au.com.shiftyjelly.pocketcasts.account.onboarding.pageobject.OnboardingWelcomePage
import au.com.shiftyjelly.pocketcasts.account.onboarding.pageobject.SignUpOptionsPage
import au.com.shiftyjelly.pocketcasts.account.onboarding.pageobject.SuggestionsPage
import au.com.shiftyjelly.pocketcasts.sharedtest.RadiographyDumpRule
import au.com.shiftyjelly.pocketcasts.ui.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith
import au.com.shiftyjelly.pocketcasts.localization.R as LR


@LargeTest
@RunWith(AndroidJUnit4::class)
class OnboardingFullAppTest {

    private val radiographyRule = RadiographyDumpRule()

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(activityRule)
        .around(radiographyRule)

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun onboarding_showsInterestsScreen() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val crime = context.getString(LR.string.discover_category_crime)
        val sports = context.getString(LR.string.discover_category_sports)
        val education = context.getString(LR.string.discover_category_education)

        OnboardingWelcomePage(composeTestRule, context)
            .waitForVisible()
            .tapGetStarted()

        InterestsSelectionPage(composeTestRule, context)
            .assertInstructionVisible()
            .waitForVisible()
            .selectInterests(crime, sports, education)
            .tapContinue()

        SuggestionsPage(composeTestRule)
            .waitForVisible()
            .tapContinue()

        SignUpOptionsPage(composeTestRule)
            .waitForVisible()
            .tapSignUpWithEmail()

        EmailSignUpPage(composeTestRule)
            .enterEmail(OnboardingTestCredentials.email)
            .enterPassword(OnboardingTestConstants.INVALID_PASSWORD)
            .tapCreateAccount()
            .assertPasswordTooShortVisible()
    }
}

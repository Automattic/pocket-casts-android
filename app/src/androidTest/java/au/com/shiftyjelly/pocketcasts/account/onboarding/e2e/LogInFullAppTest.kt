package au.com.shiftyjelly.pocketcasts.account.onboarding.e2e

import android.content.Context
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.rules.ActivityScenarioRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import au.com.shiftyjelly.pocketcasts.account.onboarding.testutil.OnboardingTestCredentials
import au.com.shiftyjelly.pocketcasts.account.onboarding.testutil.OnboardingStateResetRule
import au.com.shiftyjelly.pocketcasts.account.onboarding.pageobject.AccountPage
import au.com.shiftyjelly.pocketcasts.account.onboarding.pageobject.BottomNavigationPage
import au.com.shiftyjelly.pocketcasts.account.onboarding.pageobject.EmailLogInPage
import au.com.shiftyjelly.pocketcasts.account.onboarding.pageobject.OnboardingWelcomePage
import au.com.shiftyjelly.pocketcasts.account.onboarding.pageobject.SubscriptionBoardinghouse
import au.com.shiftyjelly.pocketcasts.sharedtest.RadiographyDumpRule
import au.com.shiftyjelly.pocketcasts.ui.MainActivity
import org.junit.Rule
import org.junit.Test
import org.junit.rules.RuleChain
import org.junit.runner.RunWith


@LargeTest
@RunWith(AndroidJUnit4::class)
class LogInFullAppTest {

    private val radiographyRule = RadiographyDumpRule()
    private val onboardingStateResetRule = OnboardingStateResetRule()

    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    private val activityRule = ActivityScenarioRule(MainActivity::class.java)

    @get:Rule
    val ruleChain: RuleChain = RuleChain.outerRule(onboardingStateResetRule)
        .around(activityRule)
        .around(radiographyRule)

    @OptIn(ExperimentalTestApi::class)
    @Test
    fun loginFlow_useEmailAndPassword() {
        val context = ApplicationProvider.getApplicationContext<Context>()

        OnboardingWelcomePage(composeTestRule, context)
            .waitForVisibleLogIn()
            .tapLogIn()

        EmailLogInPage(composeTestRule)
            .enterEmail(OnboardingTestCredentials.email)
            .enterPassword(OnboardingTestCredentials.password)
            .tapCreateAccount()

        SubscriptionBoardinghouse(composeTestRule, context)
            .waitForVisibleSubText()
            .tapClose()

        BottomNavigationPage()
            .assertAccountVisible()
            .tapAccount()

        AccountPage(composeTestRule)
            .checkAccountBottomIsVisible()
            .checkEmailIsVisible(OnboardingTestCredentials.email)

    }
}

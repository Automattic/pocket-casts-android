package au.com.shiftyjelly.pocketcasts

import androidx.activity.compose.setContent
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import au.com.shiftyjelly.pocketcasts.account.onboarding.OnboardingActivity
import au.com.shiftyjelly.pocketcasts.account.onboarding.OnboardingFlowComposable
import au.com.shiftyjelly.pocketcasts.account.onboarding.OnboardingNavRoute
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import junit.framework.TestCase.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock

/**
 * This test class is in the app component so Hilt can access the application for injection.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class OnboardingFlowComposableTest {

    @get:Rule val composeTestRule = createAndroidComposeRule<OnboardingActivity>()
    lateinit var navController: TestNavHostController

    fun setupAppNavHost(
        flow: OnboardingFlow,
        signInState: SignInState = mock(),
        exitOnboarding: () -> Unit = {},
        completeOnboardingToDiscover: () -> Unit = {},
    ) {
        composeTestRule.activity.setContent {
            navController = TestNavHostController(LocalContext.current).apply {
                navigatorProvider.addNavigator(ComposeNavigator())
            }
            OnboardingFlowComposable(
                theme = Theme.ThemeType.LIGHT,
                flow = flow,
                exitOnboarding = exitOnboarding,
                completeOnboardingToDiscover = completeOnboardingToDiscover,
                signInState = signInState,
                navController = navController,
            )
        }

        // Make sure lateinit navController field is initialized before proceeding
        composeTestRule.waitForIdle()
    }

    @Test
    fun startDestination_LoggedOut() {
        assertStartDestinationForFlow(
            startDestination = OnboardingNavRoute.logInOrSignUp,
            flow = OnboardingFlow.LoggedOut,
        )
    }

    @Test
    fun startDestination_PlusAccountUpgradeNeedsLogin() {
        assertStartDestinationForFlow(
            startDestination = OnboardingNavRoute.logInOrSignUp,
            flow = OnboardingFlow.PlusAccountUpgradeNeedsLogin,
        )
    }

    @Test
    fun startDestination_InitialOnboarding() {
        assertStartDestinationForFlow(
            startDestination = OnboardingNavRoute.logInOrSignUp,
            flow = OnboardingFlow.InitialOnboarding,
        )
    }

    @Test
    fun startDestination_PlusAccountUpgrade() {
        assertStartDestinationForFlow(
            startDestination = OnboardingNavRoute.PlusUpgrade.route,
            flow = OnboardingFlow.PlusAccountUpgrade(OnboardingUpgradeSource.ACCOUNT_DETAILS),
        )
    }

    @Test
    fun startDestination_PlusFlow_PlusAccountUpgrade() {
        assertStartDestinationForFlow(
            startDestination = OnboardingNavRoute.PlusUpgrade.route,
            flow = OnboardingFlow.PlusAccountUpgrade(OnboardingUpgradeSource.ACCOUNT_DETAILS),
        )
    }

    @Test
    fun startDestination_PlusFlow_PlusUpsell() {
        assertStartDestinationForFlow(
            startDestination = OnboardingNavRoute.PlusUpgrade.route,
            flow = OnboardingFlow.PlusUpsell(OnboardingUpgradeSource.ACCOUNT_DETAILS),
        )
    }

    @Test
    fun startDestination_PlusFlow_PatronAccountUpgrade() {
        assertStartDestinationForFlow(
            startDestination = OnboardingNavRoute.PlusUpgrade.route,
            flow = OnboardingFlow.PatronAccountUpgrade(OnboardingUpgradeSource.ACCOUNT_DETAILS),
        )
    }

    private fun assertStartDestinationForFlow(
        startDestination: String,
        flow: OnboardingFlow,
    ) {
        setupAppNavHost(flow)
        val route = navController.currentBackStackEntry?.destination?.route
        assertEquals(startDestination, route)
    }
}

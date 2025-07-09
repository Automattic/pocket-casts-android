package au.com.shiftyjelly.pocketcasts

import android.content.Context
import androidx.activity.compose.setContent
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import au.com.shiftyjelly.pocketcasts.account.onboarding.OnboardingActivity
import au.com.shiftyjelly.pocketcasts.account.onboarding.OnboardingFlowComposable
import au.com.shiftyjelly.pocketcasts.account.onboarding.OnboardingNavRoute
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingUpgradeFeaturesViewModel
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.payment.BillingCycle
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingExitInfo
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import junit.framework.TestCase.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.mock
import au.com.shiftyjelly.pocketcasts.ui.R as UR

/**
 * This test class is in the app component so Hilt can access the application for injection.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class OnboardingFlowComposableTest {

    @get:Rule val composeTestRule = createEmptyComposeRule()
    lateinit var navController: TestNavHostController

    fun setupAppNavHost(
        flow: OnboardingFlow,
        signInState: SignInState = mock(),
        exitOnboarding: (OnboardingExitInfo) -> Unit = {},
        completeOnboardingToDiscover: () -> Unit = {},
    ) {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val onboardingIntent = OnboardingActivity.newInstance(context, flow)

        ActivityScenario.launch<OnboardingActivity>(onboardingIntent).use { rule ->
            rule.onActivity { activity ->
                activity.setContent {
                    CompositionLocalProvider(
                        LocalContext provides ContextThemeWrapper(LocalContext.current, UR.style.ThemeDark),
                    ) {
                        navController = TestNavHostController(LocalContext.current).apply {
                            navigatorProvider.addNavigator(ComposeNavigator())
                        }
                        val viewModel = hiltViewModel<OnboardingUpgradeFeaturesViewModel, OnboardingUpgradeFeaturesViewModel.Factory>(
                            creationCallback = { factory -> factory.create(flow) },
                        )
                        OnboardingFlowComposable(
                            featuresViewModel = viewModel,
                            state = viewModel.state.collectAsState().value,
                            theme = Theme.ThemeType.LIGHT,
                            flow = flow,
                            exitOnboarding = exitOnboarding,
                            completeOnboardingToDiscover = completeOnboardingToDiscover,
                            signInState = signInState,
                            navController = navController,
                            onUpdateSystemBars = {},
                        )
                    }
                }
            }

            // Make sure lateinit navController field is initialized before proceeding
            composeTestRule.waitForIdle()
        }
    }

    @Test
    fun startDestination_LoggedOut() {
        assertStartDestinationForFlow(
            startDestination = OnboardingNavRoute.LOG_IN_OR_SIGN_UP,
            flow = OnboardingFlow.LoggedOut,
        )
    }

    @Test
    fun startDestination_PlusAccountUpgradeNeedsLogin() {
        assertStartDestinationForFlow(
            startDestination = OnboardingNavRoute.LOG_IN_OR_SIGN_UP,
            flow = OnboardingFlow.PlusAccountUpgradeNeedsLogin,
        )
    }

    @Test
    fun startDestination_InitialOnboarding() {
        assertStartDestinationForFlow(
            startDestination = OnboardingNavRoute.LOG_IN_OR_SIGN_UP,
            flow = OnboardingFlow.InitialOnboarding,
        )
    }

    @Test
    fun startDestination_PlusAccountUpgrade() {
        assertStartDestinationForFlow(
            startDestination = OnboardingNavRoute.PlusUpgrade.ROUTE,
            flow = OnboardingFlow.PlusAccountUpgrade(OnboardingUpgradeSource.ACCOUNT_DETAILS, SubscriptionTier.Plus, BillingCycle.Yearly),
        )
    }

    @Test
    fun startDestination_PlusFlow_PlusAccountUpgrade() {
        assertStartDestinationForFlow(
            startDestination = OnboardingNavRoute.PlusUpgrade.ROUTE,
            flow = OnboardingFlow.PlusAccountUpgrade(OnboardingUpgradeSource.ACCOUNT_DETAILS, SubscriptionTier.Plus, BillingCycle.Yearly),
        )
    }

    @Test
    fun startDestination_PlusFlow_PlusUpsell() {
        assertStartDestinationForFlow(
            startDestination = OnboardingNavRoute.PlusUpgrade.ROUTE,
            flow = OnboardingFlow.Upsell(OnboardingUpgradeSource.ACCOUNT_DETAILS),
        )
    }

    @Test
    fun startDestination_PlusFlow_PatronAccountUpgrade() {
        assertStartDestinationForFlow(
            startDestination = OnboardingNavRoute.PlusUpgrade.ROUTE,
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

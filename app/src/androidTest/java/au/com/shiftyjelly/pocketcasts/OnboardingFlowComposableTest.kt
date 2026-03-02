package au.com.shiftyjelly.pocketcasts

import android.content.Context
import androidx.activity.compose.setContent
import androidx.appcompat.view.ContextThemeWrapper
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.test.junit4.createEmptyComposeRule
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.ComposeNavigator
import androidx.navigation.testing.TestNavHostController
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import au.com.shiftyjelly.pocketcasts.account.onboarding.OnboardingActivity
import au.com.shiftyjelly.pocketcasts.account.onboarding.OnboardingFlowComposable
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingFlowRoutes
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
import au.com.shiftyjelly.pocketcasts.ui.R as UR

/**
 * This test class is in the app component so Hilt can access the application for injection.
 */
@LargeTest
@RunWith(AndroidJUnit4::class)
class OnboardingFlowComposableTest {
    @get:Rule
    val composeTestRule = createEmptyComposeRule()

    lateinit var navController: TestNavHostController

    fun setupAppNavHost(
        flow: OnboardingFlow,
        signInState: SignInState = SignInState.SignedIn(email = "", subscription = null),
        exitOnboarding: (OnboardingExitInfo) -> Unit = {},
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
    fun startDestination_newOnboarding_LoggedOut() {
        assertStartDestinationForFlow(
            startDestination = OnboardingFlowRoutes.ROUTE_SIGN_UP,
            flow = OnboardingFlow.LoggedOut,
        )
    }

    @Test
    fun startDestination_newOnboarding_PlusAccountUpgradeNeedsLogin() {
        assertStartDestinationForFlow(
            startDestination = OnboardingFlowRoutes.ROUTE_INTRO_CAROUSEL,
            flow = OnboardingFlow.PlusAccountUpgradeNeedsLogin,
        )
    }

    @Test
    fun startDestination_newOnboarding_InitialOnboarding() {
        assertStartDestinationForFlow(
            startDestination = OnboardingFlowRoutes.ROUTE_INTRO_CAROUSEL,
            flow = OnboardingFlow.InitialOnboarding,
        )
    }

    @Test
    fun startDestination_newOnboarding_PlusAccountUpgrade() {
        assertStartDestinationForFlow(
            startDestination = OnboardingFlowRoutes.PlusUpgrade.ROUTE,
            flow = OnboardingFlow.PlusAccountUpgrade(OnboardingUpgradeSource.ACCOUNT_DETAILS, SubscriptionTier.Plus, BillingCycle.Yearly),
        )
    }

    @Test
    fun startDestination_newOnboarding_PlusFlow_PlusAccountUpgrade() {
        assertStartDestinationForFlow(
            startDestination = OnboardingFlowRoutes.PlusUpgrade.ROUTE,
            flow = OnboardingFlow.PlusAccountUpgrade(OnboardingUpgradeSource.ACCOUNT_DETAILS, SubscriptionTier.Plus, BillingCycle.Yearly),
        )
    }

    @Test
    fun startDestination_newOnboarding_PlusFlow_PlusUpsell() {
        assertStartDestinationForFlow(
            startDestination = OnboardingFlowRoutes.PlusUpgrade.ROUTE,
            flow = OnboardingFlow.Upsell(OnboardingUpgradeSource.ACCOUNT_DETAILS),
        )
    }

    @Test
    fun startDestination_newOnboarding_PlusFlow_PatronAccountUpgrade() {
        assertStartDestinationForFlow(
            startDestination = OnboardingFlowRoutes.PlusUpgrade.ROUTE,
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

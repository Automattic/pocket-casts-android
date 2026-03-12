package au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.NavType
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import au.com.shiftyjelly.pocketcasts.account.onboarding.AccountBenefitsPage
import au.com.shiftyjelly.pocketcasts.account.onboarding.OnboardingCreateAccountPage
import au.com.shiftyjelly.pocketcasts.account.onboarding.OnboardingForgotPasswordPage
import au.com.shiftyjelly.pocketcasts.account.onboarding.OnboardingGetStartedPage
import au.com.shiftyjelly.pocketcasts.account.onboarding.OnboardingLoginOrSignUpPage
import au.com.shiftyjelly.pocketcasts.account.onboarding.OnboardingLoginPage
import au.com.shiftyjelly.pocketcasts.account.onboarding.recommendations.OnboardingRecommendationsFlow
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingAccountBenefitsViewModel
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingCreateAccountViewModel
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingLogInViewModel
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingLoginOrSignUpViewModel
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingUpgradeFeaturesState
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingUpgradeFeaturesViewModel
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.bars.SystemBarsStyles
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingExitInfo
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.requireBoolean
import au.com.shiftyjelly.pocketcasts.utils.extensions.requireSerializable
import au.com.shiftyjelly.pocketcasts.localization.R as LR

object OnboardingFlowRoutes {
    const val CREATE_FREE_ACCOUNT = "create_free_account"
    const val ENCOURAGE_FREE_ACCOUNT = "encourage_free_account"
    const val FORGOT_PASSWORD = "forgot_password"
    const val LOG_IN = "log_in"
    const val LOG_IN_OR_SIGN_UP = "log_in_or_sign_up"
    const val ROUTE_INTRO_CAROUSEL = "new_intro_carousel"
    const val ROUTE_LOG_IN = "new_log_in"
    const val ROUTE_SIGN_UP = "new_sign_up"

    object PlusUpgrade {
        private const val ROUTE_BASE = "plus_upgrade"

        const val SOURCE_ARGUMENT_KEY = "source"
        const val FORCE_PURCHASE_ARGUMENT_KEY = "force_purchase"

        // The route variable should only be used to navigate to the PlusUpgrade screens
        // when they are the startDestination and the args for these startDestinations are set using default values.
        // They are parsed based on this deep-link-like route by the navigation component.
        // For more details check here: https://developer.android.com/jetpack/compose/navigation#nav-with-args
        // In all other cases, use the routeWithSource function.
        const val ROUTE = "$ROUTE_BASE/{$SOURCE_ARGUMENT_KEY}?$FORCE_PURCHASE_ARGUMENT_KEY={$FORCE_PURCHASE_ARGUMENT_KEY}"

        fun routeWithSource(
            source: OnboardingUpgradeSource,
            forcePurchase: Boolean = false,
        ) = "$ROUTE_BASE/$source?$FORCE_PURCHASE_ARGUMENT_KEY=$forcePurchase"
    }

    fun startDestination(flow: OnboardingFlow) = when (flow) {
        is OnboardingFlow.Welcome,
        is OnboardingFlow.PlusAccountUpgradeNeedsLogin,
        is OnboardingFlow.InitialOnboarding,
        is OnboardingFlow.EngageSdk,
        is OnboardingFlow.ReferralLoginOrSignUp,
        -> ROUTE_INTRO_CAROUSEL

        is OnboardingFlow.AccountEncouragement -> ENCOURAGE_FREE_ACCOUNT

        is OnboardingFlow.LoggedOut,
        -> ROUTE_SIGN_UP

        // Cannot use OnboardingNavRoute.PlusUpgrade.routeWithSource here, it is set as a defaultValue in the PlusUpgrade composable,
        // see https://stackoverflow.com/a/70410872/1910286
        is OnboardingFlow.PlusAccountUpgrade,
        is OnboardingFlow.PatronAccountUpgrade,
        is OnboardingFlow.Upsell,
        is OnboardingFlow.UpsellSuggestedFolder,
        is OnboardingFlow.AccountUpgrade,
        -> PlusUpgrade.ROUTE
    }

    fun NavGraphBuilder.flowGraph(
        theme: Theme.ThemeType,
        flow: OnboardingFlow,
        navController: NavController,
        featuresViewModel: OnboardingUpgradeFeaturesViewModel,
        state: OnboardingUpgradeFeaturesState,
        signInState: SignInState,
        onUpdateSystemBars: (SystemBarsStyles) -> Unit,
        finishOnboardingFlow: () -> Unit,
        exitOnboarding: (OnboardingExitInfo) -> Unit,
        onAccountCreated: () -> Unit,
        onLoginToExistingAccount: (OnboardingFlow, Subscription?, (OnboardingExitInfo) -> Unit) -> Unit,
    ) {
        composable(LOG_IN_OR_SIGN_UP) {
            OnboardingLoginOrSignUpPage(
                theme = theme,
                flow = flow,
                onDismiss = {
                    when (flow) {
                        // This should never happen. If the user isn't logged in they should be in the AccountUpgradeNeedsLogin flow
                        is OnboardingFlow.PlusAccountUpgrade,
                        is OnboardingFlow.PatronAccountUpgrade,
                        is OnboardingFlow.Welcome,
                        -> error("Account upgrade flow tried to present LoginOrSignupPage")

                        is OnboardingFlow.AccountEncouragement,
                        is OnboardingFlow.PlusAccountUpgradeNeedsLogin,
                        is OnboardingFlow.Upsell,
                        is OnboardingFlow.UpsellSuggestedFolder,
                        is OnboardingFlow.AccountUpgrade,
                        -> {
                            val popped = navController.popBackStack()
                            if (!popped) {
                                exitOnboarding(OnboardingExitInfo.Simple)
                            }
                        }

                        is OnboardingFlow.InitialOnboarding,
                        is OnboardingFlow.LoggedOut,
                        is OnboardingFlow.EngageSdk,
                        is OnboardingFlow.ReferralLoginOrSignUp,
                        -> exitOnboarding(OnboardingExitInfo.Simple)
                    }
                },
                onSignUpClick = { navController.navigate(CREATE_FREE_ACCOUNT) },
                onLoginClick = { navController.navigate(LOG_IN) },
                onContinueWithGoogleComplete = { state, subscription ->
                    if (state.isNewAccount) {
                        onAccountCreated()
                    } else {
                        onLoginToExistingAccount(flow, subscription, exitOnboarding)
                    }
                },
                onUpdateSystemBars = onUpdateSystemBars,
            )
        }

        composable(LOG_IN) {
            OnboardingLoginPage(
                theme = theme,
                onBackPress = { navController.popBackStack() },
                onLoginComplete = { subscription ->
                    onLoginToExistingAccount(flow, subscription, exitOnboarding)
                },
                onForgotPasswordClick = { navController.navigate(FORGOT_PASSWORD) },
                onUpdateSystemBars = onUpdateSystemBars,
            )
        }

        composable(ROUTE_INTRO_CAROUSEL) {
            val viewModel: OnboardingLoginOrSignUpViewModel = hiltViewModel()
            OnboardingGetStartedPage(
                viewModel = viewModel,
                displayTheme = theme,
                flow = flow,
                onGetStartedClick = {
                    viewModel.onGetStartedClicked(flow)
                    if (flow is OnboardingFlow.Upsell || flow is OnboardingFlow.LoggedOut) {
                        navController.navigate(ROUTE_SIGN_UP)
                    } else {
                        navController.navigate(OnboardingRecommendationsFlow.ROUTE)
                    }
                },
                onLoginClick = {
                    viewModel.onLoginClicked(flow)
                    navController.navigate(ROUTE_LOG_IN)
                },
                onUpdateSystemBars = onUpdateSystemBars,
            )
        }

        composable(ROUTE_SIGN_UP) {
            val viewModel: OnboardingCreateAccountViewModel = hiltViewModel()
            OnboardingCreateAccountPage(
                viewModel = viewModel,
                theme = theme,
                flow = flow,
                onBackPress = {
                    val hasPopped = navController.popBackStack()
                    if (!hasPopped) {
                        finishOnboardingFlow()
                    }
                },
                onSkip = finishOnboardingFlow,
                onCreateAccount = {
                    viewModel.onSignUpEmailPressed(flow)
                    navController.navigate(CREATE_FREE_ACCOUNT)
                },
                onUpdateSystemBars = onUpdateSystemBars,
                onContinueWithGoogleComplete = { state, subscription ->
                    if (state.isNewAccount) {
                        onAccountCreated()
                    } else {
                        onLoginToExistingAccount(flow, subscription, exitOnboarding)
                    }
                },
                onClickLogin = {
                    viewModel.onLogInPressed(flow)
                    navController.navigate(ROUTE_LOG_IN)
                },
            )
        }

        composable(CREATE_FREE_ACCOUNT) {
            OnboardingCreateAccountPage(
                theme = theme,
                flow = flow,
                onBackPress = { navController.popBackStack() },
                onCreateAccount = onAccountCreated,
                onUpdateSystemBars = onUpdateSystemBars,
            )
        }

        composable(ROUTE_LOG_IN) {
            val viewModel: OnboardingLogInViewModel = hiltViewModel()
            OnboardingLoginPage(
                viewModel = viewModel,
                theme = theme,
                flow = flow,
                onBackPress = {
                    navController.popBackStack()
                },
                onLoginComplete = { subscription ->
                    onLoginToExistingAccount(flow, subscription, exitOnboarding)
                },
                onForgotPasswordClick = { navController.navigate(FORGOT_PASSWORD) },
                onUpdateSystemBars = onUpdateSystemBars,
                onContinueWithGoogleComplete = { state, subscription ->
                    if (state.isNewAccount) {
                        onAccountCreated()
                    } else {
                        onLoginToExistingAccount(flow, subscription, exitOnboarding)
                    }
                },
            )
        }

        composable(FORGOT_PASSWORD) {
            OnboardingForgotPasswordPage(
                theme = theme,
                onBackPress = { navController.popBackStack() },
                onComplete = { exitOnboarding(OnboardingExitInfo.Simple) },
                onUpdateSystemBars = onUpdateSystemBars,
            )
        }

        composable(ENCOURAGE_FREE_ACCOUNT) {
            val viewModel = hiltViewModel<OnboardingAccountBenefitsViewModel>()

            CallOnce {
                viewModel.onScreenShown()
            }

            AppTheme(theme) {
                AccountBenefitsPage(
                    mainCtaColor = MaterialTheme.theme.colors.primaryInteractive01,
                    mainCtaLabel = stringResource(LR.string.onboarding_create_account),
                    onGetStartedClick = {
                        viewModel.onGetStartedClick()
                        navController.navigate(ROUTE_SIGN_UP)
                    },
                    onLogIn = {
                        viewModel.onLogInClick()
                        navController.navigate(ROUTE_LOG_IN)
                    },
                    onClose = {
                        viewModel.onDismissClick()
                        exitOnboarding(OnboardingExitInfo.Simple)
                    },
                    onShowBenefit = { benefit ->
                        viewModel.onBenefitShown(benefit.analyticsValue)
                    },
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }

        composable(
            route = PlusUpgrade.ROUTE,
            arguments = listOf(
                navArgument(PlusUpgrade.SOURCE_ARGUMENT_KEY) {
                    type = NavType.EnumType(OnboardingUpgradeSource::class.java)
                    /* Set default value for onboarding flows with startDestination. */
                    when (flow) {
                        is OnboardingFlow.PatronAccountUpgrade,
                        is OnboardingFlow.PlusAccountUpgrade,
                        is OnboardingFlow.Upsell,
                        is OnboardingFlow.UpsellSuggestedFolder,
                        is OnboardingFlow.AccountUpgrade,
                        -> {
                            defaultValue = flow.source
                        }

                        // Not a startDestination, default value should not be set.
                        is OnboardingFlow.AccountEncouragement,
                        is OnboardingFlow.EngageSdk,
                        is OnboardingFlow.InitialOnboarding,
                        is OnboardingFlow.LoggedOut,
                        is OnboardingFlow.PlusAccountUpgradeNeedsLogin,
                        is OnboardingFlow.ReferralLoginOrSignUp,
                        is OnboardingFlow.Welcome,
                        -> Unit
                    }
                },
                navArgument(PlusUpgrade.FORCE_PURCHASE_ARGUMENT_KEY) {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) { navBackStackEntry ->
            val arguments = requireNotNull(navBackStackEntry.arguments)
            val upgradeSource = arguments.requireSerializable<OnboardingUpgradeSource>(PlusUpgrade.SOURCE_ARGUMENT_KEY)
            val forcePurchase = arguments.requireBoolean(PlusUpgrade.FORCE_PURCHASE_ARGUMENT_KEY)

            val userCreatedNewAccount = when (upgradeSource) {
                OnboardingUpgradeSource.ACCOUNT_DETAILS,
                OnboardingUpgradeSource.APPEARANCE,
                OnboardingUpgradeSource.ICONS,
                OnboardingUpgradeSource.THEMES,
                OnboardingUpgradeSource.BANNER_AD,
                OnboardingUpgradeSource.BOOKMARKS,
                OnboardingUpgradeSource.BOOKMARKS_SHELF_ACTION,
                OnboardingUpgradeSource.END_OF_YEAR,
                OnboardingUpgradeSource.FILES,
                OnboardingUpgradeSource.FOLDERS,
                OnboardingUpgradeSource.SUGGESTED_FOLDERS,
                OnboardingUpgradeSource.FOLDERS_PODCAST_SCREEN,
                OnboardingUpgradeSource.HEADPHONE_CONTROLS_SETTINGS,
                OnboardingUpgradeSource.LOGIN,
                OnboardingUpgradeSource.LOGIN_PLUS_PROMOTION,
                OnboardingUpgradeSource.OVERFLOW_MENU,
                OnboardingUpgradeSource.PLUS_DETAILS,
                OnboardingUpgradeSource.PROFILE,
                OnboardingUpgradeSource.SKIP_CHAPTERS,
                OnboardingUpgradeSource.SETTINGS,
                OnboardingUpgradeSource.SLUMBER_STUDIOS,
                OnboardingUpgradeSource.UP_NEXT_SHUFFLE,
                OnboardingUpgradeSource.GENERATED_TRANSCRIPTS,
                OnboardingUpgradeSource.DEEP_LINK,
                OnboardingUpgradeSource.FINISHED_ONBOARDING,
                OnboardingUpgradeSource.UNKNOWN,
                -> false

                OnboardingUpgradeSource.RECOMMENDATIONS -> true
            }

            OnboardingUpgradeFlow(
                viewModel = @Suppress("ktlint:compose:vm-forwarding-check") featuresViewModel,
                state = state,
                flow = flow,
                source = upgradeSource,
                isLoggedIn = signInState.isSignedIn,
                forcePurchase = forcePurchase,
                onBackPress = {
                    if (userCreatedNewAccount) {
                        navController.popBackStack()
                    } else {
                        exitOnboarding(OnboardingExitInfo.Simple)
                    }
                },
                onNeedLogin = { navController.navigate(ROUTE_SIGN_UP) },
                onProceed = { finishOnboardingFlow() },
                onUpdateSystemBars = onUpdateSystemBars,
            )
        }
    }
}

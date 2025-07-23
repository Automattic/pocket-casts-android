package au.com.shiftyjelly.pocketcasts.account.onboarding

import androidx.annotation.VisibleForTesting
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import au.com.shiftyjelly.pocketcasts.account.onboarding.import.OnboardingImportFlow
import au.com.shiftyjelly.pocketcasts.account.onboarding.import.OnboardingImportFlow.importFlowGraph
import au.com.shiftyjelly.pocketcasts.account.onboarding.recommendations.OnboardingRecommendationsFlow
import au.com.shiftyjelly.pocketcasts.account.onboarding.recommendations.OnboardingRecommendationsFlow.onboardingRecommendationsFlowGraph
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingUpgradeFlow
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingAccountBenefitsViewModel
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingUpgradeFeaturesState
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingUpgradeFeaturesViewModel
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.bars.SystemBarsStyles
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingExitInfo
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.getSerializableCompat

@Composable
fun OnboardingFlowComposable(
    featuresViewModel: OnboardingUpgradeFeaturesViewModel,
    state: OnboardingUpgradeFeaturesState,
    theme: Theme.ThemeType,
    flow: OnboardingFlow,
    exitOnboarding: (OnboardingExitInfo) -> Unit,
    completeOnboardingToDiscover: () -> Unit,
    signInState: SignInState,
    onUpdateSystemBars: (SystemBarsStyles) -> Unit,
    navController: NavHostController = rememberNavController(),
) {
    if (flow is OnboardingFlow.PlusAccountUpgrade) {
        Content(
            featuresViewModel = @Suppress("ktlint:compose:vm-forwarding-check") featuresViewModel,
            state = state,
            theme = theme,
            flow = flow,
            exitOnboarding = exitOnboarding,
            completeOnboardingToDiscover = completeOnboardingToDiscover,
            signInState = signInState,
            navController = navController,
            onUpdateSystemBars = onUpdateSystemBars,
        )
    } else {
        AppThemeWithBackground(theme) {
            Content(
                featuresViewModel = @Suppress("ktlint:compose:vm-forwarding-check") featuresViewModel,
                state = state,
                theme = theme,
                flow = flow,
                exitOnboarding = exitOnboarding,
                completeOnboardingToDiscover = completeOnboardingToDiscover,
                signInState = signInState,
                navController = navController,
                onUpdateSystemBars = onUpdateSystemBars,
            )
        }
    }
}

@Composable
private fun Content(
    featuresViewModel: OnboardingUpgradeFeaturesViewModel,
    state: OnboardingUpgradeFeaturesState,
    theme: Theme.ThemeType,
    flow: OnboardingFlow,
    exitOnboarding: (OnboardingExitInfo) -> Unit,
    completeOnboardingToDiscover: () -> Unit,
    signInState: SignInState,
    navController: NavHostController,
    onUpdateSystemBars: (SystemBarsStyles) -> Unit,
) {
    val startDestination = when (flow) {
        is OnboardingFlow.LoggedOut,
        is OnboardingFlow.PlusAccountUpgradeNeedsLogin,
        is OnboardingFlow.InitialOnboarding,
        is OnboardingFlow.EngageSdk,
        is OnboardingFlow.ReferralLoginOrSignUp,
        -> OnboardingNavRoute.LOG_IN_OR_SIGN_UP

        // Cannot use OnboardingNavRoute.PlusUpgrade.routeWithSource here, it is set as a defaultValue in the PlusUpgrade composable,
        // see https://stackoverflow.com/a/70410872/1910286
        is OnboardingFlow.PlusAccountUpgrade,
        is OnboardingFlow.PatronAccountUpgrade,
        is OnboardingFlow.Upsell,
        is OnboardingFlow.UpsellSuggestedFolder,
        is OnboardingFlow.NewOnboardingAccountUpgrade,
        -> OnboardingNavRoute.PlusUpgrade.ROUTE

        is OnboardingFlow.Welcome -> OnboardingNavRoute.WELCOME

        is OnboardingFlow.AccountEncouragement -> OnboardingNavRoute.ENCOURAGE_FREE_ACCOUNT
    }

    val onAccountCreated: () -> Unit = {
        fun goBack() {
            navController.navigate(OnboardingRecommendationsFlow.ROUTE) {
                // clear backstack after account is created
                popUpTo(OnboardingNavRoute.LOG_IN_OR_SIGN_UP) {
                    inclusive = true
                }
            }
        }

        fun goToUpsell() {
            navController.navigate(OnboardingNavRoute.PlusUpgrade.routeWithSource(flow.source, forcePurchase = true)) {
                // clear backstack after account is created
                popUpTo(OnboardingNavRoute.LOG_IN_OR_SIGN_UP) {
                    inclusive = true
                }
            }
        }

        when (flow) {
            is OnboardingFlow.ReferralLoginOrSignUp -> {
                exitOnboarding(OnboardingExitInfo.ShowReferralWelcome)
            }

            is OnboardingFlow.Upsell, is OnboardingFlow.UpsellSuggestedFolder -> {
                if (flow.source in forcedPurchaseSources) {
                    goToUpsell()
                } else {
                    goBack()
                }
            }

            else -> {
                goBack()
            }
        }
    }

    fun finishOnboardingFlow() {
        val exitInfo = if (flow is OnboardingFlow.UpsellSuggestedFolder) {
            OnboardingExitInfo.ApplySuggestedFolders(flow.action)
        } else {
            OnboardingExitInfo.Simple
        }
        exitOnboarding(exitInfo)
    }

    NavHost(navController, startDestination) {
        importFlowGraph(theme, navController, flow, onUpdateSystemBars)

        onboardingRecommendationsFlowGraph(
            theme,
            flow = flow,
            onBackPress = { exitOnboarding(OnboardingExitInfo.Simple) },
            onComplete = {
                navController.navigate(
                    if (signInState.isSignedInAsPlusOrPatron) {
                        OnboardingNavRoute.WELCOME
                    } else {
                        OnboardingNavRoute.PlusUpgrade.routeWithSource(OnboardingUpgradeSource.RECOMMENDATIONS)
                    },
                )
            },
            navController = navController,
            onUpdateSystemBars = onUpdateSystemBars,
        )

        composable(OnboardingNavRoute.ENCOURAGE_FREE_ACCOUNT) {
            val viewModel = hiltViewModel<OnboardingAccountBenefitsViewModel>()

            CallOnce {
                viewModel.onScreenShown()
            }

            AppTheme(theme) {
                AccountBenefitsPage(
                    onGetStartedClick = {
                        viewModel.onGetStartedClick()
                        navController.navigate(OnboardingNavRoute.LOG_IN_OR_SIGN_UP) {
                            popUpTo(OnboardingNavRoute.ENCOURAGE_FREE_ACCOUNT) {
                                inclusive = true
                            }
                        }
                    },
                    onLogIn = {
                        viewModel.onLogInClick()
                        navController.navigate(OnboardingNavRoute.LOG_IN_OR_SIGN_UP) {
                            popUpTo(OnboardingNavRoute.ENCOURAGE_FREE_ACCOUNT) {
                                inclusive = true
                            }
                        }
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

        composable(OnboardingNavRoute.LOG_IN_OR_SIGN_UP) {
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
                        is OnboardingFlow.NewOnboardingAccountUpgrade,
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
                onSignUpClick = { navController.navigate(OnboardingNavRoute.CREATE_FREE_ACCOUNT) },
                onLoginClick = { navController.navigate(OnboardingNavRoute.LOG_IN) },
                onContinueWithGoogleComplete = { state, subscription ->
                    if (state.isNewAccount) {
                        onAccountCreated()
                    } else {
                        onLoginToExistingAccount(flow, subscription, exitOnboarding, navController)
                    }
                },
                onUpdateSystemBars = onUpdateSystemBars,
            )
        }

        composable(OnboardingNavRoute.CREATE_FREE_ACCOUNT) {
            OnboardingCreateAccountPage(
                theme = theme,
                onBackPress = { navController.popBackStack() },
                onCreateAccount = onAccountCreated,
                onUpdateSystemBars = onUpdateSystemBars,
            )
        }

        composable(OnboardingNavRoute.LOG_IN) {
            OnboardingLoginPage(
                theme = theme,
                onBackPress = { navController.popBackStack() },
                onLoginComplete = { subscription ->
                    onLoginToExistingAccount(flow, subscription, exitOnboarding, navController)
                },
                onForgotPasswordClick = { navController.navigate(OnboardingNavRoute.FORGOT_PASSWORD) },
                onUpdateSystemBars = onUpdateSystemBars,
            )
        }

        composable(OnboardingNavRoute.FORGOT_PASSWORD) {
            OnboardingForgotPasswordPage(
                theme = theme,
                onBackPress = { navController.popBackStack() },
                onComplete = { exitOnboarding(OnboardingExitInfo.Simple) },
                onUpdateSystemBars = onUpdateSystemBars,
            )
        }

        composable(
            route = OnboardingNavRoute.PlusUpgrade.ROUTE,
            arguments = listOf(
                navArgument(OnboardingNavRoute.PlusUpgrade.SOURCE_ARGUMENT_KEY) {
                    type = NavType.EnumType(OnboardingUpgradeSource::class.java)
                    /* Set default value for onboarding flows with startDestination. */
                    when (flow) {
                        is OnboardingFlow.PatronAccountUpgrade,
                        is OnboardingFlow.PlusAccountUpgrade,
                        is OnboardingFlow.Upsell,
                        is OnboardingFlow.UpsellSuggestedFolder,
                        is OnboardingFlow.NewOnboardingAccountUpgrade,
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
                navArgument(OnboardingNavRoute.PlusUpgrade.FORCE_PURCHASE_ARGUMENT_KEY) {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) { navBackStackEntry ->
            val upgradeSource = navBackStackEntry.arguments
                ?.getSerializableCompat(OnboardingNavRoute.PlusUpgrade.SOURCE_ARGUMENT_KEY, OnboardingUpgradeSource::class.java)
                ?: throw IllegalStateException("Missing upgrade source argument")

            val forcePurchase = navBackStackEntry.arguments
                ?.getBoolean(OnboardingNavRoute.PlusUpgrade.FORCE_PURCHASE_ARGUMENT_KEY)
                ?: throw IllegalStateException("Missing force purchase argument")

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
                onNeedLogin = { navController.navigate(OnboardingNavRoute.LOG_IN_OR_SIGN_UP) },
                onProceed = {
                    if (userCreatedNewAccount || forcePurchase) {
                        navController.navigate(OnboardingNavRoute.WELCOME)
                    } else {
                        finishOnboardingFlow()
                    }
                },
                onUpdateSystemBars = onUpdateSystemBars,
            )
        }

        composable(OnboardingNavRoute.WELCOME) {
            OnboardingWelcomePage(
                theme = theme,
                flow = flow,
                isSignedInAsPlusOrPatron = signInState.isSignedInAsPlusOrPatron,
                onComplete = {
                    finishOnboardingFlow()
                },
                onContinueToDiscover = completeOnboardingToDiscover,
                onImportclick = { navController.navigate(OnboardingImportFlow.ROUTE) },
                onBackPress = {
                    // Don't allow navigation back to the upgrade screen after the user upgrades
                    if (signInState.isSignedInAsPlusOrPatron) {
                        finishOnboardingFlow()
                    } else {
                        navController.popBackStack()
                    }
                },
                onUpdateSystemBars = onUpdateSystemBars,
            )
        }
    }
}

private fun onLoginToExistingAccount(
    flow: OnboardingFlow,
    subscription: Subscription?,
    exitOnboarding: (OnboardingExitInfo) -> Unit,
    navController: NavHostController,
) {
    when (flow) {
        is OnboardingFlow.AccountEncouragement,
        is OnboardingFlow.InitialOnboarding,
        is OnboardingFlow.LoggedOut,
        is OnboardingFlow.EngageSdk,
        -> exitOnboarding(OnboardingExitInfo.ShowPlusPromotion)

        is OnboardingFlow.ReferralLoginOrSignUp -> exitOnboarding(OnboardingExitInfo.Simple)

        // this should never happens, login is not initiated from welcome screen
        is OnboardingFlow.Welcome -> Unit

        is OnboardingFlow.PlusAccountUpgrade,
        is OnboardingFlow.PatronAccountUpgrade,
        is OnboardingFlow.PlusAccountUpgradeNeedsLogin,
        is OnboardingFlow.Upsell,
        is OnboardingFlow.UpsellSuggestedFolder,
        is OnboardingFlow.NewOnboardingAccountUpgrade,
        -> {
            if (subscription == null) {
                navController.navigate(OnboardingNavRoute.PlusUpgrade.routeWithSource(OnboardingUpgradeSource.LOGIN)) {
                    // clear backstack after successful login
                    popUpTo(OnboardingNavRoute.LOG_IN_OR_SIGN_UP) { inclusive = true }
                }
            } else {
                val exitInfo = if (flow is OnboardingFlow.UpsellSuggestedFolder) {
                    OnboardingExitInfo.ApplySuggestedFolders(flow.action)
                } else {
                    OnboardingExitInfo.Simple
                }
                exitOnboarding(exitInfo)
            }
        }
    }
}

@VisibleForTesting
object OnboardingNavRoute {

    const val CREATE_FREE_ACCOUNT = "create_free_account"
    const val ENCOURAGE_FREE_ACCOUNT = "encourage_free_account"
    const val FORGOT_PASSWORD = "forgot_password"
    const val LOG_IN = "log_in"
    const val LOG_IN_OR_SIGN_UP = "log_in_or_sign_up"
    const val WELCOME = "welcome"

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
}

private val forcedPurchaseSources = listOf(
    OnboardingUpgradeSource.BOOKMARKS,
    OnboardingUpgradeSource.BOOKMARKS_SHELF_ACTION,
    OnboardingUpgradeSource.GENERATED_TRANSCRIPTS,
    OnboardingUpgradeSource.SKIP_CHAPTERS,
    OnboardingUpgradeSource.SUGGESTED_FOLDERS,
    OnboardingUpgradeSource.THEMES,
    OnboardingUpgradeSource.UP_NEXT_SHUFFLE,
)

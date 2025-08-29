package au.com.shiftyjelly.pocketcasts.account.onboarding

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import au.com.shiftyjelly.pocketcasts.account.onboarding.import.OnboardingImportFlow.importFlowGraph
import au.com.shiftyjelly.pocketcasts.account.onboarding.recommendations.OnboardingRecommendationsFlow
import au.com.shiftyjelly.pocketcasts.account.onboarding.recommendations.OnboardingRecommendationsFlow.onboardingRecommendationsFlowGraph
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.NewOnboardingFlow
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.NewOnboardingFlow.newOnboardingFlowGraph
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OldOnboardingFlow
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OldOnboardingFlow.oldOnboardingFlowGraph
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingUpgradeFeaturesState
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingUpgradeFeaturesViewModel
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.SystemBarsStyles
import au.com.shiftyjelly.pocketcasts.models.type.SignInState
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingExitInfo
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag

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
    fun onAccountCreated(rootDestination: String) {
        fun goBack() {
            navController.navigate(OnboardingRecommendationsFlow.ROUTE) {
                // clear backstack after account is created
                popUpTo(rootDestination) {
                    inclusive = true
                }
            }
        }

        fun goToUpsell() {
            navController.navigate(OldOnboardingFlow.PlusUpgrade.routeWithSource(flow.source, forcePurchase = true)) {
                // clear backstack after account is created
                popUpTo(rootDestination) {
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
                    if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_ACCOUNT_CREATION)) {
                        exitOnboarding(OnboardingExitInfo.ShowPlusPromotion)
                    } else {
                        goBack()
                    }
                }
            }

            else -> if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_ACCOUNT_CREATION)) {
                exitOnboarding(OnboardingExitInfo.ShowPlusPromotion)
            } else {
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

    val rootDestination = if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_ACCOUNT_CREATION)) {
        NewOnboardingFlow.startDestination(flow)
    } else {
        OldOnboardingFlow.startDestination(flow)
    }
    NavHost(navController, rootDestination) {
        importFlowGraph(theme, navController, flow, onUpdateSystemBars)

        onboardingRecommendationsFlowGraph(
            theme,
            flow = flow,
            onBackPress = {
                if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_ACCOUNT_CREATION)) {
                    navController.popBackStack()
                } else {
                    exitOnboarding(OnboardingExitInfo.Simple)
                }
            },
            onComplete = {
                val route = if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_ACCOUNT_CREATION)) {
                    NewOnboardingFlow.ROUTE_SIGN_UP
                } else {
                    if (signInState.isSignedInAsPlusOrPatron) {
                        OldOnboardingFlow.WELCOME
                    } else {
                        OldOnboardingFlow.PlusUpgrade.routeWithSource(OnboardingUpgradeSource.RECOMMENDATIONS)
                    }
                }
                navController.navigate(route)
            },
            navController = navController,
            onUpdateSystemBars = onUpdateSystemBars,
        )

        if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_ACCOUNT_CREATION)) {
            newOnboardingFlowGraph(
                theme = theme,
                flow = flow,
                navController = navController,
                onUpdateSystemBars = onUpdateSystemBars,
                signInState = signInState,
                featuresViewModel = featuresViewModel,
                state = state,
                onAccountCreated = { onAccountCreated(rootDestination) },
                exitOnboarding = exitOnboarding,
                finishOnboardingFlow = ::finishOnboardingFlow,
                onLoginToExistingAccount = { flow, subscription, exitInfo -> onLoginToExistingAccount(flow, subscription, exitInfo, navController) },
            )
        } else {
            oldOnboardingFlowGraph(
                theme = theme,
                flow = flow,
                navController = navController,
                onUpdateSystemBars = onUpdateSystemBars,
                signInState = signInState,
                featuresViewModel = featuresViewModel,
                state = state,
                onAccountCreated = { onAccountCreated(rootDestination) },
                exitOnboarding = exitOnboarding,
                finishOnboardingFlow = ::finishOnboardingFlow,
                onLoginToExistingAccount = { flow, subscription, exitInfo -> onLoginToExistingAccount(flow, subscription, exitInfo, navController) },
                completeOnboardingToDiscover = { completeOnboardingToDiscover() },
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
                navController.navigate(OldOnboardingFlow.PlusUpgrade.routeWithSource(source = flow.source, forcePurchase = true)) {
                    // clear backstack after successful login
                    val route = if (FeatureFlag.isEnabled(Feature.NEW_ONBOARDING_ACCOUNT_CREATION)) {
                        OldOnboardingFlow.LOG_IN_OR_SIGN_UP
                    } else {
                        NewOnboardingFlow.ROUTE_INTRO_CAROUSEL
                    }
                    popUpTo(route) { inclusive = true }
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

val forcedPurchaseSources = listOf(
    OnboardingUpgradeSource.BOOKMARKS,
    OnboardingUpgradeSource.BOOKMARKS_SHELF_ACTION,
    OnboardingUpgradeSource.GENERATED_TRANSCRIPTS,
    OnboardingUpgradeSource.SKIP_CHAPTERS,
    OnboardingUpgradeSource.SUGGESTED_FOLDERS,
    OnboardingUpgradeSource.THEMES,
    OnboardingUpgradeSource.UP_NEXT_SHUFFLE,
)

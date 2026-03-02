package au.com.shiftyjelly.pocketcasts.account.onboarding

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import au.com.shiftyjelly.pocketcasts.account.onboarding.import.OnboardingImportFlow.importFlowGraph
import au.com.shiftyjelly.pocketcasts.account.onboarding.recommendations.OnboardingRecommendationsFlow.onboardingRecommendationsFlowGraph
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingFlowRoutes
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingFlowRoutes.flowGraph
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

@Composable
fun OnboardingFlowComposable(
    featuresViewModel: OnboardingUpgradeFeaturesViewModel,
    state: OnboardingUpgradeFeaturesState,
    theme: Theme.ThemeType,
    flow: OnboardingFlow,
    exitOnboarding: (OnboardingExitInfo) -> Unit,
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
    signInState: SignInState,
    navController: NavHostController,
    onUpdateSystemBars: (SystemBarsStyles) -> Unit,
) {
    fun onAccountCreated(rootDestination: String) {
        fun goToUpsell() {
            navController.navigate(OnboardingFlowRoutes.PlusUpgrade.routeWithSource(flow.source, forcePurchase = true)) {
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
                    exitOnboarding(OnboardingExitInfo.ShowPlusPromotion)
                }
            }

            else -> exitOnboarding(OnboardingExitInfo.ShowPlusPromotion)
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

    val rootDestination = OnboardingFlowRoutes.startDestination(flow)

    NavHost(navController, rootDestination) {
        importFlowGraph(theme, navController, flow, onUpdateSystemBars)

        onboardingRecommendationsFlowGraph(
            theme,
            flow = flow,
            onBackPress = {
                navController.popBackStack()
            },
            onComplete = {
                navController.navigate(OnboardingFlowRoutes.ROUTE_SIGN_UP)
            },
            navController = navController,
            onUpdateSystemBars = onUpdateSystemBars,
        )

        flowGraph(
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
        is OnboardingFlow.AccountUpgrade,
        -> {
            if (subscription == null) {
                navController.navigate(OnboardingFlowRoutes.PlusUpgrade.routeWithSource(source = flow.source, forcePurchase = true)) {
                    // clear backstack after successful login
                    popUpTo(OnboardingFlowRoutes.LOG_IN_OR_SIGN_UP) { inclusive = true }
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

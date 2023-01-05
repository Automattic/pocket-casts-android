package au.com.shiftyjelly.pocketcasts.account.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import au.com.shiftyjelly.pocketcasts.account.onboarding.AnalyticsProp.flow
import au.com.shiftyjelly.pocketcasts.account.onboarding.AnalyticsProp.recommendationsSource
import au.com.shiftyjelly.pocketcasts.account.onboarding.import.OnboardingImportFlow
import au.com.shiftyjelly.pocketcasts.account.onboarding.import.OnboardingImportFlow.importFlowGraph
import au.com.shiftyjelly.pocketcasts.account.onboarding.recommendations.OnboardingRecommendationsFlow
import au.com.shiftyjelly.pocketcasts.account.onboarding.recommendations.OnboardingRecommendationsFlow.onboardingRecommendationsFlowGraph
import au.com.shiftyjelly.pocketcasts.account.onboarding.upgrade.OnboardingPlusUpgradeFlow
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.models.to.SignInState
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
fun OnboardingFlowComposable(
    theme: Theme.ThemeType,
    completeOnboarding: () -> Unit,
    completeOnboardingToDiscover: () -> Unit,
    signInState: SignInState?
) {
    AppThemeWithBackground(theme) {
        val navController = rememberNavController()

        NavHost(
            navController = navController,
            startDestination = OnboardingNavRoute.logInOrSignUp
        ) {

            importFlowGraph(theme, navController, flow)

            onboardingRecommendationsFlowGraph(
                theme = theme,
                flow = flow,
                onBackPressed = completeOnboarding,
                onComplete = {
                    navController.navigate(OnboardingNavRoute.plusUpgrade)
                },
                navController = navController,
            )

            composable(OnboardingNavRoute.logInOrSignUp) {
                OnboardingLoginOrSignUpPage(
                    theme = theme,
                    flow = flow,
                    onDismiss = { completeOnboarding() },
                    onSignUpClicked = { navController.navigate(OnboardingNavRoute.createFreeAccount) },
                    onLoginClicked = { navController.navigate(OnboardingNavRoute.logIn) },
                    onContinueWithGoogleClicked = { navController.navigate(OnboardingNavRoute.logInGoogle) },
                )
            }

            composable(OnboardingNavRoute.createFreeAccount) {
                OnboardingCreateAccountPage(
                    theme = theme,
                    onBackPressed = { navController.popBackStack() },
                    onAccountCreated = {
                        navController.navigate(OnboardingRecommendationsFlow.route) {
                            // clear backstack after account is created
                            popUpTo(OnboardingNavRoute.logInOrSignUp) {
                                inclusive = true
                            }
                        }
                    },
                )
            }

            composable(OnboardingNavRoute.logIn) {
                OnboardingLoginPage(
                    theme = theme,
                    onBackPressed = { navController.popBackStack() },
                    onLoginComplete = completeOnboarding,
                    onForgotPasswordTapped = { navController.navigate(OnboardingNavRoute.forgotPassword) },
                )
            }

            composable(OnboardingNavRoute.logInGoogle) {
                OnboardingLoginGooglePage()
            }

            composable(OnboardingNavRoute.forgotPassword) {
                OnboardingForgotPasswordPage(
                    theme = theme,
                    onBackPressed = { navController.popBackStack() },
                    onCompleted = completeOnboarding,
                )
            }

            composable(OnboardingNavRoute.plusUpgrade) {
                OnboardingPlusUpgradeFlow(
                    flow = flow,
                    source = recommendationsSource,
                    onBackPressed = { navController.popBackStack() },
                    onNotNowPressed = { navController.navigate(OnboardingNavRoute.welcome) },
                    onCompleteUpgrade = {
                        navController.navigate(OnboardingNavRoute.welcome) {
                            // Don't allow navigation back to the upgrade screen after the user upgrades
                            popUpTo(OnboardingNavRoute.plusUpgrade) {
                                inclusive = true
                            }
                        }
                    },
                )
            }

            composable(OnboardingNavRoute.welcome) {
                OnboardingWelcomePage(
                    activeTheme = theme,
                    flow = flow,
                    isSignedInAsPlus = signInState?.isSignedInAsPlus ?: false,
                    onDone = completeOnboarding,
                    onContinueToDiscover = completeOnboardingToDiscover,
                    onImportTapped = { navController.navigate(OnboardingImportFlow.route) },
                    onBackPressed = { navController.popBackStack() },
                )
            }
        }
    }
}

private object AnalyticsProp {
    const val flow = "initial_onboarding"
    const val recommendationsSource = "recommendations"
}

private object OnboardingNavRoute {
    const val createFreeAccount = "create_free_account"
    const val forgotPassword = "forgot_password"
    const val logIn = "log_in"
    const val logInGoogle = "log_in_google"
    const val logInOrSignUp = "log_in_or_sign_up"
    const val plusUpgrade = "upgrade_upgrade"
    const val welcome = "welcome"
}

package au.com.shiftyjelly.pocketcasts.account.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
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
    activeTheme: Theme.ThemeType,
    completeOnboarding: () -> Unit,
    completeOnboardingToDiscover: () -> Unit,
    abortOnboarding: () -> Unit,
    signInState: SignInState?
) {
    AppThemeWithBackground(activeTheme) {
        val navController = rememberNavController()

        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = backStackEntry?.destination?.route

        BackHandler {
            val failedToPop = !navController.popBackStack()
            if (failedToPop) {
                // The ony time the back stack will be empty and the user is aborting
                // onboarding is from the logInOrSignUp screen
                val abortingOnboarding = currentDestination == OnboardingNavRoute.logInOrSignUp
                if (abortingOnboarding) {
                    abortOnboarding()
                } else {
                    completeOnboarding()
                }
            }
        }

        NavHost(
            navController = navController,
            startDestination = OnboardingNavRoute.logInOrSignUp
        ) {

            importFlowGraph(navController, flow)

            onboardingRecommendationsFlowGraph(
                flow = flow,
                onBackPressed = completeOnboarding,
                onComplete = {
                    navController.navigate(OnboardingNavRoute.plusUpgrade)
                },
                navController = navController,
            )

            composable(OnboardingNavRoute.logInOrSignUp) {
                OnboardingLoginOrSignUpPage(
                    flow = flow,
                    onDismiss = { completeOnboarding() },
                    onSignUpClicked = { navController.navigate(OnboardingNavRoute.createFreeAccount) },
                    onLoginClicked = { navController.navigate(OnboardingNavRoute.logIn) },
                    onContinueWithGoogleClicked = { navController.navigate(OnboardingNavRoute.logInGoogle) },
                )
            }

            composable(OnboardingNavRoute.createFreeAccount) {
                OnboardingCreateAccountPage(
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

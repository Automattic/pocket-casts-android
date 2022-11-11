package au.com.shiftyjelly.pocketcasts.account.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
fun OnboardingFlowComposable(
    activeTheme: Theme.ThemeType,
    completeOnboarding: () -> Unit,
    abortOnboarding: () -> Unit,
    analyticsTracker: AnalyticsTrackerWrapper
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
            composable(OnboardingNavRoute.logInOrSignUp) {
                OnboardingLoginOrSignUpPage(
                    onNotNowClicked = {
                        analyticsTracker.track(AnalyticsEvent.SETUP_ACCOUNT_DISMISSED, AnalyticsProp.source)
                        completeOnboarding()
                    },
                    onSignUpFreeClicked = {
                        analyticsTracker.track(AnalyticsEvent.SETUP_ACCOUNT_BUTTON_TAPPED, AnalyticsProp.ButtonTapped.createAccount)
                        navController.navigate(OnboardingNavRoute.createFreeAccount)
                    },
                    onLoginClicked = {
                        analyticsTracker.track(AnalyticsEvent.SETUP_ACCOUNT_BUTTON_TAPPED, AnalyticsProp.ButtonTapped.signIn)
                        navController.navigate(OnboardingNavRoute.logIn)
                    },
                    onContinueWithGoogleClicked = {
                        analyticsTracker.track(AnalyticsEvent.SETUP_ACCOUNT_BUTTON_TAPPED, AnalyticsProp.ButtonTapped.continueWithGoogle)
                        navController.navigate(OnboardingNavRoute.logInGoogle)
                    },
                    onShown = { analyticsTracker.track(AnalyticsEvent.SETUP_ACCOUNT_SHOWN, AnalyticsProp.source) }
                )
            }

            composable(OnboardingNavRoute.createFreeAccount) {
                OnboardingCreateAccountPage(
                    onShown = { analyticsTracker.track(AnalyticsEvent.CREATE_ACCOUNT_SHOWN) },
                    onBackPressed = {
                        analyticsTracker.track(AnalyticsEvent.CREATE_ACCOUNT_DISMISSED)
                        navController.popBackStack()
                    },
                    onAccountCreated = {
                        navController.navigate(OnboardingNavRoute.recommendations) {
                            // clear backstack when opening recommendations
                            popUpTo(OnboardingNavRoute.logInOrSignUp) {
                                inclusive = true
                            }
                        }
                    },
                )
            }

            composable(OnboardingNavRoute.logIn) {
                OnboardingLoginPage(
                    onShown = { analyticsTracker.track(AnalyticsEvent.SIGNIN_SHOWN) },
                    onBackPressed = {
                        analyticsTracker.track(AnalyticsEvent.SIGNIN_DISMISSED)
                        navController.popBackStack()
                    },
                    onLoginComplete = {
                        navController.navigate(OnboardingNavRoute.recommendations) {
                            // clear backstack when opening recommendations
                            popUpTo(OnboardingNavRoute.logInOrSignUp) {
                                inclusive = true
                            }
                        }
                    },
                    onForgotPasswordTapped = { navController.navigate(OnboardingNavRoute.forgotPassword) },
                )
            }

            composable(OnboardingNavRoute.logInGoogle) {
                OnboardingLoginGooglePage()
            }

            composable(OnboardingNavRoute.forgotPassword) {
                OnboardingForgotPassword(onBackPressed = { navController.popBackStack() })
            }

            composable(OnboardingNavRoute.recommendations) {
                OnboardingRecommendations()
            }
        }
    }
}

private object AnalyticsProp {
    object ButtonTapped {
        private const val button = "button"
        val signIn = source + mapOf(button to "sign_in")
        val createAccount = source + mapOf(button to "create_account")
        val continueWithGoogle = source + mapOf(button to "continue_with_google")
    }
    val source = mapOf("source" to "onboarding")
}

private object OnboardingNavRoute {
    const val logInOrSignUp = "log_in_or_sign_up"
    const val createFreeAccount = "create_free_account"
    const val logIn = "log_in"
    const val logInGoogle = "log_in_google"
    const val forgotPassword = "forgot_password"
    const val recommendations = "recommendations"
}

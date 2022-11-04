package au.com.shiftyjelly.pocketcasts.account.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
fun OnboardingFlowComposable(
    activeTheme: Theme.ThemeType,
    completeOnboarding: () -> Unit,
) {
    AppThemeWithBackground(activeTheme) {
        val navController = rememberNavController()

        val backStackEntry by navController.currentBackStackEntryAsState()
        val currentDestination = backStackEntry?.destination?.route

        // Only exit the app during onboarding if we are on the first onboarding screen
        val shouldExitAppOnBackPress = currentDestination == OnboardingNavRoute.logInOrSignUp

        // If the BackHandler is disabled, then the normal back pressed behavior
        // takes over and exits the app
        BackHandler(enabled = !shouldExitAppOnBackPress) {
            val failedToPop = !navController.popBackStack()
            if (failedToPop) {
                // Nothing to pop from backstack, so let the user exit the onboarding flow
                completeOnboarding()
            }
        }

        NavHost(
            navController = navController,
            startDestination = OnboardingNavRoute.logInOrSignUp
        ) {
            composable(OnboardingNavRoute.logInOrSignUp) {
                OnboardingLoginOrSignUpPage(
                    onNotNowClicked = completeOnboarding,
                    onSignUpFreeClicked = { navController.navigate(OnboardingNavRoute.createFreeAccount) },
                    onLoginClicked = { navController.navigate(OnboardingNavRoute.logIn) },
                    onLoginGoogleClicked = { navController.navigate(OnboardingNavRoute.logInGoogle) }
                )
            }

            composable(OnboardingNavRoute.createFreeAccount) {
                OnboardingCreateAccountPage(
                    onBackPressed = { navController.popBackStack() },
                    onAccountCreated = {
                        navController.navigate(OnboardingNavRoute.recommendations) {
                            // clear backstack when opening recommendations
                            popUpTo(OnboardingNavRoute.logInOrSignUp) {
                                inclusive = true
                            }
                        }
                    }
                )
            }

            composable(OnboardingNavRoute.logIn) {
                OnboardingLoginPage(
                    onBackPressed = { navController.popBackStack() },
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

private object OnboardingNavRoute {
    const val logInOrSignUp = "log_in_or_sign_up"
    const val createFreeAccount = "create_free_account"
    const val logIn = "log_in"
    const val logInGoogle = "log_in_google"
    const val forgotPassword = "forgot_password"
    const val recommendations = "recommendations"
}

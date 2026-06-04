package au.com.shiftyjelly.pocketcasts.onboarding

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.tv.material3.MaterialTheme
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.home.TvScaffold
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
fun TvOnboardingNavHost(
    viewModel: TvOnboardingViewModel = hiltViewModel(),
) {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            val navController = rememberNavController()
            NavHost(
                navController = navController,
                startDestination = viewModel.startDestination,
            ) {
                composable(TvOnboardingRoutes.LANDING) {
                    TvLandingScreen(
                        onSignIn = { navController.navigate(TvOnboardingRoutes.SIGN_IN) },
                        onCreateAccount = { navController.navigate(TvOnboardingRoutes.SIGN_IN) },
                        onContinueWithoutAccount = {
                            viewModel.completeOnboarding()
                            navController.navigate(TvOnboardingRoutes.HOME) {
                                popUpTo(TvOnboardingRoutes.LANDING) { inclusive = true }
                            }
                        },
                    )
                }
                composable(TvOnboardingRoutes.SIGN_IN) {
                    TvSignInScreen(
                        onSignInComplete = {
                            navController.navigate(TvOnboardingRoutes.SYNCING) {
                                popUpTo(TvOnboardingRoutes.LANDING) { inclusive = true }
                            }
                        },
                    )
                }
                composable(TvOnboardingRoutes.SYNCING) {
                    TvSyncingScreen(
                        onSyncComplete = {
                            viewModel.completeOnboarding()
                            navController.navigate(TvOnboardingRoutes.HOME) {
                                popUpTo(TvOnboardingRoutes.SYNCING) { inclusive = true }
                            }
                        },
                    )
                }
                composable(TvOnboardingRoutes.HOME) {
                    TvScaffold()
                }
            }
        }
    }
}

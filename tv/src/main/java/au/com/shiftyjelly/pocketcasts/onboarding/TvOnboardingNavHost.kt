package au.com.shiftyjelly.pocketcasts.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import au.com.shiftyjelly.pocketcasts.component.LocalTvToastHostState
import au.com.shiftyjelly.pocketcasts.component.TvToastHost
import au.com.shiftyjelly.pocketcasts.component.TvToastHostState
import au.com.shiftyjelly.pocketcasts.home.TvScaffold
import au.com.shiftyjelly.pocketcasts.onboarding.createaccount.TvCreateAccountScreen
import au.com.shiftyjelly.pocketcasts.onboarding.signin.TvSignInScreen
import au.com.shiftyjelly.pocketcasts.onboarding.signin.TvSyncingScreen
import au.com.shiftyjelly.pocketcasts.onboarding.welcome.TvWelcomeScreen

@Composable
fun TvOnboardingNavHost(
    modifier: Modifier = Modifier,
    viewModel: TvOnboardingViewModel = hiltViewModel(),
) {
    val navController = rememberNavController()
    val toastHostState = remember { TvToastHostState() }
    CompositionLocalProvider(LocalTvToastHostState provides toastHostState) {
        Box(modifier = modifier.fillMaxSize()) {
            NavHost(
                navController = navController,
                startDestination = viewModel.startDestination,
                modifier = Modifier.fillMaxSize(),
            ) {
                composable(TvOnboardingRoutes.LANDING) {
                    TvWelcomeScreen(
                        onSignIn = { navController.navigate(TvOnboardingRoutes.SIGN_IN) },
                        onCreateAccount = { navController.navigate(TvOnboardingRoutes.CREATE_ACCOUNT) },
                        onContinueWithoutAccount = {
                            navController.navigate(TvOnboardingRoutes.HOME) {
                                popUpTo(TvOnboardingRoutes.LANDING) { inclusive = true }
                            }
                        },
                    )
                }
                composable(TvOnboardingRoutes.CREATE_ACCOUNT) {
                    TvCreateAccountScreen(
                        onSignIn = { navController.navigate(TvOnboardingRoutes.SIGN_IN) },
                    )
                }
                composable(TvOnboardingRoutes.SIGN_IN) {
                    TvSignInScreen(
                        onSignInComplete = {
                            navController.navigate(TvOnboardingRoutes.SYNCING) {
                                popUpTo(navController.graph.id) { inclusive = true }
                            }
                        },
                    )
                }
                composable(TvOnboardingRoutes.SYNCING) {
                    TvSyncingScreen(
                        onSyncComplete = {
                            navController.navigate(TvOnboardingRoutes.HOME) {
                                popUpTo(TvOnboardingRoutes.SYNCING) { inclusive = true }
                            }
                        },
                    )
                }
                composable(TvOnboardingRoutes.HOME) {
                    TvScaffold(
                        onLogIn = { navController.navigate(TvOnboardingRoutes.SIGN_IN) },
                        onCreateAccount = { navController.navigate(TvOnboardingRoutes.CREATE_ACCOUNT) },
                    )
                }
            }
            TvToastHost(
                state = toastHostState,
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = 80.dp, end = 48.dp),
            )
        }
    }
}

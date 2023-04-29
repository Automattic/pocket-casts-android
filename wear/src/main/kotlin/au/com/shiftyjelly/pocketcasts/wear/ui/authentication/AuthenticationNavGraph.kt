package au.com.shiftyjelly.pocketcasts.wear.ui.authentication

import androidx.compose.runtime.remember
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.navigation
import au.com.shiftyjelly.pocketcasts.account.viewmodel.SignInViewModel
import au.com.shiftyjelly.pocketcasts.wear.ui.LoginScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.WatchListScreen
import com.google.android.horologist.compose.navscaffold.NavScaffoldViewModel
import com.google.android.horologist.compose.navscaffold.composable
import com.google.android.horologist.compose.navscaffold.scrollable

const val authenticationSubGraph = "authentication_graph"

private object AuthenticationNavRoutes {
    const val password = "authentication_password"
    const val loginScreen = "login_screen"
    const val loginWithGoogle = "login_with_google"
    const val loginWithPhone = "login_with_phone"
    const val loginWithEmail = "login_with_email"
}

fun NavGraphBuilder.authenticationNavGraph(navController: NavController) {
    navigation(startDestination = AuthenticationNavRoutes.loginScreen, route = authenticationSubGraph) {

        scrollable(AuthenticationNavRoutes.loginScreen) {
            LoginScreen(
                columnState = it.columnState,
                onLoginWithGoogleClick = {},
                onLoginWithPhoneClick = {
                    navController.navigate(AuthenticationNavRoutes.loginWithPhone)
                },
                onLoginWithEmailClick = {
                    navController.navigate(AuthenticationNavRoutes.loginWithEmail)
                },
            )
        }

        scrollable(AuthenticationNavRoutes.loginWithEmail) {
            it.viewModel.timeTextMode = NavScaffoldViewModel.TimeTextMode.Off

            val parentEntry = remember(it.backStackEntry) {
                navController.getBackStackEntry(authenticationSubGraph)
            }
            val viewModel = hiltViewModel<SignInViewModel>(parentEntry)

            LoginWithEmailScreen(
                navigateToPasswordScreen = { navController.navigate(AuthenticationNavRoutes.password) },
                listState = it.scrollableState,
                viewModel = viewModel
            )
        }

        composable(AuthenticationNavRoutes.password) {
            it.viewModel.timeTextMode = NavScaffoldViewModel.TimeTextMode.Off

            val parentEntry = remember(it.backStackEntry) {
                navController.getBackStackEntry(authenticationSubGraph)
            }
            val viewModel = hiltViewModel<SignInViewModel>(parentEntry)

            PasswordScreen(
                viewModel = viewModel,
                navigateOnSignInSuccess = {
                    navController.popBackStack(WatchListScreen.route, inclusive = false)
                }
            )
        }

        scrollable(AuthenticationNavRoutes.loginWithPhone) {
            LoginWithPhoneScreen(
                columnState = it.columnState,
                onDone = { navController.popBackStack() },
            )
        }
    }
}

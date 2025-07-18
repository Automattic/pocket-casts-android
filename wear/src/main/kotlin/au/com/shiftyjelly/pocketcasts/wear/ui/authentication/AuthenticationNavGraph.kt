@file:Suppress("DEPRECATION")

package au.com.shiftyjelly.pocketcasts.wear.ui.authentication

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.navigation
import androidx.wear.compose.navigation.composable
import com.google.android.gms.auth.api.signin.GoogleSignInAccount

const val AUTHENTICATION_SUB_GRAPH = "authentication_graph"

private object AuthenticationNavRoutes {
    const val LOGIN_SCREEN = "login_screen"
    const val LOGIN_WITH_GOOGLE = "login_with_google"
    const val LOGIN_WITH_PHONE = "login_with_phone"
    const val LOGIN_WITH_EMAIL = "login_with_email"
}

fun NavGraphBuilder.authenticationNavGraph(
    navController: NavController,
    onEmailSignInSuccess: () -> Unit,
    googleSignInSuccessScreen: @Composable (GoogleSignInAccount?) -> Unit,
) {
    navigation(
        startDestination = AuthenticationNavRoutes.LOGIN_SCREEN,
        route = AUTHENTICATION_SUB_GRAPH,
    ) {
        composable(
            route = AuthenticationNavRoutes.LOGIN_SCREEN,
        ) {
            LoginScreen(
                onLoginWithGoogleClick = {
                    navController.navigate(AuthenticationNavRoutes.LOGIN_WITH_GOOGLE)
                },
                onLoginWithPhoneClick = {
                    navController.navigate(AuthenticationNavRoutes.LOGIN_WITH_PHONE)
                },
                onLoginWithEmailClick = {
                    navController.navigate(AuthenticationNavRoutes.LOGIN_WITH_EMAIL)
                },
            )
        }

        composable(
            route = AuthenticationNavRoutes.LOGIN_WITH_EMAIL,
        ) {
            LoginWithEmailScreen(
                onSignInSuccess = onEmailSignInSuccess,
            )
        }

        composable(
            route = AuthenticationNavRoutes.LOGIN_WITH_PHONE,
        ) {
            LoginWithPhoneScreen(
                onLoginClick = { navController.popBackStack() },
            )
        }

        composable(
            route = AuthenticationNavRoutes.LOGIN_WITH_GOOGLE,
        ) {
            LoginWithGoogleScreen(
                signInSuccessScreen = googleSignInSuccessScreen,
                onCancel = { navController.popBackStack() },
            )
        }
    }
}

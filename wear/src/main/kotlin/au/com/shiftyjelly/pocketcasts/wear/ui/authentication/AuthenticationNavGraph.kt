package au.com.shiftyjelly.pocketcasts.wear.ui.authentication

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.navigation
import androidx.wear.compose.navigation.composable
import com.google.android.gms.auth.api.signin.GoogleSignInAccount

const val authenticationSubGraph = "authentication_graph"

private object AuthenticationNavRoutes {
    const val loginScreen = "login_screen"
    const val loginWithGoogle = "login_with_google"
    const val loginWithPhone = "login_with_phone"
    const val loginWithEmail = "login_with_email"
}

fun NavGraphBuilder.authenticationNavGraph(
    navController: NavController,
    onEmailSignInSuccess: () -> Unit,
    googleSignInSuccessScreen: @Composable (GoogleSignInAccount?) -> Unit,
) {
    navigation(
        startDestination = AuthenticationNavRoutes.loginScreen,
        route = authenticationSubGraph,
    ) {
        composable(
            route = AuthenticationNavRoutes.loginScreen,
        ) {
            LoginScreen(
                onLoginWithGoogleClick = {
                    navController.navigate(AuthenticationNavRoutes.loginWithGoogle)
                },
                onLoginWithPhoneClick = {
                    navController.navigate(AuthenticationNavRoutes.loginWithPhone)
                },
                onLoginWithEmailClick = {
                    navController.navigate(AuthenticationNavRoutes.loginWithEmail)
                },
            )
        }

        composable(
            route = AuthenticationNavRoutes.loginWithEmail,
        ) {
            LoginWithEmailScreen(
                onSignInSuccess = onEmailSignInSuccess,
            )
        }

        composable(
            route = AuthenticationNavRoutes.loginWithPhone,
        ) {
            LoginWithPhoneScreen(
                onDone = { navController.popBackStack() },
            )
        }

        composable(
            route = AuthenticationNavRoutes.loginWithGoogle,
        ) {
            LoginWithGoogleScreen(
                signInSuccessScreen = googleSignInSuccessScreen,
                onAuthCanceled = { navController.popBackStack() },
            )
        }
    }
}

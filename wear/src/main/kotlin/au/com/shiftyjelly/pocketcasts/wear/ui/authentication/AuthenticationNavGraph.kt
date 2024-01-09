package au.com.shiftyjelly.pocketcasts.wear.ui.authentication

import androidx.compose.runtime.Composable
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.navigation
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.horologist.compose.navscaffold.composable
import com.google.android.horologist.compose.navscaffold.scrollable

const val authenticationSubGraph = "authentication_graph"

private object AuthenticationNavRoutes {
    const val loginScreen = "login_screen"
    const val loginWithGoogle = "login_with_google"
    const val loginWithPhone = "login_with_phone"
}

fun NavGraphBuilder.authenticationNavGraph(
    navController: NavController,
    googleSignInSuccessScreen: @Composable (GoogleSignInAccount?) -> Unit,
) {
    navigation(startDestination = AuthenticationNavRoutes.loginScreen, route = authenticationSubGraph) {
        scrollable(AuthenticationNavRoutes.loginScreen) {
            LoginScreen(
                columnState = it.columnState,
                onLoginWithGoogleClick = {
                    navController.navigate(AuthenticationNavRoutes.loginWithGoogle)
                },
                onLoginWithPhoneClick = {
                    navController.navigate(AuthenticationNavRoutes.loginWithPhone)
                },
            )
        }

        scrollable(AuthenticationNavRoutes.loginWithPhone) {
            LoginWithPhoneScreen(
                columnState = it.columnState,
                onDone = { navController.popBackStack() },
            )
        }

        composable(AuthenticationNavRoutes.loginWithGoogle) {
            LoginWithGoogleScreen(
                signInSuccessScreen = googleSignInSuccessScreen,
                onAuthCanceled = { navController.popBackStack() },
            )
        }
    }
}

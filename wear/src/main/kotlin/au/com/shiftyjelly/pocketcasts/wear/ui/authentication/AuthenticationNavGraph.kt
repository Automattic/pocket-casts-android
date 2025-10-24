@file:Suppress("DEPRECATION")

package au.com.shiftyjelly.pocketcasts.wear.ui.authentication

import android.os.Build
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.navigation
import androidx.wear.compose.navigation.composable
import kotlinx.coroutines.delay
import au.com.shiftyjelly.pocketcasts.localization.R as LR

const val AUTHENTICATION_SUB_GRAPH = "authentication_graph"

private object AuthenticationNavRoutes {
    const val LOGIN_SCREEN = "login_screen"
    const val LOGIN_WITH_GOOGLE = "login_with_google"
    const val LOGIN_WITH_GOOGLE_LEGACY = "login_with_google_legacy"
    const val LOGIN_WITH_PHONE = "login_with_phone"
    const val LOGIN_WITH_EMAIL = "login_with_email"
}

data class GoogleAccountData(
    val name: String,
    val avatarUrl: String? = null,
)

fun NavGraphBuilder.authenticationNavGraph(
    navController: NavController,
    onEmailSignInSuccess: () -> Unit,
    googleSignInSuccessScreen: @Composable (GoogleAccountData) -> Unit,
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
                    val route = if (Build.VERSION.SDK_INT > Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        AuthenticationNavRoutes.LOGIN_WITH_GOOGLE
                    } else {
                        AuthenticationNavRoutes.LOGIN_WITH_GOOGLE_LEGACY
                    }
                    navController.navigate(route)
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
            val activity = LocalActivity.current

            var showErrorToastMessage by remember { mutableStateOf<String?>(null) }

            LaunchedEffect(activity, showErrorToastMessage) {
                showErrorToastMessage?.let {
                    Toast.makeText(activity, it, Toast.LENGTH_SHORT).show()
                    delay(500L)
                    navController.popBackStack()
                }
            }
            val defaultErrorMessage = stringResource(LR.string.onboarding_continue_with_google_error)

            LoginWithGoogleScreen(
                successContent = {
                    googleSignInSuccessScreen(GoogleAccountData(name = it.name, avatarUrl = it.avatarUrl))
                },
                onError = {
                    showErrorToastMessage = it?.toString() ?: defaultErrorMessage
                },
                onGoogleNotAvailable = {
                    showErrorToastMessage = defaultErrorMessage
                },
                onCancel = {
                    navController.popBackStack()
                },
            )
        }

        composable(
            route = AuthenticationNavRoutes.LOGIN_WITH_GOOGLE_LEGACY,
        ) {
            LegacyLoginWithGoogleScreen(
                signInSuccessScreen = {
                    googleSignInSuccessScreen(GoogleAccountData(name = it?.givenName.orEmpty(), avatarUrl = it?.photoUrl?.toString()))
                },
                onCancel = { navController.popBackStack() },
            )
        }
    }
}

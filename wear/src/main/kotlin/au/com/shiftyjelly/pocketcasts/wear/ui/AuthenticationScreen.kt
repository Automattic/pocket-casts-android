package au.com.shiftyjelly.pocketcasts.wear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.navigation
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import au.com.shiftyjelly.pocketcasts.account.viewmodel.SignInState
import au.com.shiftyjelly.pocketcasts.account.viewmodel.SignInViewModel
import au.com.shiftyjelly.pocketcasts.podcasts.view.compose.components.FormField
import com.google.android.horologist.compose.navscaffold.NavScaffoldViewModel
import com.google.android.horologist.compose.navscaffold.wearNavComposable
import kotlinx.coroutines.delay
import au.com.shiftyjelly.pocketcasts.localization.R as LR

const val authenticationSubGraph = "authentication_screen"
private object AuthenticationRoutes {
    const val email = "authentication_email"
    const val password = "authentication_password"
}

fun NavGraphBuilder.authenticationGraph(navController: NavController) {
    navigation(startDestination = AuthenticationRoutes.email, route = authenticationSubGraph) {
        wearNavComposable(route = AuthenticationRoutes.email) { backStackEntry, scaffoldViewModel ->
            scaffoldViewModel.timeTextMode = NavScaffoldViewModel.TimeTextMode.Off

            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(authenticationSubGraph)
            }
            val viewModel = hiltViewModel<SignInViewModel>(parentEntry)

            EmailScreen(navigateToRoute = navController::navigate, viewModel = viewModel)
        }
        wearNavComposable(AuthenticationRoutes.password) { backStackEntry, scaffoldViewModel ->
            scaffoldViewModel.timeTextMode = NavScaffoldViewModel.TimeTextMode.Off

            val parentEntry = remember(backStackEntry) {
                navController.getBackStackEntry(authenticationSubGraph)
            }
            val viewModel = hiltViewModel<SignInViewModel>(parentEntry)

            PasswordScreen(
                viewModel = viewModel,
                navigateOnSignInSuccess = {
                    navController.popBackStack(WatchListScreen.route, inclusive = true)
                }
            )
        }
    }
}

@Composable
private fun EmailScreen(
    viewModel: SignInViewModel = hiltViewModel(),
    navigateToRoute: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {

        Text(stringResource(LR.string.wear_enter_email))

        val onNext = { navigateToRoute(AuthenticationRoutes.password) }
        val email by viewModel.email.observeAsState()
        FormField(
            value = email ?: "",
            onValueChange = { viewModel.updateEmail(it) },
            placeholder = "",
            label = { Text(stringResource(LR.string.profile_email)) },
            onNext = onNext,
            modifier = Modifier
                .padding(all = 8.dp)
        )

        Button(
            onClick = onNext,
            enabled = email?.isNotBlank() == true
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = stringResource(LR.string.profile_confirm),
                modifier = Modifier
                    .size(24.dp)
                    .wrapContentSize(align = Alignment.Center)
            )
        }
    }
}

@Composable
private fun PasswordScreen(
    viewModel: SignInViewModel = hiltViewModel(),
    navigateOnSignInSuccess: () -> Unit
) {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {

        val signInState by viewModel.signInState.observeAsState()
        if (signInState is SignInState.Success) {
            LaunchedEffect(Unit) {
                delay(1000)
                navigateOnSignInSuccess()
            }
        }

        val password by viewModel.password.observeAsState()
        val onNext = { viewModel.signIn() }
        FormField(
            value = password ?: "",
            onValueChange = { viewModel.updatePassword(it) },
            placeholder = "",
            label = { Text(stringResource(LR.string.profile_password)) },
            onNext = onNext
        )

        Button(
            onClick = onNext,
            enabled = password?.isNotBlank() == true
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = stringResource(LR.string.profile_confirm),
                modifier = Modifier
                    .size(24.dp)
                    .wrapContentSize(align = Alignment.Center)
            )
        }

        Text(
            text = signInState.toString(),
            fontSize = 6.sp
        )
    }
}

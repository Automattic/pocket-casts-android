package au.com.shiftyjelly.pocketcasts.wear.ui.authentication

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.compose.CallOnce

@Composable
fun LoginWithGoogleScreen(
    onError: () -> Unit,
    onGoogleNotAvailable: () -> Unit,
    viewModel: LoginWithGoogleViewModel = hiltViewModel(),
    successContent: @Composable (LoginWithGoogleViewModel.State.SignedInWithGoogle) -> Unit,
) {
    val state = viewModel.state.collectAsState().value
    val activity = LocalActivity.current

    CallOnce {
        activity?.let {
            viewModel.tryCredentialsManager(activity)
        } ?: onError()
    }

    when (state) {
        is LoginWithGoogleViewModel.State.Failed.GoogleLoginUnavailable -> onGoogleNotAvailable()
        is LoginWithGoogleViewModel.State.Failed.Other -> onError()
        is LoginWithGoogleViewModel.State.SignedInWithGoogle -> {
            successContent(state)
        }
        is LoginWithGoogleViewModel.State.Idle -> Unit
    }
}

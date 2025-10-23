package au.com.shiftyjelly.pocketcasts.wear.ui.authentication

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.compose.CallOnce

@Composable
fun LoginWithGoogleScreen(
    onError: (t: Throwable?) -> Unit,
    onGoogleNotAvailable: () -> Unit,
    onCancel: () -> Unit,
    viewModel: LoginWithGoogleViewModel = hiltViewModel(),
    successContent: @Composable (LoginWithGoogleViewModel.State.SignedInWithGoogle) -> Unit,
) {
    val state = viewModel.state.collectAsState().value
    val activity = LocalActivity.current

    CallOnce {
        activity?.let {
            viewModel.tryCredentialsManager(activity)
        } ?: onError(null)
    }

    when (state) {
        is LoginWithGoogleViewModel.State.Failed.GoogleLoginUnavailable -> onGoogleNotAvailable()
        is LoginWithGoogleViewModel.State.Failed.CredentialError -> onError(state.exception)
        is LoginWithGoogleViewModel.State.Failed.Other -> onError(null)
        is LoginWithGoogleViewModel.State.Failed.Cancelled -> onCancel()
        is LoginWithGoogleViewModel.State.SignedInWithGoogle -> {
            successContent(state)
        }
        is LoginWithGoogleViewModel.State.Idle -> Unit
    }
}

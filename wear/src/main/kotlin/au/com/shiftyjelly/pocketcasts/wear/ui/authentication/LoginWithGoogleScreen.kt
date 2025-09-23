package au.com.shiftyjelly.pocketcasts.wear.ui.authentication

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.compose.CallOnce

@Composable
fun LoginWithGoogleScreen(
    onError: () -> Unit,
    viewModel: LoginWithGoogleViewModel = hiltViewModel(),
    successContent: @Composable (LoginWithGoogleViewModel.State.SignedInWithGoogle) -> Unit,
) {
    val state = viewModel.state.collectAsState().value
    val context = LocalActivity.current

    CallOnce {
        context?.let {
            viewModel.tryCredentialsManager(context)
        } ?: onError()
    }

    when (state) {
        is LoginWithGoogleViewModel.State.Failed -> onError()
        is LoginWithGoogleViewModel.State.SignedInWithGoogle -> {
            successContent(state)
        }
        is LoginWithGoogleViewModel.State.Idle -> Unit
    }
}

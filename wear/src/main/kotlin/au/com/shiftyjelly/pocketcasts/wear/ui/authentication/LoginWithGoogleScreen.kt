package au.com.shiftyjelly.pocketcasts.wear.ui.authentication

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.compose.CallOnce

@Composable
fun LoginWithGoogleScreen(
    onSuccess: () -> Unit,
    onError: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: LoginWithGoogleViewModel = hiltViewModel()
) {
    val state = viewModel.state.collectAsState().value
    val context = LocalActivity.current

    CallOnce {
        context?.let {
            viewModel.tryCredentialsManager(context)
        } ?: onError()
    }

    LaunchedEffect(state) {
        when (state) {
            is LoginWithGoogleViewModel.State.Failed -> onError()
            is LoginWithGoogleViewModel.State.SignedInWithGoogle -> onSuccess()
            is LoginWithGoogleViewModel.State.Idle -> Unit
        }
    }
}
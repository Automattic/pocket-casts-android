package au.com.shiftyjelly.pocketcasts.wear.ui.authentication

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.wear.ui.LoggingInScreen
import com.google.android.horologist.auth.composables.screens.AuthErrorScreen
import com.google.android.horologist.auth.ui.googlesignin.signin.GoogleSignInScreen
import timber.log.Timber

@Composable
fun LoginWithGoogleScreen(
    onAuthSucceed: () -> Unit,
    onAuthCanceled: () -> Unit,
) {
    val viewModel = hiltViewModel<LoginWithGoogleScreenViewModel>()
    val state by viewModel.state.collectAsState()

    GoogleSignInScreen(
        viewModel = state.googleSignInViewModel,
        onAuthCancelled = {
            Timber.i("Google sign in cancelled")
            onAuthCanceled()
        },
        failedContent = { AuthErrorScreen() },
        content = { _ ->
            LoggingInScreen(
                avatarUrl = state.avatarUri?.toString(),
                name = state.firstName,
                onClose = onAuthSucceed
            )
        },
    )
}

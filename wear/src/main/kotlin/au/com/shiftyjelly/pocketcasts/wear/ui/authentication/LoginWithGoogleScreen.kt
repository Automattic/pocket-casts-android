package au.com.shiftyjelly.pocketcasts.wear.ui.authentication

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.horologist.auth.ui.googlesignin.signin.GoogleSignInScreen
import timber.log.Timber

@Composable
fun LoginWithGoogleScreen(
    onAuthSucceed: () -> Unit,
    onAuthCanceled: () -> Unit,
) {
    GoogleSignInScreen(
        viewModel = hiltViewModel<LoginWithGoogleScreenViewModel>(),
        onAuthCancelled = {
            Timber.i("Google sign in cancelled")
            onAuthCanceled()
        },
        onAuthSucceed = {
            Timber.i("Google sign in successful")
            onAuthSucceed()
        },
    )
}

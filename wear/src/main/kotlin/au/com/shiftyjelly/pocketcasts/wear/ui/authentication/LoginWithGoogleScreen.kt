package au.com.shiftyjelly.pocketcasts.wear.ui.authentication

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.hilt.navigation.compose.hiltViewModel
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.horologist.auth.composables.screens.AuthErrorScreen
import com.google.android.horologist.auth.ui.googlesignin.signin.GoogleSignInScreen
import timber.log.Timber

@Composable
fun LoginWithGoogleScreen(
    signInSuccessScreen: @Composable (GoogleSignInAccount?) -> Unit,
    onAuthCanceled: () -> Unit,
) {
    val viewModel = hiltViewModel<LoginWithGoogleScreenViewModel>()
    val state by viewModel.state.collectAsState()

    GoogleSignInScreen(
        viewModel = viewModel.googleSignInViewModel,
        onAuthCancelled = {
            Timber.i("Google sign in cancelled")
            onAuthCanceled()
        },
        failedContent = { AuthErrorScreen() },
        content = {
            signInSuccessScreen(state.googleSignInAccount)
        },
    )
}

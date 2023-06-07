package au.com.shiftyjelly.pocketcasts.wear.ui.authentication

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.utils.Network
import au.com.shiftyjelly.pocketcasts.wear.ui.component.ErrorScreen
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.horologist.auth.ui.googlesignin.signin.GoogleSignInScreen
import timber.log.Timber
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import com.google.android.horologist.auth.composables.R as HR

@Composable
fun LoginWithGoogleScreen(
    signInSuccessScreen: @Composable (GoogleSignInAccount?) -> Unit,
    onAuthCanceled: () -> Unit,
) {
    val viewModel = hiltViewModel<LoginWithGoogleScreenViewModel>()
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current

    CallOnce {
        // Allow the user to sign in with a different account
        viewModel.clearPreviousSignIn()
    }

    GoogleSignInScreen(
        viewModel = viewModel.googleSignInViewModel,
        onAuthCancelled = {
            Timber.i("Google sign in cancelled")
            onAuthCanceled()
        },
        failedContent = {
            val message = if (Network.isConnected(context)) {
                HR.string.horologist_auth_error_message
            } else {
                LR.string.log_in_no_network
            }
            ErrorScreen(stringResource(message))
        },
        content = {
            signInSuccessScreen(state.googleSignInAccount)
        },
    )
}

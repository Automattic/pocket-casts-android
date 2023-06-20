package au.com.shiftyjelly.pocketcasts.account.onboarding.components

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.account.viewmodel.GoogleSignInButtonViewModel
import au.com.shiftyjelly.pocketcasts.account.viewmodel.GoogleSignInState
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowOutlinedButton
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import au.com.shiftyjelly.pocketcasts.utils.Network
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

/**
 * Let the user sign into Pocket Casts with their Google account.
 * The One Tap for Android library is used. Sign in doesn't work when no Google accounts are set up on the device. In this case, fallback to the legacy Google Sign-In for Android.
 */
@Composable
fun ContinueWithGoogleButton(
    flow: OnboardingFlow?,
    fontSize: TextUnit? = null,
    includePadding: Boolean = true,
    onComplete: (GoogleSignInState) -> Unit,
) {

    val viewModel = hiltViewModel<GoogleSignInButtonViewModel>()
    val context = LocalContext.current

    val showContinueWithGoogleButton = GoogleSignInButtonViewModel.showContinueWithGoogleButton(context)
    if (!showContinueWithGoogleButton) return

    val errorMessage = if (!Network.isConnected(context)) {
        stringResource(LR.string.log_in_no_network)
    } else {
        stringResource(LR.string.onboarding_continue_with_google_error)
    }

    val showError = {
        Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show()
    }

    // request legacy Google Sign-In and process the result
    val googleLegacySignInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        viewModel.onGoogleLegacySignInResult(
            result = result,
            onSuccess = onComplete,
            onError = showError
        )
    }

    // request Google One Tap Sign-In and process the result
    val googleOneTapSignInLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartIntentSenderForResult()) { result ->
        viewModel.onGoogleOneTapSignInResult(
            result = result,
            onSuccess = onComplete,
            onError = {
                viewModel.startGoogleLegacySignIn(
                    onSuccess = { request -> googleLegacySignInLauncher.launch(request) },
                    onError = showError
                )
            }
        )
    }

    val onSignInClick = {
        viewModel.startGoogleOneTapSignIn(
            flow = flow,
            onSuccess = { request -> googleOneTapSignInLauncher.launch(request) },
            onError = {
                viewModel.startGoogleLegacySignIn(
                    onSuccess = { request -> googleLegacySignInLauncher.launch(request) },
                    onError = showError
                )
            }
        )
    }

    RowOutlinedButton(
        text = stringResource(LR.string.onboarding_continue_with_google),
        leadingIcon = painterResource(IR.drawable.google_g),
        tintIcon = false,
        border = BorderStroke(2.dp, MaterialTheme.theme.colors.primaryInteractive03),
        fontSize = fontSize,
        includePadding = includePadding,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.theme.colors.primaryText01),
        onClick = onSignInClick
    )
}

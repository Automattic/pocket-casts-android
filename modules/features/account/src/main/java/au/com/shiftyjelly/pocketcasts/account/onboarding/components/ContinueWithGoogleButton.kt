package au.com.shiftyjelly.pocketcasts.account.onboarding.components

import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.account.viewmodel.GoogleSignInButtonViewModel
import au.com.shiftyjelly.pocketcasts.account.viewmodel.GoogleSignInState
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowOutlinedButton
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
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
    onComplete: (GoogleSignInState, Subscription?) -> Unit,
    fontSize: TextUnit? = null,
    includePadding: Boolean = true,
    viewModel: GoogleSignInButtonViewModel = hiltViewModel(),
    event: AnalyticsEvent = AnalyticsEvent.SETUP_ACCOUNT_BUTTON_TAPPED,
    label: String = stringResource(LR.string.onboarding_continue_with_google),
) {
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

    val onSignInClick = {
        viewModel.startGoogleOneTapSignIn(
            flow = flow,
            onSuccess = onComplete,
            onError = showError,
            event = event,
        )
    }

    RowOutlinedButton(
        text = label,
        leadingIcon = painterResource(IR.drawable.google_g),
        tintIcon = false,
        border = BorderStroke(2.dp, MaterialTheme.theme.colors.primaryInteractive03),
        fontSize = fontSize,
        includePadding = includePadding,
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.theme.colors.primaryText01),
        onClick = onSignInClick,
    )
}

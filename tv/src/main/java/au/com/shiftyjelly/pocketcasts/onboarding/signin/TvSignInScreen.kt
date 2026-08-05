package au.com.shiftyjelly.pocketcasts.onboarding.signin

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.theme.TvButtonDefaults
import au.com.shiftyjelly.pocketcasts.theme.TvColors
import au.com.shiftyjelly.pocketcasts.theme.TvTextStyles
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvSignInScreen(
    onSignInComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TvSignInViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnSignInComplete by rememberUpdatedState(onSignInComplete)

    LaunchedEffect(uiState) {
        if (uiState is TvSignInUiState.Complete) {
            currentOnSignInComplete()
        }
    }

    when (val state = uiState) {
        is TvSignInUiState.Loading -> TvSignInLoading(modifier)

        is TvSignInUiState.Ready -> TvSignInContent(
            userCode = state.userCode,
            verificationUri = state.verificationUri,
            verificationUriComplete = state.verificationUriComplete,
            modifier = modifier,
        )

        is TvSignInUiState.Error -> TvSignInError(onRetry = viewModel::retry, modifier = modifier)

        is TvSignInUiState.Complete -> TvSignInLoading(modifier)
    }
}

@Composable
private fun TvSignInLoading(modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(TvColors.BackgroundSunken)
            .focusRequester(focusRequester)
            .focusable(),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(IR.drawable.ic_pocket_casts_logo),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(LR.string.tv_onboarding_sign_in_title),
                color = TvColors.TextPrimary,
                style = TvTextStyles.WelcomeTitle,
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun TvSignInError(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(TvColors.BackgroundSunken),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Image(
                painter = painterResource(IR.drawable.ic_pocket_casts_logo),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(LR.string.error_generic_message),
                color = TvColors.TextSecondary,
                style = TvTextStyles.SignInSubtitle,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onRetry,
                colors = TvButtonDefaults.filledButtonColors(),
                modifier = Modifier.focusRequester(focusRequester),
            ) {
                Text(text = stringResource(LR.string.retry))
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun TvSignInContent(
    userCode: List<String>,
    verificationUri: String,
    verificationUriComplete: String,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val steps = signInSteps(verificationUri)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(TvColors.BackgroundSunken)
            .focusRequester(focusRequester)
            .focusable(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(48.dp),
        ) {
            Text(
                text = stringResource(LR.string.tv_sign_in_title),
                color = TvColors.TextPrimary,
                style = TvTextStyles.WelcomeTitle,
            )
            TvSignInQrContent(
                userCode = userCode,
                verificationUriComplete = verificationUriComplete,
                steps = steps,
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun signInSteps(verificationUri: String): List<String> {
    val url = remember(verificationUri) { verificationDisplayUrl(verificationUri) }
    return listOf(
        stringResource(LR.string.tv_sign_in_step_scan, url),
        stringResource(LR.string.tv_sign_in_step_login),
        stringResource(LR.string.tv_sign_in_step_confirm_code),
    )
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvSignInScreenLoadingPreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            TvSignInLoading()
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvSignInScreenErrorPreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            TvSignInError(onRetry = {})
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvSignInScreenContentPreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            TvSignInContent(
                userCode = listOf("J", "M", "R", "3", "W", "2"),
                verificationUri = "https://pocketcasts.com/pair",
                verificationUriComplete = "https://pocketcasts.com/pair?code=JMR3W2",
            )
        }
    }
}

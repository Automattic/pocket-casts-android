package au.com.shiftyjelly.pocketcasts.onboarding.createaccount

import androidx.compose.foundation.Image
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.onboarding.signin.TvSignInQrContent
import au.com.shiftyjelly.pocketcasts.onboarding.signin.TvSignInUiState
import au.com.shiftyjelly.pocketcasts.onboarding.signin.verificationDisplayUrl
import au.com.shiftyjelly.pocketcasts.onboarding.tvOnboardingBackground
import au.com.shiftyjelly.pocketcasts.theme.TvButtonDefaults
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvCreateAccountScreen(
    onCreateAccountComplete: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TvCreateAccountViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnComplete by rememberUpdatedState(onCreateAccountComplete)

    CallOnce { viewModel.trackShown() }

    LaunchedEffect(uiState) {
        if (uiState is TvSignInUiState.Complete) {
            currentOnComplete()
        }
    }

    when (val state = uiState) {
        is TvSignInUiState.Loading, is TvSignInUiState.Complete -> TvCreateAccountLoading(modifier)

        is TvSignInUiState.Ready -> TvCreateAccountContent(
            userCode = state.userCode,
            verificationUri = state.verificationUri,
            verificationUriComplete = state.verificationUriComplete,
            modifier = modifier,
        )

        is TvSignInUiState.Error -> TvCreateAccountError(onRetry = viewModel::retry, modifier = modifier)
    }
}

@Composable
private fun TvCreateAccountLoading(modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .tvOnboardingBackground(MaterialTheme.tvColors.backgroundBase, MaterialTheme.tvColors.backgroundSunken)
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
                text = stringResource(LR.string.tv_create_account_title),
                color = MaterialTheme.tvColors.textPrimary,
                style = MaterialTheme.tvTypography.title3.copy(textAlign = TextAlign.Center),
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun TvCreateAccountError(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .tvOnboardingBackground(MaterialTheme.tvColors.backgroundBase, MaterialTheme.tvColors.backgroundSunken),
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
                color = MaterialTheme.tvColors.textSecondary,
                style = MaterialTheme.tvTypography.body.copy(textAlign = TextAlign.Center),
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
private fun TvCreateAccountContent(
    userCode: List<String>,
    verificationUri: String,
    verificationUriComplete: String,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val steps = createAccountSteps(verificationUri)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .tvOnboardingBackground(MaterialTheme.tvColors.backgroundBase, MaterialTheme.tvColors.backgroundSunken)
            .focusRequester(focusRequester)
            .focusable(),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(40.dp),
        ) {
            Text(
                text = stringResource(LR.string.tv_create_account_title),
                color = MaterialTheme.tvColors.textPrimary,
                style = MaterialTheme.tvTypography.title3.copy(textAlign = TextAlign.Center),
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
private fun createAccountSteps(verificationUri: String): List<String> {
    val url = remember(verificationUri) { verificationDisplayUrl(verificationUri) }
    return listOf(
        stringResource(LR.string.tv_sign_in_step_scan, url),
        stringResource(LR.string.tv_create_account_modal_step_create),
        stringResource(LR.string.tv_sign_in_step_confirm_code),
    )
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvCreateAccountScreenLoadingPreview() {
    TvTheme {
        TvCreateAccountLoading()
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvCreateAccountScreenErrorPreview() {
    TvTheme {
        TvCreateAccountError(onRetry = {})
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvCreateAccountScreenContentPreview() {
    TvTheme {
        TvCreateAccountContent(
            userCode = listOf("J", "M", "R", "3", "W", "2"),
            verificationUri = "https://pocketcasts.com/pair",
            verificationUriComplete = "https://pocketcasts.com/pair?code=JMR3W2",
        )
    }
}

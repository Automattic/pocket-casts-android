package au.com.shiftyjelly.pocketcasts.onboarding.createaccount

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.component.TvModal
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.onboarding.signin.TvSignInErrorContent
import au.com.shiftyjelly.pocketcasts.onboarding.signin.TvSignInQrContent
import au.com.shiftyjelly.pocketcasts.onboarding.signin.TvSignInUiState
import au.com.shiftyjelly.pocketcasts.onboarding.signin.verificationDisplayUrl
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvCreateAccountModal(
    uiState: TvSignInUiState,
    onRetry: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TvCreateAccountModalViewModel = hiltViewModel(),
) {
    CallOnce {
        viewModel.trackShown()
    }
    TvModal(
        onDismissRequest = onDismissRequest,
        width = ModalWidth,
        modifier = modifier,
    ) {
        Column(
            verticalArrangement = Arrangement.spacedBy(32.dp),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Header()
            when (uiState) {
                is TvSignInUiState.Ready -> ReadyContent(uiState)
                is TvSignInUiState.Error -> ErrorContent(onRetry = onRetry)
                is TvSignInUiState.Loading, is TvSignInUiState.Complete -> LoadingContent()
            }
        }
    }
}

@Composable
private fun Header(modifier: Modifier = Modifier) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp),
        modifier = modifier.fillMaxWidth(),
    ) {
        Text(
            text = stringResource(LR.string.tv_create_account_modal_title),
            color = MaterialTheme.tvColors.textPrimary,
            style = MaterialTheme.tvTypography.headline,
            textAlign = TextAlign.Start,
        )
        Text(
            text = stringResource(LR.string.tv_create_account_modal_subtitle),
            color = MaterialTheme.tvColors.textSecondary,
            style = MaterialTheme.tvTypography.body,
        )
    }
}

@Composable
private fun ReadyContent(state: TvSignInUiState.Ready, modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    val url = remember(state.verificationUri) { verificationDisplayUrl(state.verificationUri) }
    val steps = listOf(
        stringResource(LR.string.tv_sign_in_step_scan, url),
        stringResource(LR.string.tv_create_account_modal_step_create),
        stringResource(LR.string.tv_sign_in_step_confirm_code),
    )

    TvSignInQrContent(
        userCode = state.userCode,
        verificationUriComplete = state.verificationUriComplete,
        steps = steps,
        modifier = modifier
            .focusRequester(focusRequester)
            .focusable(),
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    val focusRequester = remember { FocusRequester() }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(ContentHeight)
            .focusRequester(focusRequester)
            .focusable(),
    ) {
        LoadingView(color = MaterialTheme.tvColors.textPrimary)
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun ErrorContent(
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    TvSignInErrorContent(
        onRetry = onRetry,
        modifier = modifier
            .fillMaxWidth()
            .height(ContentHeight),
    )
}

private val ModalWidth = 760.dp
private val ContentHeight = 220.dp

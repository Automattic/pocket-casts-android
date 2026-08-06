package au.com.shiftyjelly.pocketcasts.onboarding.createaccount

import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
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
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.component.TvModal
import au.com.shiftyjelly.pocketcasts.compose.loading.LoadingView
import au.com.shiftyjelly.pocketcasts.onboarding.signin.TvSignInQrContent
import au.com.shiftyjelly.pocketcasts.onboarding.signin.TvSignInUiState
import au.com.shiftyjelly.pocketcasts.onboarding.signin.verificationDisplayUrl
import au.com.shiftyjelly.pocketcasts.theme.TvButtonDefaults
import au.com.shiftyjelly.pocketcasts.theme.TvColors
import au.com.shiftyjelly.pocketcasts.theme.TvTextStyles
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvCreateAccountModal(
    uiState: TvSignInUiState,
    onRetry: () -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
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
            color = TvColors.TextPrimary,
            style = TvTextStyles.Headline,
            textAlign = TextAlign.Start,
        )
        Text(
            text = stringResource(LR.string.tv_create_account_modal_subtitle),
            color = TvColors.TextSecondary,
            style = TvTextStyles.Body,
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
            .height(LoadingHeight)
            .focusRequester(focusRequester)
            .focusable(),
    ) {
        LoadingView(color = TvColors.TextPrimary)
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
    val focusRequester = remember { FocusRequester() }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .fillMaxWidth()
            .height(LoadingHeight),
    ) {
        Text(
            text = stringResource(LR.string.error_generic_message),
            color = TvColors.TextSecondary,
            style = TvTextStyles.Body.copy(textAlign = TextAlign.Center),
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

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

private val ModalWidth = 760.dp
private val LoadingHeight = 220.dp

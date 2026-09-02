package au.com.shiftyjelly.pocketcasts.onboarding.signedout

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.theme.TvButtonDefaults
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvSignedOutScreen(
    onLogIn: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TvSignedOutViewModel = hiltViewModel(),
) {
    CallOnce { viewModel.trackShown() }

    TvSignedOutContent(
        onLogIn = {
            viewModel.logOut()
            onLogIn()
        },
        modifier = modifier,
    )
}

@Composable
private fun TvSignedOutContent(
    onLogIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.tvColors.backgroundSunken),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.padding(horizontal = 36.dp),
        ) {
            Image(
                painter = painterResource(IR.drawable.ic_pocket_casts_logo),
                contentDescription = null,
                modifier = Modifier.size(27.dp),
            )
            Text(
                text = stringResource(LR.string.tv_account_signed_out_alert_title),
                color = MaterialTheme.tvColors.textPrimary,
                style = MaterialTheme.tvTypography.title2.copy(textAlign = TextAlign.Center),
            )
            Text(
                text = stringResource(LR.string.tv_account_signed_out_alert_message),
                color = MaterialTheme.tvColors.textSecondary,
                style = MaterialTheme.tvTypography.body.copy(textAlign = TextAlign.Center),
            )
            Spacer(modifier = Modifier.height(12.dp))
            Button(
                onClick = onLogIn,
                colors = TvButtonDefaults.filledButtonColors(),
                modifier = Modifier.focusRequester(focusRequester),
            ) {
                Text(text = stringResource(LR.string.log_in))
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvSignedOutScreenPreview() {
    TvTheme {
        TvSignedOutContent(onLogIn = {})
    }
}

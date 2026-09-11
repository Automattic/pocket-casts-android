package au.com.shiftyjelly.pocketcasts.onboarding.welcome

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.graphics.Brush
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
fun TvWelcomeScreen(
    onSignIn: () -> Unit,
    onCreateAccount: () -> Unit,
    onContinueWithoutAccount: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: TvWelcomeViewModel = hiltViewModel(),
) {
    CallOnce { viewModel.trackShown() }

    TvWelcomeContent(
        onSignIn = {
            viewModel.trackSignInTapped()
            onSignIn()
        },
        onCreateAccount = {
            viewModel.trackCreateAccountTapped()
            onCreateAccount()
        },
        onContinueWithoutAccount = {
            viewModel.trackBrowseNoAccountTapped()
            onContinueWithoutAccount()
        },
        modifier = modifier,
    )
}

@Composable
private fun TvWelcomeContent(
    onSignIn: () -> Unit,
    onCreateAccount: () -> Unit,
    onContinueWithoutAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.tvColors.backgroundSunken),
    ) {
        TvAnimatedPodcastGrid(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter),
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colorStops = arrayOf(
                            0f to MaterialTheme.tvColors.backgroundSunken.copy(alpha = 0.5f),
                            0.15f to MaterialTheme.tvColors.backgroundSunken.copy(alpha = 0.5f),
                            0.40f to MaterialTheme.tvColors.backgroundSunken,
                            1f to MaterialTheme.tvColors.backgroundSunken,
                        ),
                    ),
                ),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 27.dp),
        ) {
            Image(
                painter = painterResource(IR.drawable.ic_pocket_casts_logo),
                contentDescription = null,
                modifier = Modifier.size(40.dp),
            )
            Spacer(modifier = Modifier.height(20.dp))
            Text(
                text = stringResource(LR.string.tv_onboarding_welcome_tv),
                color = MaterialTheme.tvColors.textPrimary,
                style = MaterialTheme.tvTypography.title1.copy(textAlign = TextAlign.Center),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(LR.string.tv_onboarding_subtitle),
                color = MaterialTheme.tvColors.textSecondary,
                style = MaterialTheme.tvTypography.headline.copy(textAlign = TextAlign.Center),
            )
            Spacer(modifier = Modifier.height(20.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onSignIn,
                    colors = TvButtonDefaults.filledButtonColors(),
                    modifier = Modifier.focusRequester(focusRequester),
                ) {
                    Text(text = stringResource(LR.string.log_in))
                }
                Button(
                    onClick = onCreateAccount,
                    colors = TvButtonDefaults.filledButtonColors(),
                ) {
                    Text(text = stringResource(LR.string.tv_onboarding_create_free_account))
                }
            }
        }

        Button(
            onClick = onContinueWithoutAccount,
            colors = TvButtonDefaults.borderlessButtonColors(),
            border = TvButtonDefaults.borderlessButtonBorder(),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 27.dp),
        ) {
            Text(text = stringResource(LR.string.tv_onboarding_browse_without_account))
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvWelcomeScreenPreview() {
    TvTheme {
        TvWelcomeContent(
            onSignIn = {},
            onCreateAccount = {},
            onContinueWithoutAccount = {},
        )
    }
}

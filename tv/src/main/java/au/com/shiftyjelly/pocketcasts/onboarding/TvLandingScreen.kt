package au.com.shiftyjelly.pocketcasts.onboarding

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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.OutlinedButton
import androidx.tv.material3.OutlinedButtonDefaults
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.theme.TvColors
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvLandingScreen(
    onSignIn: () -> Unit,
    onCreateAccount: () -> Unit,
    onContinueWithoutAccount: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(TvColors.Dark)
            .padding(horizontal = 48.dp, vertical = 27.dp),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                painter = painterResource(IR.drawable.ic_pocket_casts_logo),
                contentDescription = null,
                modifier = Modifier.size(80.dp),
            )
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(LR.string.tv_onboarding_welcome),
                color = Color.White,
                fontSize = 32.sp,
            )
            Spacer(modifier = Modifier.height(48.dp))
            Button(
                onClick = onSignIn,
                colors = ButtonDefaults.colors(
                    containerColor = Color.White,
                    contentColor = TvColors.Dark,
                    focusedContainerColor = Color.White,
                    focusedContentColor = TvColors.Dark,
                ),
                modifier = Modifier
                    .widthIn(min = 280.dp)
                    .focusRequester(focusRequester),
            ) {
                Text(text = stringResource(LR.string.sign_in))
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onCreateAccount,
                colors = OutlinedButtonDefaults.colors(
                    containerColor = Color.Transparent,
                    contentColor = Color.White,
                    focusedContainerColor = Color.White,
                    focusedContentColor = TvColors.Dark,
                ),
                modifier = Modifier.widthIn(min = 280.dp),
            ) {
                Text(text = stringResource(LR.string.create_account))
            }
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = onContinueWithoutAccount,
                colors = OutlinedButtonDefaults.colors(
                    containerColor = Color.Transparent,
                    contentColor = TvColors.TextSecondary,
                    focusedContainerColor = Color.White,
                    focusedContentColor = TvColors.Dark,
                ),
                modifier = Modifier.widthIn(min = 280.dp),
            ) {
                Text(text = stringResource(LR.string.tv_onboarding_continue_without_account))
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvLandingScreenPreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            TvLandingScreen(
                onSignIn = {},
                onCreateAccount = {},
                onContinueWithoutAccount = {},
            )
        }
    }
}

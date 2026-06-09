package au.com.shiftyjelly.pocketcasts.onboarding.createaccount

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.tv.material3.Button
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.BuildConfig
import au.com.shiftyjelly.pocketcasts.component.rememberQrPainter
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.theme.TvButtonDefaults
import au.com.shiftyjelly.pocketcasts.theme.TvColors
import au.com.shiftyjelly.pocketcasts.theme.TvTextStyles
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun TvCreateAccountScreen(
    onSignIn: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val createAccountUrl = remember { "https://${BuildConfig.WEB_BASE_HOST}/create" }
    val qrPainter = rememberQrPainter(content = createAccountUrl, size = 160.dp)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(TvColors.Dark),
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Image(
                painter = painterResource(IR.drawable.ic_pocket_casts_logo),
                contentDescription = null,
                modifier = Modifier.size(36.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(LR.string.tv_create_account_title),
                color = Color.White,
                style = TvTextStyles.WelcomeTitle,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(LR.string.tv_create_account_subtitle),
                color = TvColors.TextSecondary,
                style = TvTextStyles.SignInSubtitle,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(4.dp))
                    .padding(4.dp),
            ) {
                Image(
                    painter = qrPainter,
                    contentDescription = stringResource(LR.string.tv_create_account_subtitle),
                    modifier = Modifier.size(160.dp),
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Text(
                text = stringResource(LR.string.tv_create_account_come_back),
                color = TvColors.TextSecondary,
                style = TvTextStyles.SignInSubtitle,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onSignIn,
                colors = TvButtonDefaults.prominentButtonColors(),
                modifier = Modifier.focusRequester(focusRequester),
            ) {
                Text(text = stringResource(LR.string.sign_in))
            }
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvCreateAccountScreenPreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            TvCreateAccountScreen(onSignIn = {})
        }
    }
}

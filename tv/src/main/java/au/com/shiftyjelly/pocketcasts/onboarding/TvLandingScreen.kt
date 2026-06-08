package au.com.shiftyjelly.pocketcasts.onboarding

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Button
import androidx.tv.material3.ButtonDefaults
import androidx.tv.material3.MaterialTheme
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
        modifier = modifier
            .fillMaxSize()
            .background(TvColors.Dark),
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
                            0f to TvColors.Dark.copy(alpha = 0.5f),
                            0.17f to TvColors.Dark.copy(alpha = 0.5f),
                            0.45f to TvColors.Dark,
                            1f to TvColors.Dark,
                        ),
                    ),
                ),
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 48.dp, vertical = 27.dp),
        ) {
            Spacer(modifier = Modifier.weight(0.45f))

            Image(
                painter = painterResource(IR.drawable.ic_pocket_casts_logo),
                contentDescription = null,
                modifier = Modifier.size(72.dp),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(LR.string.tv_onboarding_welcome_tv),
                color = Color.White,
                fontSize = 32.sp,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(LR.string.tv_onboarding_subtitle),
                color = TvColors.TextSecondary,
                fontSize = 18.sp,
            )
            Spacer(modifier = Modifier.height(32.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = onSignIn,
                    colors = ButtonDefaults.colors(
                        containerColor = TvColors.DarkGray,
                        contentColor = Color.White,
                        focusedContainerColor = Color.White,
                        focusedContentColor = TvColors.Dark,
                    ),
                    modifier = Modifier.focusRequester(focusRequester),
                ) {
                    Text(text = stringResource(LR.string.sign_in))
                }
                Button(
                    onClick = onCreateAccount,
                    colors = ButtonDefaults.colors(
                        containerColor = TvColors.DarkGray,
                        contentColor = Color.White,
                        focusedContainerColor = Color.White,
                        focusedContentColor = TvColors.Dark,
                    ),
                ) {
                    Text(text = stringResource(LR.string.tv_onboarding_create_free_account))
                }
            }

            Spacer(modifier = Modifier.weight(0.55f))

            Button(
                onClick = onContinueWithoutAccount,
                colors = ButtonDefaults.colors(
                    containerColor = Color.Transparent,
                    contentColor = TvColors.TextSecondary,
                    focusedContainerColor = Color.White.copy(alpha = 0.1f),
                    focusedContentColor = Color.White,
                ),
                border = ButtonDefaults.border(
                    border = Border.None,
                    focusedBorder = Border.None,
                ),
            ) {
                Text(text = stringResource(LR.string.tv_onboarding_browse_without_account))
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

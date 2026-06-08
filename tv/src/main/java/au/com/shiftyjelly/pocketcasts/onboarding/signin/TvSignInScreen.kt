package au.com.shiftyjelly.pocketcasts.onboarding.signin

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.component.rememberQrPainter
import au.com.shiftyjelly.pocketcasts.theme.TvColors
import au.com.shiftyjelly.pocketcasts.theme.TvTextStyles
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import kotlinx.coroutines.delay
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private const val MOCK_QR_URL = "https://pocketcasts.com/pair?code=JMR3W2"
private val MOCK_CODE_CHARACTERS = listOf("J", "M", "R", "3", "W", "2")
private const val MOCK_SIGN_IN_DELAY_MS = 15_000L

@Composable
fun TvSignInScreen(
    onSignInComplete: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val focusRequester = remember { FocusRequester() }
    val currentOnSignInComplete by rememberUpdatedState(onSignInComplete)
    val qrPainter = rememberQrPainter(content = MOCK_QR_URL, size = 148.dp)
    val url = stringResource(LR.string.tv_sign_in_url)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxSize()
            .background(TvColors.Dark)
            .focusRequester(focusRequester)
            .focusable(),
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
                text = stringResource(LR.string.tv_onboarding_sign_in_title),
                color = Color.White,
                style = TvTextStyles.WelcomeTitle,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = stringResource(LR.string.tv_onboarding_sign_in_instructions),
                color = TvColors.TextSecondary,
                style = TvTextStyles.WelcomeSubtitle,
            )
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .background(Color.White, RoundedCornerShape(8.dp))
                    .padding(8.dp),
            ) {
                Image(
                    painter = qrPainter,
                    contentDescription = stringResource(LR.string.tv_onboarding_sign_in_title),
                    modifier = Modifier.size(148.dp),
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .width(377.dp)
                    .height(1.dp)
                    .background(TvColors.Gray),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(LR.string.tv_sign_in_or_enter_code),
                color = TvColors.TextSecondary,
                style = TvTextStyles.WelcomeSubtitle,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MOCK_CODE_CHARACTERS.forEach { char ->
                    CodeCharacterBox(character = char)
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            val formattedGoToUrl = stringResource(LR.string.tv_sign_in_go_to_url, url)
            val urlStart = formattedGoToUrl.indexOf(url)
            Text(
                text = buildAnnotatedString {
                    append(formattedGoToUrl)
                    addStyle(SpanStyle(color = TvColors.TextSecondary), 0, formattedGoToUrl.length)
                    if (urlStart >= 0) {
                        addStyle(
                            SpanStyle(color = Color.White, fontWeight = FontWeight.Bold),
                            urlStart,
                            urlStart + url.length,
                        )
                    }
                },
                style = TvTextStyles.WelcomeSubtitle,
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    LaunchedEffect(Unit) {
        delay(MOCK_SIGN_IN_DELAY_MS)
        currentOnSignInComplete()
    }
}

@Composable
private fun CodeCharacterBox(
    character: String,
    modifier: Modifier = Modifier,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .size(width = 48.dp, height = 56.dp)
            .background(TvColors.Gray, RoundedCornerShape(8.dp)),
    ) {
        Text(
            text = character,
            color = Color.White,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvSignInScreenPreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            TvSignInScreen(onSignInComplete = {})
        }
    }
}

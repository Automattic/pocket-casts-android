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
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.component.rememberQrPainter
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
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

        is TvSignInUiState.Error -> TvSignInLoading(modifier)

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
            .background(TvColors.Dark)
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
                color = Color.White,
                style = TvTextStyles.WelcomeTitle,
            )
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
    val qrPainter = rememberQrPainter(content = verificationUriComplete, size = 118.dp)
    val url = remember(verificationUri) {
        verificationUri
            .removePrefix("https://")
            .removePrefix("http://")
            .trimEnd('/')
    }

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
                    contentDescription = stringResource(LR.string.tv_onboarding_sign_in_instructions),
                    modifier = Modifier.size(118.dp),
                )
            }
            Spacer(modifier = Modifier.height(24.dp))
            Box(
                modifier = Modifier
                    .width(377.dp)
                    .height(0.5.dp)
                    .background(TvColors.Divider),
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = stringResource(LR.string.tv_sign_in_or_enter_code),
                color = TvColors.TextSecondary,
                style = TvTextStyles.SignInSubtitle,
            )
            Spacer(modifier = Modifier.height(16.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                userCode.forEach { char ->
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
                style = TvTextStyles.SignInSubtitle,
            )
        }
    }

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
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
            .background(TvColors.BgActive20, RoundedCornerShape(8.dp)),
    ) {
        Text(
            text = character,
            color = TvColors.TextSecondary,
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
        )
    }
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

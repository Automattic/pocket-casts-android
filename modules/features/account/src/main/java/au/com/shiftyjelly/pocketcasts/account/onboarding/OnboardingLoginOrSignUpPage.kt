package au.com.shiftyjelly.pocketcasts.account.onboarding

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.account.viewmodel.OnboardingLoginOrSignUpViewModel
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowOutlinedButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowTextButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.images.HorizontalLogo
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun OnboardingLoginOrSignUpPage(
    onNotNowClicked: () -> Unit,
    onSignUpFreeClicked: () -> Unit,
    onLoginClicked: () -> Unit,
    onContinueWithGoogleClicked: () -> Unit,
    onShown: () -> Unit,
    viewModel: OnboardingLoginOrSignUpViewModel = hiltViewModel()
) {

    LaunchedEffect(Unit) {
        onShown()
    }

    Column {

        Row(
            Modifier
                .padding(vertical = 12.dp, horizontal = 16.dp)
                .fillMaxWidth()
        ) {
            Spacer(Modifier.weight(1f))

            HorizontalLogo(
                modifier = Modifier
                    .align(Alignment.CenterVertically)
                    .height(28.dp)
            )

            Box(Modifier.weight(1f)) {
                TextH30(
                    text = stringResource(LR.string.not_now),
                    textAlign = TextAlign.End,
                    modifier = Modifier
                        .clickable { onNotNowClicked() }
                        .padding(all = 4.dp)
                        .align(Alignment.CenterEnd)
                )
            }
        }

        Column(
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            Box(
                modifier = Modifier
                    .aspectRatio(1.2f)
                    .fillMaxWidth()
                    .background(Color.Gray)
            )

            Spacer(Modifier.height(8.dp))

            TextH10(
                text = stringResource(LR.string.onboarding_discover_your_next_favorite_podcast),
                modifier = Modifier
                    .padding(horizontal = 24.dp),
                textAlign = TextAlign.Center,
                maxLines = 2
            )

            Spacer(Modifier.height(8.dp))

            TextH40(
                text = stringResource(LR.string.onboarding_create_an_account_to),
                modifier = Modifier.padding(horizontal = 24.dp),
                textAlign = TextAlign.Center
            )

            if (viewModel.showContinueWithGoogleButton) {
                Spacer(Modifier.height(8.dp))
                ContinueWithGoogleButton(onClick = onContinueWithGoogleClicked)
            } else {
                Spacer(Modifier.height(32.dp))
            }
            SignUpButton(onClick = onSignUpFreeClicked)
            LogInButton(onClick = onLoginClicked)
        }
    }
}

@Composable
private fun ContinueWithGoogleButton(onClick: () -> Unit) {
    RowOutlinedButton(
        text = stringResource(LR.string.onboarding_continue_with_google),
        leadingIcon = painterResource(IR.drawable.google_g),
        tintIcon = false,
        border = BorderStroke(2.dp, MaterialTheme.theme.colors.primaryInteractive03),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.theme.colors.primaryText01),
        onClick = onClick
    )
}

@Composable
private fun SignUpButton(onClick: () -> Unit) {
    RowButton(
        text = stringResource(LR.string.onboarding_sign_up_free),
        colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.theme.colors.primaryText01, contentColor = MaterialTheme.theme.colors.primaryUi01),
        includePadding = false,
        onClick = onClick,
        modifier = Modifier.padding(horizontal = 16.dp)
    )
}

@Composable
private fun LogInButton(onClick: () -> Unit) {
    RowTextButton(
        text = stringResource(LR.string.log_in),
        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.theme.colors.primaryText01),
        includePadding = false,
        onClick = onClick,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, end = 16.dp, bottom = 16.dp)
    )
}

@Preview(showBackground = true)
@Composable
private fun RowOutlinedButtonPreview(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppThemeWithBackground(themeType) {
        OnboardingLoginOrSignUpPage(
            onNotNowClicked = {},
            onSignUpFreeClicked = {},
            onLoginClicked = {},
            onContinueWithGoogleClicked = {},
            onShown = {}
        )
    }
}

package au.com.shiftyjelly.pocketcasts.account.onboarding

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
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.buttons.OutlinedRowButton
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.images.HorizontalLogo
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
internal fun OnboardingLoginOrSignUpPage(
    onNotNowClicked: () -> Unit,
    onSignUpFreeClicked: () -> Unit,
    onLoginClicked: () -> Unit,
    onContinueWithGoogleClicked: () -> Unit,
    onShown: () -> Unit
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

            TextH30(
                text = stringResource(LR.string.not_now),
                textAlign = TextAlign.End,
                modifier = Modifier
                    .weight(1f)
                    .clickable { onNotNowClicked() }
            )
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

            Spacer(Modifier.height(8.dp))

            OutlinedRowButton(
                text = stringResource(LR.string.onboarding_continue_with_google),
                leadingIcon = IR.drawable.google_g,
                onClick = onContinueWithGoogleClicked
            )

            RowButton(
                text = stringResource(LR.string.onboarding_sign_up_free),
                onClick = onSignUpFreeClicked
            )

            OutlinedRowButton(
                text = stringResource(LR.string.log_in),
                onClick = onLoginClicked
            )
        }
    }
}

@Preview
@Composable
private fun Preview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
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

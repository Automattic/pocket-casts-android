package au.com.shiftyjelly.pocketcasts.account.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun OnboardingForgotPassword(
    onShown: () -> Unit,
    onBackPressed: () -> Unit,
) {

    LaunchedEffect(Unit) { onShown() }
    BackHandler { onBackPressed() }

    Column {
        ThemedTopAppBar(
            title = stringResource(LR.string.profile_reset_password),
            onNavigationClick = onBackPressed
        )
    }
}

@Preview
@Composable
fun OnboardingForgotPasswordPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        OnboardingForgotPassword(
            onShown = {},
            onBackPressed = {}
        )
    }
}

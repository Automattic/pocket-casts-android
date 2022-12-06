package au.com.shiftyjelly.pocketcasts.account.onboarding.import

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun OnboardingImportOtherApps(
    onBackPressed: () -> Unit,
) {
    Column {

        ThemedTopAppBar(
            onNavigationClick = onBackPressed,
        )

        Column(Modifier.padding(horizontal = 24.dp)) {

            Image(
                painter = painterResource(IR.drawable.other_apps),
                contentDescription = null,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            TextH10(stringResource(LR.string.onboarding_import_from_other_apps))

            Spacer(Modifier.height(16.dp))
            TextP40(stringResource(LR.string.onboarding_can_import_from_opml))

            Spacer(Modifier.height(16.dp))
            NumberedList(
                stringResource(LR.string.onboarding_import_from_other_apps_step_1),
                stringResource(LR.string.onboarding_import_from_other_apps_step_2),
            )
        }
        Spacer(Modifier.weight(1f))
    }
}

@Preview
@Composable
private fun OnboardingImportOtherAppsPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        OnboardingImportOtherApps(
            onBackPressed = {},
        )
    }
}

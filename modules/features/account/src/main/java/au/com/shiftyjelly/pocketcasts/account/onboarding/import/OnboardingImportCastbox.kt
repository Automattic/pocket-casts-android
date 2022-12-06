package au.com.shiftyjelly.pocketcasts.account.onboarding.import

import android.content.ActivityNotFoundException
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.buttons.RowButton
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun OnboardingImportCastbox(
    onBackPressed: () -> Unit
) {
    Column {

        ThemedTopAppBar(
            onNavigationClick = onBackPressed,
        )

        Column(Modifier.padding(horizontal = 24.dp)) {

            Image(
                painter = painterResource(IR.drawable.castbox),
                contentDescription = null,
                modifier = Modifier.padding(vertical = 16.dp)
            )

            TextH10(stringResource(LR.string.onboarding_import_from_castbox))

            Spacer(Modifier.height(16.dp))
            NumberedList(
                stringResource(LR.string.onboarding_import_from_castbox_step_1),
                stringResource(LR.string.onboarding_import_from_castbox_step_2),
                stringResource(LR.string.onboarding_import_from_castbox_step_3),
                stringResource(LR.string.onboarding_import_from_castbox_step_4),
                stringResource(LR.string.onboarding_import_from_castbox_step_5),
            )
        }

        Spacer(Modifier.weight(1f))

        val openCastbox = openCastboxFun()
        // only show button if we can open the Castbox app
        if (openCastbox != null) {
            RowButton(
                text = stringResource(LR.string.onboarding_import_from_castbox_open),
                onClick = openCastbox,
            )
        }
    }
}

@Composable
private fun openCastboxFun(): (() -> Unit)? {
    val context = LocalContext.current
    return context
        .packageManager
        .getLaunchIntentForPackage("fm.castbox.audiobook.radio.podcast")
        ?.let { intent ->
            {
                try {
                    context.startActivity(intent)
                } catch (e: ActivityNotFoundException) {
                    // should only happen if the user uninstalls castbox after this screen is composed
                }
            }
        }
}

@Preview
@Composable
fun OnboardingImportCastboxPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        OnboardingImportCastbox(
            onBackPressed = {}
        )
    }
}

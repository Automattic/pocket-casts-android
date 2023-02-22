package au.com.shiftyjelly.pocketcasts.account.onboarding.import

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun OnboardingImportStartPage(
    theme: Theme.ThemeType,
    onShown: () -> Unit,
    onCastboxClicked: () -> Unit,
    onOtherAppsClicked: () -> Unit,
    onBackPressed: () -> Unit,
) {
    val systemUiController = rememberSystemUiController()
    val pocketCastsTheme = MaterialTheme.theme

    CallOnce {
        onShown()
    }

    LaunchedEffect(Unit) {
        systemUiController.apply {
            // Use secondaryUI01 so the status bar matches the ThemedTopAppBar
            setStatusBarColor(pocketCastsTheme.colors.secondaryUi01, darkIcons = !theme.defaultLightIcons)
            setNavigationBarColor(Color.Transparent, darkIcons = !theme.darkTheme)
        }
    }

    Column {

        Column(
            Modifier
                .fillMaxHeight()
                .verticalScroll(rememberScrollState())
        ) {

            Spacer(Modifier.windowInsetsPadding(WindowInsets.statusBars))

            ThemedTopAppBar(
                onNavigationClick = onBackPressed,
            )

            Spacer(Modifier.height(12.dp))
            TextH10(
                text = stringResource(LR.string.onboarding_bring_your_podcasts),
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(Modifier.height(8.dp))
            TextP40(
                text = stringResource(LR.string.onboarding_coming_from_another_app),
                modifier = Modifier.padding(horizontal = 24.dp)
            )

            Spacer(Modifier.height(24.dp))
            ImportRow(
                text = stringResource(LR.string.onboarding_import_from_castbox),
                iconRes = IR.drawable.castbox,
                onClick = onCastboxClicked,
            )

            ImportRow(
                text = stringResource(LR.string.onboarding_import_from_other_apps),
                iconRes = IR.drawable.other_apps,
                onClick = onOtherAppsClicked,
            )

            Spacer(Modifier.weight(1f))
            Spacer(Modifier.windowInsetsPadding(WindowInsets.navigationBars))
        }
    }
}

@Composable
private fun ImportRow(
    text: String,
    @DrawableRes iconRes: Int,
    onClick: () -> Unit,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 8.dp, horizontal = 24.dp)
    ) {
        Image(
            painter = painterResource(iconRes),
            contentDescription = null,
        )
        Spacer(Modifier.width(16.dp))
        TextH40(text)
    }
}

@Preview
@Composable
private fun OnboardingImportStartPagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        OnboardingImportStartPage(
            theme = themeType,
            onShown = {},
            onCastboxClicked = {},
            onOtherAppsClicked = {},
            onBackPressed = {},
        )
    }
}

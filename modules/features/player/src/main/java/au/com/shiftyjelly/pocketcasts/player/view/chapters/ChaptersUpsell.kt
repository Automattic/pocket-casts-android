package au.com.shiftyjelly.pocketcasts.player.view.chapters

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.NoContentBanner
import au.com.shiftyjelly.pocketcasts.compose.components.NoContentBannerColors
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ChaptersUpsell(
    onUpgradeClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val chaptersTheme = LocalChaptersTheme.current
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .background(chaptersTheme.background)
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(vertical = 32.dp),
    ) {
        NoContentBanner(
            title = stringResource(LR.string.chapters_generated_upsell_title),
            body = stringResource(LR.string.chapters_generated_upsell_message),
            iconResourceId = IR.drawable.ic_book_open,
            primaryButtonText = stringResource(LR.string.chapters_generated_upsell_button),
            onPrimaryButtonClick = onUpgradeClick,
            colors = NoContentBannerColors.default(MaterialTheme.theme.colors).copy(
                primaryText = chaptersTheme.headerToggle,
                secondaryText = chaptersTheme.headerTitle,
                icon = chaptersTheme.headerToggle,
                button = chaptersTheme.headerToggle,
                buttonText = chaptersTheme.background,
            ),
        )
    }
}

@Preview(name = "Light")
@Composable
private fun ChaptersUpsellLightPreview() = ChaptersUpsellPreview(Theme.ThemeType.LIGHT)

@Preview(name = "Dark")
@Composable
private fun ChaptersUpsellDarkPreview() = ChaptersUpsellPreview(Theme.ThemeType.DARK)

@Composable
private fun ChaptersUpsellPreview(theme: Theme.ThemeType) {
    AppThemeWithBackground(theme) {
        ChaptersTheme {
            ChaptersUpsell(onUpgradeClick = {})
        }
    }
}

package au.com.shiftyjelly.pocketcasts.player.view.chapters

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionIconForTier
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import com.airbnb.android.showkase.annotation.ShowkaseComposable
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ChaptersHeader(
    totalChaptersCount: Int,
    hiddenChaptersCount: Int,
    onSkipChaptersClick: (Boolean) -> Unit,
    isTogglingChapters: Boolean,
    showSubscriptionIcon: Boolean,
) {
    HeaderRow(
        text = getHeaderTitle(totalChaptersCount, hiddenChaptersCount),
        toggle = TextToggle(
            checked = isTogglingChapters,
            text = if (isTogglingChapters) {
                stringResource(LR.string.done)
            } else {
                stringResource(LR.string.skip_chapters)
            },
        ),
        showSubscriptionIcon = showSubscriptionIcon,
        onClick = { onSkipChaptersClick(!isTogglingChapters) },
    )
    Divider(
        color = LocalChaptersTheme.current.divider,
        thickness = 1.dp,
    )
}

@Composable
private fun HeaderRow(
    text: String,
    modifier: Modifier = Modifier,
    toggle: TextToggle,
    showSubscriptionIcon: Boolean,
    onClick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .padding(
                start = 20.dp,
                end = 4.dp,
            ),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f),
        ) {
            TextH50(
                text = text,
                modifier = Modifier
                    .padding(vertical = 16.dp),
                color = LocalChaptersTheme.current.headerTitle,
            )
        }

        Spacer(Modifier.width(12.dp))

        TextButton(
            text = toggle.text,
            showSubscriptionIcon = showSubscriptionIcon,
            onClick = onClick,
        )
    }
}

@Composable
private fun TextButton(
    text: String,
    showSubscriptionIcon: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val screenWidth = LocalConfiguration.current.screenWidthDp.dp
    Row(
        modifier = modifier
            .clickable { onClick() }
            .widthIn(max = screenWidth / 2)
            .padding(horizontal = 12.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextH50(
            text = text,
            textAlign = TextAlign.End,
            color = LocalChaptersTheme.current.headerToggle,
        )

        if (showSubscriptionIcon) {
            Spacer(modifier = Modifier.width(8.dp))
            SubscriptionIconForTier(SubscriptionTier.fromFeatureTier(Feature.DESELECT_CHAPTERS))
        }
    }
}

@Composable
private fun getHeaderTitle(
    chaptersTotalCount: Int,
    unselectedChaptersCount: Int,
) = if (unselectedChaptersCount > 0) {
    if (chaptersTotalCount > 1) {
        stringResource(LR.string.number_of_chapters_summary_plural, chaptersTotalCount, unselectedChaptersCount)
    } else {
        stringResource(LR.string.number_of_chapters_summary_singular, unselectedChaptersCount)
    }
} else {
    if (chaptersTotalCount > 1) {
        stringResource(LR.string.number_of_chapters, chaptersTotalCount)
    } else {
        stringResource(LR.string.single_chapter)
    }
}

data class TextToggle(
    val checked: Boolean,
    val text: String,
)

@ShowkaseComposable(name = "ChaptersHeader", group = "Chapter", styleName = "Light")
@Preview(name = "Light")
@Composable
fun ChaptersHeaderLightPreview() = ChaptersHeaderPreview(Theme.ThemeType.LIGHT)

@ShowkaseComposable(name = "ChaptersHeader", group = "Chapter", styleName = "Dark")
@Preview(name = "Dark")
@Composable
fun ChaptersHeaderDarkPreview() = ChaptersHeaderPreview(Theme.ThemeType.DARK)

@ShowkaseComposable(name = "ChaptersHeader", group = "Chapter", styleName = "Rose")
@Preview(name = "Rose")
@Composable
fun ChaptersHeaderRosePreview() = ChaptersHeaderPreview(Theme.ThemeType.ROSE)

@ShowkaseComposable(name = "ChaptersHeader", group = "Chapter", styleName = "Indigo")
@Preview(name = "Indigo")
@Composable
fun ChaptersHeaderIndigoPreview() = ChaptersHeaderPreview(Theme.ThemeType.INDIGO)

@ShowkaseComposable(name = "ChaptersHeader", group = "Chapter", styleName = "ExtraDark")
@Preview(name = "ExtraDark")
@Composable
fun ChaptersHeaderExtraDarkPreview() = ChaptersHeaderPreview(Theme.ThemeType.EXTRA_DARK)

@ShowkaseComposable(name = "ChaptersHeader", group = "Chapter", styleName = "DarkContrast")
@Preview(name = "DarkContrast")
@Composable
fun ChaptersHeaderDarkContrastPreview() = ChaptersHeaderPreview(Theme.ThemeType.DARK_CONTRAST)

@ShowkaseComposable(name = "ChaptersHeader", group = "Chapter", styleName = "LightContrast")
@Preview(name = "LightContrast")
@Composable
fun ChaptersHeaderLightContrastPreview() = ChaptersHeaderPreview(Theme.ThemeType.LIGHT_CONTRAST)

@ShowkaseComposable(name = "ChaptersHeader", group = "Chapter", styleName = "Electric")
@Preview(name = "Electric")
@Composable
fun ChaptersHeaderElectricPreview() = ChaptersHeaderPreview(Theme.ThemeType.ELECTRIC)

@ShowkaseComposable(name = "ChaptersHeader", group = "Chapter", styleName = "Classic")
@Preview(name = "Classic")
@Composable
fun ChaptersHeaderClassicPreview() = ChaptersHeaderPreview(Theme.ThemeType.CLASSIC_LIGHT)

@ShowkaseComposable(name = "ChaptersHeader", group = "Chapter", styleName = "Radioactive")
@Preview(name = "Radioactive")
@Composable
fun ChaptersHeaderRadioactivePreview() = ChaptersHeaderPreview(Theme.ThemeType.RADIOACTIVE)

@Composable
private fun ChaptersHeaderPreview(theme: Theme.ThemeType) {
    AppThemeWithBackground(theme) {
        ChaptersTheme {
            Column {
                ChaptersHeader(
                    totalChaptersCount = 5,
                    hiddenChaptersCount = 2,
                    onSkipChaptersClick = {},
                    isTogglingChapters = false,
                    showSubscriptionIcon = true,
                )
                ChaptersHeader(
                    totalChaptersCount = 5,
                    hiddenChaptersCount = 0,
                    onSkipChaptersClick = {},
                    isTogglingChapters = false,
                    showSubscriptionIcon = true,
                )
                ChaptersHeader(
                    totalChaptersCount = 5,
                    hiddenChaptersCount = 2,
                    onSkipChaptersClick = {},
                    isTogglingChapters = true,
                    showSubscriptionIcon = false,
                )
                ChaptersHeader(
                    totalChaptersCount = 1,
                    hiddenChaptersCount = 0,
                    onSkipChaptersClick = {},
                    isTogglingChapters = true,
                    showSubscriptionIcon = false,
                )
            }
        }
    }
}

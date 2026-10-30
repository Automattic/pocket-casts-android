package au.com.shiftyjelly.pocketcasts.player.view.chapters

import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.Divider
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionIconForTier
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.payment.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ChaptersHeader(
    totalChaptersCount: Int,
    hiddenChaptersCount: Int,
    isTogglingChapters: Boolean,
    showSubscriptionIcon: Boolean,
    isGenerated: Boolean,
    onSkipChaptersClick: (Boolean) -> Unit,
    onTurnOffGeneratedClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(start = 20.dp, end = 4.dp),
        ) {
            if (isGenerated) {
                Icon(
                    painter = painterResource(IR.drawable.ic_ai),
                    contentDescription = null,
                    tint = LocalChaptersTheme.current.headerTitle,
                    modifier = Modifier.size(16.dp),
                )
                Spacer(Modifier.width(8.dp))
            }
            TextH50(
                text = when {
                    !isGenerated -> getHeaderTitle(totalChaptersCount, hiddenChaptersCount)
                    hiddenChaptersCount > 0 -> pluralStringResource(LR.plurals.chapters_generated_count_with_hidden, totalChaptersCount, totalChaptersCount, hiddenChaptersCount)
                    else -> pluralStringResource(LR.plurals.chapters_generated_count, totalChaptersCount, totalChaptersCount)
                },
                color = LocalChaptersTheme.current.headerTitle,
                modifier = Modifier
                    .weight(1f)
                    .padding(vertical = 16.dp),
            )

            Spacer(Modifier.width(12.dp))

            if (isGenerated && !isTogglingChapters) {
                GeneratedChaptersMenu(
                    showSubscriptionIcon = showSubscriptionIcon,
                    onPreselectClick = { onSkipChaptersClick(true) },
                    onTurnOffClick = onTurnOffGeneratedClick,
                )
            } else {
                TextButton(
                    text = if (isTogglingChapters) {
                        stringResource(LR.string.done)
                    } else {
                        stringResource(LR.string.skip_chapters)
                    },
                    showSubscriptionIcon = showSubscriptionIcon,
                    onClick = { onSkipChaptersClick(!isTogglingChapters) },
                )
            }
        }
        Divider(
            color = LocalChaptersTheme.current.divider,
            thickness = 1.dp,
        )
    }
}

@Composable
internal fun GeneratedChaptersMenu(
    showSubscriptionIcon: Boolean,
    onPreselectClick: (() -> Unit)?,
    onTurnOffClick: () -> Unit,
) {
    var isMenuShown by remember { mutableStateOf(false) }
    Box {
        IconButton(onClick = { isMenuShown = true }) {
            Icon(
                painter = painterResource(IR.drawable.ic_ellipsis_horizontal),
                contentDescription = stringResource(LR.string.more_options),
                tint = LocalChaptersTheme.current.headerToggle,
            )
        }
        DropdownMenu(
            expanded = isMenuShown,
            onDismissRequest = { isMenuShown = false },
        ) {
            if (onPreselectClick != null) {
                GeneratedChaptersMenuItem(
                    iconRes = IR.drawable.ic_tick_circle,
                    textRes = LR.string.skip_chapters,
                    color = MaterialTheme.theme.colors.primaryText01,
                    showSubscriptionIcon = showSubscriptionIcon,
                    onClick = {
                        isMenuShown = false
                        onPreselectClick()
                    },
                )
            }
            GeneratedChaptersMenuItem(
                iconRes = IR.drawable.ic_minus,
                textRes = LR.string.chapters_generated_turn_off,
                color = MaterialTheme.theme.colors.support05,
                showSubscriptionIcon = false,
                onClick = {
                    isMenuShown = false
                    onTurnOffClick()
                },
            )
        }
    }
}

@Composable
private fun GeneratedChaptersMenuItem(
    @DrawableRes iconRes: Int,
    @StringRes textRes: Int,
    color: Color,
    showSubscriptionIcon: Boolean,
    onClick: () -> Unit,
) {
    DropdownMenuItem(onClick = onClick) {
        Icon(
            painter = painterResource(iconRes),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(20.dp),
        )
        Spacer(Modifier.width(12.dp))
        TextP40(
            text = stringResource(textRes),
            color = color,
        )
        if (showSubscriptionIcon) {
            Spacer(Modifier.width(8.dp))
            SubscriptionIconForTier(SubscriptionTier.Plus)
        }
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
            SubscriptionIconForTier(SubscriptionTier.Plus)
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

@Preview(name = "Light")
@Composable
private fun ChaptersHeaderLightPreview() = ChaptersHeaderPreview(Theme.ThemeType.LIGHT)

@Preview(name = "Dark")
@Composable
private fun ChaptersHeaderDarkPreview() = ChaptersHeaderPreview(Theme.ThemeType.DARK)

@Preview(name = "Rose")
@Composable
private fun ChaptersHeaderRosePreview() = ChaptersHeaderPreview(Theme.ThemeType.ROSE)

@Preview(name = "Indigo")
@Composable
private fun ChaptersHeaderIndigoPreview() = ChaptersHeaderPreview(Theme.ThemeType.INDIGO)

@Preview(name = "ExtraDark")
@Composable
private fun ChaptersHeaderExtraDarkPreview() = ChaptersHeaderPreview(Theme.ThemeType.EXTRA_DARK)

@Preview(name = "DarkContrast")
@Composable
private fun ChaptersHeaderDarkContrastPreview() = ChaptersHeaderPreview(Theme.ThemeType.DARK_CONTRAST)

@Preview(name = "LightContrast")
@Composable
private fun ChaptersHeaderLightContrastPreview() = ChaptersHeaderPreview(Theme.ThemeType.LIGHT_CONTRAST)

@Preview(name = "Electric")
@Composable
private fun ChaptersHeaderElectricPreview() = ChaptersHeaderPreview(Theme.ThemeType.ELECTRIC)

@Preview(name = "Classic")
@Composable
private fun ChaptersHeaderClassicPreview() = ChaptersHeaderPreview(Theme.ThemeType.CLASSIC_LIGHT)

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
                    isGenerated = false,
                    onTurnOffGeneratedClick = {},
                )
                ChaptersHeader(
                    totalChaptersCount = 5,
                    hiddenChaptersCount = 0,
                    onSkipChaptersClick = {},
                    isTogglingChapters = false,
                    showSubscriptionIcon = true,
                    isGenerated = false,
                    onTurnOffGeneratedClick = {},
                )
                ChaptersHeader(
                    totalChaptersCount = 5,
                    hiddenChaptersCount = 2,
                    onSkipChaptersClick = {},
                    isTogglingChapters = true,
                    showSubscriptionIcon = false,
                    isGenerated = false,
                    onTurnOffGeneratedClick = {},
                )
                ChaptersHeader(
                    totalChaptersCount = 1,
                    hiddenChaptersCount = 0,
                    onSkipChaptersClick = {},
                    isTogglingChapters = true,
                    showSubscriptionIcon = false,
                    isGenerated = false,
                    onTurnOffGeneratedClick = {},
                )
                ChaptersHeader(
                    totalChaptersCount = 6,
                    hiddenChaptersCount = 0,
                    onSkipChaptersClick = {},
                    isTogglingChapters = false,
                    showSubscriptionIcon = true,
                    isGenerated = true,
                    onTurnOffGeneratedClick = {},
                )
                ChaptersHeader(
                    totalChaptersCount = 6,
                    hiddenChaptersCount = 2,
                    onSkipChaptersClick = {},
                    isTogglingChapters = false,
                    showSubscriptionIcon = false,
                    isGenerated = true,
                    onTurnOffGeneratedClick = {},
                )
                ChaptersHeader(
                    totalChaptersCount = 6,
                    hiddenChaptersCount = 2,
                    onSkipChaptersClick = {},
                    isTogglingChapters = true,
                    showSubscriptionIcon = false,
                    isGenerated = true,
                    onTurnOffGeneratedClick = {},
                )
            }
        }
    }
}

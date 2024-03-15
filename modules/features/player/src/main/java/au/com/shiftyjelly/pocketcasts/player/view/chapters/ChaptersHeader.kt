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
import androidx.compose.material.MaterialTheme
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
import au.com.shiftyjelly.pocketcasts.compose.theme
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
        color = MaterialTheme.theme.colors.playerContrast06,
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
                color = MaterialTheme.theme.colors.playerContrast02,
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
            color = MaterialTheme.theme.colors.playerContrast01,
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

@ShowkaseComposable(name = "ChaptersHeader", group = "Chapter", styleName = "Default - DARK")
@Preview(name = "Dark")
@Composable
fun ChaptersHeaderPreview() {
    AppThemeWithBackground(Theme.ThemeType.DARK) {
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

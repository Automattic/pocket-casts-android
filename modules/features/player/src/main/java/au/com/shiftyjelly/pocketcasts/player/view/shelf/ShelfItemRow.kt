package au.com.shiftyjelly.pocketcasts.player.view.shelf

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RippleConfiguration
import androidx.compose.material.RippleDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.preferences.model.ShelfItem
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.util.Date
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ShelfItemRow(
    episode: BaseEpisode?,
    item: ShelfItem,
    modifier: Modifier = Modifier,
    isEditable: Boolean = true,
    isTranscriptAvailable: Boolean = false,
    onClick: ((ShelfItem, Boolean) -> Unit)? = null,
) {
    val subtitleResId = item.subtitleId(episode)
    val isEnabled = item != ShelfItem.Transcript || isTranscriptAvailable
    CompositionLocalProvider(
        LocalRippleConfiguration provides if (isEditable) {
            null
        } else {
            RippleConfiguration(
                color = Color.White,
                rippleAlpha = RippleDefaults.rippleAlpha(Color.White, true),
            )
        },
    ) {
        Row(
            modifier = modifier
                .fillMaxWidth()
                .heightIn(min = 64.dp)
                .alpha(if (isEnabled || isEditable) 1f else 0.4f)
                .clickable { onClick?.invoke(item, isEnabled) },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                painter = painterResource(item.iconId(episode)),
                contentDescription = null,
                modifier = Modifier
                    .padding(start = 16.dp)
                    .size(24.dp),
                tint = MaterialTheme.theme.colors.playerContrast02,
            )
            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 24.dp, vertical = 8.dp),
            ) {
                TextH40(
                    text = stringResource(item.titleId(episode)),
                    color = MaterialTheme.theme.colors.playerContrast01,
                )
                if (isEditable && subtitleResId != null) {
                    TextH50(
                        text = stringResource(subtitleResId),
                        color = MaterialTheme.theme.colors.playerContrast03,
                    )
                }
            }
            if (isEditable) {
                Icon(
                    painter = painterResource(IR.drawable.ic_reorder),
                    contentDescription = stringResource(LR.string.rearrange_actions),
                    modifier = Modifier
                        .padding(end = 16.dp)
                        .size(24.dp),
                    tint = MaterialTheme.theme.colors.playerContrast02,
                )
            }
        }
    }
}

@Preview
@Composable
private fun ShelfItemWithoutSubtitlePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        ShelfItemRow(
            episode = PodcastEpisode(
                title = "Podcast Episode",
                uuid = "",
                publishedDate = Date(),
            ),
            item = ShelfItem.Star,
            onClick = { _, _ -> },
        )
    }
}

@Preview
@Composable
private fun ShelfItemWithSubtitlePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        ShelfItemRow(
            episode = UserEpisode(
                title = "User Episode",
                uuid = "",
                publishedDate = Date(),
            ),
            item = ShelfItem.Star,
            onClick = { _, _ -> },
        )
    }
}

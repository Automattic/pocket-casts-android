package au.com.shiftyjelly.pocketcasts.playlists

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextP50
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistPreview
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun PlaylistPreviewRow(
    playlist: PlaylistPreview,
    showDivider: Boolean,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
        ) {
            PlaylistArtwork(
                podcasts = playlist.podcasts,
                artworkSize = 56.dp,
            )
            Spacer(
                modifier = Modifier.width(16.dp),
            )
            Column {
                TextH40(
                    text = playlist.title,
                )
                TextP50(
                    text = stringResource(LR.string.smart_playlist),
                    color = MaterialTheme.theme.colors.primaryText02,
                )
            }
            Spacer(
                modifier = Modifier.width(16.dp),
            )
            Spacer(
                modifier = Modifier.weight(1f),
            )
            TextP50(
                text = "${playlist.episodeCount}",
                color = MaterialTheme.theme.colors.primaryText02,
            )
            Image(
                painter = painterResource(IR.drawable.ic_chevron_small_right),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryText02),
                modifier = Modifier
                    .padding(3.dp)
                    .size(24.dp),
            )
        }
        if (showDivider) {
            HorizontalDivider(startIndent = 16.dp)
        }
    }
}

@Preview
@Composable
private fun PlaylistPreviewRowPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        Column {
            PlaylistPreviewRow(
                playlist = PlaylistPreview(
                    uuid = "",
                    title = "New Releases",
                    episodeCount = 0,
                    podcasts = emptyList(),
                ),
                showDivider = true,
                modifier = Modifier.fillMaxWidth(),
            )
            PlaylistPreviewRow(
                playlist = PlaylistPreview(
                    uuid = "",
                    title = "In progress",
                    episodeCount = 1,
                    podcasts = List(1) { Podcast(uuid = "$it") },
                ),
                showDivider = true,
                modifier = Modifier.fillMaxWidth(),
            )
            PlaylistPreviewRow(
                playlist = PlaylistPreview(
                    uuid = "",
                    title = "Starred",
                    episodeCount = 328,
                    podcasts = List(4) { Podcast(uuid = "$it") },
                ),
                showDivider = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

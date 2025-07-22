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
import androidx.compose.ui.graphics.Color
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
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistPreview
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.util.Date
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun PlaylistPreviewRow(
    playlist: PlaylistPreview,
    showDivider: Boolean,
    useEpisodeArtwork: Boolean,
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
                episodes = playlist.artworkEpisodes,
                artworkSize = 56.dp,
                useEpisodeArtwork = useEpisodeArtwork,
            )
            Spacer(
                modifier = Modifier.width(16.dp),
            )
            Column {
                TextH40(
                    text = playlist.title,
                )
                TextP50(
                    text = stringResource(LR.string.smart_playlists),
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
        HorizontalDivider(
            startIndent = 16.dp,
            color = if (showDivider) null else Color.Transparent,
        )
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
                    artworkEpisodes = emptyList(),
                ),
                showDivider = true,
                useEpisodeArtwork = false,
                modifier = Modifier.fillMaxWidth(),
            )
            PlaylistPreviewRow(
                playlist = PlaylistPreview(
                    uuid = "",
                    title = "In progress",
                    episodeCount = 1,
                    artworkEpisodes = List(1) { PodcastEpisode(uuid = "$it", publishedDate = Date()) },
                ),
                showDivider = true,
                useEpisodeArtwork = false,
                modifier = Modifier.fillMaxWidth(),
            )
            PlaylistPreviewRow(
                playlist = PlaylistPreview(
                    uuid = "",
                    title = "Starred",
                    episodeCount = 328,
                    artworkEpisodes = List(4) { PodcastEpisode(uuid = "$it", publishedDate = Date()) },
                ),
                showDivider = false,
                useEpisodeArtwork = false,
                modifier = Modifier.fillMaxWidth(),
            )
        }
    }
}

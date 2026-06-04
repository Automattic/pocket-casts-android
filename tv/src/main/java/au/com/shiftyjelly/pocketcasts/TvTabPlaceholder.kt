package au.com.shiftyjelly.pocketcasts

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.tv.material3.MaterialTheme
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

@Composable
fun TvTabPlaceholder(
    tab: TvTab,
    modifier: Modifier = Modifier,
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        item { Spacer(modifier = Modifier.height(8.dp)) }

        item {
            TvRow(
                title = "Made for TV",
                items = (1..3).toList(),
                itemSpacing = 32.dp,
            ) { index ->
                TvFeaturedTile(
                    artworkUrl = "https://picsum.photos/seed/featured$index/500/500",
                    isSponsored = true,
                    sponsoredLabel = "Sponsored \u00B7 iHeartPodcasts and Kaleidoscope",
                    title = "Superhuman",
                    description = "SuperHuman is a high-stakes, edge-of-your-seat docuseries",
                    onGoToPodcast = {},
                    onPlayLastEpisode = {},
                )
            }
        }

        item {
            TvRow(
                title = "Trending Videos",
                items = (1..6).toList(),
                itemSpacing = 32.dp,
            ) { index ->
                TvVideoTile(
                    thumbnailUrl = "https://picsum.photos/seed/video$index/716/403",
                    podcastArtworkUrl = "https://picsum.photos/seed/podcast$index/100/100",
                    podcastTitle = "Huberman Lab",
                    episodeTitle = "How to overcome Social Anxiety",
                    onPlayEpisode = {},
                    onGoToPodcast = {},
                )
            }
        }

        item {
            TvRow(
                title = "Recommendations",
                items = (1..8).toList(),
            ) { index ->
                TvPodcastTile(
                    artworkUrl = "https://picsum.photos/seed/rec$index/272/272",
                    onClick = {},
                )
            }
        }

        item {
            TvRow(
                title = "Because you liked Podcast",
                items = (1..8).toList(),
            ) { index ->
                TvPodcastTile(
                    artworkUrl = "https://picsum.photos/seed/liked$index/272/272",
                    onClick = {},
                )
            }
        }

        item { Spacer(modifier = Modifier.height(8.dp)) }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvTabPlaceholderPreview() {
    AppTheme(themeType = Theme.ThemeType.EXTRA_DARK) {
        MaterialTheme {
            Box(modifier = Modifier.background(TvColors.Dark)) {
                TvTabPlaceholder(tab = TvTab.Home)
            }
        }
    }
}

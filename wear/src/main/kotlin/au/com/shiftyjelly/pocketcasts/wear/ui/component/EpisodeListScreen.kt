package au.com.shiftyjelly.pocketcasts.wear.ui.component

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.tooling.preview.devices.WearDevices
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.wear.theme.WearAppTheme
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnState
import java.util.Date
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun EpisodeListScreen(
    columnState: ScalingLazyColumnState,
    uiState: EpisodeListUiState,
    @StringRes title: Int,
    useEpisodeArtwork: Boolean,
    onItemClick: (PodcastEpisode) -> Unit,
) {
    when (uiState) {
        is EpisodeListUiState.Loading -> ContentLoading(columnState = columnState)

        is EpisodeListUiState.Empty -> ContentEmpty(
            columnState = columnState,
            title = title,
        )

        is EpisodeListUiState.Loaded -> ContentLoaded(
            columnState = columnState,
            episodes = uiState.episodes,
            title = title,
            useEpisodeArtwork = useEpisodeArtwork,
            onItemClick = onItemClick,
        )
    }
}

@Composable
private fun ContentLoading(columnState: ScalingLazyColumnState) {
    LoadingScreen(
        modifier = Modifier.padding(columnState.contentPadding),
    )
}

@Composable
private fun ContentEmpty(
    columnState: ScalingLazyColumnState,
    @StringRes title: Int,
) {
    NoContentScreen(
        title = title,
        message = LR.string.no_episodes,
        modifier = Modifier.padding(columnState.contentPadding),
    )
}

@Composable
private fun ContentLoaded(
    columnState: ScalingLazyColumnState,
    episodes: List<PodcastEpisode>,
    @StringRes title: Int,
    useEpisodeArtwork: Boolean,
    onItemClick: (PodcastEpisode) -> Unit,
) {
    ScalingLazyColumn(
        columnState = columnState,
    ) {
        item {
            ScreenHeaderChip(text = title)
        }

        items(
            items = episodes,
            key = { it.uuid },
        ) { episode ->
            EpisodeChip(
                episode = episode,
                useEpisodeArtwork = useEpisodeArtwork,
                onClick = { onItemClick(episode) },
            )
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND)
@Composable
private fun DownloadsScreenPreview() {
    WearAppTheme {
        EpisodeListScreen(
            columnState = ScalingLazyColumnState(),
            title = LR.string.downloads,
            useEpisodeArtwork = false,
            onItemClick = {},
            uiState = EpisodeListUiState.Loaded(
                episodes = listOf(
                    PodcastEpisode(
                        uuid = "57853d71-30ac-4477-af73-e8fe2b1d4dda",
                        podcastUuid = "b643cb50-2c52-013b-ef7a-0acc26574db2",
                        title = "Such a great episode title, but it's so long that it is definitely going to be more than two lines",
                        publishedDate = Date(),
                        playedUpTo = 0.0,
                        duration = 20.0,
                    ),
                    PodcastEpisode(
                        uuid = "c146e703-e408-4979-852c-f9927ce19ef7",
                        podcastUuid = "3df2e780-0063-0135-ec79-4114446340cb",
                        title = "1 line title",
                        publishedDate = Date(),
                        playedUpTo = 0.0,
                        duration = 20.0,
                    ),
                ),
            ),
        )
    }
}

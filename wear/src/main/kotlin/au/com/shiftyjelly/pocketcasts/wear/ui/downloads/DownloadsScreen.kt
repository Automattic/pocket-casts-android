package au.com.shiftyjelly.pocketcasts.wear.ui.downloads

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.items
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.wear.theme.WearAppTheme
import au.com.shiftyjelly.pocketcasts.wear.ui.component.EpisodeChip
import au.com.shiftyjelly.pocketcasts.wear.ui.component.ScreenHeaderChip
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnState
import java.util.Date
import au.com.shiftyjelly.pocketcasts.localization.R as LR

object DownloadsScreen {
    const val route = "downloads_screen"
}

@Composable
fun DownloadsScreen(
    columnState: ScalingLazyColumnState,
    onItemClick: (PodcastEpisode) -> Unit,
) {

    val viewModel = hiltViewModel<DownloadsScreenViewModel>()
    val state by viewModel.stateFlow.collectAsState()

    Content(columnState, state, onItemClick)
}

@Composable
private fun Content(
    columnState: ScalingLazyColumnState,
    episodes: List<PodcastEpisode>?,
    onItemClick: (PodcastEpisode) -> Unit,
) {
    ScalingLazyColumn(
        columnState = columnState,
    ) {
        if (episodes != null) {
            item {
                ScreenHeaderChip(
                    text = if (episodes.isEmpty()) {
                        LR.string.profile_empty_downloaded
                    } else {
                        LR.string.downloads
                    },
                )
            }

            items(episodes) { episode ->
                EpisodeChip(
                    episode = episode,
                    onClick = { onItemClick(episode) }
                )
            }
        }
    }
}

@Preview(
    widthDp = 200,
    heightDp = 200,
    uiMode = Configuration.UI_MODE_TYPE_WATCH,
)
@Composable
private fun DownloadsScreenPreview() {
    WearAppTheme(Theme.ThemeType.DARK) {
        Content(
            columnState = ScalingLazyColumnState(),
            onItemClick = {},
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
            )
        )
    }
}

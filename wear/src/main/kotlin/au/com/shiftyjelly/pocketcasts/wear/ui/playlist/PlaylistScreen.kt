package au.com.shiftyjelly.pocketcasts.wear.ui.playlist

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.playlist.Playlist
import au.com.shiftyjelly.pocketcasts.wear.ui.component.EpisodeChip
import au.com.shiftyjelly.pocketcasts.wear.ui.component.LoadingScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.component.ScreenHeaderChip
import au.com.shiftyjelly.pocketcasts.wear.ui.playlist.PlaylistViewModel.UiState
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnState
import au.com.shiftyjelly.pocketcasts.localization.R as LR

object PlaylistScreen {
    const val ARGUMENT_PLAYLIST_UUID = "playlistUuid"
    const val ARGUMENT_PLAYLIST_TYPE = "playlistType"
    const val ROUTE = "playlist/{$ARGUMENT_PLAYLIST_UUID}/{$ARGUMENT_PLAYLIST_TYPE}"

    fun navigateRoute(playlistUuid: String, playlistType: Playlist.Type) = "playlist/$playlistUuid/${playlistType.analyticsValue}"
}

@Composable
fun PlaylistScreen(
    columnState: ScalingLazyColumnState,
    onEpisodeTap: (PodcastEpisode) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaylistViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState(UiState.Loading)
    val artworkConfiguration by viewModel.artworkConfiguration.collectAsState()

    when (val state = uiState) {
        is UiState.Loaded -> Content(
            state = state,
            useEpisodeArtwork = artworkConfiguration.useEpisodeArtwork,
            onEpisodeTap = onEpisodeTap,
            modifier = modifier,
            listState = columnState,
        )

        is UiState.Loading -> LoadingScreen(
            modifier = modifier,
        )
        is UiState.Empty -> {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = modifier.fillMaxSize(),
            ) {
                state.playlistTitle?.let {
                    ScreenHeaderChip(it)
                    Text(
                        text = stringResource(LR.string.filters_no_episodes),
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.onPrimary,
                        style = MaterialTheme.typography.body1,
                    )
                } ?: run {
                    Text(
                        text = stringResource(LR.string.playlists_not_found),
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.onPrimary,
                        style = MaterialTheme.typography.body1,
                    )
                }
            }
        }
    }
}

@Composable
private fun Content(
    listState: ScalingLazyColumnState,
    state: UiState.Loaded,
    useEpisodeArtwork: Boolean,
    onEpisodeTap: (PodcastEpisode) -> Unit,
    modifier: Modifier = Modifier,
) {
    ScalingLazyColumn(
        modifier = modifier.fillMaxWidth(),
        columnState = listState,
    ) {
        item {
            ScreenHeaderChip(state.playlistTitle)
        }
        items(items = state.episodes, key = { episode -> episode.uuid }) { episode ->
            EpisodeChip(
                episode = episode,
                onClick = {
                    onEpisodeTap(episode)
                },
                useEpisodeArtwork = useEpisodeArtwork,
                showImage = true,
            )
        }
    }
}

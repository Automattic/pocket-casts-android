package au.com.shiftyjelly.pocketcasts.wear.ui.filter

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.items
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.wear.ui.component.EpisodeChip
import au.com.shiftyjelly.pocketcasts.wear.ui.component.LoadingScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.component.ScreenHeaderChip
import au.com.shiftyjelly.pocketcasts.wear.ui.filter.FilterViewModel.UiState
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnState

object FilterScreen {
    const val argumentFilterUuid = "filterUuid"
    const val route = "filter/{$argumentFilterUuid}"

    fun navigateRoute(filterUuid: String) = "filter/$filterUuid"
}

@Composable
fun FilterScreen(
    onEpisodeTap: (PodcastEpisode) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: FilterViewModel = hiltViewModel(),
    columnState: ScalingLazyColumnState,
) {
    val uiState by viewModel.uiState.collectAsState(UiState.Loading)
    when (val state = uiState) {
        is UiState.Loaded -> Content(
            state = state,
            onEpisodeTap = onEpisodeTap,
            modifier = modifier,
            listState = columnState,
        )
        is UiState.Loading -> LoadingScreen()
        is UiState.Empty -> Unit
    }
}

@Composable
private fun Content(
    state: UiState.Loaded,
    onEpisodeTap: (PodcastEpisode) -> Unit,
    modifier: Modifier = Modifier,
    listState: ScalingLazyColumnState,
) {
    ScalingLazyColumn(
        modifier = modifier.fillMaxWidth(),
        columnState = listState
    ) {
        item {
            ScreenHeaderChip(state.filter.title)
        }
        items(items = state.episodes, key = { episode -> episode.uuid }) { episode ->
            EpisodeChip(
                episode = episode,
                onClick = {
                    onEpisodeTap(episode)
                },
                showImage = true,
            )
        }
    }
}

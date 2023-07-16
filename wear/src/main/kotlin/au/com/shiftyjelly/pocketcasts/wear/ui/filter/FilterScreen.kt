package au.com.shiftyjelly.pocketcasts.wear.ui.filter

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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.wear.ui.component.EpisodeChip
import au.com.shiftyjelly.pocketcasts.wear.ui.component.LoadingScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.component.ScreenHeaderChip
import au.com.shiftyjelly.pocketcasts.wear.ui.filter.FilterViewModel.UiState
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnState
import au.com.shiftyjelly.pocketcasts.localization.R as LR

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
        is UiState.Empty -> {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                state.filter?.let {
                    ScreenHeaderChip(it.title)
                    Text(
                        text = stringResource(LR.string.no_results_for_filters, state.filter.title),
                        modifier = Modifier.padding(16.dp),
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.colors.onPrimary,
                        style = MaterialTheme.typography.body1,
                    )
                } ?: run {
                    Text(
                        text = stringResource(LR.string.no_result_found),
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

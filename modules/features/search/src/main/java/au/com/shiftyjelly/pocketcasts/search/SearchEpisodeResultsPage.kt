package au.com.shiftyjelly.pocketcasts.search

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.res.stringResource
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.models.to.EpisodeItem
import au.com.shiftyjelly.pocketcasts.search.component.SearchEpisodeItem
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun SearchEpisodeResultsPage(
    viewModel: SearchViewModel,
    onBackClick: () -> Unit,
    onEpisodeClick: (EpisodeItem) -> Unit,
) {
    val state by viewModel.state.collectAsState()
    Column {
        ThemedTopAppBar(
            title = stringResource(LR.string.search_results_all_episodes),
            bottomShadow = true,
            onNavigationClick = { onBackClick() },
        )
        SearchEpisodeResultsView(
            state = state as SearchState.Results,
            onEpisodeClick = onEpisodeClick,
        )
    }
}

@Composable
private fun SearchEpisodeResultsView(
    state: SearchState.Results,
    onEpisodeClick: (EpisodeItem) -> Unit,
) {
    LazyColumn {
        items(
            items = state.episodes,
            key = { it.uuid }
        ) {
            SearchEpisodeItem(
                episode = it,
                onClick = onEpisodeClick,
            )
        }
    }
}

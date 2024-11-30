package au.com.shiftyjelly.pocketcasts.podcasts.view.share

import androidx.compose.foundation.layout.Column
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import au.com.shiftyjelly.pocketcasts.R
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.podcast.PodcastSelectGrid

/**
 * The first page when sharing a list of podcasts. Select the podcasts to share.
 */
@Composable
fun ShareListCreatePodcastsPage(
    onCloseClick: () -> Unit,
    onNextClick: (Int) -> Unit,
    viewModel: ShareListCreateViewModel,
    modifier: Modifier = Modifier,
) {
    val state: ShareListCreateViewModel.State by viewModel.state.collectAsState()
    Column {
        ThemedTopAppBar(
            title = stringResource(R.string.podcasts_share_select_podcasts),
            navigationButton = NavigationButton.Close,
            onNavigationClick = onCloseClick,
            actions = {
                IconButton(
                    onClick = { onNextClick(state.selectedPodcasts.size) },
                    enabled = state.selectedPodcasts.isNotEmpty(),
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = stringResource(R.string.next),
                    )
                }
            },
        )
        PodcastSelectGrid(
            podcasts = state.podcasts,
            selectedPodcasts = state.selectedPodcasts,
            onPodcastSelected = { podcast -> viewModel.selectPodcast(podcast) },
            onPodcastUnselected = { podcast -> viewModel.unselectPodcast(podcast) },
            onSelectAll = { viewModel.selectAll() },
            onSelectNone = { viewModel.selectNone() },
            modifier = modifier,
        )
    }
}

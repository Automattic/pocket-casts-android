package au.com.shiftyjelly.pocketcasts.podcasts.view.share

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.podcast.PodcastSelectGrid
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.R as LR

/**
 * The first page when sharing a list of podcasts. Select the podcasts to share.
 */
@Composable
fun ShareListCreatePodcastsPage(
    onCloseClick: () -> Unit,
    onNextClick: () -> Unit,
    viewModel: ShareListCreateViewModel,
    modifier: Modifier = Modifier
) {
    val state: ShareListCreateViewModel.State by viewModel.state.collectAsState()
    Column(modifier = modifier.background(MaterialTheme.theme.colors.primaryUi01)) {
        ThemedTopAppBar(
            title = stringResource(LR.string.podcasts_share_select_podcasts),
            navigationButton = NavigationButton.Close,
            onNavigationClick = onCloseClick,
            actions = {
                IconButton(onClick = onNextClick, enabled = state.selectedPodcasts.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Filled.ArrowForward,
                        contentDescription = stringResource(LR.string.next)
                    )
                }
            }
        )
        PodcastSelectGrid(
            podcasts = state.podcasts,
            selectedPodcasts = state.selectedPodcasts,
            onPodcastSelected = { podcast -> viewModel.selectPodcast(podcast) },
            onPodcastUnselected = { podcast -> viewModel.unselectPodcast(podcast) },
            onSelectAll = { viewModel.selectAll() },
            onSelectNone = { viewModel.selectNone() }
        )
    }
}

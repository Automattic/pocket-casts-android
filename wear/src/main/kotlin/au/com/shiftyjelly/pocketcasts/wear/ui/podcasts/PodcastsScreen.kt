package au.com.shiftyjelly.pocketcasts.wear.ui.podcasts

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Text
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.models.to.FolderItem

object PodcastsScreen {
    const val route = "podcasts_screen"
}

@Composable
fun PodcastsScreen(
    modifier: Modifier = Modifier,
    viewModel: PodcastsViewModel = hiltViewModel(),
    listState: ScalingLazyListState,
    onNavigateToPodcast: (String) -> Unit
) {
    val uiState = viewModel.uiState

    ScalingLazyColumn(
        modifier = modifier.fillMaxWidth(),
        state = listState,
    ) {
        items(uiState.items) { item ->
            if (item is FolderItem.Podcast) {
                PodcastChip(podcast = item, onClick = onNavigateToPodcast)
            }
        }
    }
}

@Composable
private fun PodcastChip(podcast: FolderItem.Podcast, onClick: (String) -> Unit, modifier: Modifier = Modifier) {
    Chip(
        onClick = { onClick(podcast.uuid) },
        colors = ChipDefaults.secondaryChipColors(),
        label = {
            Text(podcast.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        icon = {
            PodcastImage(uuid = podcast.uuid, dropShadow = false, modifier = Modifier.size(30.dp))
        },
        modifier = modifier.fillMaxWidth()
    )
}

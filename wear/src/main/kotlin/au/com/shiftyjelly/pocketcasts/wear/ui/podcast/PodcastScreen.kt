package au.com.shiftyjelly.pocketcasts.wear.ui.podcast

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.wear.theme.theme

object PodcastScreen {
    const val argument = "podcastUuid"
    const val route = "podcast/{$argument}"

    fun navigateRoute(podcastUuid: String) = "podcast/$podcastUuid"
}

@Composable
fun PodcastScreen(
    onNavigateToNowPlaying: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PodcastViewModel = hiltViewModel(),
) {
    val podcast = viewModel.uiState.podcast ?: return

    ScalingLazyColumn(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 26.dp),
    ) {
        item {
            PodcastImage(
                uuid = podcast.uuid,
                modifier = Modifier.size(100.dp)
            )
            Spacer(Modifier.height(4.dp))
        }
        item {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.theme.colors.primaryText01,
                text = podcast.title
            )
        }
        item {
            Text(
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                color = MaterialTheme.theme.colors.primaryText02,
                text = podcast.author
            )
        }
        items(viewModel.uiState.episodes) { item ->
            EpisodeChip(
                episode = item,
                onClick = onNavigateToNowPlaying
            )
        }
    }
}

@Composable
private fun EpisodeChip(
    episode: Episode,
    onClick: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Chip(
        onClick = { onClick(episode.uuid) },
        colors = ChipDefaults.secondaryChipColors(),
        label = {
            Text(episode.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
        },
        modifier = modifier.fillMaxWidth()
    )
}

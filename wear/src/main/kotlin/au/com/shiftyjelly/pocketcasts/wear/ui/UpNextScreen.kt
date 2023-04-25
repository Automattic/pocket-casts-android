package au.com.shiftyjelly.pocketcasts.wear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.localization.R as LR

object UpNextScreen {
    const val route = "up_next_screen"
}

@Composable
fun UpNextScreen(
    navigateToEpisode: (episodeUuid: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UpNextViewModel = hiltViewModel(),
    listState: ScalingLazyListState,
) {
    val queueState by viewModel.upNextQueue.subscribeAsState(initial = null)

    when (queueState) {

        null -> { /* Show nothing while loading */ }

        UpNextQueue.State.Empty -> {
            Column(
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxSize()
            ) {
                Text(
                    text = stringResource(LR.string.player_up_next_empty),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.title3,
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(LR.string.player_up_next_empty_desc_watch),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.body2,
                )
            }
        }

        is UpNextQueue.State.Loaded -> {
            val list = (queueState as UpNextQueue.State.Loaded).queue
            ScalingLazyColumn(
                modifier = modifier.fillMaxWidth(),
                state = listState,
            ) {
                items(list) { episode ->
                    Chip(
                        onClick = {
                            navigateToEpisode(episode.uuid)
                        },
                        colors = ChipDefaults.secondaryChipColors(),
                        label = {
                            Text(episode.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        icon = {
                            PodcastImage(
                                uuid = episode.uuid,
                                dropShadow = false,
                                modifier = Modifier.size(30.dp)
                            )
                        },
                        modifier = modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

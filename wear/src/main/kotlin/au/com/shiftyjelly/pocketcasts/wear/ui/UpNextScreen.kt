package au.com.shiftyjelly.pocketcasts.wear.ui

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rxjava2.subscribeAsState
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
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue

object UpNextScreen {
    const val route = "up_next_screen"
}

@Composable
fun UpNextScreen(
    navigateToNowPlaying: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: UpNextViewModel = hiltViewModel(),
    listState: ScalingLazyListState,
) {
    val queueState by viewModel.upNextQueue.subscribeAsState(initial = null)

    when (queueState) {

        UpNextQueue.State.Empty -> {}

        null -> {}

        is UpNextQueue.State.Loaded -> {
            val list = (queueState as UpNextQueue.State.Loaded).queue
            ScalingLazyColumn(
                modifier = modifier.fillMaxWidth(),
                state = listState,
            ) {
                items(list) { playable ->
                    Chip(
                        onClick = {
                            viewModel.play(playable)
                            navigateToNowPlaying()
                        },
                        colors = ChipDefaults.secondaryChipColors(),
                        label = {
                            Text(playable.title, maxLines = 1, overflow = TextOverflow.Ellipsis)
                        },
                        icon = {
                            PodcastImage(
                                uuid = playable.uuid,
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

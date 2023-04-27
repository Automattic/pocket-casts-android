package au.com.shiftyjelly.pocketcasts.wear.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import au.com.shiftyjelly.pocketcasts.wear.ui.component.EpisodeChip
import au.com.shiftyjelly.pocketcasts.wear.ui.component.ScreenHeaderChip
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnState
import au.com.shiftyjelly.pocketcasts.localization.R as LR

object FilesScreen {
    const val route = "files_screen"
}

@Composable
fun FilesScreen(
    columnState: ScalingLazyColumnState,
    navigateToEpisode: (episodeUuid: String) -> Unit,
) {

    val viewModel = hiltViewModel<FilesViewModel>()
    val userEpisodesState = viewModel.userEpisodes.collectAsState(null)
    val userEpisodes = userEpisodesState.value

    when {
        // Show nothing while screen is loading
        userEpisodes == null -> return

        userEpisodes.isEmpty() -> EmptyState()

        else -> {
            ScalingLazyColumn(
                columnState = columnState
            ) {

                item { ScreenHeaderChip(LR.string.profile_navigation_files) }

                items(userEpisodes) { episode ->
                    EpisodeChip(
                        episode = episode,
                        onClick = { navigateToEpisode(episode.uuid) }
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState() {
    Column(
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize()
    ) {
        Text(
            text = stringResource(LR.string.profile_cloud_no_files_uploaded),
            textAlign = TextAlign.Center,
            style = MaterialTheme.typography.title3,
        )
    }
}

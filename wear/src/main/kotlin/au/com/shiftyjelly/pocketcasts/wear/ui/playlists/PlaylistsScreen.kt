package au.com.shiftyjelly.pocketcasts.wear.ui.playlists

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.items
import au.com.shiftyjelly.pocketcasts.compose.components.PlaylistArtwork
import au.com.shiftyjelly.pocketcasts.repositories.extensions.drawableId
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistPreview
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.wear.theme.WearColors
import au.com.shiftyjelly.pocketcasts.wear.ui.component.LoadingScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.component.ScreenHeaderChip
import au.com.shiftyjelly.pocketcasts.wear.ui.component.WatchListChip
import au.com.shiftyjelly.pocketcasts.wear.ui.playlists.PlaylistsViewModel.UiState
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnState
import au.com.shiftyjelly.pocketcasts.localization.R as LR

object PlaylistsScreen {
    const val ROUTE = "playlists_screen"
}

@Composable
fun PlaylistsScreen(
    columnState: ScalingLazyColumnState,
    onClickPlaylist: (PlaylistPreview) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: PlaylistsViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsState()
    when (val state = uiState) { // the state needs to be immutable or the following error will happen 'Smart cast is impossible'
        is UiState.Loaded -> Content(
            playlists = state.playlists,
            onClickPlaylist = onClickPlaylist,
            modifier = modifier,
            columnState = columnState,
        )

        is UiState.Loading -> LoadingScreen()
    }
}

@Composable
private fun Content(
    columnState: ScalingLazyColumnState,
    playlists: List<PlaylistPreview>,
    onClickPlaylist: (PlaylistPreview) -> Unit,
    modifier: Modifier = Modifier,
) {
    val usePlaylists = FeatureFlag.isEnabled(Feature.PLAYLISTS_REBRANDING, immutable = true)
    ScalingLazyColumn(
        modifier = modifier.fillMaxWidth(),
        columnState = columnState,
    ) {
        item {
            ScreenHeaderChip(if (usePlaylists) LR.string.playlists else LR.string.filters)
        }
        items(
            items = playlists,
            key = { playlist -> playlist.uuid },
        ) { playlist ->
            WatchListChip(
                title = playlist.title,
                onClick = { onClickPlaylist(playlist) },
                icon = {
                    if (usePlaylists) {
                        PlaylistArtwork(
                            podcastUuids = playlist.artworkPodcastUuids,
                            artworkSize = 32.dp,
                            elevation = 0.dp,
                            modifier = Modifier.padding(horizontal = 8.dp),
                        )
                    } else {
                        Icon(
                            painter = painterResource(playlist.icon.drawableId),
                            contentDescription = null,
                            tint = WearColors.getFilterColor(playlist.icon),
                            modifier = Modifier
                                .padding(horizontal = 8.dp)
                                .size(24.dp),
                        )
                    }
                },
            )
        }
    }
}

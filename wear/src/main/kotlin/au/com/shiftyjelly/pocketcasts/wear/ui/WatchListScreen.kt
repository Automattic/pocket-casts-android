package au.com.shiftyjelly.pocketcasts.wear.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.wear.tooling.preview.devices.WearDevices
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.wear.theme.WearAppTheme
import au.com.shiftyjelly.pocketcasts.wear.ui.component.WatchListChip
import au.com.shiftyjelly.pocketcasts.wear.ui.downloads.DownloadsScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.playlists.PlaylistsScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.podcasts.PodcastsScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.settings.SettingsScreen
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnState
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

object WatchListScreen {
    const val ROUTE = "watch_list_screen"
}

@Composable
fun WatchListScreen(
    columnState: ScalingLazyColumnState,
    navigateToRoute: (String) -> Unit,
    toNowPlaying: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: WatchListScreenViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsState()
    val upNextState = state.upNextQueue

    CallOnce {
        viewModel.onShown()
    }

    ScalingLazyColumn(
        columnState = columnState,
        modifier = modifier.fillMaxWidth(),
    ) {
        item {
            // Need this to position the first chip correctly when the screen loads
            Spacer(Modifier)
        }

        if (upNextState is UpNextQueue.State.Loaded) {
            item {
                NowPlayingChip(onClick = {
                    viewModel.onNowPlayingClicked()
                    toNowPlaying()
                })
            }
        }

        item {
            WatchListChip(
                title = stringResource(LR.string.podcasts),
                iconRes = IR.drawable.ic_podcasts,
                onClick = {
                    viewModel.onPodcastsClicked()
                    navigateToRoute(PodcastsScreen.ROUTE_HOME_FOLDER)
                },
            )
        }

        item {
            WatchListChip(
                title = stringResource(LR.string.downloads),
                iconRes = IR.drawable.ic_download,
                onClick = {
                    viewModel.onDownloadsClicked()
                    navigateToRoute(DownloadsScreen.ROUTE)
                },
            )
        }

        item {
            val usePlaylists = FeatureFlag.isEnabled(Feature.PLAYLISTS_REBRANDING, immutable = true)
            WatchListChip(
                title = if (usePlaylists) {
                    stringResource(LR.string.playlists)
                } else {
                    stringResource(LR.string.filters)
                },
                iconRes = if (usePlaylists) {
                    IR.drawable.ic_playlists
                } else {
                    IR.drawable.ic_filters
                },
                onClick = {
                    viewModel.onPlaylistsClicked()
                    navigateToRoute(PlaylistsScreen.ROUTE)
                },
            )
        }

        item {
            WatchListChip(
                title = stringResource(LR.string.profile_navigation_files),
                iconRes = IR.drawable.ic_file,
                onClick = {
                    viewModel.onFilesClicked()
                    navigateToRoute(FilesScreen.ROUTE)
                },
            )
        }

        item {
            WatchListChip(
                title = stringResource(LR.string.settings),
                iconRes = IR.drawable.ic_profile_settings,
                onClick = {
                    viewModel.onSettingsClicked()
                    navigateToRoute(SettingsScreen.ROUTE)
                },
            )
        }
    }
}

@Preview(device = WearDevices.SMALL_ROUND, showSystemUi = true)
@Composable
private fun WatchListPreview() {
    WearAppTheme {
        WatchListScreen(
            toNowPlaying = {},
            navigateToRoute = {},
            columnState = ScalingLazyColumnState(),
        )
    }
}

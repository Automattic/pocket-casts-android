package au.com.shiftyjelly.pocketcasts.wear.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.wear.theme.WearAppTheme
import au.com.shiftyjelly.pocketcasts.wear.ui.component.WatchListChip
import au.com.shiftyjelly.pocketcasts.wear.ui.downloads.DownloadsScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.filters.FiltersScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.podcasts.PodcastsScreen
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnState
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.profile.R as PR

object WatchListScreen {
    const val route = "watch_list_screen"
}

@Composable
fun WatchListScreen(
    scrollState: ScalingLazyColumnState,
    navigateToRoute: (String) -> Unit,
    toNowPlaying: () -> Unit,
) {

    val viewModel = hiltViewModel<WatchListScreenViewModel>()
    val state by viewModel.state.collectAsState()
    val upNextState = state.upNextQueue

    CallOnce {
        viewModel.onShown()
    }

    ScalingLazyColumn(
        columnState = scrollState,
        modifier = Modifier.fillMaxWidth(),
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
                    navigateToRoute(PodcastsScreen.routeHomeFolder)
                }
            )
        }

        item {
            WatchListChip(
                title = stringResource(LR.string.downloads),
                iconRes = IR.drawable.ic_download,
                onClick = {
                    viewModel.onDownloadsClicked()
                    navigateToRoute(DownloadsScreen.route)
                }
            )
        }

        item {
            WatchListChip(
                title = stringResource(LR.string.filters),
                iconRes = IR.drawable.ic_filters,
                onClick = {
                    viewModel.onFiltersClicked()
                    navigateToRoute(FiltersScreen.route)
                }
            )
        }

        item {
            WatchListChip(
                title = stringResource(LR.string.profile_navigation_files),
                iconRes = PR.drawable.ic_file,
                onClick = {
                    viewModel.onFilesClicked()
                    navigateToRoute(FilesScreen.route)
                }
            )
        }

        item {
            WatchListChip(
                title = stringResource(LR.string.settings),
                iconRes = IR.drawable.ic_profile_settings,
                onClick = {
                    viewModel.onSettingsClicked()
                    navigateToRoute(SettingsScreen.route)
                }
            )
        }
    }
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
private fun WatchListPreview() {
    WearAppTheme {
        WatchListScreen(
            toNowPlaying = {},
            navigateToRoute = {},
            scrollState = ScalingLazyColumnState()
        )
    }
}

package au.com.shiftyjelly.pocketcasts.wear.ui

import androidx.compose.foundation.gestures.ScrollableDefaults
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
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.wear.theme.WearAppTheme
import au.com.shiftyjelly.pocketcasts.wear.ui.component.WatchListChip
import au.com.shiftyjelly.pocketcasts.wear.ui.downloads.DownloadsScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.podcasts.PodcastsScreen
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.profile.R as PR

object WatchListScreen {
    const val route = "watch_list_screen"

    // Key for boolean value in SavedStateHandle that is used to have this screen scroll to the top
    const val scrollToTop = "scroll_to_top"
}

@Composable
fun WatchListScreen(
    scrollState: ScalingLazyListState,
    navigateToRoute: (String) -> Unit,
    toNowPlaying: () -> Unit,
) {

    val viewModel = hiltViewModel<WatchListScreenViewModel>()
    val state by viewModel.state.collectAsState()
    val upNextState = state.upNextQueue

    ScalingLazyColumn(
        state = scrollState,
        flingBehavior = ScrollableDefaults.flingBehavior(),
        modifier = Modifier.fillMaxWidth(),
    ) {

        item {
            // Need this to position the first chip correctly when the screen loads
            Spacer(Modifier)
        }

        if (upNextState is UpNextQueue.State.Loaded) {
            item {
                NowPlayingChip(onClick = toNowPlaying)
            }
        }

        item {
            WatchListChip(
                title = stringResource(LR.string.podcasts),
                iconRes = IR.drawable.ic_podcasts,
                onClick = { navigateToRoute(PodcastsScreen.route) }
            )
        }

        item {
            WatchListChip(
                title = stringResource(LR.string.downloads),
                iconRes = IR.drawable.ic_download,
                onClick = { navigateToRoute(DownloadsScreen.route) }
            )
        }

        item {
            WatchListChip(
                title = stringResource(LR.string.filters),
                iconRes = IR.drawable.ic_filters,
                onClick = { navigateToRoute(FiltersScreen.route) }
            )
        }

        item {
            WatchListChip(
                title = stringResource(LR.string.profile_navigation_files),
                iconRes = PR.drawable.ic_file,
                onClick = { navigateToRoute(FilesScreen.route) }
            )
        }

        item {
            WatchListChip(
                title = stringResource(LR.string.settings),
                iconRes = IR.drawable.ic_profile_settings,
                onClick = { navigateToRoute(SettingsScreen.route) }
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
            scrollState = ScalingLazyListState()
        )
    }
}

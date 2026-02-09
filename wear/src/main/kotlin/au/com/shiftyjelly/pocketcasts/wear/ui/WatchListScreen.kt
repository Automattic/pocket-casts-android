package au.com.shiftyjelly.pocketcasts.wear.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import androidx.wear.tooling.preview.devices.WearDevices
import au.com.shiftyjelly.pocketcasts.compose.CallOnce
import au.com.shiftyjelly.pocketcasts.models.to.RefreshState
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.wear.theme.WearAppTheme
import au.com.shiftyjelly.pocketcasts.wear.ui.component.PullToRefresh
import au.com.shiftyjelly.pocketcasts.wear.ui.component.WatchListChip
import au.com.shiftyjelly.pocketcasts.wear.ui.downloads.DownloadsScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.playlists.PlaylistsScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.podcasts.PodcastsScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.settings.SettingsScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.starred.StarredScreen
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

    PullToRefresh(
        state = state.refreshState,
        onRefresh = { viewModel.refreshPodcasts() },
        modifier = modifier,
    ) {
        ScalingLazyColumn(
            columnState = columnState,
            modifier = Modifier.fillMaxWidth(),
        ) {
            item {
                // Need this to position the first chip correctly when the screen loads
                Spacer(Modifier)
            }

            if (state.refreshState is RefreshState.Refreshing) {
                item {
                    RefreshStatusHeader()
                }
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
                WatchListChip(
                    title = stringResource(LR.string.playlists),
                    iconRes = IR.drawable.ic_playlists,
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
                    title = stringResource(LR.string.profile_navigation_starred),
                    iconRes = IR.drawable.ic_starred,
                    onClick = {
                        viewModel.onStarredClicked()
                        navigateToRoute(StarredScreen.ROUTE)
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
}

@Composable
private fun RefreshStatusHeader(
    modifier: Modifier = Modifier,
) {
    val infiniteTransition = rememberInfiniteTransition(label = "refresh_rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "refresh_rotation",
    )

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            painter = painterResource(IR.drawable.ic_retry),
            contentDescription = null,
            modifier = Modifier
                .size(12.dp)
                .rotate(rotation),
            tint = MaterialTheme.colors.onSurfaceVariant.copy(alpha = 0.7f),
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = stringResource(LR.string.profile_refreshing),
            style = MaterialTheme.typography.caption3,
            color = MaterialTheme.colors.onSurfaceVariant.copy(alpha = 0.7f),
            textAlign = TextAlign.Start,
        )
    }
}

@Preview(device = WearDevices.SMALL_ROUND)
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

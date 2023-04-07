package au.com.shiftyjelly.pocketcasts.wear.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rxjava2.subscribeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.material.Chip
import androidx.wear.compose.material.ChipDefaults
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.wear.theme.WearAppTheme
import au.com.shiftyjelly.pocketcasts.wear.theme.theme
import au.com.shiftyjelly.pocketcasts.wear.ui.component.WatchListChip
import au.com.shiftyjelly.pocketcasts.wear.ui.downloads.DownloadsScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.player.NowPlayingScreen
import au.com.shiftyjelly.pocketcasts.wear.ui.podcasts.PodcastsScreen
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.profile.R as PR

object WatchListScreen {
    const val route = "watch_list_screen"
}

@Composable
fun WatchListScreen(
    navigateToRoute: (String) -> Unit,
    scrollState: ScalingLazyListState,
    upNextViewModel: UpNextViewModel = hiltViewModel(),
) {

    val upNextState by upNextViewModel.upNextQueue.subscribeAsState(null)

    ScalingLazyColumn(
        state = scrollState,
        modifier = Modifier.fillMaxWidth(),
    ) {

        item {
            // Need this to position the first chip correctly when the screen loads
            Spacer(Modifier)
        }

        item {
            WatchListChip(
                titleRes = LR.string.player_tab_playing_wide,
                iconRes = IR.drawable.ic_play_all,
                secondaryLabel = (upNextState as? UpNextQueue.State.Loaded)?.episode?.title,
                onClick = { navigateToRoute(NowPlayingScreen.route) },
            )
        }

        item {
            WatchListChip(
                titleRes = LR.string.podcasts,
                iconRes = IR.drawable.ic_podcasts,
                onClick = { navigateToRoute(PodcastsScreen.route) }
            )
        }

        item {
            val num = (upNextState as? UpNextQueue.State.Loaded)?.queue?.size ?: 0
            UpNextChip(
                navigateToRoute = navigateToRoute,
                numInUpNext = num
            )
        }

        item {
            WatchListChip(
                titleRes = LR.string.filters,
                iconRes = IR.drawable.ic_filters,
                onClick = { navigateToRoute(FiltersScreen.route) }
            )
        }

        item {
            WatchListChip(
                titleRes = LR.string.downloads,
                iconRes = IR.drawable.ic_download,
                onClick = { navigateToRoute(DownloadsScreen.route) }
            )
        }

        item {
            WatchListChip(
                titleRes = LR.string.profile_navigation_files,
                iconRes = PR.drawable.ic_file,
                onClick = { navigateToRoute(FilesScreen.route) }
            )
        }

        item {
            WatchListChip(
                titleRes = LR.string.settings,
                iconRes = IR.drawable.ic_profile_settings,
                onClick = { navigateToRoute(SettingsScreen.route) }
            )
        }
    }
}

@Composable
private fun UpNextChip(navigateToRoute: (String) -> Unit, numInUpNext: Int) {
    val title = stringResource(LR.string.up_next)
    Chip(
        onClick = { navigateToRoute(UpNextScreen.route) },
        colors = ChipDefaults.secondaryChipColors(),
        border = ChipDefaults.chipBorder(),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxHeight()
        ) {
            Box(
                modifier = Modifier.wrapContentSize(align = Alignment.Center),
                content = {
                    Icon(
                        painter = painterResource(IR.drawable.ic_upnext),
                        contentDescription = title
                    )
                }
            )

            Text(
                text = title,
                modifier = Modifier.padding(horizontal = 6.dp)
            )

            Spacer(modifier = Modifier.weight(1f))

            // Only show number in queue if queue isn't empty
            if (numInUpNext != 0) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .padding(vertical = 8.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.theme.colors.primaryIcon02Active)
                ) {
                    val num = if (numInUpNext < 100) numInUpNext.toString() else "99+"
                    Text(
                        text = num,
                        textAlign = TextAlign.Center,
                        color = MaterialTheme.theme.colors.primaryUi01,
                        modifier = Modifier.padding(horizontal = 6.dp)
                    )
                }
            }
        }
    }
}

@Preview(device = Devices.WEAR_OS_SMALL_ROUND, showSystemUi = true)
@Composable
private fun WatchListPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    WearAppTheme(themeType) {
        WatchListScreen(
            navigateToRoute = {},
            scrollState = ScalingLazyListState()
        )
    }
}

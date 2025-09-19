package au.com.shiftyjelly.pocketcasts.player.view.shelf

import android.view.View
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.mediarouter.app.MediaRouteButton
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.bottomsheet.Pill
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfSharedViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfSharedViewModel.ShelfItemSource
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfViewModel
import au.com.shiftyjelly.pocketcasts.preferences.model.ShelfItem
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.google.android.gms.cast.framework.CastButtonFactory
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun ShelfBottomSheetPage(
    shelfViewModel: ShelfViewModel,
    shelfSharedViewModel: ShelfSharedViewModel,
    playerViewModel: PlayerViewModel,
    onEditButtonClick: () -> Unit,
    onDismiss: () -> Unit,
) {
    val shelfUiState by shelfSharedViewModel.uiState.collectAsStateWithLifecycle()
    val performMediaRouteClick = remember { MutableSharedFlow<Unit>() }
    val coroutineScope = rememberCoroutineScope()
    Content(
        onEditButtonClick = {
            shelfViewModel.onEditButtonClick()
            onEditButtonClick()
        },
        mediaRouteButton = {
            MediaRouteButton(
                clickTrigger = performMediaRouteClick,
            )
        },
    ) {
        val uiState by shelfViewModel.uiState.collectAsState()
        MenuShelfItems(
            state = uiState,
            onClick = { item, enabled ->
                when (item) {
                    ShelfItem.Effects -> shelfSharedViewModel.onEffectsClick(ShelfItemSource.OverflowMenu)
                    ShelfItem.Sleep -> shelfSharedViewModel.onSleepClick(ShelfItemSource.OverflowMenu)
                    ShelfItem.Star -> shelfSharedViewModel.onStarClick(ShelfItemSource.OverflowMenu)
                    ShelfItem.Transcript -> shelfSharedViewModel.onTranscriptClick(enabled, ShelfItemSource.OverflowMenu)
                    ShelfItem.Share -> {
                        val podcast = playerViewModel.podcast ?: return@MenuShelfItems
                        val episode = playerViewModel.episode as? PodcastEpisode ?: return@MenuShelfItems

                        if (podcast.canShare) {
                            shelfSharedViewModel.onShareClick(podcast, episode, ShelfItemSource.OverflowMenu)
                        } else {
                            shelfSharedViewModel.onShareNotAvailable(ShelfItemSource.OverflowMenu)
                        }
                    }

                    ShelfItem.Podcast -> shelfSharedViewModel.onShowPodcastOrCloudFiles(playerViewModel.podcast, ShelfItemSource.OverflowMenu)
                    ShelfItem.Cast -> {
                        coroutineScope.launch {
                            shelfSharedViewModel.trackShelfAction(item, ShelfItemSource.OverflowMenu)
                            performMediaRouteClick.emit(Unit)
                            delay(100)
                            onDismiss()
                        }
                    }

                    ShelfItem.Played -> {
                        shelfSharedViewModel.onPlayedClick(
                            onMarkAsPlayedConfirmed = { episode, shouldShuffleUpNext ->
                                playerViewModel.markAsPlayedConfirmed(episode, shouldShuffleUpNext)
                            },
                            source = ShelfItemSource.OverflowMenu,
                        )
                    }

                    ShelfItem.Archive -> {
                        shelfSharedViewModel.onArchiveClick(
                            onArchiveConfirmed = { playerViewModel.archiveConfirmed(it) },
                            source = ShelfItemSource.OverflowMenu,
                        )
                    }

                    ShelfItem.Bookmark -> shelfSharedViewModel.onAddBookmarkClick(
                        OnboardingUpgradeSource.BOOKMARKS_SHELF_ACTION,
                        ShelfItemSource.OverflowMenu,
                    )
                    ShelfItem.Download -> {
                        playerViewModel.handleDownloadClickFromPlaybackActions(
                            onDownloadStart = { shelfSharedViewModel.onEpisodeDownloadStart(ShelfItemSource.OverflowMenu) },
                            onDeleteStart = { shelfSharedViewModel.onEpisodeRemoveClick(ShelfItemSource.OverflowMenu) },
                        )
                    }
                    ShelfItem.AddToPlaylist -> {
                        val episodeUuid = playerViewModel.episode?.uuid ?: return@MenuShelfItems
                        shelfSharedViewModel.onAddToPlaylistClick(
                            episodeUuid = episodeUuid,
                            source = ShelfItemSource.OverflowMenu,
                        )
                    }
                }
                if (item != ShelfItem.Cast) onDismiss()
            },
            onMove = { from, to ->
                shelfViewModel.onShelfItemMove(from, to)
            },
        )
    }
    LaunchedEffect(shelfUiState) {
        shelfViewModel.setData(shelfUiState.playerBottomSheetShelfItems, shelfUiState.episode)
    }
}

@Composable
private fun Content(
    onEditButtonClick: () -> Unit,
    mediaRouteButton: @Composable () -> Unit,
    menuShelfItems: @Composable () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth(),
    ) {
        Spacer(Modifier.height(12.dp))

        Pill(
            backgroundColor = MaterialTheme.theme.colors.playerContrast01,
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )

        Spacer(Modifier.height(8.dp))

        mediaRouteButton()

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp),
        ) {
            TextH30(
                text = stringResource(LR.string.player_more_actions),
                color = MaterialTheme.theme.colors.playerContrast01,
            )

            IconButton(
                onClick = onEditButtonClick,
            ) {
                Icon(
                    painter = painterResource(IR.drawable.ic_edit),
                    contentDescription = stringResource(LR.string.edit),
                    tint = MaterialTheme.theme.colors.playerContrast01,
                )
            }
        }
        menuShelfItems()
    }
}

@Composable
private fun MediaRouteButton(
    clickTrigger: Flow<Unit>,
) {
    val scope = rememberCoroutineScope()
    AndroidView(
        factory = { context ->
            MediaRouteButton(context).apply {
                visibility = View.GONE
                CastButtonFactory.setUpMediaRouteButton(context, this)
            }
        },
        update = { view ->
            scope.launch {
                clickTrigger.collect { view.performClick() }
            }
        },
    )
}

@Preview
@Composable
private fun ShelfBottomSheetPageContentPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        Content(
            onEditButtonClick = {},
            mediaRouteButton = {},
            menuShelfItems = {},
        )
    }
}

package au.com.shiftyjelly.pocketcasts.player.view.shelf

import android.view.View
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.map
import androidx.mediarouter.app.MediaRouteButton
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.PlayerColors
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfSharedViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfSharedViewModel.PlayerShelfData
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfSharedViewModel.ShelfItemSource
import au.com.shiftyjelly.pocketcasts.preferences.model.ShelfItem
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.extensions.updateColor
import com.airbnb.lottie.LottieProperty
import com.airbnb.lottie.SimpleColorFilter
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.LottieConstants
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.airbnb.lottie.compose.rememberLottieDynamicProperties
import com.airbnb.lottie.compose.rememberLottieDynamicProperty
import com.google.android.gms.cast.framework.CastButtonFactory
import android.graphics.Color as AndroidColor
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun PlayerShelf(
    playerColors: PlayerColors,
    shelfSharedViewModel: ShelfSharedViewModel,
    playerViewModel: PlayerViewModel,
    modifier: Modifier = Modifier,
) {
    val shelfItemsState by shelfSharedViewModel.uiState.collectAsStateWithLifecycle()
    val playerShelfData by playerViewModel.listDataLive
        .map {
            PlayerShelfData(
                theme = it.podcastHeader.theme,
                iconTintColor = it.podcastHeader.iconTintColor,
                isUserEpisode = it.podcastHeader.isUserEpisode,
                isEffectsOn = it.podcastHeader.isEffectsOn,
                isSleepRunning = it.podcastHeader.isSleepRunning,
                isStarred = it.podcastHeader.isStarred,
                downloadData = PlayerShelfData.DownloadData(
                    isDownloading = it.podcastHeader.episode?.isDownloading == true,
                    isQueued = it.podcastHeader.episode?.isQueued == true,
                    isDownloaded = it.podcastHeader.episode?.isDownloaded == true,
                ),
            )
        }
        .observeAsState(PlayerShelfData())

    PlayerShelfContent(
        shelfItems = shelfItemsState.playerShelfItems,
        isTranscriptAvailable = shelfItemsState.isTranscriptAvailable,
        playerShelfData = playerShelfData,
        playerColors = playerColors,
        onEffectsClick = {
            shelfSharedViewModel.onEffectsClick(ShelfItemSource.Shelf)
        },
        onSleepClick = {
            shelfSharedViewModel.onSleepClick(ShelfItemSource.Shelf)
        },
        onStarClick = {
            shelfSharedViewModel.onStarClick(ShelfItemSource.Shelf)
        },
        onShareClick = {
            val podcast = playerViewModel.podcast ?: return@PlayerShelfContent
            val episode = playerViewModel.episode as? PodcastEpisode ?: return@PlayerShelfContent

            if (podcast.canShare) {
                shelfSharedViewModel.onShareClick(podcast, episode, ShelfItemSource.Shelf)
            } else {
                shelfSharedViewModel.onShareNotAvailable(ShelfItemSource.Shelf)
            }
        },
        onShowPodcast = {
            shelfSharedViewModel.onShowPodcastOrCloudFiles(playerViewModel.podcast, ShelfItemSource.Shelf)
        },
        onCastClick = {
            shelfSharedViewModel.trackShelfAction(ShelfItem.Cast, ShelfItemSource.Shelf)
        },
        onPlayedClick = {
            shelfSharedViewModel.onPlayedClick(
                onMarkAsPlayedConfirmed = { episode, shouldShuffleUpNext ->
                    playerViewModel.markAsPlayedConfirmed(episode, shouldShuffleUpNext)
                },
                ShelfItemSource.Shelf,
            )
        },
        onArchiveClick = {
            shelfSharedViewModel.trackShelfAction(ShelfItem.Archive, ShelfItemSource.Shelf)
            shelfSharedViewModel.onArchiveClick(
                { playerViewModel.archiveConfirmed(it) },
                ShelfItemSource.Shelf,
            )
        },
        onDownloadClick = {
            playerViewModel.handleDownloadClickFromPlaybackActions(
                onDownloadStart = { shelfSharedViewModel.onEpisodeDownloadStart(ShelfItemSource.Shelf) },
                onDeleteStart = { shelfSharedViewModel.onEpisodeRemoveClick(ShelfItemSource.Shelf) },
            )
        },
        onAddBookmarkClick = {
            shelfSharedViewModel.onAddBookmarkClick(OnboardingUpgradeSource.BOOKMARKS_SHELF_ACTION, ShelfItemSource.Shelf)
        },
        onTranscriptClick = { isTranscriptAvailable: Boolean ->
            shelfSharedViewModel.onTranscriptClick(isTranscriptAvailable, ShelfItemSource.Shelf)
        },
        onMoreClick = {
            shelfSharedViewModel.onMoreClick()
        },
        onAddToPlaylistClick = {
            val episodeUuid = playerViewModel.episode?.uuid ?: return@PlayerShelfContent
            shelfSharedViewModel.onAddToPlaylistClick(episodeUuid, ShelfItemSource.Shelf)
        },
        modifier = modifier,
    )
}

@Composable
private fun PlayerShelfContent(
    shelfItems: List<ShelfItem>,
    isTranscriptAvailable: Boolean,
    playerShelfData: PlayerShelfData,
    onEffectsClick: () -> Unit,
    onSleepClick: () -> Unit,
    onStarClick: () -> Unit,
    onShareClick: () -> Unit,
    onShowPodcast: () -> Unit,
    onCastClick: () -> Unit,
    onPlayedClick: () -> Unit,
    onArchiveClick: () -> Unit,
    onDownloadClick: () -> Unit,
    onAddBookmarkClick: () -> Unit,
    onTranscriptClick: (Boolean) -> Unit,
    onAddToPlaylistClick: () -> Unit,
    onMoreClick: () -> Unit,
    modifier: Modifier = Modifier,
    playerColors: PlayerColors = MaterialTheme.theme.rememberPlayerColorsOrDefault(),
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(
                color = playerColors.contrast06,
                shape = RoundedCornerShape(12.dp),
            ),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        shelfItems.forEach { shelfItem ->
            when (shelfItem) {
                ShelfItem.Effects -> EffectsButton(
                    isEffectsOn = playerShelfData.isEffectsOn,
                    playerColors = playerColors,
                    onClick = onEffectsClick,
                )

                ShelfItem.Sleep -> SleepButton(
                    isSleepRunning = playerShelfData.isSleepRunning,
                    playerColors = playerColors,
                    onClick = onSleepClick,
                )

                ShelfItem.Star -> StarButton(
                    isStarred = playerShelfData.isStarred,
                    playerColors = playerColors,
                    onClick = onStarClick,
                )

                ShelfItem.Transcript -> TranscriptButton(
                    isTranscriptAvailable = isTranscriptAvailable,
                    playerColors = playerColors,
                    onClick = onTranscriptClick,
                )

                ShelfItem.Download -> DownloadButton(
                    isPodcastEpisode = !playerShelfData.isUserEpisode,
                    downloadData = playerShelfData.downloadData,
                    playerColors = playerColors,
                    onClick = onDownloadClick,
                )

                ShelfItem.Share -> ShareButton(
                    playerColors = playerColors,
                    onClick = onShareClick,
                )

                ShelfItem.Podcast -> PodcastButton(
                    playerColors = playerColors,
                    onClick = onShowPodcast,
                )

                ShelfItem.Cast -> CastButton(
                    playerColors = playerColors,
                    onClick = onCastClick,
                )

                ShelfItem.Played -> PlayedButton(
                    playerColors = playerColors,
                    onClick = onPlayedClick,
                )

                ShelfItem.Bookmark -> BookmarkButton(
                    playerColors = playerColors,
                    onClick = onAddBookmarkClick,
                )

                ShelfItem.Archive -> ArchiveButton(
                    isUserEpisode = playerShelfData.isUserEpisode,
                    playerColors = playerColors,
                    onClick = onArchiveClick,
                )

                ShelfItem.AddToPlaylist -> AddToPlaylistButton(
                    playerColors = playerColors,
                    onClick = onAddToPlaylistClick,
                )
            }
        }
        MoreButton(
            playerColors = playerColors,
            onClick = onMoreClick,
        )
    }
}

@Composable
private fun EffectsButton(
    isEffectsOn: Boolean,
    playerColors: PlayerColors,
    onClick: () -> Unit,
) {
    val effectsTint = if (isEffectsOn) playerColors.highlight01 else playerColors.contrast03
    val effectsResource = if (isEffectsOn) R.drawable.ic_effects_on_32 else R.drawable.ic_effects_off_32
    IconButton(onClick = onClick) {
        Icon(
            painterResource(id = effectsResource),
            contentDescription = stringResource(LR.string.player_effects),
            tint = effectsTint,
        )
    }
}

@Composable
private fun SleepButton(
    playerColors: PlayerColors,
    onClick: () -> Unit,
    isSleepRunning: Boolean = false,
) {
    val sleepTint = if (isSleepRunning) playerColors.highlight01 else playerColors.contrast03
    val alpha = if (isSleepRunning) 1f else AndroidColor.alpha(sleepTint.toArgb()) / 255f * 2f
    val lottieComposition by rememberLottieComposition(LottieCompositionSpec.RawRes(R.raw.sleep_button))
    val progress by animateLottieCompositionAsState(
        lottieComposition,
        iterations = LottieConstants.IterateForever,
        isPlaying = isSleepRunning,
    )
    val dynamicProperties = rememberLottieDynamicProperties(
        rememberLottieDynamicProperty(
            property = LottieProperty.COLOR_FILTER,
            value = SimpleColorFilter(sleepTint.toArgb()),
            keyPath = arrayOf("**"),
        ),
    )
    IconButton(
        onClick = onClick,
        modifier = Modifier.alpha(alpha),
    ) {
        LottieAnimation(
            composition = lottieComposition,
            progress = { if (isSleepRunning) progress else 0.5f },
            dynamicProperties = dynamicProperties,
        )
    }
}

@Composable
private fun StarButton(
    isStarred: Boolean,
    playerColors: PlayerColors,
    onClick: () -> Unit,
) {
    val starTint = if (isStarred) playerColors.highlight01 else playerColors.contrast03
    val starResource = if (isStarred) R.drawable.ic_star_filled_32 else R.drawable.ic_star_32
    IconButton(onClick = onClick) {
        Icon(
            painterResource(id = starResource),
            contentDescription = stringResource(LR.string.player_star),
            tint = starTint,
        )
    }
}

@Composable
private fun ShareButton(
    playerColors: PlayerColors,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            painterResource(id = R.drawable.ic_share_android_32),
            contentDescription = stringResource(LR.string.share_podcast),
            tint = playerColors.contrast03,
        )
    }
}

@Composable
private fun PodcastButton(
    playerColors: PlayerColors,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            painterResource(id = IR.drawable.ic_goto_32),
            contentDescription = stringResource(LR.string.go_to_podcast),
            tint = playerColors.contrast03,
        )
    }
}

@Composable
fun CastButton(
    playerColors: PlayerColors,
    onClick: () -> Unit,
) {
    AndroidView(
        factory = { context ->
            MediaRouteButton(context).apply {
                CastButtonFactory.setUpMediaRouteButton(context, this)
                visibility = View.VISIBLE
                updateColor(playerColors.contrast03.toArgb())
                setOnClickListener { onClick() }
                CastButtonFactory.setUpMediaRouteButton(context, this)
            }
        },
    )
}

@Composable
private fun PlayedButton(
    playerColors: PlayerColors,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            painterResource(id = R.drawable.ic_tick_circle_ol_32),
            contentDescription = stringResource(LR.string.mark_as_played),
            tint = playerColors.contrast03,
        )
    }
}

@Composable
private fun ArchiveButton(
    isUserEpisode: Boolean,
    playerColors: PlayerColors,
    onClick: () -> Unit,
) {
    val archiveResource = if (isUserEpisode) R.drawable.ic_delete_32 else R.drawable.ic_archive_32
    IconButton(onClick = onClick) {
        Icon(
            painterResource(id = archiveResource),
            contentDescription = stringResource(LR.string.archive_episode),
            tint = playerColors.contrast03,
        )
    }
}

@Composable
private fun AddToPlaylistButton(
    playerColors: PlayerColors,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            painterResource(id = IR.drawable.ic_playlist_add_episode),
            contentDescription = stringResource(LR.string.add_to_playlist_description),
            tint = playerColors.contrast03,
        )
    }
}

@Composable
private fun BookmarkButton(
    playerColors: PlayerColors,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            painterResource(id = IR.drawable.ic_bookmark),
            contentDescription = stringResource(LR.string.add_bookmark),
            tint = playerColors.contrast03,
        )
    }
}

@Composable
private fun TranscriptButton(
    isTranscriptAvailable: Boolean,
    playerColors: PlayerColors,
    onClick: (Boolean) -> Unit,
) {
    val alpha = if (isTranscriptAvailable) 1f else 0.4f
    IconButton(onClick = { onClick(isTranscriptAvailable) }) {
        Icon(
            painterResource(id = IR.drawable.ic_transcript_24),
            contentDescription = stringResource(LR.string.transcript),
            tint = playerColors.contrast03,
            modifier = Modifier.alpha(alpha),
        )
    }
}

@Composable
private fun DownloadButton(
    isPodcastEpisode: Boolean,
    downloadData: PlayerShelfData.DownloadData,
    playerColors: PlayerColors,
    onClick: () -> Unit,
) {
    val downloadIcon = when {
        isPodcastEpisode && (downloadData.isDownloading || downloadData.isQueued) -> IR.drawable.ic_download
        isPodcastEpisode && downloadData.isDownloaded -> IR.drawable.ic_downloaded_24dp
        else -> IR.drawable.ic_download
    }
    val contentDescription = when {
        isPodcastEpisode && (downloadData.isDownloading || downloadData.isQueued) -> stringResource(LR.string.episode_downloading)
        isPodcastEpisode && downloadData.isDownloaded -> stringResource(LR.string.remove_downloaded_file)
        else -> stringResource(LR.string.download)
    }
    IconButton(onClick = onClick) {
        Icon(
            painterResource(id = downloadIcon),
            contentDescription = contentDescription,
            tint = playerColors.contrast03,
        )
    }
}

@Composable
private fun MoreButton(
    playerColors: PlayerColors,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            painterResource(id = R.drawable.ic_more),
            contentDescription = stringResource(LR.string.more),
            tint = playerColors.contrast03,
        )
    }
}

@Preview
@Composable
private fun PlayerShelfPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        PlayerShelfContent(
            shelfItems = ShelfItem.entries.toList().take(4),
            isTranscriptAvailable = false,
            playerShelfData = PlayerShelfData(),
            onEffectsClick = {},
            onSleepClick = {},
            onStarClick = {},
            onShareClick = {},
            onShowPodcast = {},
            onCastClick = {},
            onPlayedClick = {},
            onArchiveClick = {},
            onDownloadClick = {},
            onAddBookmarkClick = {},
            onTranscriptClick = {},
            onAddToPlaylistClick = {},
            onMoreClick = {},
        )
    }
}

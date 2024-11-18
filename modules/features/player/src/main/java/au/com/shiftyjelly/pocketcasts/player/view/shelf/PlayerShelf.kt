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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
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
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.player.R
import au.com.shiftyjelly.pocketcasts.player.view.transcripts.TranscriptViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfSharedViewModel
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfSharedViewModel.PlayerShelfData
import au.com.shiftyjelly.pocketcasts.player.viewmodel.ShelfSharedViewModel.ShelfItemSource
import au.com.shiftyjelly.pocketcasts.preferences.model.ShelfItem
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.ui.helper.ColorUtils
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.ui.theme.ThemeColor
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
    theme: Theme,
    shelfSharedViewModel: ShelfSharedViewModel,
    transcriptViewModel: TranscriptViewModel,
    playerViewModel: PlayerViewModel,
) {
    val transcriptUiState by transcriptViewModel.uiState.collectAsStateWithLifecycle()
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

    val normalColor = MaterialTheme.theme.colors.playerContrast03
    val iconColors = remember(theme, playerShelfData) {
        val playerHighlightColor = playerHighlightColor(theme, playerShelfData)
        val highlightColor = Color(AndroidColor.parseColor(ColorUtils.colorIntToHexString(playerHighlightColor)))
        PlayerShelfIconColors(
            normalColor = normalColor,
            highlightColor = highlightColor,
        )
    }

    PlayerShelfContent(
        shelfItems = shelfItemsState.playerShelfItems,
        transcriptUiState = transcriptUiState,
        iconColors = iconColors,
        playerShelfData = playerShelfData,
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
            shelfSharedViewModel.onShareClick(podcast, episode, ShelfItemSource.Shelf)
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
        onReportClick = {
            shelfSharedViewModel.onReportClick(ShelfItemSource.Shelf)
        },
        onMoreClick = {
            shelfSharedViewModel.onMoreClick()
        },
    )
}

@Composable
private fun PlayerShelfContent(
    shelfItems: List<ShelfItem>,
    transcriptUiState: TranscriptViewModel.UiState,
    iconColors: PlayerShelfIconColors,
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
    onReportClick: () -> Unit,
    onMoreClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .background(
                color = MaterialTheme.theme.colors.playerContrast06,
                shape = RoundedCornerShape(12.dp),
            ),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        shelfItems.forEach { shelfItem ->
            when (shelfItem) {
                ShelfItem.Effects -> EffectsButton(
                    isEffectsOn = playerShelfData.isEffectsOn,
                    iconColors = iconColors,
                    onClick = onEffectsClick,
                )
                ShelfItem.Sleep -> SleepButton(
                    isSleepRunning = playerShelfData.isSleepRunning,
                    iconColors = iconColors,
                    onClick = onSleepClick,
                )
                ShelfItem.Star -> StarButton(
                    isStarred = playerShelfData.isStarred,
                    iconColors = iconColors,
                    onClick = onStarClick,
                )
                ShelfItem.Transcript -> TranscriptButton(
                    isTranscriptAvailable = transcriptUiState !is TranscriptViewModel.UiState.Empty,
                    iconColors = iconColors,
                    onClick = onTranscriptClick,
                )
                ShelfItem.Download -> DownloadButton(
                    isPodcastEpisode = !playerShelfData.isUserEpisode,
                    downloadData = playerShelfData.downloadData,
                    iconColors = iconColors,
                    onClick = onDownloadClick,
                )
                ShelfItem.Share -> ShareButton(
                    iconColors = iconColors,
                    onClick = onShareClick,
                )
                ShelfItem.Podcast -> PodcastButton(
                    iconColors = iconColors,
                    onClick = onShowPodcast,
                )
                ShelfItem.Cast -> CastButton(
                    iconColors = iconColors,
                    onClick = onCastClick,
                )
                ShelfItem.Played -> PlayedButton(
                    iconColors = iconColors,
                    onClick = onPlayedClick,
                )
                ShelfItem.Bookmark -> BookmarkButton(
                    iconColors = iconColors,
                    onClick = onAddBookmarkClick,
                )
                ShelfItem.Archive -> ArchiveButton(
                    isUserEpisode = playerShelfData.isUserEpisode,
                    iconColors = iconColors,
                    onClick = onArchiveClick,
                )
                ShelfItem.Report -> ReportButton(
                    iconColors = iconColors,
                    onClick = onReportClick,
                )
            }
        }
        MoreButton(
            iconColors = iconColors,
            onClick = onMoreClick,
        )
    }
}

@Composable
fun EffectsButton(
    isEffectsOn: Boolean,
    iconColors: PlayerShelfIconColors,
    onClick: () -> Unit,
) {
    val effectsTint =
        if (isEffectsOn) iconColors.highlightColor else iconColors.normalColor
    val effectsResource =
        if (isEffectsOn) R.drawable.ic_effects_on_32 else R.drawable.ic_effects_off_32
    IconButton(onClick = onClick) {
        Icon(
            painterResource(id = effectsResource),
            contentDescription = stringResource(LR.string.player_effects),
            tint = effectsTint,
        )
    }
}

@Composable
fun SleepButton(
    isSleepRunning: Boolean = false,
    iconColors: PlayerShelfIconColors,
    onClick: () -> Unit,
) {
    val sleepTint =
        if (isSleepRunning) iconColors.highlightColor else iconColors.normalColor
    val alpha = if (isSleepRunning) 1F else AndroidColor.alpha(sleepTint.toArgb()) / 255F * 2F
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
fun StarButton(
    isStarred: Boolean,
    iconColors: PlayerShelfIconColors,
    onClick: () -> Unit,
) {
    val starTint = if (isStarred) iconColors.highlightColor else iconColors.normalColor
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
fun ShareButton(
    iconColors: PlayerShelfIconColors,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            painterResource(id = R.drawable.ic_share_android_32),
            contentDescription = stringResource(LR.string.share_podcast),
            tint = iconColors.normalColor,
        )
    }
}

@Composable
fun PodcastButton(
    iconColors: PlayerShelfIconColors,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            painterResource(id = IR.drawable.ic_goto_32),
            contentDescription = stringResource(LR.string.go_to_podcast),
            tint = iconColors.normalColor,
        )
    }
}

@Composable
fun CastButton(
    iconColors: PlayerShelfIconColors,
    onClick: () -> Unit,
) {
    AndroidView(
        factory = { context ->
            MediaRouteButton(context).apply {
                CastButtonFactory.setUpMediaRouteButton(context, this)
                visibility = View.VISIBLE
                updateColor(iconColors.normalColor.toArgb())
                setOnClickListener { onClick() }
                CastButtonFactory.setUpMediaRouteButton(context, this)
            }
        },
    )
}

@Composable
fun PlayedButton(
    iconColors: PlayerShelfIconColors,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            painterResource(id = R.drawable.ic_tick_circle_ol_32),
            contentDescription = stringResource(LR.string.mark_as_played),
            tint = iconColors.normalColor,
        )
    }
}

@Composable
fun ArchiveButton(
    isUserEpisode: Boolean,
    iconColors: PlayerShelfIconColors,
    onClick: () -> Unit,
) {
    val archiveResource = (if (isUserEpisode) R.drawable.ic_delete_32 else R.drawable.ic_archive_32)
    IconButton(onClick = onClick) {
        Icon(
            painterResource(id = archiveResource),
            contentDescription = stringResource(LR.string.archive_episode),
            tint = iconColors.normalColor,
        )
    }
}

@Composable
fun BookmarkButton(
    iconColors: PlayerShelfIconColors,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            painterResource(id = IR.drawable.ic_bookmark),
            contentDescription = stringResource(LR.string.add_bookmark),
            tint = iconColors.normalColor,
        )
    }
}

@Composable
fun TranscriptButton(
    isTranscriptAvailable: Boolean,
    iconColors: PlayerShelfIconColors,
    onClick: (Boolean) -> Unit,
) {
    val alpha = if (isTranscriptAvailable) 1f else 0.4f
    IconButton(onClick = { onClick(isTranscriptAvailable) }) {
        Icon(
            painterResource(id = IR.drawable.ic_transcript_24),
            contentDescription = stringResource(LR.string.transcript),
            tint = iconColors.normalColor,
            modifier = Modifier.alpha(alpha),
        )
    }
}

@Composable
fun DownloadButton(
    isPodcastEpisode: Boolean,
    downloadData: PlayerShelfData.DownloadData,
    iconColors: PlayerShelfIconColors,
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
            tint = iconColors.normalColor,
        )
    }
}

@Composable
fun ReportButton(
    iconColors: PlayerShelfIconColors,
    onClick: () -> Unit,
) {
    IconButton(onClick = onClick) {
        Icon(
            painterResource(id = IR.drawable.ic_flag),
            contentDescription = stringResource(LR.string.report),
            tint = iconColors.normalColor,
        )
    }
}

@Composable
fun MoreButton(
    onClick: () -> Unit,
    iconColors: PlayerShelfIconColors,
) {
    IconButton(onClick = onClick) {
        Icon(
            painterResource(id = R.drawable.ic_more),
            contentDescription = stringResource(LR.string.more),
            tint = iconColors.normalColor,
        )
    }
}

private fun playerHighlightColor(
    theme: Theme,
    playerShelfData: PlayerShelfData,
) = if (playerShelfData.isUserEpisode) {
    theme.getUserFilePlayerHighlightColor()
} else {
    ThemeColor.playerHighlight01(playerShelfData.theme, playerShelfData.iconTintColor)
}

data class PlayerShelfIconColors(
    val normalColor: Color,
    val highlightColor: Color,
)

@Preview
@Composable
private fun PlayerShelfPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        PlayerShelfContent(
            shelfItems = ShelfItem.entries.toList().take(4),
            transcriptUiState = TranscriptViewModel.UiState.Empty(),
            iconColors = PlayerShelfIconColors(
                normalColor = MaterialTheme.theme.colors.playerContrast03,
                highlightColor = MaterialTheme.theme.colors.playerContrast01,
            ),
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
            onReportClick = {},
            onMoreClick = {},
        )
    }
}

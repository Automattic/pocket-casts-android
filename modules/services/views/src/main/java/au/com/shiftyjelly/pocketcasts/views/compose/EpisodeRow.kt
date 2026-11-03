package au.com.shiftyjelly.pocketcasts.views.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.swipe.rememberSwipeRowState
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.repositories.extensions.getSummaryText
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeRowData
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeRowDataProvider
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.views.buttons.PlayButtonType
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeAction
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeRowActions
import au.com.shiftyjelly.pocketcasts.views.swipe.leadingSwipeRowActions
import au.com.shiftyjelly.pocketcasts.views.swipe.trailingSwipeRowActions
import com.automattic.eventhorizon.EventHorizon
import java.util.Date

sealed interface EpisodeRowTrailing {
    data class PlayButton(
        val listener: au.com.shiftyjelly.pocketcasts.views.buttons.PlayButton.OnClickListener,
        val eventHorizon: EventHorizon,
        val fromListUuid: String? = null,
    ) : EpisodeRowTrailing

    class Content(val content: @Composable () -> Unit) : EpisodeRowTrailing

    data object None : EpisodeRowTrailing
}

internal val InitialRowData = EpisodeRowData(
    downloadProgress = 0,
    playbackState = PlaybackState(),
    isInUpNext = false,
    hasBookmarks = false,
    hasHlsAlternateEnclosure = false,
)

@Composable
fun EpisodeRow(
    episode: PodcastEpisode,
    rowDataProvider: EpisodeRowDataProvider,
    imageRequestFactory: PocketCastsImageRequestFactory,
    swipeActions: () -> SwipeRowActions?,
    trailing: EpisodeRowTrailing,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSwipeAction: (SwipeAction) -> Unit,
    modifier: Modifier = Modifier,
    showArtwork: Boolean = true,
    useEpisodeArtwork: Boolean = false,
    streamByDefault: Boolean = false,
    isMultiSelecting: Boolean = false,
    isSelected: Boolean = false,
    animateMultiSelection: Boolean = false,
    colors: EpisodeRowColors = EpisodeRowDefaults.colors(),
) {
    key(episode.uuid) {
        val rowDataFlow = remember(rowDataProvider) { rowDataProvider.episodeRowDataFlow(episode.uuid) }
        val rowData by rowDataFlow.collectAsStateWithLifecycle(initialValue = InitialRowData)
        EpisodeRow(
            episode = episode,
            rowData = rowData,
            imageRequestFactory = imageRequestFactory,
            swipeActions = swipeActions,
            trailing = trailing,
            onClick = onClick,
            onLongClick = onLongClick,
            onSwipeAction = onSwipeAction,
            modifier = modifier,
            showArtwork = showArtwork,
            useEpisodeArtwork = useEpisodeArtwork,
            streamByDefault = streamByDefault,
            isMultiSelecting = isMultiSelecting,
            isSelected = isSelected,
            animateMultiSelection = animateMultiSelection,
            colors = colors,
        )
    }
}

@Composable
internal fun EpisodeRow(
    episode: PodcastEpisode,
    rowData: EpisodeRowData,
    imageRequestFactory: PocketCastsImageRequestFactory,
    swipeActions: () -> SwipeRowActions?,
    trailing: EpisodeRowTrailing,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSwipeAction: (SwipeAction) -> Unit,
    modifier: Modifier = Modifier,
    showArtwork: Boolean = true,
    useEpisodeArtwork: Boolean = false,
    streamByDefault: Boolean = false,
    isMultiSelecting: Boolean = false,
    isSelected: Boolean = false,
    animateMultiSelection: Boolean = false,
    colors: EpisodeRowColors = EpisodeRowDefaults.colors(),
) {
    val context = LocalContext.current
    val isPlaying = rowData.playbackState.isPlaying && rowData.playbackState.episodeUuid == episode.uuid
    val buttonType = PlayButtonType.from(episode, streamByDefault, isPlaying)
    val status = EpisodeRowStatus.from(episode, rowData, context)
    val dateFormatter = remember(context) { RelativeDateFormatter(context) }
    val date = episode.getSummaryText(dateFormatter, colors.tint.toArgb(), showDuration = false, context)
    val actions = swipeActions() ?: SwipeRowActions.Empty
    val isGreyedOut = episode.playingStatus == EpisodePlayingStatus.COMPLETED || episode.isArchived
    EpisodeRowContainer(
        swipeRowState = rememberSwipeRowState(episode.uuid),
        leadingActions = actions.leadingSwipeRowActions(context, onSwipeAction),
        trailingActions = actions.trailingSwipeRowActions(context, onSwipeAction),
        isMultiSelecting = isMultiSelecting,
        isSelected = isSelected,
        colors = colors,
        contentDescription = episodeRowContentDescription(episode, date, status, rowData.isInUpNext, context),
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
    ) {
        EpisodeRowMainLayout(
            transition = rememberMultiSelectionTransition(isMultiSelecting, animateMultiSelection),
            checkbox = { EpisodeRowCheckbox(isSelected, onClick) },
            artwork = if (showArtwork) {
                { EpisodeRowArtwork(episode, useEpisodeArtwork, imageRequestFactory, isGreyedOut) }
            } else {
                null
            },
            trailing = trailing.content(episode, buttonType, colors),
        ) {
            Column {
                EpisodeRowHeader(date, colors.secondaryText(isGreyedOut), episode.showsVideoIcon(rowData.hasHlsAlternateEnclosure), colors)
                EpisodeRowTitle(episode.title, colors.title(isGreyedOut), if (episode.isStarred) colors.theme.support08 else null)
                StatusLine(
                    text = status.text,
                    bottomPadding = with(LocalDensity.current) { 12.dp.roundToPx().toDp() },
                    isGreyedOut = isGreyedOut,
                    colors = colors,
                    isInUpNext = rowData.isInUpNext,
                    iconId = status.iconId,
                    iconTint = colors.statusIconTint(status.iconTint),
                    hasBookmarks = rowData.hasBookmarks,
                    progressCircle = status.progressCircle,
                    isBuffering = status.isBuffering,
                    modifier = Modifier.clearAndSetSemantics {},
                )
            }
        }
    }
}

internal fun EpisodeRowTrailing.content(
    episode: BaseEpisode,
    buttonType: PlayButtonType,
    colors: EpisodeRowColors,
): (@Composable () -> Unit)? = when (this) {
    is EpisodeRowTrailing.PlayButton -> {
        { EpisodePlayButton(episode, buttonType, colors.tint, listener, eventHorizon, fromListUuid = fromListUuid) }
    }

    is EpisodeRowTrailing.Content -> content

    EpisodeRowTrailing.None -> null
}

@Preview
@Composable
private fun EpisodeRowPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        Column {
            listOf(EpisodePlayingStatus.NOT_PLAYED, EpisodePlayingStatus.COMPLETED).forEach { playingStatus ->
                EpisodeRow(
                    episode = PodcastEpisode(
                        uuid = playingStatus.name,
                        title = "Episode title",
                        publishedDate = Date(),
                        duration = 3600.0,
                        playingStatus = playingStatus,
                        isStarred = true,
                    ),
                    rowData = InitialRowData.copy(isInUpNext = true, hasBookmarks = true),
                    imageRequestFactory = PocketCastsImageRequestFactory(LocalContext.current),
                    swipeActions = { null },
                    trailing = EpisodeRowTrailing.None,
                    onClick = {},
                    onLongClick = {},
                    onSwipeAction = {},
                )
            }
        }
    }
}

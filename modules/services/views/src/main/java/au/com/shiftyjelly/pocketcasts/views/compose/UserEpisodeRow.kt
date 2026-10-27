package au.com.shiftyjelly.pocketcasts.views.compose

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import au.com.shiftyjelly.pocketcasts.compose.swipe.rememberSwipeRowState
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.repositories.extensions.getSummaryText
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeRowDataProvider
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeRowData
import au.com.shiftyjelly.pocketcasts.views.buttons.PlayButtonType
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeAction
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeRowActions
import au.com.shiftyjelly.pocketcasts.views.swipe.leadingSwipeRowActions
import au.com.shiftyjelly.pocketcasts.views.swipe.trailingSwipeRowActions

@Composable
fun UserEpisodeRow(
    episode: UserEpisode,
    rowDataProvider: EpisodeRowDataProvider,
    imageRequestFactory: PocketCastsImageRequestFactory,
    swipeActions: () -> SwipeRowActions,
    trailing: EpisodeRowTrailing,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSwipeAction: (SwipeAction) -> Unit,
    modifier: Modifier = Modifier,
    useEpisodeArtwork: Boolean = false,
    streamByDefault: Boolean = false,
    isMultiSelecting: Boolean = false,
    isSelected: Boolean = false,
    animateMultiSelection: Boolean = false,
    colors: EpisodeRowColors = EpisodeRowDefaults.colors(),
) {
    val rowDataFlow = remember(episode.uuid) { rowDataProvider.userEpisodeRowDataFlow(episode.uuid) }
    val rowData by rowDataFlow.collectAsStateWithLifecycle(initialValue = null)
    UserEpisodeRow(
        episode = episode,
        rowData = rowData,
        imageRequestFactory = imageRequestFactory,
        swipeActions = swipeActions,
        trailing = trailing,
        onClick = onClick,
        onLongClick = onLongClick,
        onSwipeAction = onSwipeAction,
        modifier = modifier,
        useEpisodeArtwork = useEpisodeArtwork,
        streamByDefault = streamByDefault,
        isMultiSelecting = isMultiSelecting,
        isSelected = isSelected,
        animateMultiSelection = animateMultiSelection,
        colors = colors,
    )
}

@Composable
internal fun UserEpisodeRow(
    episode: UserEpisode,
    rowData: UserEpisodeRowData?,
    imageRequestFactory: PocketCastsImageRequestFactory,
    swipeActions: () -> SwipeRowActions,
    trailing: EpisodeRowTrailing,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onSwipeAction: (SwipeAction) -> Unit,
    modifier: Modifier = Modifier,
    useEpisodeArtwork: Boolean = false,
    streamByDefault: Boolean = false,
    isMultiSelecting: Boolean = false,
    isSelected: Boolean = false,
    animateMultiSelection: Boolean = false,
    colors: EpisodeRowColors = EpisodeRowDefaults.colors(),
) {
    val context = LocalContext.current
    val statusData = rowData ?: UserEpisodeRowData(episode, 0, 0, PlaybackState(), isInUpNext = false, hasBookmarks = false)
    val playbackState = statusData.playbackState
    val isPlaying = playbackState.isPlaying && playbackState.episodeUuid == episode.uuid
    val buttonType = remember(episode, isPlaying, streamByDefault) {
        episode.playing = isPlaying
        PlayButtonType.from(episode, streamByDefault)
    }
    val dateFormatter = remember(context) { RelativeDateFormatter(context) }
    val date = episode.getSummaryText(dateFormatter, colors.tint.toArgb(), showDuration = false, context)
    val actions = remember(episode, rowData) { swipeActions() }
    val isGreyedOut = episode.playingStatus == EpisodePlayingStatus.COMPLETED || episode.isArchived
    EpisodeRowContainer(
        swipeRowState = rememberSwipeRowState(episode.uuid),
        leadingActions = actions.leadingSwipeRowActions(context, onSwipeAction),
        trailingActions = actions.trailingSwipeRowActions(context, onSwipeAction),
        isMultiSelecting = isMultiSelecting,
        isSelected = isSelected,
        colors = colors,
        contentDescription = "${episode.title} $date ${fileStatusText(statusData)}",
        onClick = onClick,
        onLongClick = onLongClick,
        modifier = modifier,
    ) {
        EpisodeRowMainLayout(
            transition = rememberMultiSelectionTransition(isMultiSelecting, animateMultiSelection),
            checkbox = { EpisodeRowCheckbox(isSelected, onClick) },
            artwork = { EpisodeRowArtwork(episode, useEpisodeArtwork, imageRequestFactory, isGreyedOut) },
            trailing = trailing.content(episode, buttonType, colors),
        ) {
            Column {
                EpisodeRowHeader(date, colors.secondaryText(isGreyedOut), episode.isVideo, colors)
                EpisodeRowTitle(episode.title, colors.title(isGreyedOut), starTint = null)
                FileStatusIcons(statusData, colors, hideErrorDetails = false, modifier = Modifier.clearAndSetSemantics {})
            }
        }
    }
}

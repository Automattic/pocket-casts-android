package au.com.shiftyjelly.pocketcasts.playlists.manual

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.AppBarDefaults
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.bars.NavigationButton
import au.com.shiftyjelly.pocketcasts.compose.bars.ThemedTopAppBar
import au.com.shiftyjelly.pocketcasts.compose.components.EpisodeImage
import au.com.shiftyjelly.pocketcasts.compose.components.FadedLazyColumn
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextC70
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH60
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.text.toAnnotatedString
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.ManualPlaylistEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.repositories.extensions.getSummaryText
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory.PlaceholderType
import au.com.shiftyjelly.pocketcasts.ui.extensions.getThemeColor
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme.ThemeType
import java.util.Date
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@Composable
internal fun EditPlaylistPage(
    episodes: List<PlaylistEpisode>,
    useEpisodeArtwork: Boolean,
    onReorderEpisodes: (List<PlaylistEpisode>) -> Unit,
    onDeleteEpisode: (PlaylistEpisode) -> Unit,
    onClickBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val dateFormatter = remember(context) { RelativeDateFormatter(context) }

    Column(
        modifier = modifier
            .background(MaterialTheme.theme.colors.primaryUi02)
            .fillMaxSize(),
    ) {
        ThemedTopAppBar(
            modifier = Modifier.fillMaxWidth(),
            title = stringResource(LR.string.playlist_edit_episodes_title),
            navigationButton = NavigationButton.Back,
            style = ThemedTopAppBar.Style.Immersive,
            backgroundColor = Color.Transparent,
            windowInsets = AppBarDefaults.topAppBarWindowInsets.only(WindowInsetsSides.Vertical),
            onNavigationClick = onClickBack,
        )

        val hapticFeedback = LocalHapticFeedback.current
        val listState = rememberLazyListState()
        val reorderableListState = rememberReorderableLazyListState(listState) { from, to ->
            val newEpisodes = episodes.toMutableList().apply {
                add(to.index, removeAt(from.index))
            }
            onReorderEpisodes(newEpisodes)
            hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
        }
        val baseColor = MaterialTheme.theme.colors.primaryUi02
        val highlightColor = MaterialTheme.theme.colors.primaryIcon01
        val draggedColor = remember(highlightColor, baseColor) {
            highlightColor.copy(alpha = 0.15f).compositeOver(baseColor.copy(alpha = 1f))
        }

        FadedLazyColumn(
            modifier = Modifier.weight(1f),
            state = listState,
            contentPadding = WindowInsets.navigationBars.asPaddingValues(),
        ) {
            itemsIndexed(
                items = episodes,
                key = { _, episode -> episode.uuid },
            ) { index, episode ->
                ReorderableItem(reorderableListState, key = episode.uuid) { isDragging ->
                    val elevation by animateDpAsState(if (isDragging) 4.dp else 0.dp)
                    val backgroundColor by animateColorAsState(if (isDragging) draggedColor else baseColor)

                    EpisodeRow(
                        episodeWrapper = episode,
                        useEpisodeArtwork = useEpisodeArtwork,
                        showDivider = index != episodes.lastIndex,
                        dateFormatter = dateFormatter,
                        onClickDelete = { onDeleteEpisode(episode) },
                        modifier = Modifier
                            .shadow(elevation)
                            .background(backgroundColor)
                            .longPressDraggableHandle(
                                onDragStarted = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragStopped = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                },
                            )
                            .animateItem(),
                        dragHandleModifier = Modifier
                            .draggableHandle(
                                onDragStarted = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                                },
                                onDragStopped = {
                                    hapticFeedback.performHapticFeedback(HapticFeedbackType.GestureEnd)
                                },
                            ),
                    )
                }
            }
        }
    }
}

@Composable
private fun EpisodeRow(
    episodeWrapper: PlaylistEpisode,
    useEpisodeArtwork: Boolean,
    showDivider: Boolean,
    dateFormatter: RelativeDateFormatter,
    onClickDelete: () -> Unit,
    modifier: Modifier = Modifier,
    dragHandleModifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(IntrinsicSize.Min)
                .padding(vertical = 12.dp, horizontal = 4.dp),
        ) {
            IconButton(
                onClick = onClickDelete,
            ) {
                Icon(
                    painter = painterResource(IR.drawable.ic_playlist_remove_episode),
                    contentDescription = null,
                    tint = MaterialTheme.theme.colors.support05,
                )
            }
            PlaylistEpisodeImage(
                episodeWrapper = episodeWrapper,
                useEpisodeArtwork = useEpisodeArtwork,
                modifier = Modifier.alphaIfUnavailable(episodeWrapper),
            )
            Spacer(
                modifier = Modifier.width(12.dp),
            )
            Column(
                verticalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .alphaIfUnavailable(episodeWrapper),
            ) {
                TextC70(
                    text = episodeWrapper.rememberHeaderText(dateFormatter),
                )
                TextH40(
                    text = episodeWrapper.title(),
                    lineHeight = 15.sp,
                    maxLines = 2,
                )
                Footer(
                    episodeWrapper = episodeWrapper,
                )
            }
            Box(
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .then(dragHandleModifier),
            ) {
                Icon(
                    painter = painterResource(IR.drawable.ic_playlist_edit),
                    contentDescription = null,
                    tint = MaterialTheme.theme.colors.primaryIcon02,
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(startIndent = 16.dp)
        }
    }
}

@Composable
private fun PlaylistEpisodeImage(
    episodeWrapper: PlaylistEpisode,
    useEpisodeArtwork: Boolean,
    modifier: Modifier = Modifier,
) {
    when (episodeWrapper) {
        is PlaylistEpisode.Available -> {
            EpisodeImage(
                episode = episodeWrapper.episode,
                useEpisodeArtwork = useEpisodeArtwork,
                placeholderType = PlaceholderType.Small,
                corners = 4.dp,
                modifier = modifier
                    .size(56.dp)
                    .shadow(1.dp, RoundedCornerShape(4.dp)),
            )
        }

        is PlaylistEpisode.Unavailable -> {
            PodcastImage(
                uuid = episodeWrapper.episode.podcastUuid,
                placeholderType = PlaceholderType.Small,
                cornerSize = 4.dp,
                elevation = 1.dp,
                imageSize = 56.dp,
                modifier = modifier,
            )
        }
    }
}

@Composable
private fun Footer(
    episodeWrapper: PlaylistEpisode,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.Bottom,
        modifier = modifier,
    ) {
        val isArchived = episodeWrapper is PlaylistEpisode.Available && episodeWrapper.episode.isArchived
        if (isArchived) {
            Image(
                painter = painterResource(IR.drawable.ic_archive),
                contentDescription = null,
                colorFilter = ColorFilter.tint(MaterialTheme.theme.colors.primaryText02),
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(14.dp),
            )
        }
        TextH60(
            text = buildString {
                if (isArchived) {
                    append(stringResource(LR.string.archived))
                    append(" • ")
                }
                append(episodeWrapper.rememberFooterText())
            },
            color = MaterialTheme.theme.colors.primaryText02,
        )
    }
}

@Composable
private fun PlaylistEpisode.rememberHeaderText(formatter: RelativeDateFormatter): AnnotatedString {
    return when (this) {
        is PlaylistEpisode.Available -> remember(episode.playingStatus, formatter) {
            val tintColor = formatter.context.getThemeColor(UR.attr.primary_icon_01)
            val spannable = episode.getSummaryText(formatter, tintColor, showDuration = false, formatter.context)
            spannable.toAnnotatedString()
        }

        is PlaylistEpisode.Unavailable -> remember(episode.publishedAt, formatter) {
            AnnotatedString(formatter.format(Date.from(episode.publishedAt)))
        }
    }
}

private fun PlaylistEpisode.title(): String {
    return when (this) {
        is PlaylistEpisode.Available -> episode.title
        is PlaylistEpisode.Unavailable -> episode.title
    }
}

@Composable
private fun PlaylistEpisode.rememberFooterText(): String {
    val context = LocalContext.current
    return when (this) {
        is PlaylistEpisode.Available -> remember(episode.playedUpToMs, episode.durationMs, episode.isInProgress, context) {
            TimeHelper.getTimeLeft(episode.playedUpToMs, episode.durationMs.toLong(), episode.isInProgress, context).text
        }

        is PlaylistEpisode.Unavailable -> stringResource(LR.string.unavailable)
    }
}

private fun Modifier.alphaIfUnavailable(episodeWrapper: PlaylistEpisode) = when (episodeWrapper) {
    is PlaylistEpisode.Available -> if (episodeWrapper.episode.isArchived) alpha(0.4f) else this
    is PlaylistEpisode.Unavailable -> alpha(0.4f)
}

@Preview
@Composable
private fun EditPlaylistPagePreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: ThemeType,
) {
    var episodes by remember {
        mutableStateOf(
            listOf(
                PlaylistEpisode.Available(
                    PodcastEpisode(
                        uuid = "episode-id-0",
                        title = "Episode 0",
                        publishedDate = Date(0),
                        duration = 10000.0,
                    ),
                ),
                PlaylistEpisode.Available(
                    PodcastEpisode(
                        uuid = "episode-id-1",
                        title = "Episode 1",
                        publishedDate = Date(1700000000000),
                        duration = 10000.0,
                        isArchived = true,
                    ),
                ),
                PlaylistEpisode.Unavailable(
                    ManualPlaylistEpisode.test(
                        episodeUuid = "episode-id-2",
                        title = "Episode 2",
                    ),
                ),
            ),
        )
    }

    AppTheme(themeType) {
        EditPlaylistPage(
            episodes = episodes,
            useEpisodeArtwork = false,
            onReorderEpisodes = { episodes = it },
            onDeleteEpisode = { episodes -= it },
            onClickBack = {},
        )
    }
}

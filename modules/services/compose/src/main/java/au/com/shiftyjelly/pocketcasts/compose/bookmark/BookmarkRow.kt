package au.com.shiftyjelly.pocketcasts.compose.bookmark

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Checkbox
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.PlayerColors
import au.com.shiftyjelly.pocketcasts.compose.ThemeColors
import au.com.shiftyjelly.pocketcasts.compose.buttons.TimePlayButton
import au.com.shiftyjelly.pocketcasts.compose.components.EpisodeImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH70
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.swipe.SwipeRow
import au.com.shiftyjelly.pocketcasts.compose.swipe.SwipeRowActionDefaults
import au.com.shiftyjelly.pocketcasts.compose.swipe.SwipeRowState
import au.com.shiftyjelly.pocketcasts.compose.swipe.rememberSwipeRowState
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.toLocalizedFormatPattern
import java.util.Date
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

data class BookmarkRowColors(
    val background: Color,
    val selectedBackground: Color,
    val primaryText: Color,
    val secondaryText: Color,
    val divider: Color,
) {
    companion object {
        fun default(colors: ThemeColors) = BookmarkRowColors(
            background = colors.primaryUi02,
            selectedBackground = colors.primaryUi02Selected,
            primaryText = colors.primaryText01,
            secondaryText = colors.primaryText02,
            divider = colors.primaryUi05,
        )

        fun player(colors: PlayerColors) = BookmarkRowColors(
            background = colors.background01,
            selectedBackground = colors.contrast06,
            primaryText = colors.contrast01,
            secondaryText = colors.contrast02,
            divider = colors.contrast06,
        )
    }
}

@Composable
fun BookmarkRow(
    bookmark: Bookmark,
    episode: BaseEpisode?,
    isSelecting: Boolean,
    isSelected: Boolean,
    showIcon: Boolean,
    showEpisodeTitle: Boolean,
    useEpisodeArtwork: Boolean,
    onPlayClick: () -> Unit,
    modifier: Modifier = Modifier,
    onArtworkClick: (() -> Unit)? = null,
    isLoading: Boolean = false,
    colors: BookmarkColors = rememberBookmarkColors(),
    onFetchEpisode: (suspend () -> BaseEpisode?)? = null,
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    onShareClick: ((SwipeRowState) -> Unit)? = null,
    onDeleteClick: ((SwipeRowState) -> Unit)? = null,
) {
    var fetchedEpisode by remember(bookmark.uuid) { mutableStateOf<BaseEpisode?>(null) }
    val latestEpisode by rememberUpdatedState(episode)
    val latestFetch by rememberUpdatedState(onFetchEpisode)
    LaunchedEffect(bookmark.uuid) {
        if (latestEpisode == null) {
            fetchedEpisode = latestFetch?.invoke()
        }
    }
    val displayEpisode = episode ?: fetchedEpisode
    val canShare = displayEpisode is PodcastEpisode && displayEpisode.uuid.isNotEmpty()
    val swipeState = rememberSwipeRowState()
    Column(
        modifier = modifier,
    ) {
        Divider(
            color = colors.bookmarkRow.divider,
            modifier = Modifier.fillMaxWidth(),
        )
        SwipeRow(
            state = swipeState,
            isSwipeEnabled = !isSelecting,
            leadingAction = onShareClick
                ?.takeIf { canShare }
                ?.let { onShare -> SwipeRowActionDefaults.share(onClick = onShare) },
            trailingAction = onDeleteClick
                ?.let { onDelete -> SwipeRowActionDefaults.delete(onClick = onDelete) },
        ) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .background(if (isSelected) colors.bookmarkRow.selectedBackground else colors.bookmarkRow.background)
                    .combinedClickable(
                        enabled = onClick != null || onLongClick != null,
                        interactionSource = null,
                        indication = null,
                        role = Role.Button,
                        onClick = { onClick?.invoke() },
                        onLongClick = onLongClick,
                    ),
            ) {
                val createdAtText = bookmark.createdAt
                    .toLocalizedFormatPattern(bookmark.createdAtDatePattern())

                if (isSelecting) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = null,
                        modifier = Modifier.padding(start = 16.dp),
                    )
                }

                if (showIcon) {
                    Box(
                        modifier = Modifier
                            .padding(start = 16.dp)
                            .then(
                                if (!isSelecting && onArtworkClick != null) {
                                    Modifier.clickable(role = Role.Button, onClick = onArtworkClick)
                                } else {
                                    Modifier
                                },
                            ),
                    ) {
                        if (displayEpisode != null) {
                            EpisodeImage(
                                episode = displayEpisode,
                                corners = 8.dp,
                                useEpisodeArtwork = useEpisodeArtwork,
                                modifier = Modifier.size(56.dp),
                            )
                        } else {
                            Image(
                                painter = painterResource(if (MaterialTheme.theme.isDark) IR.drawable.defaultartwork_dark else IR.drawable.defaultartwork),
                                contentDescription = bookmark.title,
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(RoundedCornerShape(8.dp)),
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 16.dp),
                ) {
                    val episodeTitle = bookmark.episodeTitle.ifEmpty { displayEpisode?.title.orEmpty() }
                    val shouldShowEpisodeTitle = showEpisodeTitle && episodeTitle.isNotEmpty()
                    if (shouldShowEpisodeTitle) {
                        TextH70(
                            text = episodeTitle,
                            color = colors.bookmarkRow.secondaryText,
                            maxLines = 1,
                            modifier = Modifier.padding(top = 8.dp),
                        )
                    }

                    Spacer(
                        modifier = Modifier.padding(
                            top = if (shouldShowEpisodeTitle) 4.dp else 16.dp,
                        ),
                    )

                    TextH40(
                        text = bookmark.title,
                        color = colors.bookmarkRow.primaryText,
                        maxLines = 1,
                        lineHeight = 18.sp,
                    )

                    TextH70(
                        text = createdAtText,
                        color = colors.bookmarkRow.secondaryText,
                        maxLines = 1,
                        modifier = Modifier.padding(top = 4.dp),
                    )

                    Spacer(
                        modifier = Modifier.padding(
                            bottom = if (shouldShowEpisodeTitle) 8.dp else 16.dp,
                        ),
                    )
                }

                Box(modifier = Modifier.padding(end = 16.dp)) {
                    TimePlayButton(
                        timeSecs = bookmark.timeSecs,
                        contentDescriptionId = LR.string.bookmark_play,
                        onClick = { onPlayClick() },
                        isLoading = isLoading,
                        colors = colors.playButton,
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun BookmarkRowNormalPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        Column {
            BookmarkRowPreview(
                isSelecting = false,
                isSelected = false,
            )
            BookmarkRowPreview(
                isSelecting = true,
                isSelected = false,
            )
            BookmarkRowPreview(
                isSelecting = true,
                isSelected = true,
            )
        }
    }
}

@Composable
private fun BookmarkRowPreview(
    isSelecting: Boolean,
    isSelected: Boolean,
) {
    BookmarkRow(
        bookmark = Bookmark(
            uuid = "",
            podcastUuid = "",
            episodeTitle = "Episode Title",
            timeSecs = 10,
            title = "Bookmark Title",
            createdAt = Date(),
        ),
        episode = PodcastEpisode(
            uuid = "",
            publishedDate = Date(),
        ),
        isSelecting = isSelecting,
        isSelected = isSelected,
        showIcon = false,
        showEpisodeTitle = false,
        useEpisodeArtwork = false,
        onPlayClick = {},
    )
}

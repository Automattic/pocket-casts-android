package au.com.shiftyjelly.pocketcasts.compose.episode

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Checkbox
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.GradientIcon
import au.com.shiftyjelly.pocketcasts.compose.components.HorizontalDivider
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextC70
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.components.TextH60
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.swipe.SwipeRow
import au.com.shiftyjelly.pocketcasts.compose.swipe.SwipeRowAction
import au.com.shiftyjelly.pocketcasts.compose.swipe.SwipeRowState
import au.com.shiftyjelly.pocketcasts.compose.swipe.rememberSwipeRowState
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.type.EpisodePlayingStatus
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import java.util.Date
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Immutable
data class EpisodeRowStatus(
    val text: String,
    val contentDescription: String = text,
    @DrawableRes val iconId: Int? = null,
    val isIconHighlighted: Boolean = false,
    val progress: Float? = null,
    val isBuffering: Boolean = false,
    val isInUpNext: Boolean = false,
    val isUploaded: Boolean = false,
    val hasBookmarks: Boolean = false,
)

@Composable
fun EpisodeRow(
    title: String,
    header: AnnotatedString,
    status: EpisodeRowStatus,
    artwork: @Composable () -> Unit,
    modifier: Modifier = Modifier,
    isStarred: Boolean = false,
    isGreyedOut: Boolean = false,
    showVideoIcon: Boolean = false,
    isMultiSelecting: Boolean = false,
    isSelected: Boolean = false,
    contentDescription: String? = null,
    leadingSwipeActions: List<SwipeRowAction> = emptyList(),
    trailingSwipeActions: List<SwipeRowAction> = emptyList(),
    swipeRowState: SwipeRowState = rememberSwipeRowState(),
    onClick: (() -> Unit)? = null,
    onLongClick: (() -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null,
    backgroundColor: Color = MaterialTheme.theme.colors.primaryUi02,
) {
    val colors = MaterialTheme.theme.colors
    SwipeRow(
        state = swipeRowState,
        leadingActions = leadingSwipeActions,
        trailingActions = trailingSwipeActions,
        isSwipeEnabled = !isMultiSelecting,
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(if (isMultiSelecting && isSelected) colors.primaryUi02Selected else backgroundColor)
                .then(
                    if (onClick != null || onLongClick != null) {
                        Modifier.combinedClickable(
                            onClick = {
                                swipeRowState.settle()
                                onClick?.invoke()
                            },
                            onLongClick = onLongClick?.let { longClick ->
                                {
                                    swipeRowState.settle()
                                    longClick()
                                }
                            },
                        )
                    } else {
                        Modifier
                    },
                )
                .semantics(mergeDescendants = true) {
                    if (contentDescription != null) {
                        this.contentDescription = contentDescription
                    }
                },
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .padding(start = 16.dp),
            ) {
                AnimatedVisibility(visible = isMultiSelecting) {
                    Checkbox(
                        checked = isSelected,
                        onCheckedChange = onClick?.let { click -> { click() } },
                        modifier = Modifier.padding(end = 16.dp),
                    )
                }
                Box(
                    modifier = Modifier
                        .padding(vertical = 12.dp)
                        .size(56.dp)
                        .alpha(if (isGreyedOut) 0.5f else 1f)
                        .clearAndSetSemantics {},
                ) {
                    artwork()
                }
                Spacer(
                    modifier = Modifier.width(16.dp),
                )
                Column(
                    verticalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .padding(vertical = 12.dp)
                        .alpha(if (isGreyedOut) 0.5f else 1f),
                ) {
                    EpisodeRowHeader(header, showVideoIcon)
                    EpisodeRowTitle(title, isStarred)
                    EpisodeRowStatusLine(status)
                }
                AnimatedVisibility(visible = !isMultiSelecting && trailing != null) {
                    if (trailing != null) {
                        trailing()
                    }
                }
                if (isMultiSelecting || trailing == null) {
                    Spacer(
                        modifier = Modifier.width(16.dp),
                    )
                }
            }
            HorizontalDivider(
                startIndent = 16.dp,
            )
        }
    }
}

@Composable
private fun EpisodeRowHeader(header: AnnotatedString, showVideoIcon: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (showVideoIcon) {
            StatusIcon(IR.drawable.ic_video_small_fill, MaterialTheme.theme.colors.support01)
        }
        TextC70(
            text = header.toUpperCase(),
            maxLines = 1,
        )
    }
}

@Composable
private fun EpisodeRowTitle(title: String, isStarred: Boolean) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        TextH40(
            text = title,
            maxLines = 2,
            modifier = Modifier.weight(1f, fill = false),
        )
        if (isStarred) {
            Icon(
                painter = painterResource(IR.drawable.ic_starred_row),
                contentDescription = null,
                tint = MaterialTheme.theme.colors.support08,
                modifier = Modifier.padding(start = 4.dp),
            )
        }
    }
}

@Composable
private fun EpisodeRowStatusLine(status: EpisodeRowStatus) {
    val colors = MaterialTheme.theme.colors
    Row(
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (status.isInUpNext) {
            StatusIcon(IR.drawable.ic_upnext, colors.support01)
        }
        if (status.iconId != null) {
            StatusIcon(status.iconId, if (status.isIconHighlighted) colors.support02 else colors.primaryIcon02)
        }
        if (status.isUploaded) {
            GradientIcon(
                painter = painterResource(IR.drawable.ic_cloud),
                colors = listOf(colors.gradient01A, colors.gradient01E),
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(16.dp),
            )
        }
        if (status.hasBookmarks) {
            StatusIcon(IR.drawable.ic_bookmark, colors.primaryIcon01)
        }
        if (status.progress != null) {
            CircularProgressIndicator(
                progress = status.progress,
                color = colors.primaryText02,
                backgroundColor = colors.primaryText02.copy(alpha = 0.3f),
                strokeWidth = 2.dp,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(16.dp),
            )
        }
        if (status.isBuffering) {
            CircularProgressIndicator(
                color = colors.primaryText02,
                strokeWidth = 2.dp,
                modifier = Modifier
                    .padding(end = 4.dp)
                    .size(16.dp),
            )
        }
        TextH60(
            text = status.text,
            color = colors.primaryText02,
            maxLines = 1,
        )
    }
}

@Composable
private fun StatusIcon(
    @DrawableRes iconId: Int,
    tint: Color,
) {
    Icon(
        painter = painterResource(iconId),
        contentDescription = null,
        tint = tint,
        modifier = Modifier
            .padding(end = 4.dp)
            .size(16.dp),
    )
}

@Preview
@Composable
private fun EpisodeRowPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        Column {
            EpisodeRow(
                episode = PodcastEpisode(uuid = "1", title = "Episode title", publishedDate = Date(), duration = 3600.0, isStarred = true),
                useEpisodeArtwork = false,
                trailing = { StatusIcon(IR.drawable.button_play, MaterialTheme.theme.colors.primaryIcon01) },
            )
            EpisodeRow(
                episode = PodcastEpisode(uuid = "2", title = "Played episode", publishedDate = Date(), playingStatus = EpisodePlayingStatus.COMPLETED),
                useEpisodeArtwork = false,
                isMultiSelecting = true,
                isSelected = true,
            )
            EpisodeRow(
                title = "Unavailable episode",
                header = AnnotatedString("1 January"),
                status = EpisodeRowStatus("Unavailable", iconId = IR.drawable.ic_episode_unavailable),
                artwork = { PodcastImage(uuid = "", imageSize = 56.dp, cornerSize = 4.dp) },
                isGreyedOut = true,
            )
        }
    }
}

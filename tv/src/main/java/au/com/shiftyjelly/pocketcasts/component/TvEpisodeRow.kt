package au.com.shiftyjelly.pocketcasts.component

import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Devices
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.Icon
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.Text
import au.com.shiftyjelly.pocketcasts.localization.helper.RelativeDateFormatter
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory
import au.com.shiftyjelly.pocketcasts.repositories.images.PocketCastsImageRequestFactory.PlaceholderType
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage
import au.com.shiftyjelly.pocketcasts.theme.TvTheme
import au.com.shiftyjelly.pocketcasts.theme.tvColors
import au.com.shiftyjelly.pocketcasts.theme.tvTypography
import java.util.Date
import au.com.shiftyjelly.pocketcasts.images.R as IR

/** Whether episode rows render episode artwork over podcast artwork; mirrors the shared appearance setting. */
val LocalUseEpisodeArtwork = staticCompositionLocalOf { false }

@Composable
fun TvEpisodeRow(
    episode: PodcastEpisode,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    dateFormatter: RelativeDateFormatter,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val titleColor = if (isFocused) MaterialTheme.tvColors.textPrimaryActive else MaterialTheme.tvColors.textPrimary
    val captionColor = if (isFocused) {
        MaterialTheme.tvColors.textSecondaryActive
    } else {
        MaterialTheme.tvColors.textSecondary
    }

    TvTile(
        onClick = onClick,
        onLongClick = onLongClick,
        scale = CardDefaults.scale(focusedScale = 1.02f),
        shape = CardDefaults.shape(RoundedCornerShape(8.dp)),
        colors = CardDefaults.colors(
            containerColor = MaterialTheme.tvColors.backgroundBase,
            focusedContainerColor = MaterialTheme.tvColors.backgroundActive,
        ),
        interactionSource = interactionSource,
        modifier = modifier.alpha(if (episode.isArchived && !isFocused) 0.3f else 1f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            EpisodeArtwork(episode = episode)
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    if (episode.isVideo) {
                        Icon(
                            painter = painterResource(IR.drawable.ic_video_small_fill),
                            contentDescription = null,
                            tint = captionColor,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                    Text(
                        text = episode.rememberPublishedDateText(dateFormatter),
                        style = MaterialTheme.tvTypography.caption2,
                        color = captionColor,
                    )
                }
                Text(
                    text = episode.title,
                    style = MaterialTheme.tvTypography.callout,
                    color = titleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Text(
                        text = episode.rememberPlaybackTimeText(),
                        style = MaterialTheme.tvTypography.caption2,
                        color = captionColor,
                    )
                    if (episode.isInProgress && episode.duration > 0.0) {
                        EpisodeProgressBar(
                            progress = (episode.playedUpTo / episode.duration).toFloat().coerceIn(0f, 1f),
                            color = captionColor,
                            modifier = Modifier.width(48.dp),
                        )
                    } else if (episode.isFinished) {
                        Icon(
                            painter = painterResource(IR.drawable.ic_check),
                            contentDescription = null,
                            tint = captionColor,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TvResumeCard(
    episode: PodcastEpisode,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    dateFormatter: RelativeDateFormatter,
    modifier: Modifier = Modifier,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val titleColor = if (isFocused) MaterialTheme.tvColors.textPrimaryActive else MaterialTheme.tvColors.textPrimary
    val captionColor = if (isFocused) {
        MaterialTheme.tvColors.textSecondaryActive
    } else {
        MaterialTheme.tvColors.textSecondary
    }

    TvTile(
        onClick = onClick,
        onLongClick = onLongClick,
        scale = CardDefaults.scale(focusedScale = 1.02f),
        shape = CardDefaults.shape(RoundedCornerShape(8.dp)),
        colors = CardDefaults.colors(
            containerColor = MaterialTheme.tvColors.backgroundBase,
            focusedContainerColor = MaterialTheme.tvColors.backgroundActive,
        ),
        interactionSource = interactionSource,
        modifier = modifier
            .width(621.dp)
            .alpha(if (episode.isArchived && !isFocused) 0.3f else 1f),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(24.dp),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            EpisodeArtwork(episode = episode, size = 136.dp, cornerRadius = 6.dp)
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = episode.rememberPublishedDateText(dateFormatter),
                    style = MaterialTheme.tvTypography.body,
                    color = captionColor,
                )
                Text(
                    text = episode.title,
                    style = MaterialTheme.tvTypography.title3,
                    color = titleColor,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                if (episode.isInProgress && episode.duration > 0.0) {
                    EpisodeProgressBar(
                        progress = (episode.playedUpTo / episode.duration).toFloat().coerceIn(0f, 1f),
                        color = captionColor,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Text(
                    text = episode.rememberPlaybackTimeText(),
                    style = MaterialTheme.tvTypography.body,
                    color = captionColor,
                )
            }
        }
    }
}

@Composable
private fun EpisodeArtwork(
    episode: PodcastEpisode,
    modifier: Modifier = Modifier,
    size: Dp = 62.dp,
    cornerRadius: Dp = 4.dp,
) {
    val useEpisodeArtwork = LocalUseEpisodeArtwork.current
    val context = LocalContext.current
    val model = if (useEpisodeArtwork) {
        remember(episode.uuid) {
            PocketCastsImageRequestFactory(context, placeholderType = PlaceholderType.None)
                .create(episode, useEpisodeArtwork = true)
        }
    } else {
        PodcastImage.getMediumArtworkUrl(episode.podcastUuid)
    }
    TvArtworkImage(
        model = model,
        modifier = modifier
            .size(size)
            .clip(RoundedCornerShape(cornerRadius)),
    )
}

@Composable
private fun EpisodeProgressBar(
    progress: Float,
    color: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .height(3.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.3f)),
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .fillMaxWidth(progress)
                .background(color),
        )
    }
}

@Composable
private fun PodcastEpisode.rememberPublishedDateText(dateFormatter: RelativeDateFormatter): String {
    return remember(publishedDate, dateFormatter) { dateFormatter.format(publishedDate) }
}

@Composable
private fun PodcastEpisode.rememberPlaybackTimeText(): String {
    val context = LocalContext.current
    return remember(playedUpToMs, durationMs, isInProgress) {
        if (isInProgress) {
            TimeHelper.getTimeLeft(playedUpToMs, durationMs.toLong(), inProgress = true, context).text
        } else {
            TimeHelper.getTimeDurationShortString(durationMs.toLong(), context)
        }
    }
}

@Preview(device = Devices.TV_1080p)
@Composable
private fun TvEpisodeRowPreview() {
    TvTheme {
        TvEpisodeRow(
            episode = PodcastEpisode(
                uuid = "episode-uuid",
                title = "A very long episode title that spans over multiple lines to test the ellipsis",
                duration = 6000.0,
                playedUpTo = 1200.0,
                publishedDate = Date(0),
            ),
            onClick = {},
            dateFormatter = RelativeDateFormatter(LocalContext.current),
        )
    }
}

package au.com.shiftyjelly.pocketcasts.wear.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.Absolute.spacedBy
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import au.com.shiftyjelly.pocketcasts.compose.components.EpisodeImage
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.utils.extensions.toLocalizedFormatPattern
import au.com.shiftyjelly.pocketcasts.wear.theme.WearColors
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun EpisodeChip(
    episode: BaseEpisode,
    useEpisodeArtwork: Boolean,
    useUpNextIcon: Boolean = true,
    onClick: () -> Unit,
    showImage: Boolean = true,
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colors.surface)
            .clickable { onClick() }
            .padding(horizontal = 10.dp)
            .fillMaxWidth()
            .padding(vertical = 10.dp),
    ) {
        val viewModel = hiltViewModel<EpisodeChipViewModel>()

        // Make sure the episode is always up-to-date
        @Suppress("NAME_SHADOWING")
        val episode by viewModel
            .observeByUuid(episode)
            .collectAsState()

        val queueState by viewModel.upNextQueue.collectAsState(UpNextQueue.State.Empty)
        val upNextQueue = (queueState as? UpNextQueue.State.Loaded)
            ?.queue
            ?: emptyList()
        val isInUpNextQueue = upNextQueue.any { it.uuid == episode.uuid }
        val showUpNextIcon = useUpNextIcon && isInUpNextQueue
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.height(IntrinsicSize.Max),
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center,
            ) {
                if (showImage) {
                    EpisodeImage(
                        episode = episode,
                        useEpisodeArtwork = useEpisodeArtwork,
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(4.dp)),
                    )
                    IconsRow(
                        showUpNextIcon,
                        episode,
                        Modifier.padding(top = 4.dp),
                    )
                }
            }

            Spacer(Modifier.width(6.dp))

            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(
                    text = episode.title,
                    lineHeight = 16.sp,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colors.onPrimary,
                    style = MaterialTheme.typography.button.merge(
                        @Suppress("DEPRECATION")
                        (
                            TextStyle(
                                platformStyle = PlatformTextStyle(
                                    // So we can align the top of the text as closely as possible to the image
                                    includeFontPadding = false,
                                ),
                            )
                            ),
                    ),
                    maxLines = 2,
                )
                val shortDate = episode.publishedDate.toLocalizedFormatPattern("dd MMM")
                val timeLeft = TimeHelper.getTimeLeft(
                    currentTimeMs = episode.playedUpToMs,
                    durationMs = episode.durationMs.toLong(),
                    inProgress = episode.isInProgress,
                    context = LocalContext.current,
                ).text
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    if (!showImage) {
                        IconsRow(
                            showUpNextIcon,
                            episode,
                            modifier = Modifier
                                .padding(end = if (showUpNextIcon || episode.isDownloaded) 4.dp else 0.dp),
                        )
                    }
                    Text(
                        text = "$shortDate • $timeLeft",
                        color = MaterialTheme.colors.onSecondary,
                        style = MaterialTheme.typography.caption2,
                    )
                }
                episode.playErrorDetails?.let {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Icon(
                            painter = painterResource(IR.drawable.ic_alert_small),
                            contentDescription = stringResource(LR.string.podcast_episode_playback_error),
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colors.onSecondary,
                        )
                        Text(
                            text = it,
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            color = MaterialTheme.colors.onSecondary,
                            style = MaterialTheme.typography.caption3,
                            modifier = Modifier.padding(start = 5.dp),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun IconsRow(
    showUpNextIcon: Boolean,
    episode: BaseEpisode,
    modifier: Modifier,
) {
    Row(
        horizontalArrangement = spacedBy(4.dp),
        modifier = modifier,
    ) {
        if (showUpNextIcon) {
            Icon(
                painter = painterResource(R.drawable.ic_upnext),
                contentDescription = stringResource(LR.string.episode_in_up_next),
                tint = WearColors.upNextIcon,
                modifier = Modifier.size(12.dp),
            )
        }

        if (episode.isDownloaded) {
            Icon(
                painter = painterResource(R.drawable.ic_downloaded),
                contentDescription = stringResource(LR.string.downloaded),
                tint = WearColors.downloadedIcon,
                modifier = Modifier.size(12.dp),
            )
        }
    }
}

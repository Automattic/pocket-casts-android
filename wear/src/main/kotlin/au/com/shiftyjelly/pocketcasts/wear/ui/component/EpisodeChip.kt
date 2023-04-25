package au.com.shiftyjelly.pocketcasts.wear.ui.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import au.com.shiftyjelly.pocketcasts.compose.components.EpisodeImage
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.utils.extensions.toLocalizedFormatPattern
import au.com.shiftyjelly.pocketcasts.wear.theme.theme

@Composable
fun EpisodeChip(episode: BaseEpisode, onClick: () -> Unit) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .clip(RoundedCornerShape(24.dp))
            .background(MaterialTheme.colors.surface)
            .clickable { onClick() }
            .padding(horizontal = 10.dp)
            .fillMaxWidth()
            .height(72.dp)
    ) {
        Row {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {

                EpisodeImage(
                    episode = episode,
                    modifier = Modifier
                        .size(30.dp)
                        .clip(RoundedCornerShape(4.dp)),
                )

                if (episode.isDownloaded) {
                    Spacer(Modifier.height(4.dp))
                    Icon(
                        painter = painterResource(R.drawable.ic_downloaded),
                        contentDescription = null,
                        tint = MaterialTheme.theme.colors.support02,
                        modifier = Modifier.size(12.dp),
                    )
                }
            }

            Spacer(Modifier.width(6.dp))

            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = episode.title,
                    lineHeight = 14.sp,
                    overflow = TextOverflow.Ellipsis,
                    style = MaterialTheme.typography.button.merge(
                        @Suppress("DEPRECATION")
                        (
                            TextStyle(
                                platformStyle = PlatformTextStyle(
                                    // So we can align the top of the text as closely as possible to the image
                                    includeFontPadding = false,
                                ),
                            )
                            )
                    ),
                    maxLines = 2,
                )
                val shortDate = episode.publishedDate.toLocalizedFormatPattern("dd MMM")
                val timeLeft = TimeHelper.getTimeLeft(
                    currentTimeMs = episode.playedUpToMs,
                    durationMs = episode.durationMs.toLong(),
                    inProgress = episode.isInProgress,
                    context = LocalContext.current
                ).text
                Text(
                    text = "$shortDate • $timeLeft",
                    color = MaterialTheme.theme.colors.primaryText02,
                    style = MaterialTheme.typography.caption2
                )
            }
        }
    }
}

package au.com.shiftyjelly.pocketcasts.wear.ui.downloads

import android.content.res.Configuration
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.wear.compose.foundation.lazy.items
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.utils.extensions.toLocalizedFormatPattern
import au.com.shiftyjelly.pocketcasts.wear.theme.WearAppTheme
import au.com.shiftyjelly.pocketcasts.wear.theme.theme
import au.com.shiftyjelly.pocketcasts.wear.ui.component.ChipScreenHeader
import com.google.android.horologist.compose.layout.ScalingLazyColumn
import com.google.android.horologist.compose.layout.ScalingLazyColumnState
import java.util.Date
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

object DownloadsScreen {
    const val route = "downloads_screen"
}

@Composable
fun DownloadsScreen(
    columnState: ScalingLazyColumnState,
    onItemClick: (PodcastEpisode) -> Unit,
) {

    val viewModel = hiltViewModel<DownloadsScreenViewModel>()
    val state by viewModel.stateFlow.collectAsState()

    Content(columnState, state, onItemClick)
}

@Composable
private fun Content(
    columnState: ScalingLazyColumnState,
    episodes: List<PodcastEpisode>?,
    onItemClick: (PodcastEpisode) -> Unit,
) {
    ScalingLazyColumn(
        columnState = columnState,
    ) {
        if (episodes != null) {
            item {
                ChipScreenHeader(
                    text = if (episodes.isEmpty()) {
                        LR.string.profile_empty_downloaded
                    } else {
                        LR.string.downloads
                    },
                )
            }

            items(episodes) { episode ->
                Download(
                    episode = episode,
                    onClick = { onItemClick(episode) }
                )
            }
        }
    }
}

@Composable
private fun Download(episode: PodcastEpisode, onClick: () -> Unit) {
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
                PodcastImage(
                    uuid = episode.podcastUuid,
                    dropShadow = false,
                    modifier = Modifier.size(30.dp),
                )
                Spacer(Modifier.height(4.dp))
                Icon(
                    painter = painterResource(IR.drawable.ic_downloaded),
                    contentDescription = null,
                    tint = MaterialTheme.theme.colors.support02,
                    modifier = Modifier.size(12.dp),
                )
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
                        TextStyle(
                            platformStyle = PlatformTextStyle(
                                // So we can align the top of the text as closely as possible to the image
                                includeFontPadding = false,
                            ),
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

@Preview(
    widthDp = 200,
    heightDp = 200,
    uiMode = Configuration.UI_MODE_TYPE_WATCH,
)
@Composable
private fun DownloadsScreenPreview() {
    WearAppTheme(Theme.ThemeType.DARK) {
        Content(
            columnState = ScalingLazyColumnState(),
            onItemClick = {},
            episodes = listOf(
                PodcastEpisode(
                    uuid = "57853d71-30ac-4477-af73-e8fe2b1d4dda",
                    podcastUuid = "b643cb50-2c52-013b-ef7a-0acc26574db2",
                    title = "Such a great episode title, but it's so long that it is definitely going to be more than two lines",
                    publishedDate = Date(),
                    playedUpTo = 0.0,
                    duration = 20.0,
                ),
                PodcastEpisode(
                    uuid = "c146e703-e408-4979-852c-f9927ce19ef7",
                    podcastUuid = "3df2e780-0063-0135-ec79-4114446340cb",
                    title = "1 line title",
                    publishedDate = Date(),
                    playedUpTo = 0.0,
                    duration = 20.0,
                ),
            )
        )
    }
}

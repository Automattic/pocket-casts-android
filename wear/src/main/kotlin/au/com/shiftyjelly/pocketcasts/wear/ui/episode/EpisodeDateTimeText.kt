package au.com.shiftyjelly.pocketcasts.wear.ui.episode

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Text
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.extensions.toLocalizedFormatPattern
import au.com.shiftyjelly.pocketcasts.wear.theme.WearAppTheme

@Composable
fun EpisodeDateTimeText(episode: BaseEpisode, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val shortDate = episode.publishedDate.toLocalizedFormatPattern("dd MMM")
    val timeLeft = TimeHelper.getTimeLeft(
        currentTimeMs = episode.playedUpToMs,
        durationMs = episode.durationMs.toLong(),
        inProgress = episode.isInProgress,
        context = context
    ).text
    val downloadSize = Util.formattedBytes(
        bytes = episode.sizeInBytes,
        context = context
    )
    LayoutContent(
        shortDate = shortDate,
        timeLeft = timeLeft,
        downloadSize = downloadSize,
        modifier = modifier
    )
}

@Composable
private fun LayoutContent(
    shortDate: String,
    timeLeft: String,
    downloadSize: String,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Text(
            text = "$shortDate • $timeLeft",
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onPrimary
        )
        Text(
            text = downloadSize,
            style = MaterialTheme.typography.body2,
            color = MaterialTheme.colors.onPrimary
        )
    }
}

@Preview
@Composable
private fun Preview() {
    WearAppTheme {
        LayoutContent(
            shortDate = "06 Dec",
            timeLeft = "25m left",
            downloadSize = "23 mb",
        )
    }
}

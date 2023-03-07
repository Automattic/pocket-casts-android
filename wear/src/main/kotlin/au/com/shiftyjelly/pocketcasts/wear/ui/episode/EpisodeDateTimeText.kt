package au.com.shiftyjelly.pocketcasts.wear.ui.episode

import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import au.com.shiftyjelly.pocketcasts.compose.components.TextC70
import au.com.shiftyjelly.pocketcasts.localization.helper.TimeHelper
import au.com.shiftyjelly.pocketcasts.models.entity.Episode
import au.com.shiftyjelly.pocketcasts.utils.Util
import au.com.shiftyjelly.pocketcasts.utils.extensions.toLocalizedFormatPattern

@Composable
fun EpisodeDateTimeText(episode: Episode, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {

        val shortDate = episode.publishedDate.toLocalizedFormatPattern("dd MMM")
        val timeLeft = TimeHelper.getTimeLeft(
            episode.playedUpToMs,
            episode.durationMs.toLong(),
            episode.isInProgress,
            context
        ).text
        TextC70("$shortDate • $timeLeft")

        val downloadSize = Util.formattedBytes(episode.sizeInBytes, context)
        TextC70(downloadSize)
    }
}

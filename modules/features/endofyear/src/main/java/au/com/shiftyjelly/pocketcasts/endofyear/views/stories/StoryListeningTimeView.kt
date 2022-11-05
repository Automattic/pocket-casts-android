package au.com.shiftyjelly.pocketcasts.endofyear.views.stories

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.endofyear.stories.StoryListeningTime
import au.com.shiftyjelly.pocketcasts.settings.stats.StatsHelper
import au.com.shiftyjelly.pocketcasts.settings.util.FunnyTimeConverter
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun StoryListeningTimeView(
    story: StoryListeningTime,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current

    val timeText = remember {
        StatsHelper.secondsToFriendlyString(story.listeningTimeInSecs, context.resources)
    }
    val funnyText = remember {
        FunnyTimeConverter().timeSecsToFunnyText(
            story.listeningTimeInSecs,
            context.resources
        )
    }
    Column(modifier.padding(16.dp)) {
        TextH30(
            text = stringResource(LR.string.end_of_year_listening_time, timeText),
            textAlign = TextAlign.Center,
            color = story.tintColor,
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
        TextH30(
            text = funnyText,
            textAlign = TextAlign.Center,
            color = story.tintColor,
            modifier = modifier
                .fillMaxWidth()
                .padding(16.dp)
        )
    }
}

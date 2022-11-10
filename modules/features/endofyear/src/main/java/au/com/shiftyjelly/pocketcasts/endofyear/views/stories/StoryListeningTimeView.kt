package au.com.shiftyjelly.pocketcasts.endofyear.views.stories

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Matrix
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastImage
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.endofyear.R
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryListeningTime
import au.com.shiftyjelly.pocketcasts.settings.stats.StatsHelper
import au.com.shiftyjelly.pocketcasts.settings.util.FunnyTimeConverter
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private const val PodcastCoverRotationAngle = -30f
private const val PodcastCoverSkew = 0.45f

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

    val backgroundColor = if (story.podcasts.isEmpty()) {
        Podcast().getTintColor(false)
    } else {
        // Get the middle podcast
        story.podcasts.take(2).last().toPodcast()
            .getTintColor(false)
    }
    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(color = Color(backgroundColor))
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = modifier.weight(1f))

        Title(timeText, story, modifier)

        FunnyText(funnyText, story, modifier)

        Spacer(modifier = modifier.weight(1f))

        PodcastCoverRow(story, modifier)

        Spacer(modifier = modifier.weight(1f))

        Logo(modifier)
    }
}

@Composable
private fun Title(
    timeText: String,
    story: StoryListeningTime,
    modifier: Modifier,
) {
    TextH20(
        text = stringResource(LR.string.end_of_year_listening_time, timeText),
        textAlign = TextAlign.Center,
        color = story.tintColor,
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 40.dp, end = 40.dp)
    )
}

@Composable
private fun FunnyText(
    funnyText: String,
    story: StoryListeningTime,
    modifier: Modifier,
) {
    TextP40(
        text = funnyText,
        textAlign = TextAlign.Center,
        color = story.tintColor,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .fillMaxWidth()
            .alpha(0.8f)
            .padding(top = 24.dp, start = 40.dp, end = 40.dp)
    )
}

@Composable
private fun PodcastCoverRow(
    story: StoryListeningTime,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val currentLocalView = LocalView.current
    val coverWidth = remember { (currentLocalView.width.pxToDp(context).dp - 15.dp) / 3 }
    val translateBy = remember { 20.dpToPx(context) }
    val transformMatrix = remember { mutableStateOf(Matrix()) }
    Row(
        modifier
            .graphicsLayer(
                rotationZ = PodcastCoverRotationAngle
            )
            .drawWithContent {
                withTransform(
                    transformBlock = {
                        transformMatrix.value.values[Matrix.SkewX] = PodcastCoverSkew
                        transform(transformMatrix.value)
                    }
                ) {
                    this@drawWithContent.drawContent()
                }
            }
            .graphicsLayer(
                translationX = -translateBy.toFloat()
            )

    ) {
        story.podcasts.forEach { podcast ->
            Row {
                PodcastImage(
                    uuid = podcast.uuid,
                    dropShadow = false,
                    modifier = modifier
                        .size(coverWidth)
                )
                Spacer(modifier = modifier.width(5.dp))
            }
        }
    }
}

@Composable
private fun Logo(modifier: Modifier) {
    Image(
        painter = painterResource(R.drawable.logo_white),
        contentDescription = null,
        modifier = modifier.padding(bottom = 40.dp)
    )
}

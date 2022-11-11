package au.com.shiftyjelly.pocketcasts.endofyear.views.stories

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.endofyear.components.PodcastCover
import au.com.shiftyjelly.pocketcasts.endofyear.components.PodcastCoverType
import au.com.shiftyjelly.pocketcasts.endofyear.components.transformPodcastCover
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryListenedCategories
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun StoryListenedCategoriesView(
    story: StoryListenedCategories,
    modifier: Modifier = Modifier,
) {
    val backgroundColor = story.listenedCategories[0].toPodcast().getTintColor(false)

    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .background(color = Color(backgroundColor))
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = modifier.height(40.dp))

        Spacer(modifier = modifier.weight(0.7f))

        PodcastCoverStack(story)

        Spacer(modifier = modifier.weight(0.3f))

        PrimaryText(story)

        Spacer(modifier = modifier.weight(0.25f))

        SecondaryText(story)

        Spacer(modifier = modifier.weight(0.8f))

        Logo()

        Spacer(modifier = modifier.height(40.dp))
    }
}

@Composable
private fun PodcastCoverStack(
    story: StoryListenedCategories,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val currentLocalView = LocalView.current
    val coverWidth = (currentLocalView.width.pxToDp(context).dp) / 2.5f
    val translateBy = (coverWidth.value * .2).toInt().dpToPx(context)

    Box {
        (0..2).reversed().forEach { index ->
            Box(
                modifier = modifier
                    .padding(top = (index * (coverWidth.value * .25)).dp)
                    .transformPodcastCover()
                    .graphicsLayer(translationX = -translateBy.toFloat())
            ) {
                val podcastIndex = index.coerceAtMost(story.listenedCategories.size - 1)
                PodcastCover(
                    uuid = story.listenedCategories[podcastIndex].mostListenedPodcastId,
                    coverWidth = coverWidth,
                    coverType = PodcastCoverType.BIG
                )
            }
        }
    }
}

@Composable
private fun PrimaryText(
    story: StoryListenedCategories,
    modifier: Modifier = Modifier,
) {
    TextH20(
        text = stringResource(
            id = LR.string.end_of_year_story_listened_to_categories,
            story.listenedCategories.count()
        ),
        textAlign = TextAlign.Center,
        color = story.tintColor,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp)
    )
}

@Composable
private fun SecondaryText(
    story: StoryListenedCategories,
    modifier: Modifier = Modifier,
) {
    TextP40(
        text = stringResource(id = LR.string.end_of_year_story_listened_to_categories_subtitle),
        textAlign = TextAlign.Center,
        color = story.tintColor,
        fontWeight = FontWeight.Bold,
        modifier = modifier
            .fillMaxWidth()
            .alpha(0.8f)
            .padding(horizontal = 40.dp)
    )
}

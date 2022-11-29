package au.com.shiftyjelly.pocketcasts.endofyear.views.stories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.CoverSize
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastCover
import au.com.shiftyjelly.pocketcasts.compose.components.transformPodcastCover
import au.com.shiftyjelly.pocketcasts.endofyear.components.PodcastLogoWhite
import au.com.shiftyjelly.pocketcasts.endofyear.components.StoryPrimaryText
import au.com.shiftyjelly.pocketcasts.endofyear.components.StorySecondaryText
import au.com.shiftyjelly.pocketcasts.endofyear.utils.podcastDynamicBackground
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryListenedCategories
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@Composable
fun StoryListenedCategoriesView(
    story: StoryListenedCategories,
    modifier: Modifier = Modifier,
) {
    Column(
        verticalArrangement = Arrangement.Top,
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .podcastDynamicBackground(story.listenedCategories[0].toPodcast())
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

        PodcastLogoWhite()

        Spacer(modifier = modifier.height(30.dp))
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

    Box {
        (0..2).reversed().forEach { index ->
            Box(
                modifier = modifier
                    .padding(top = (index * (coverWidth.value * .17)).dp)
                    .transformPodcastCover()
            ) {
                val podcastIndex = index.coerceAtMost(story.listenedCategories.size - 1)
                PodcastCover(
                    uuid = story.listenedCategories[podcastIndex].mostListenedPodcastId,
                    coverWidth = coverWidth,
                    coverSize = CoverSize.BIG
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
    val language = Locale.current.language
    val textResId = if (language == "en") {
        LR.string.end_of_year_story_listened_to_categories_english_only
    } else {
        LR.string.end_of_year_story_listened_to_categories
    }
    val text = stringResource(
        id = textResId,
        story.listenedCategories.count()
    )
    StoryPrimaryText(text = text, color = story.tintColor, modifier = modifier)
}

@Composable
private fun SecondaryText(
    story: StoryListenedCategories,
    modifier: Modifier = Modifier,
) {
    val text = stringResource(id = LR.string.end_of_year_story_listened_to_categories_subtitle)
    StorySecondaryText(text = text, color = story.tintColor, modifier = modifier)
}

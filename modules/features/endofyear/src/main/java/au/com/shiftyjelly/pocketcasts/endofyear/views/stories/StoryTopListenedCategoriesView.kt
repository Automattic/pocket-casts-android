package au.com.shiftyjelly.pocketcasts.endofyear.views.stories

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.endofyear.components.CategoryPillar
import au.com.shiftyjelly.pocketcasts.endofyear.components.PodcastLogoWhite
import au.com.shiftyjelly.pocketcasts.endofyear.utils.dynamicBackground
import au.com.shiftyjelly.pocketcasts.models.db.helper.ListenedCategory
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryTopListenedCategories
import au.com.shiftyjelly.pocketcasts.settings.stats.StatsHelper
import au.com.shiftyjelly.pocketcasts.localization.R as LR

private const val BackgroundColor = 0xFF744F9D

@Composable
fun StoryTopListenedCategoriesView(
    story: StoryTopListenedCategories,
    modifier: Modifier = Modifier,
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
            .fillMaxSize()
            .dynamicBackground(Color(BackgroundColor))
            .verticalScroll(rememberScrollState())
    ) {
        Spacer(modifier = modifier.height(40.dp))

        Spacer(modifier = modifier.weight(1f))

        Title(story)

        Spacer(modifier = modifier.weight(0.5f))

        CategoryPillars(story, modifier)

        Spacer(modifier = modifier.weight(1f))

        PodcastLogoWhite()

        Spacer(modifier = modifier.height(30.dp))
    }
}

@Composable
private fun Title(
    story: StoryTopListenedCategories,
    modifier: Modifier = Modifier,
) {
    val text = stringResource(LR.string.end_of_year_story_top_categories)
    TextH30(
        text = text,
        textAlign = TextAlign.Center,
        color = story.tintColor,
        modifier = modifier
            .padding(horizontal = 40.dp)
            .fillMaxWidth()
    )
}

@Composable
private fun CategoryPillars(
    story: StoryTopListenedCategories,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.Bottom,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp)
    ) {
        listOf(1, 0, 2).forEach { index ->
            Spacer(modifier.weight(1f))

            val listenedCategory = story.listenedCategories.atIndex(index)
            listenedCategory?.let {
                val timeText =
                    StatsHelper.secondsToFriendlyString(
                        listenedCategory.totalPlayedTime,
                        context.resources
                    )
                CategoryPillar(
                    title = listenedCategory.category,
                    duration = timeText,
                    text = (index + 1).toString(),
                    height = (200 - index * 55).dp,
                    modifier = modifier
                        .padding(
                            bottom = if (index == 0) 70.dp else 0.dp,
                        )
                )
            } ?: CategoryPillar(
                title = "",
                duration = "",
                text = "",
                height = 200.dp,
                modifier = modifier.alpha(0f)
            )

            Spacer(modifier.weight(1f))
        }
    }
}

private fun List<ListenedCategory>.atIndex(index: Int) =
    if (index < size) this[index] else null

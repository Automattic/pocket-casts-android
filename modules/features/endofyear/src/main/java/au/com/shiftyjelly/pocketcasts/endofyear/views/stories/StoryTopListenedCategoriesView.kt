package au.com.shiftyjelly.pocketcasts.endofyear.views.stories

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextH50
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.endofyear.components.StoryBlurredBackground
import au.com.shiftyjelly.pocketcasts.endofyear.components.StoryPrimaryText
import au.com.shiftyjelly.pocketcasts.endofyear.components.StorySecondaryText
import au.com.shiftyjelly.pocketcasts.endofyear.components.disableScale
import au.com.shiftyjelly.pocketcasts.endofyear.utils.textGradient
import au.com.shiftyjelly.pocketcasts.models.db.helper.ListenedCategory
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.StoryTopListenedCategories
import au.com.shiftyjelly.pocketcasts.settings.stats.StatsHelper
import kotlin.math.min
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

private val CategoryColor = Color(0xFF686C74)
private val CategoryFontSize = 54.sp
private val DefaultFontFamily = FontFamily(listOf(Font(UR.font.dm_sans)))

@Composable
fun StoryTopListenedCategoriesView(
    story: StoryTopListenedCategories,
    modifier: Modifier = Modifier,
) {
    Box {
        StoryBlurredBackground(
            offset = Offset(
                -LocalView.current.width * 0.4f,
                -LocalView.current.height * 0.4f
            ),
            rotate = 110f,
        )
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(vertical = 40.dp)
        ) {
            Spacer(modifier = modifier.weight(0.2f))

            PrimaryText(story)

            Spacer(modifier = modifier.height(14.dp))

            SecondaryText(story)

            Spacer(modifier = modifier.weight(0.2f))

            CategoryList(story, modifier)

            Spacer(modifier = modifier.weight(0.5f))
        }
    }
}

@Composable
private fun PrimaryText(
    story: StoryTopListenedCategories,
    modifier: Modifier = Modifier,
) {
    val text = stringResource(
        LR.string.end_of_year_story_top_categories_title,
        story.listenedCategories[0].simplifiedCategoryName()
    )
    StoryPrimaryText(text = text, color = story.tintColor, modifier = modifier)
}

@Composable
private fun SecondaryText(
    story: StoryTopListenedCategories,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val timeText = StatsHelper.secondsToFriendlyString(
        story.listenedCategories[0].totalPlayedTime,
        context.resources
    )
    val text = stringResource(
        id = LR.string.end_of_year_story_top_categories_subtitle,
        story.listenedCategories[0].numberOfEpisodes,
        timeText
    )
    StorySecondaryText(text = text, color = story.subtitleColor, modifier = modifier)
}

@Composable
private fun CategoryList(
    story: StoryTopListenedCategories,
    modifier: Modifier = Modifier,
) {
    (0..min(story.listenedCategories.size, 3)).forEach { index ->
        val listenedCategory = story.listenedCategories.atIndex(index)
        listenedCategory?.let {
            CategoryItem(
                listenedCategory = it,
                position = index,
                subtitleColor = story.subtitleColor,
                modifier = modifier
            )
        }
    }
}

@Composable
fun CategoryItem(
    listenedCategory: ListenedCategory,
    position: Int,
    subtitleColor: Color,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 40.dp)
    ) {
        TextH30(
            text = "${position + 1}",
            color = subtitleColor,
            fontWeight = FontWeight.W700,
            fontFamily = DefaultFontFamily,
            disableScale = disableScale(),
            modifier = modifier
                .padding(end = 14.dp)
                .widthIn(min = 16.dp)
        )
        Row(
            modifier = modifier
                .padding(vertical = 10.dp)
                .weight(1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (position == 0) {
                CategoryTexts(
                    listenedCategory = listenedCategory,
                    subtitleColor = Color.White,
                    modifier = Modifier
                        .textGradient()
                )
            } else {
                CategoryTexts(
                    listenedCategory = listenedCategory,
                    titleColor = CategoryColor,
                    subtitleColor = subtitleColor,
                    modifier = Modifier
                )
            }
        }
    }
}

@Composable
private fun CategoryTexts(
    listenedCategory: ListenedCategory,
    titleColor: Color = MaterialTheme.theme.colors.primaryText01,
    subtitleColor: Color,
    modifier: Modifier,
) {
    val context = LocalContext.current
    val timeText = StatsHelper.secondsToFriendlyString(
        listenedCategory.totalPlayedTime,
        context.resources
    )
    Column {
        TextH10(
            text = listenedCategory.simplifiedCategoryName(),
            maxLines = 1,
            color = titleColor,
            fontSize = CategoryFontSize,
            fontFamily = DefaultFontFamily,
            fontWeight = FontWeight.Medium,
            disableScale = true,
            modifier = modifier,
        )
        Spacer(modifier = Modifier.height(3.dp))
        TextH50(
            text = timeText,
            color = subtitleColor,
            maxLines = 1,
            fontFamily = DefaultFontFamily,
            fontWeight = FontWeight.SemiBold,
            disableScale = disableScale(),
        )
    }
}

private fun List<ListenedCategory>.atIndex(index: Int) =
    if (index < size) this[index] else null

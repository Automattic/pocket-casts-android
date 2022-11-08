package au.com.shiftyjelly.pocketcasts.endofyear.views.stories

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppTheme
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.endofyear.stories.StoryTopListenedCategories
import au.com.shiftyjelly.pocketcasts.models.db.helper.ListenedCategory
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
fun StoryTopListenedCategoriesView(
    story: StoryTopListenedCategories,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.verticalScroll(rememberScrollState())) {
        TextH30(
            text = "Your Top Categories",
            textAlign = TextAlign.Center,
            color = story.tintColor,
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        )
        story.listenedCategories.subList(0, minOf(story.listenedCategories.count(), 5))
            .mapIndexed { index, listenedCategory ->
                CategoryItem(
                    listenedCategory = listenedCategory,
                    position = index,
                    tintColor = story.tintColor,
                )
            }
    }
}

@Composable
fun CategoryItem(
    listenedCategory: ListenedCategory,
    position: Int,
    tintColor: Color,
    modifier: Modifier = Modifier,
) {
    Column {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            TextP40(
                text = "${position + 1}",
                color = tintColor,
                modifier = modifier.padding(end = 16.dp)
            )
            Row(
                modifier = modifier
                    .padding(end = 16.dp)
                    .weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Image(
                    painter = painterResource(IR.drawable.defaultartwork),
                    contentDescription = null,
                    modifier = modifier
                        .size(64.dp)
                        .padding(top = 4.dp, end = 12.dp, bottom = 4.dp)
                )
                TextP40(
                    text = listenedCategory.category,
                    color = tintColor,
                    textAlign = TextAlign.Start,
                    modifier = modifier
                        .padding(end = 16.dp)
                        .weight(1f),
                )
                Column {
                    TextP40(
                        text = "${listenedCategory.numberOfPodcasts}",
                        color = tintColor
                    )
                    TextP40(
                        text = "Podcasts",
                        color = tintColor
                    )
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun CategoryItemPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppTheme(themeType) {
        Surface(color = Color.Black) {
            CategoryItem(
                listenedCategory = ListenedCategory(
                    numberOfPodcasts = 2,
                    totalPlayedTime = 1L,
                    category = "News"
                ),
                position = 0,
                tintColor = Color.White,
            )
        }
    }
}

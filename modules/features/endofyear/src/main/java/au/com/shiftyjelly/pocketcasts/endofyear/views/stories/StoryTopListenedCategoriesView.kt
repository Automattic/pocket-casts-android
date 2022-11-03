package au.com.shiftyjelly.pocketcasts.endofyear.views.stories

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.Dp
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
    val context = LocalContext.current
    var screenHeight by remember { mutableStateOf(1) }
    var textFieldHeight by remember { mutableStateOf(1) }
    Column(
        modifier = modifier
            .fillMaxSize()
            .onGloballyPositioned {
                screenHeight = it.size.height
            },
        verticalArrangement = Arrangement.Center
    ) {
        TextH30(
            text = "Your Top Categories",
            textAlign = TextAlign.Center,
            color = story.tintColor,
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .onGloballyPositioned {
                    textFieldHeight = it.size.height
                }
        )
        story.listenedCategories.subList(0, minOf(story.listenedCategories.count(), 5))
            .mapIndexed { index, listenedCategory ->
                CategoryItem(
                    listenedCategory = listenedCategory,
                    position = index,
                    tintColor = story.tintColor,
                    iconSize = getIconSize(screenHeight, textFieldHeight, context)
                )
            }
    }
}

@Composable
fun CategoryItem(
    listenedCategory: ListenedCategory,
    position: Int,
    tintColor: Color,
    iconSize: Dp,
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
                        .size(iconSize)
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
                iconSize = 64.dp,
            )
        }
    }
}

package au.com.shiftyjelly.pocketcasts.endofyear.views.stories

import android.content.Context
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.coerceAtMost
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.components.PodcastItem
import au.com.shiftyjelly.pocketcasts.compose.components.TextH30
import au.com.shiftyjelly.pocketcasts.endofyear.stories.StoryFake1
import au.com.shiftyjelly.pocketcasts.utils.extensions.pxToDp

@Composable
fun StoryFake1View(
    story: StoryFake1,
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
            text = "Your Top Podcasts",
            textAlign = TextAlign.Center,
            color = story.tintColor,
            modifier = modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
                .onGloballyPositioned {
                    textFieldHeight = it.size.height
                }
        )
        LazyColumn(
            modifier = modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.Center
        ) {
            items(story.podcasts.size) { index ->
                PodcastItem(
                    podcast = story.podcasts[index],
                    iconSize = getIconSize(screenHeight, textFieldHeight, context),
                    onClick = {},
                    tintColor = story.tintColor,
                    showDivider = false
                )
            }
        }
    }
}

fun getIconSize(
    screenHeight: Int,
    textFieldHeight: Int,
    context: Context
): Dp {
    return screenHeight.pxToDp(context).dp
        .minus(32.dp + textFieldHeight.dp)
        .div(5)
        .coerceAtMost(64.dp)
}

package au.com.shiftyjelly.pocketcasts.podcasts.view.podcast

import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.components.TextH20
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.compose.theme

private const val collapsedLines: Int = 3

@Composable
fun PodcastHeaderBottom(
    title: String,
    category: String,
    description: String,
    onDescriptionClicked: () -> Unit,
    modifier: Modifier = Modifier,
    ratingsContent: @Composable () -> Unit = {},
    podcastInfoContent: @Composable () -> Unit = {},
) {
    var isTitleExpanded by remember { mutableStateOf(false) }
    var isDescriptionExpanded by remember { mutableStateOf(false) }
    val interactionSource = remember { MutableInteractionSource() }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clipToBounds()
            .padding(bottom = 8.dp)
            .padding(horizontal = 16.dp)
            .background(MaterialTheme.theme.colors.primaryUi02),
    ) {
        TextH20(
            text = title,
            maxLines = if (isTitleExpanded) Int.MAX_VALUE else collapsedLines,
            overflow = if (isTitleExpanded) TextOverflow.Clip else TextOverflow.Ellipsis,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .animateContentSize(
                    animationSpec = tween(
                        durationMillis = 200,
                        easing = FastOutSlowInEasing,
                    ),
                )
                .clickable(
                    indication = null,
                    interactionSource = interactionSource,
                    onClick = {
                        isTitleExpanded = !isTitleExpanded
                    },
                ),
        )

        TextP40(
            text = category,
            fontSize = 15.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            color = MaterialTheme.theme.colors.primaryText02,
        )

        ratingsContent()

        Divider(
            color = MaterialTheme.theme.colors.primaryUi05,
            thickness = 1.dp,
        )

        TextP40(
            text = description,
            maxLines = if (isDescriptionExpanded) Int.MAX_VALUE else collapsedLines,
            overflow = if (isDescriptionExpanded) TextOverflow.Clip else TextOverflow.Ellipsis,
            lineHeight = 21.sp,
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 16.dp)
                .animateContentSize(
                    animationSpec = tween(
                        durationMillis = 200,
                        easing = FastOutSlowInEasing,
                    ),
                )
                .clickable(
                    indication = null,
                    interactionSource = interactionSource,
                    onClick = {
                        isDescriptionExpanded = !isDescriptionExpanded
                        onDescriptionClicked.invoke()
                    },
                ),
        )

        podcastInfoContent()
    }
}

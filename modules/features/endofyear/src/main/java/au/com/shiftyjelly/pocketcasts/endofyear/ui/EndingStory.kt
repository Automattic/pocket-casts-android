package au.com.shiftyjelly.pocketcasts.endofyear.ui

import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.Devices
import au.com.shiftyjelly.pocketcasts.compose.components.TextH10
import au.com.shiftyjelly.pocketcasts.compose.components.TextP40
import au.com.shiftyjelly.pocketcasts.models.to.Story
import kotlinx.coroutines.delay
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR
import au.com.shiftyjelly.pocketcasts.ui.R as UR

@Composable
internal fun EndingStory(
    story: Story.Ending,
    measurements: EndOfYearMeasurements,
    onRestartPlayback: () -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(story.backgroundColor)
            .padding(top = measurements.closeButtonBottomEdge),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        val animations = screenAnimations()

        Spacer(
            modifier = Modifier.weight(1f),
        )
        Image(
            painter = painterResource(IR.drawable.navdrawer_logo),
            contentDescription = null,
            modifier = Modifier
                .size(60.dp)
                .offset {
                    IntOffset(x = 0, y = animations.icon.offset.roundToPx())
                }
                .graphicsLayer {
                    alpha = animations.icon.alpha
                },
        )
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        TextH10(
            text = stringResource(LR.string.end_of_year_story_epilogue_title),
            fontScale = measurements.smallDeviceFactor,
            disableAutoScale = true,
            fontSize = 25.sp,
            color = colorResource(UR.color.white),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .offset {
                    IntOffset(x = 0, y = animations.title.offset.roundToPx())
                }
                .graphicsLayer {
                    alpha = animations.title.alpha
                },
            textAlign = TextAlign.Center,
        )
        Spacer(
            modifier = Modifier.height(16.dp),
        )
        TextP40(
            text = stringResource(LR.string.end_of_year_story_epilogue_subtitle),
            disableAutoScale = true,
            color = colorResource(UR.color.white),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .offset {
                    IntOffset(x = 0, y = animations.subTitle.offset.roundToPx())
                }
                .graphicsLayer {
                    alpha = animations.subTitle.alpha
                },
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.weight(1f))
        SolidEoyButton(
            text = stringResource(LR.string.end_of_year_replay),
            onClick = onRestartPlayback,
            backgroundColor = colorResource(UR.color.white),
            textColor = colorResource(UR.color.black),
        )
    }
}

private class ItemAnimation(
    val alpha: Float,
    val offset: Dp,
)

private class Animations(
    val icon: ItemAnimation,
    val title: ItemAnimation,
    val subTitle: ItemAnimation,
)

@Composable
private fun screenAnimations(): Animations {
    var iconAnimationTrigger by remember { mutableStateOf(false) }
    val iconAnimation = animateItem(start = iconAnimationTrigger, label = "icon animation")

    var titleAnimationTrigger by remember { mutableStateOf(false) }
    val titleAnimation = animateItem(start = titleAnimationTrigger, label = "title animation")
    var subTitleAnimationTrigger by remember { mutableStateOf(false) }
    val subTitleAnimation = animateItem(start = subTitleAnimationTrigger, label = "subTitle animation")

    LaunchedEffect(Unit) {
        iconAnimationTrigger = true
        delay(75)
        titleAnimationTrigger = true
        delay(75)
        subTitleAnimationTrigger = true
    }

    return Animations(
        icon = iconAnimation,
        title = titleAnimation,
        subTitle = subTitleAnimation,
    )
}

@Composable
private fun animateItem(start: Boolean, label: String): ItemAnimation {
    val transition = updateTransition(start, label)
    val alpha by transition.animateFloat(
        transitionSpec = {
            tween(durationMillis = 400, easing = FastOutLinearInEasing)
        },
    ) {
        if (it) {
            1f
        } else {
            0f
        }
    }
    val offset by transition.animateDp(
        transitionSpec = {
            tween(durationMillis = 400, easing = FastOutSlowInEasing)
        },
    ) {
        if (it) {
            0.dp
        } else {
            24.dp
        }
    }

    return ItemAnimation(alpha = alpha, offset = offset)
}

@Preview(device = Devices.PORTRAIT_REGULAR)
@Composable
private fun EndingPreview() {
    PreviewBox(currentPage = 10) { measurements ->
        EndingStory(
            story = Story.Ending,
            measurements = measurements,
            onRestartPlayback = {},
        )
    }
}

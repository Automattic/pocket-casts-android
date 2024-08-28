package au.com.shiftyjelly.pocketcasts.settings.whatsnew

import androidx.annotation.DrawableRes
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntOffset
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.BoxWithConstraintsScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.toMutableStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
fun SharingHeader() = SharingHeader(
    isCardVisible = false,
    socialPlatformsState = AnimationState.Gone,
)

@Composable
private fun SharingHeader(
    isCardVisible: Boolean,
    socialPlatformsState: AnimationState,
) {
    BoxWithConstraints(
        contentAlignment = Alignment.Center,
        modifier = Modifier.fillMaxSize(),
    ) {
        var showPodcastImage by remember { mutableStateOf(isCardVisible) }
        val transition = updateTransition(targetState = showPodcastImage, label = "podcast image state")
        val yOffset = LocalDensity.current.run { 10.dp.roundToPx() }
        val imageOffset by transition.animateIntOffset(
            label = "offset",
            transitionSpec = { tween(durationMillis = 800) },
        ) { state -> IntOffset(x = 0, y = if (state) 0 else yOffset) }
        val alpha by transition.animateFloat(
            label = "alpha",
            transitionSpec = { tween(durationMillis = 800) },
        ) { state -> if (state) 1f else 0f }

        LaunchedEffect(Unit) {
            delay(350)
            showPodcastImage = true
        }

        if (transition.currentState) {
            val platforms = SocialPlatform.entries
            val states = animationStates(initialState = socialPlatformsState, size = platforms.size)
            states.forEachIndexed { index, state ->
                SocialIcon(platform = platforms[index], animationState = state)
            }
        }

        Image(
            painter = painterResource(id = IR.drawable.whats_new_sharing_card),
            contentDescription = null,
            modifier = Modifier
                .offset { imageOffset }
                .alpha(alpha)
                .size(cardSize),
        )
    }
}

@Composable
private fun BoxWithConstraintsScope.SocialIcon(
    platform: SocialPlatform,
    animationState: AnimationState,
) {
    val iconSize = maxWidth * 0.13f
    val transitionData = updateTransitionData(
        label = "${platform.name} animation",
        animationState = animationState,
        targetOffset = LocalDensity.current.run {
            val spaceSize = DpSize(maxWidth, maxHeight)
            val cardSize = cardSize
            val (offsetX, offsetY) = platform.targetOffset(spaceSize, cardSize, iconSize)
            IntOffset(offsetX.roundToPx(), offsetY.roundToPx())
        },
    )

    Image(
        painter = painterResource(id = platform.imageId),
        contentDescription = null,
        modifier = Modifier
            .offset { transitionData.offset }
            .size(iconSize),
    )
}

private enum class SocialPlatform(
    @DrawableRes val imageId: Int,
    val targetOffset: (spaceSize: DpSize, cardSize: DpSize, iconSize: Dp) -> Pair<Dp, Dp>,
) {
    WhatsApp(
        imageId = IR.drawable.whats_new_sharing_whatsapp,
        targetOffset = { spaceSize, cardSize, iconSize ->
            val x = spaceSize.width * 0.06f + iconSize / 2 + cardSize.width / 2
            val y = -spaceSize.height * 0.09f - iconSize / 2 - cardSize.height / 2
            x to y
        },
    ),
    Instagram(
        imageId = IR.drawable.whats_new_sharing_instagram,
        targetOffset = { spaceSize, cardSize, iconSize ->
            val x = spaceSize.width * 0.07f + iconSize / 2 + cardSize.width / 2
            val y = -spaceSize.height * 0.03f + iconSize / 2 + cardSize.height / 2
            x to y
        },
    ),
    Telegram(
        imageId = IR.drawable.whats_new_sharing_telegram,
        targetOffset = { spaceSize, cardSize, iconSize ->
            val x = -spaceSize.width * 0.1f - iconSize / 2 - cardSize.width / 2
            val y = -spaceSize.height * 0.08f + iconSize / 2 + cardSize.height / 2
            x to y
        },
    ),
    Tumblr(
        imageId = IR.drawable.whats_new_sharing_tumblr,
        targetOffset = { spaceSize, cardSize, iconSize ->
            val x = -spaceSize.width * 0.05f - iconSize / 2 - cardSize.width / 2
            val y = -spaceSize.height * 0.07f - iconSize / 2 - cardSize.height / 2
            x to y
        },
    ),
}

private val BoxWithConstraintsScope.cardSize: DpSize
    get() {
        val cardWidth = maxWidth / 2.8f
        val cardHeight = cardWidth * 175 / 138
        return DpSize(cardWidth, cardHeight)
    }

private enum class AnimationState {
    Visible,
    Gone,
}

private class TransitionData(
    offset: State<IntOffset>,
) {
    val offset by offset
}

@Composable
private fun animationStates(
    initialState: AnimationState,
    size: Int,
): List<AnimationState> {
    var isAnimatingIn by remember { mutableStateOf(initialState == AnimationState.Gone) }
    val states = remember { List(size) { initialState }.toMutableStateList() }
    LaunchedEffect(Unit) {
        var isFirstRun = true
        while (isActive) {
            delay(if (isFirstRun) 1.seconds else 2.seconds)
            repeat(states.size) { index ->
                states[index] = if (isAnimatingIn) AnimationState.Visible else AnimationState.Gone
                delay(100.milliseconds)
            }
            isAnimatingIn = !isAnimatingIn
            isFirstRun = false
        }
    }
    return states
}

@Composable
private fun updateTransitionData(
    label: String,
    animationState: AnimationState,
    targetOffset: IntOffset,
): TransitionData {
    val transition = updateTransition(animationState, label)
    val offset = transition.animateIntOffset(
        label = "offset",
        transitionSpec = {
            when {
                AnimationState.Gone isTransitioningTo AnimationState.Visible -> spring(
                    dampingRatio = 0.5f,
                    stiffness = 300f,
                    visibilityThreshold = IntOffset(1, 1),
                )
                AnimationState.Visible isTransitioningTo AnimationState.Gone -> spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = 600f,
                    visibilityThreshold = IntOffset(1, 1),
                )
                else -> spring()
            }
        },
        targetValueByState = { state ->
            when (state) {
                AnimationState.Visible -> targetOffset
                AnimationState.Gone -> IntOffset(0, 0)
            }
        },
    )
    return remember(transition) { TransitionData(offset) }
}

@Preview
@Composable
private fun SharingHeaderPreview() {
    Box(modifier = Modifier.background(Color.White).size(400.dp)) {
        SharingHeader(isCardVisible = true, socialPlatformsState = AnimationState.Visible)
    }
}

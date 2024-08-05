package au.com.shiftyjelly.pocketcasts.settings.whatsnew

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateIntOffset
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.updateTransition
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material.Surface
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import au.com.shiftyjelly.pocketcasts.images.R as IR

@Composable
fun NewGiveRatingHeader() = NewGiveRatingHeader(initialState = AnimationState.Out)

@Composable
private fun NewGiveRatingHeader(
    initialState: AnimationState,
) {
    val states = animationStates(
        initialState,
        size = 5,
    )
    Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        states.forEach { Star(it) }
    }
}

@Composable
private fun Star(
    animationState: AnimationState,
) {
    val transitionData = updateTransitionData(animationState)

    Image(
        painter = painterResource(id = IR.drawable.star_filled_whats_new),
        contentDescription = null,
        modifier = Modifier
            .offset { transitionData.offset }
            .alpha(transitionData.alpha)
            .size(40.dp),
    )
}

private enum class AnimationState {
    In,
    Out,
}

private class TransitionData(
    alpha: State<Float>,
    offset: State<IntOffset>,
) {
    val alpha by alpha
    val offset by offset
}

@Composable
private fun animationStates(
    initialState: AnimationState,
    size: Int,
): List<AnimationState> {
    var isAnimatingIn by remember { mutableStateOf(initialState == AnimationState.Out) }
    val states = remember { List(size) { initialState }.toMutableStateList() }
    LaunchedEffect(Unit) {
        var isFirstRun = true
        while (isActive) {
            delay(
                when {
                    isFirstRun -> 1.seconds
                    isAnimatingIn -> 2.seconds
                    else -> 5.seconds
                },
            )
            repeat(states.size) { index ->
                states[index] = if (isAnimatingIn) AnimationState.In else AnimationState.Out
                delay(50.milliseconds)
            }
            isAnimatingIn = !isAnimatingIn
            isFirstRun = false
        }
    }
    return states
}

@Composable
private fun updateTransitionData(
    animationState: AnimationState,
): TransitionData {
    val offsetValue = LocalDensity.current.run { 16.dp.roundToPx() }
    val transition = updateTransition(animationState, "star transition")
    val alpha = transition.animateFloat(
        label = "star alpha",
        transitionSpec = {
            when {
                AnimationState.In isTransitioningTo AnimationState.Out -> spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = 150f,
                )
                else -> spring()
            }
        },
        targetValueByState = { state ->
            when (state) {
                AnimationState.In -> 1f
                AnimationState.Out -> 0f
            }
        },
    )
    val offset = transition.animateIntOffset(
        label = "star offset",
        transitionSpec = {
            when {
                AnimationState.Out isTransitioningTo AnimationState.In -> spring(
                    dampingRatio = 0.25f,
                    stiffness = 600f,
                    visibilityThreshold = IntOffset(1, 1),
                )
                AnimationState.In isTransitioningTo AnimationState.Out -> spring(
                    dampingRatio = Spring.DampingRatioNoBouncy,
                    stiffness = 150f,
                    visibilityThreshold = IntOffset(1, 1),
                )
                else -> spring()
            }
        },
        targetValueByState = { state ->
            when (state) {
                AnimationState.In -> IntOffset(0, 0)
                AnimationState.Out -> IntOffset(0, -offsetValue)
            }
        },
    )
    return remember(transition) { TransitionData(alpha, offset) }
}

@Preview
@Composable
private fun NewGiveRatingHeaderPreview(@PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType) {
    AppThemeWithBackground(themeType) {
        Surface {
            NewGiveRatingHeader(initialState = AnimationState.In)
        }
    }
}

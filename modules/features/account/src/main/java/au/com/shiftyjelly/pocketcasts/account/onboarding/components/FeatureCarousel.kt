package au.com.shiftyjelly.pocketcasts.account.onboarding.components

import androidx.compose.animation.Animatable
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.max
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val CAROUSEL_ITEM_COUNT = 3

@Composable
fun FeatureCarousel(
    modifier: Modifier = Modifier,
) {
    val delayBetweenCycles = 4.seconds
    val (activeItemIndex, sendEvent) = storyCoordinator(
        cycleRange = 0 until CAROUSEL_ITEM_COUNT,
        delayBetweenCycles = delayBetweenCycles,
        isPerpetual = false,
    )

    Column(
        modifier = modifier.padding(top = 11.dp),
    ) {
        CarouselActiveItemIndicatorBar(
            itemCount = CAROUSEL_ITEM_COUNT,
            activeItemIndex = activeItemIndex.value,
        )
        Box {
            Crossfade(
                targetState = activeItemIndex.value,
                modifier = Modifier.fillMaxSize(),
            ) { index ->
                when (index) {
                    0 -> BestAppAnimation(
                        modifier = Modifier.padding(top = 64.dp),
                        itemDisplayDuration = delayBetweenCycles,
                    )

                    1 -> CustomizationIsInsaneAnimation(
                        modifier = Modifier.padding(top = 24.dp),
                        itemDisplayDuration = delayBetweenCycles,
                    )

                    2 -> OrganizingPodcastsAnimation(
                        modifier = Modifier.padding(top = 24.dp),
                        itemDisplayDuration = delayBetweenCycles,
                    )
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth(.2f)
                    .fillMaxHeight()
                    .align(Alignment.CenterStart)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        role = Role.Button,
                        onClick = {
                            sendEvent(CounterEvent.Decrement)
                        },
                    )
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth(.2f)
                    .fillMaxHeight()
                    .align(Alignment.CenterEnd)
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() },
                        role = Role.Button,
                        onClick = {
                            sendEvent(CounterEvent.Increment)
                        },
                    )
            )
        }
    }
}

sealed interface CounterEvent {
    data object Increment : CounterEvent
    data object Decrement : CounterEvent
    data object InternalTick: CounterEvent
}

@Composable
fun storyCoordinator(
    cycleRange: IntRange,
    delayBetweenCycles: Duration = 3.seconds,
    isPerpetual: Boolean = true,
): Pair<State<Int>, (CounterEvent) -> Unit> {
    val index = remember { mutableIntStateOf(cycleRange.start) }
    val scope = rememberCoroutineScope()
    val events = remember { Channel<CounterEvent>(Channel.BUFFERED) }

    LaunchedEffect(cycleRange, delayBetweenCycles, isPerpetual) {
        fun startTicker() = launch {
            while (isActive) {
                delay(delayBetweenCycles.inWholeMilliseconds)
                events.send(CounterEvent.InternalTick)
            }
        }

        var ticker = startTicker()

        for (event in events) {
            val change = when (event) {
                CounterEvent.InternalTick,
                CounterEvent.Increment -> 1
                CounterEvent.Decrement -> -1
            }
            val newValue = max(0, (index.intValue + change) % cycleRange.count())

            val allowedValue = if (newValue == 0) {
                if (isPerpetual || event is CounterEvent.Decrement) {
                    ticker.cancel()
                    0
                } else {
                    index.intValue
                }
            } else {
                newValue
            }

             if (event !is CounterEvent.InternalTick) {
                ticker.cancel()
                ticker = startTicker()
            }

            index.intValue = allowedValue
        }
    }

    return index to {
        scope.launch {
            events.send(it)
        }
    }
}

@Composable
private fun CarouselActiveItemIndicatorBar(
    itemCount: Int,
    activeItemIndex: Int,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        (0 until itemCount).forEach { index ->
            CarouselActiveItemIndicator(
                modifier = Modifier.weight(1f),
                isActive = activeItemIndex == index,
            )
        }
    }
}

@Composable
private fun CarouselActiveItemIndicator(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    activeColor: Color = Color(0xff5B5B5B),
    inactiveColor: Color = Color(0xffD9D9D9),
) {
    val colorAnim = remember { Animatable(if (isActive) activeColor else inactiveColor) }
    LaunchedEffect(isActive) {
        colorAnim.animateTo(
            targetValue = if (isActive) activeColor else inactiveColor,
            animationSpec = tween(durationMillis = 200, easing = FastOutLinearInEasing),
        )
    }

    Box(
        modifier = modifier
            .height(4.dp)
            .background(color = colorAnim.value),
    )
}

@Preview
@Composable
private fun PreviewFeatureCarousel() = AppThemeWithBackground(Theme.ThemeType.LIGHT) {
    FeatureCarousel(
        modifier = Modifier.fillMaxSize(),
    )
}

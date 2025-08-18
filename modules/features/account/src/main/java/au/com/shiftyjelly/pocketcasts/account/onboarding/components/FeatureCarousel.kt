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
import androidx.compose.runtime.produceState
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
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.selects.onTimeout
import kotlinx.coroutines.selects.select

private const val CAROUSEL_ITEM_COUNT = 3
private val DISAPPEAR_DELAY = 300.milliseconds

@Composable
fun FeatureCarousel(
    modifier: Modifier = Modifier,
) {
    val delayBetweenCycles = 4.seconds
    val (activeItem, sendEvent) = carouselCoordinator(
        cycleRange = 0 until CAROUSEL_ITEM_COUNT,
        delayBetweenCycles = delayBetweenCycles,
        isPerpetual = false,
        disappearDuration = DISAPPEAR_DELAY,
    )

    Column(
        modifier = modifier.padding(top = 11.dp),
    ) {
        CarouselActiveItemIndicatorBar(
            itemCount = CAROUSEL_ITEM_COUNT,
            activeItemIndex = activeItem.value.selectedIndex,
        )
        Box {
            Crossfade(
                targetState = activeItem.value.selectedIndex,
                modifier = Modifier.fillMaxSize(),
            ) { index ->
                when (index) {
                    0 -> BestAppAnimation(
                        modifier = Modifier.padding(top = 64.dp),
                        isAppearing = activeItem.value.isAppearing,
                        disappearAnimDuration = DISAPPEAR_DELAY,
                    )

                    1 -> CustomizationIsInsaneAnimation(
                        modifier = Modifier.padding(top = 24.dp),
                        isAppearing = activeItem.value.isAppearing,
                        disappearAnimDuration = DISAPPEAR_DELAY,
                    )

                    2 -> OrganizingPodcastsAnimation(
                        modifier = Modifier.padding(top = 24.dp),
                        isAppearing = activeItem.value.isAppearing,
                        disappearAnimDuration = DISAPPEAR_DELAY,
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
                            sendEvent(CarouselEvent.Previous)
                        },
                    ),
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
                            sendEvent(CarouselEvent.Next)
                        },
                    ),
            )
        }
    }
}

sealed interface CarouselEvent {
    data object Next : CarouselEvent
    data object Previous : CarouselEvent
}

data class CarouselState(
    val selectedIndex: Int,
    val isAppearing: Boolean,
)

@OptIn(ExperimentalCoroutinesApi::class)
@Composable
fun carouselCoordinator(
    cycleRange: IntRange,
    disappearDuration: Duration,
    delayBetweenCycles: Duration = 3.seconds,
    isPerpetual: Boolean = true,
): Pair<State<CarouselState>, (CarouselEvent) -> Unit> {
    val scope = rememberCoroutineScope()
    val events = remember { Channel<CarouselEvent>(Channel.BUFFERED) }

    val state = produceState(
        initialValue = CarouselState(cycleRange.first, isAppearing = true),
        cycleRange,
        delayBetweenCycles,
        isPerpetual,
        disappearDuration,
    ) {
        var currentIndex = cycleRange.first

        while (isActive) {
            val event: CarouselEvent? = select {
                onTimeout(delayBetweenCycles) { CarouselEvent.Next }
                events.onReceive {
                    it
                }
            }

            val nextIndex = when (event) {
                CarouselEvent.Next -> {
                    val next = currentIndex + 1
                    when {
                        next > cycleRange.last -> {
                            if (isPerpetual) {
                                cycleRange.first
                            } else {
                                // stay at the last index — ignore timeout
                                currentIndex
                            }
                        }

                        else -> next
                    }
                }

                CarouselEvent.Previous -> {
                    val prev = currentIndex - 1
                    if (prev < cycleRange.first) cycleRange.last else prev
                }

                null -> break
            }

            if (
                nextIndex == currentIndex && event is CarouselEvent.Next && !isPerpetual || // already at last index in finite mode
                (currentIndex == cycleRange.first && event is CarouselEvent.Previous && !isPerpetual) // already at first index in finite mode
            ) {
                continue
            }

            value = CarouselState(currentIndex, isAppearing = false)
            delay(disappearDuration.inWholeMilliseconds + 100)
            currentIndex = nextIndex
            value = CarouselState(currentIndex, isAppearing = true)
        }
    }

    val sendEvent: (CarouselEvent) -> Unit = { event ->
        scope.launch { events.send(event) }
    }

    return state to sendEvent
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

package au.com.shiftyjelly.pocketcasts.account.onboarding.components

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.theme
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
private val ITEM_SHOW_DURATION = 7.seconds

@Composable
fun FeatureCarousel(
    modifier: Modifier = Modifier,
) {
    val (activeItem, sendEvent) = carouselCoordinator(
        cycleRange = 0 until CAROUSEL_ITEM_COUNT,
        timeBetweenCycles = ITEM_SHOW_DURATION,
        isPerpetual = false,
        disappearDuration = DISAPPEAR_DELAY,
    )

    Column(
        modifier = modifier.padding(top = 11.dp),
    ) {
        CarouselActiveItemIndicatorBar(
            modifier = Modifier.fillMaxWidth()
                .padding(horizontal = 12.dp),
            itemCount = CAROUSEL_ITEM_COUNT,
            activeItemIndex = activeItem.value.selectedIndex,
            pageShowDuration = ITEM_SHOW_DURATION,
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
                    .fillMaxWidth(.5f)
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
                    .fillMaxWidth(.5f)
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
    timeBetweenCycles: Duration = 3.seconds,
    isPerpetual: Boolean = true,
): Pair<State<CarouselState>, (CarouselEvent) -> Unit> {
    val scope = rememberCoroutineScope()
    val events = remember { Channel<CarouselEvent>(Channel.BUFFERED) }

    val state = produceState(
        initialValue = CarouselState(cycleRange.first, isAppearing = true),
        cycleRange,
        timeBetweenCycles,
        isPerpetual,
        disappearDuration,
    ) {
        var currentIndex = cycleRange.first

        while (isActive) {
            val event: CarouselEvent? = select {
                onTimeout(timeBetweenCycles) { CarouselEvent.Next }
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
    pageShowDuration: Duration,
    modifier: Modifier = Modifier,
) {
    var progressPercentAnim by remember(activeItemIndex) { mutableStateOf(Animatable(0f)) }

    LaunchedEffect(progressPercentAnim, pageShowDuration) {
        progressPercentAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                easing = LinearEasing,
                durationMillis = pageShowDuration.inWholeMilliseconds.toInt(),
            ),
        )
    }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        (0 until itemCount).forEach { index ->
            CarouselActiveItemIndicator(
                modifier = Modifier.weight(1f),
                progressPercent = if (index < activeItemIndex) {
                    1f
                } else if (index == activeItemIndex) {
                    progressPercentAnim.value
                } else {
                    0f
                },
            )
        }
    }
}

@Composable
private fun CarouselActiveItemIndicator(
    progressPercent: Float,
    modifier: Modifier = Modifier,
    activeColor: Color = MaterialTheme.theme.colors.primaryText01,
    inactiveColor: Color = MaterialTheme.theme.colors.primaryField03,
) {
    val normalizedProgress = progressPercent.coerceIn(0f, 1f)
    Box(
        modifier = modifier
            .height(4.dp)
            .background(
                brush = Brush.horizontalGradient(
                    0f to activeColor,
                    normalizedProgress to activeColor,
                    normalizedProgress to inactiveColor,
                    1f to inactiveColor,
                ),
            ),
    )
}

@Preview
@Composable
private fun PreviewFeatureCarousel() = AppThemeWithBackground(Theme.ThemeType.LIGHT) {
    FeatureCarousel(
        modifier = Modifier.fillMaxSize(),
    )
}

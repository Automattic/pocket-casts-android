package au.com.shiftyjelly.pocketcasts.account.onboarding.components

import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import kotlinx.coroutines.delay
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

private const val CAROUSEL_ITEM_COUNT = 3

@Composable
fun FeatureCarousel(
    modifier: Modifier = Modifier,
) {
    val activeItemIndex by cyclicCounter(
        cycleRange = 0 until CAROUSEL_ITEM_COUNT,
        delayBetweenCycles = 3.seconds,
    )

    Column(
        modifier = modifier.padding(top = 11.dp),
    ) {
        CarouselActiveItemIndicatorBar(
            itemCount = CAROUSEL_ITEM_COUNT,
            activeItemIndex = activeItemIndex,
        )
        Crossfade(targetState = activeItemIndex, modifier = Modifier.fillMaxSize()) { index ->
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                when (index) {
                    0 -> BestAppAnimation(
                        modifier = Modifier.padding(top = 64.dp),
                        itemDisplayDuration = 3.seconds,
                    )

                    1 -> CustomizationIsInsaneAnimation(
                        itemDisplayDuration = 3.seconds,
                    )

                    2 -> OrganizingPodcastsAnimation(
                        itemDisplayDuration = 3.seconds,
                    )
                }
            }
        }
    }
}

@Composable
fun cyclicCounter(
    cycleRange: IntRange,
    delayBetweenCycles: Duration = 3.seconds,
    isPerpetual: Boolean = true,
): State<Int> {
    return produceState(cycleRange.start) {
        while (true) {
            delay(delayBetweenCycles.inWholeMilliseconds)
            val nextValue = (value + 1) % cycleRange.count()
            if (nextValue == 0 && !isPerpetual) break
            value = nextValue
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
    // TODO animate bg change
    Box(
        modifier = modifier
            .height(4.dp)
            .background(color = if (isActive) activeColor else inactiveColor),
    )
}

@Preview
@Composable
private fun PreviewFeatureCarousel() = AppThemeWithBackground(Theme.ThemeType.LIGHT) {
    FeatureCarousel(
        modifier = Modifier.fillMaxSize(),
    )
}

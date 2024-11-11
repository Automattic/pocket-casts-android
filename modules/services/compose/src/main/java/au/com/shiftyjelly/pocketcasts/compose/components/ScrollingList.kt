package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit
import kotlinx.coroutines.isActive

@Composable
fun <T> ScrollingRow(
    items: List<T>,
    modifier: Modifier = Modifier,
    scrollDirection: HorizontalDirection = HorizontalDirection.Right,
    scrollSpeed: ScrollSpeed = ScrollSpeed(500.dp, 10.seconds),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    horizontalArrangement: Arrangement.Horizontal = if (!reverseLayout) Arrangement.Start else Arrangement.End,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable LazyItemScope.(T) -> Unit,
) {
    // Not using rememberLazyListState() because we want to reset
    // the scroll state on orientation changes so that the hardcoded column
    // is redisplayed, which insures the height is correctly calculated. For that
    // reason, we want to use remember, not rememberSaveable.
    val state = remember {
        LazyListState(firstVisibleItemIndex = scrollDirection.initialIndex)
    }
    AutoScrollEffect(state, scrollDirection, scrollSpeed)
    LazyRow(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = verticalAlignment,
        userScrollEnabled = false,
    ) {
        items(Int.MAX_VALUE) { index ->
            content(this, items[index % items.size])
        }
    }
}

@Composable
fun <T> ScrollingColumn(
    items: List<T>,
    modifier: Modifier = Modifier,
    scrollDirection: VerticalDirection = VerticalDirection.Top,
    scrollSpeed: ScrollSpeed = ScrollSpeed(1000.dp, 10.seconds),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    reverseLayout: Boolean = false,
    verticalArrangement: Arrangement.Vertical = if (!reverseLayout) Arrangement.Top else Arrangement.Bottom,
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable LazyItemScope.(T) -> Unit,
) {
    // Not using rememberLazyListState() because we want to reset
    // the scroll state on orientation changes so that the hardcoded column
    // is redisplayed, which insures the height is correctly calculated. For that
    // reason, we want to use remember, not rememberSaveable.
    val state = remember {
        LazyListState(firstVisibleItemIndex = scrollDirection.initialIndex)
    }
    AutoScrollEffect(state, scrollDirection, scrollSpeed)
    LazyColumn(
        modifier = modifier,
        state = state,
        contentPadding = contentPadding,
        reverseLayout = reverseLayout,
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        userScrollEnabled = false,
    ) {
        items(Int.MAX_VALUE) { index ->
            content(this, items[index % items.size])
        }
    }
}

data class ScrollSpeed(
    val distance: Dp,
    val duration: Duration,
)

sealed interface ScrollDirection {
    val initialIndex: Int
    val reverseScroll: Boolean
}

sealed interface VerticalDirection : ScrollDirection {
    data object Top : VerticalDirection {
        override val initialIndex = 0
        override val reverseScroll = false
    }

    data object Bottom : VerticalDirection {
        override val initialIndex = Int.MAX_VALUE - 1
        override val reverseScroll = true
    }
}

sealed interface HorizontalDirection : ScrollDirection {
    data object Left : HorizontalDirection {
        override val initialIndex = 0
        override val reverseScroll = false
    }

    data object Right : HorizontalDirection {
        override val initialIndex = Int.MAX_VALUE - 1
        override val reverseScroll = true
    }
}

@Composable
private fun AutoScrollEffect(
    state: LazyListState,
    direction: ScrollDirection,
    speed: ScrollSpeed,
) {
    val density = LocalDensity.current
    val (distance, spec) = remember {
        var distance = density.run { speed.distance.toPx() }
        if (direction.reverseScroll) {
            distance = -distance
        }
        distance to tween<Float>(
            durationMillis = speed.duration.toInt(DurationUnit.MILLISECONDS),
            easing = LinearEasing,
        )
    }
    LaunchedEffect(Unit) {
        while (isActive) {
            state.animateScrollBy(distance, spec)
        }
    }
}

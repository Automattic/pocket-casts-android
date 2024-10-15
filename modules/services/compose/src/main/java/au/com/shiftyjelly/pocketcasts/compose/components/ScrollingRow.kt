package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import kotlin.math.roundToLong
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

@Composable
fun <T> ScrollingRow(
    items: List<T>,
    modifier: Modifier = Modifier,
    scrollDirection: ScrollDirection = ScrollDirection.Right,
    scrollByPixels: Float = 1f,
    scrollDelay: (Density) -> Long = { (100 / it.density).roundToLong().coerceAtLeast(4L) },
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
        LazyListState(
            firstVisibleItemIndex = when (scrollDirection) {
                ScrollDirection.Left -> Int.MAX_VALUE - 1
                ScrollDirection.Right -> 0
            },
        )
    }
    val density = LocalDensity.current
    LaunchedEffect(Unit) {
        while (isActive) {
            val scrollValue = when (scrollDirection) {
                ScrollDirection.Left -> -scrollByPixels
                ScrollDirection.Right -> scrollByPixels
            }
            state.scrollBy(scrollValue)
            delay(scrollDelay(density))
        }
    }
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

enum class ScrollDirection {
    Left,
    Right,
}

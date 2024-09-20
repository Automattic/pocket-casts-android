package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.foundation.gestures.scrollBy
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import java.lang.Long.max
import kotlinx.coroutines.delay

private const val MAX_ITEMS = 500

@Composable
fun ScrollingRow(
    scrollDirection: ScrollDirection = ScrollDirection.RIGHT,
    scrollAutomatically: Boolean = true,
    spacedBy: Dp = 16.dp,
    paused: Boolean = false,
    rowItems: @Composable () -> Unit,
) {
    // Not using rememberLazyListState() because we want to reset
    // the scroll state on orientation changes so that the hardcoded column
    // is redisplayed, which insures the height is correctly calculated. For that
    // reason, we want to use remember, not rememberSaveable.
    val state = remember { LazyListState() }
    var initialScrollCompleted by remember { mutableStateOf(false) }
    val localConfiguration = LocalConfiguration.current
    LaunchedEffect(scrollAutomatically && paused) {
        if (scrollAutomatically) {
            // This seems to get a good scroll speed across multiple devices
            val scrollDelay = max(1L, (1000L - localConfiguration.densityDpi) / 90)
            if (!initialScrollCompleted) {
                if (scrollDirection == ScrollDirection.LEFT) {
                    state.scrollToItem(MAX_ITEMS - 1)
                }
                initialScrollCompleted = true
            }
            autoScroll(scrollDirection, scrollDelay, state, paused)
        }
    }
    LazyRow(
        state = state,
        horizontalArrangement = Arrangement.spacedBy(spacedBy),
        userScrollEnabled = !scrollAutomatically,
    ) {
        if (scrollAutomatically) {
            items(MAX_ITEMS) { // arbitrary large number that users will probably never hit
                // Nesting a Row of items inside the LazyRow because a Row can use IntrinsidSize.Max
                // to determine the height of the tallest list item and keep a consistent
                // height, regardless of which items are visible. This ensures that the
                // LazyRow as a whole always has a single, consistent height that does not
                // change as items scroll into/out-of view. If IntrinsicSize.Max could work
                // with LazyRows, we wouldn't need to nest Rows in the LazyRow.
                NestedRow(rowItems, spacedBy)
            }
        } else {
            item {
                NestedRow(rowItems, spacedBy)
            }
        }
    }
}

@Composable
fun NestedRow(
    rowItems: @Composable () -> Unit,
    spacedBy: Dp,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(spacedBy),
        modifier = Modifier
            .height(IntrinsicSize.Max),
    ) {
        rowItems()
    }
}

// Based on https://stackoverflow.com/a/71344813/1910286
private tailrec suspend fun autoScroll(
    scrollDirection: ScrollDirection,
    scrollDelay: Long,
    lazyListState: LazyListState,
    paused: Boolean = false,
) {
    val scrollAmount = lazyListState.scrollBy(if (scrollDirection == ScrollDirection.RIGHT) 1f else -1f)
    if (scrollAmount == 0f) {
        // If we can't scroll, we're at one end, so jump to the other end.
        // This will be an abrupt jump, but users shouldn't really ever be
        // getting to the end of the list, so it should be very rare.
        lazyListState.scrollToItem(if (scrollDirection == ScrollDirection.RIGHT) 0 else MAX_ITEMS - 1)
    }
    if (!paused) {
        delay(scrollDelay)
        autoScroll(scrollDirection, scrollDelay, lazyListState)
    }
}

enum class ScrollDirection {
    LEFT,
    RIGHT,
}

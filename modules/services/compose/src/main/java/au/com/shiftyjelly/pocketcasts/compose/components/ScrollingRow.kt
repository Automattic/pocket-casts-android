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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import java.lang.Long.max

private const val MAX_ITEMS = 500

@Composable
fun ScrollingRow(
    scrollAutomatically: Boolean = true,
    rowItems: @Composable () -> Unit,
) {

    // Not using rememberLazyListState() because we want to reset
    // the scroll state on orientation changes so that the hardcoded column
    // is redisplayed, which insures the height is correctly calculated. For that
    // reason, we want to use remember, not rememberSaveable.
    val state = remember { LazyListState() }

    val localConfiguration = LocalConfiguration.current
    LaunchedEffect(scrollAutomatically) {
        if (scrollAutomatically) {
            // This seems to get a good scroll speed across multiple devices
            val scrollDelay = max(1L, (1000L - localConfiguration.densityDpi) / 90)
            autoScroll(scrollDelay, state)
        }
    }
    LazyRow(
        state = state,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
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
                NestedRow(rowItems)
            }
        } else {
            item {
                NestedRow(rowItems)
            }
        }
    }
}

@Composable
fun NestedRow(
    rowItems: @Composable () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier
            .height(IntrinsicSize.Max)
    ) {
        rowItems()
    }
}

// Based on https://stackoverflow.com/a/71344813/1910286
private tailrec suspend fun autoScroll(
    scrollDelay: Long,
    lazyListState: LazyListState,
) {
    val scrollAmount = lazyListState.scrollBy(1f)
    if (scrollAmount == 0f) {
        // If we can't scroll, we're at the end, so jump to the beginning.
        // This will be an abrupt jump, but users shouldn't really ever be
        // getting to the end of the list, so it should be very rare.
        lazyListState.scrollToItem(0)
    }
    delay(scrollDelay)
    autoScroll(scrollDelay, lazyListState)
}

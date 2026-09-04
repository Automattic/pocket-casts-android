package au.com.shiftyjelly.pocketcasts.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * A one-shot signal the scaffold raises when Back is pressed while the top bar is visible, so the
 * active tab can scroll its content back to the top before focus moves to the top navigation.
 */
@Stable
class TvScrollToTop {
    internal var trigger by mutableIntStateOf(0)

    internal fun request() {
        trigger++
    }
}

val LocalScrollToTop = staticCompositionLocalOf<TvScrollToTop> {
    error("TvScrollToTop was not provided")
}

/** Runs [onScrollToTop] whenever a Back-triggered scroll-to-top request arrives after composition. */
@Composable
internal fun ScrollToTopEffect(onScrollToTop: suspend () -> Unit) {
    val scrollToTop = LocalScrollToTop.current
    val currentByState by rememberUpdatedState(onScrollToTop)
    var isInitial by remember { mutableStateOf(true) }
    LaunchedEffect(scrollToTop.trigger) {
        if (isInitial) {
            isInitial = false
        } else if (scrollToTop.trigger > 0) {
            currentByState()
        }
    }
}

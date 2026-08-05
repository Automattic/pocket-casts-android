package au.com.shiftyjelly.pocketcasts.component

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf

@Stable
class TvTopBarVisibility {
    private var detailCount by mutableIntStateOf(0)

    val isVisible: Boolean get() = detailCount == 0

    internal fun enterDetail() {
        detailCount += 1
    }

    internal fun exitDetail() {
        detailCount -= 1
    }
}

val LocalTvTopBarVisibility = staticCompositionLocalOf<TvTopBarVisibility> {
    error("TvTopBarVisibility was not provided")
}

@Composable
fun HideTvTopBar() {
    val visibility = LocalTvTopBarVisibility.current
    DisposableEffect(visibility) {
        visibility.enterDetail()
        onDispose { visibility.exitDetail() }
    }
}

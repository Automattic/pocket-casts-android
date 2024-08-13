package au.com.shiftyjelly.pocketcasts.sharing.clip

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import au.com.shiftyjelly.pocketcasts.sharing.ui.ClipSelectorState

@Composable
internal fun rememberClipPageState(
    firstVisibleItemIndex: Int,
) = rememberSaveable(
    saver = ClipPageState.Saver,
    init = {
        ClipPageState(
            selectorState = ClipSelectorState(
                firstVisibleItemIndex = firstVisibleItemIndex,
                firstVisibleItemScrollOffset = 0,
                scale = 1f,
                secondsPerTick = 1,
                itemWidth = 0f,
                startOffset = 0f,
                endOffset = 0f,
            ),
        )
    },
)

internal class ClipPageState(
    val selectorState: ClipSelectorState,
) {
    var topContentHeight by mutableIntStateOf(0)
    var pagerIndicatorHeight by mutableIntStateOf(0)

    companion object {
        val Saver: Saver<ClipPageState, Any> = listSaver(
            save = {
                listOf(
                    with(ClipSelectorState.Saver) { save(it.selectorState) },
                )
            },
            restore = {
                ClipPageState(
                    selectorState = requireNotNull(ClipSelectorState.Saver.restore(it[0] as Any)) {
                        "ClipSelectorState.Saver should never return null"
                    },
                )
            },
        )
    }
}

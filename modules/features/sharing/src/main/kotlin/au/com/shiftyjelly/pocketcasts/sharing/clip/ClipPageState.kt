package au.com.shiftyjelly.pocketcasts.sharing.clip

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import au.com.shiftyjelly.pocketcasts.sharing.ui.ClipSelectorState

@Composable
internal fun rememberClipPageState(
    firstVisibleItemIndex: Int,
    step: SharingStep = SharingStep.Creating,
    isSharing: Boolean = false,
) = rememberSaveable(
    saver = ClipPageState.Saver,
    init = {
        ClipPageState(
            step = step,
            selectorState = ClipSelectorState(
                firstVisibleItemIndex = firstVisibleItemIndex,
                firstVisibleItemScrollOffset = 0,
                scale = 1f,
                secondsPerTick = 1,
                itemWidth = 0f,
                startOffset = 0f,
                endOffset = 0f,
            ),
            isSharing = isSharing,
        )
    },
)

internal class ClipPageState(
    step: SharingStep,
    val selectorState: ClipSelectorState,
    isSharing: Boolean,
) {
    var step by mutableStateOf(step)
    var topContentHeight by mutableIntStateOf(0)
    var pagerIndicatorHeight by mutableIntStateOf(0)
    var isSharing by mutableStateOf(isSharing)

    companion object {
        val Saver: Saver<ClipPageState, Any> = listSaver(
            save = {
                listOf(
                    it.step,
                    with(ClipSelectorState.Saver) { save(it.selectorState) },
                )
            },
            restore = {
                ClipPageState(
                    step = it[0] as SharingStep,
                    selectorState = requireNotNull(ClipSelectorState.Saver.restore(it[1] as Any)) {
                        "ClipSelectorState.Saver should never return null"
                    },
                    // isSharing is a transient state that shouldn't survive configuration changes
                    // because clip sharing is launched in the rememberCoroutineScope() of
                    // a Composable UI
                    isSharing = false,
                )
            },
        )
    }
}

internal enum class SharingStep {
    Creating,
    Sharing,
}

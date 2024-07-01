package au.com.shiftyjelly.pocketcasts.clip

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Composable
internal fun rememberClipSelectorState() = rememberSaveable(saver = ClipSelectorState.Saver, init = ::ClipSelectorState)

internal class ClipSelectorState(
    firstVisibleItemIndex: Int = 0,
    firstVisibleItemScrollOffset: Int = 0,
    scale: Float = 1f,
    tickResolution: Int = 1,
    itemWidth: Float = 0f,
    startOffset: Float = 0f,
    endOffset: Float = 0f,
) {
    val listState = LazyListState(firstVisibleItemIndex, firstVisibleItemScrollOffset)
    val scrollOffset: Float
        get() {
            val visibleIntex = listState.firstVisibleItemIndex
            val scrolledItemsWidth = visibleIntex * itemWidth
            return scrolledItemsWidth + listState.firstVisibleItemScrollOffset
        }
    val scrollOffsetState get() = derivedStateOf { scrollOffset }
    var startOffset by mutableFloatStateOf(startOffset)
    var endOffset by mutableFloatStateOf(endOffset)
    var itemWidth by mutableFloatStateOf(itemWidth)
    var scale by mutableFloatStateOf(scale)
    var tickResolution by mutableIntStateOf(tickResolution)

    fun scaleBoxOffsets(clipRange: Clip.Range) {
        startOffset = durationToPixels(clipRange.start)
        endOffset = durationToPixels(clipRange.end)
    }

    fun durationToPixels(duration: Duration): Float {
        val seconds = duration.inWholeSeconds
        val ticks = seconds.toFloat() / tickResolution
        return itemWidth * ticks
    }

    fun pixelsToDuration(pixels: Float): Duration {
        val ticks = pixels / itemWidth
        val seconds = ticks * tickResolution
        return seconds.toDouble().seconds
    }

    companion object {
        val Saver: Saver<ClipSelectorState, *> = listSaver(
            save = {
                listOf(
                    it.listState.firstVisibleItemIndex,
                    it.listState.firstVisibleItemScrollOffset,
                    it.scale,
                    it.tickResolution,
                    it.itemWidth,
                    it.startOffset,
                    it.endOffset,
                )
            },
            restore = {
                ClipSelectorState(
                    firstVisibleItemIndex = it[0] as Int,
                    firstVisibleItemScrollOffset = it[1] as Int,
                    scale = it[2] as Float,
                    tickResolution = it[3] as Int,
                    itemWidth = it[4] as Float,
                    startOffset = it[5] as Float,
                    endOffset = it[6] as Float,
                )
            },
        )
    }
}

package au.com.shiftyjelly.pocketcasts.sharing.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.sharing.clip.Clip
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.DurationUnit

@Composable
internal fun rememberClipSelectorState(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int = 0,
    scale: Float = 1f,
) = rememberSaveable(
    saver = ClipSelectorState.Saver,
    init = {
        ClipSelectorState(
            firstVisibleItemIndex = firstVisibleItemIndex,
            firstVisibleItemScrollOffset = firstVisibleItemScrollOffset,
            scale = scale,
            secondsPerTick = 1,
            itemWidth = 0f,
            startOffset = 0f,
            endOffset = 0f,
        )
    },
)

internal class ClipSelectorState(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
    scale: Float,
    secondsPerTick: Int,
    itemWidth: Float,
    startOffset: Float,
    endOffset: Float,
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
    var secondsPerTick by mutableIntStateOf(secondsPerTick)

    val tickWidthDp = TickWidth
    var spaceWidthDp by mutableStateOf(SpaceWidth * scale)

    /**
     * Represents the width at which the distance between ticks in a larger resolution
     * matches the distance between five ticks in a smaller resolution.
     *
     * For example, if each tick represents 5 seconds, when the user zooms in,
     * we want to switch to 1 second per tick at a certain point. This switch point
     * occurs when the distances between the ticks match.
     *
     * Illustration:
     * |                   | - 5 seconds per tick
     * | - | - | - | - | - | - 1 second per tick
     *
     * At this switch point, the tick resolution will change depending on
     * whether the user is zooming in or zooming out.
     */
    private val scaleTippingPoint = (TickWidth + SpaceWidth) * 5

    /**
     * Compute maxium tick resolution in seconds that would allow to diplay episode duration
     * on a timeline at least once.
     */
    fun calculateMaxSecondsPerTick(episodeDuration: Duration, timelineWidth: Dp): Int {
        val secondsCount = episodeDuration.inWholeSeconds.toInt()
        var resolution = 1
        val minItemWidth = itemWidth(scale = 1f)
        // Assume max seconds per tick at 125 seconds, which fits 125 minutes in 60 ticks.
        while (resolution < 125) {
            val newResolution = resolution * 5
            val tickCount = (secondsCount / newResolution) + 1
            val totalWidth = minItemWidth * tickCount
            if (totalWidth <= timelineWidth) {
                break
            } else {
                resolution = newResolution
            }
        }
        return resolution.coerceAtLeast(1)
    }

    fun updateTimelineScale(
        zoom: Float,
        maxSecondsPerTick: Int,
        onTimelineScaleUpdate: (Float, Int) -> Unit,
    ) {
        val newScale = scale * zoom
        val expectedSpace = SpaceWidth * newScale
        when {
            newScale > 1f && expectedSpace >= scaleTippingPoint -> if (secondsPerTick != 1) {
                secondsPerTick /= 5
                scale = 1f
                onTimelineScaleUpdate(scale, secondsPerTick)
            }
            newScale < 1f && expectedSpace <= scaleTippingPoint -> if (secondsPerTick != maxSecondsPerTick) {
                secondsPerTick *= 5
                scale = scaleTippingPoint / expectedSpace
                onTimelineScaleUpdate(scale, secondsPerTick)
            }
            else -> {
                scale = newScale
                onTimelineScaleUpdate(scale, secondsPerTick)
            }
        }
    }

    fun refreshItemWidth(density: Density) {
        spaceWidthDp = SpaceWidth * scale
        itemWidth = with(density) { (tickWidthDp + spaceWidthDp).toPx() }
    }

    fun scaleBoxOffsets(clipRange: Clip.Range) {
        startOffset = durationToPixels(clipRange.start)
        endOffset = durationToPixels(clipRange.end)
    }

    fun durationToPixels(duration: Duration, accuracy: DurationUnit = DurationUnit.SECONDS): Float {
        val ticks = duration.inWholeUnitsAsSeconds(accuracy) / secondsPerTick
        return itemWidth * ticks
    }

    fun pixelsToDuration(pixels: Float): Duration {
        val ticks = pixels / itemWidth
        val seconds = ticks * secondsPerTick
        return seconds.toDouble().seconds
    }

    private fun Duration.inWholeUnitsAsSeconds(accuracy: DurationUnit) = when (accuracy) {
        DurationUnit.NANOSECONDS -> inWholeNanoseconds.toFloat() / 1_000_000_000
        DurationUnit.MICROSECONDS -> inWholeMicroseconds.toFloat() / 1_000_000
        DurationUnit.MILLISECONDS -> inWholeMilliseconds.toFloat() / 1_000
        DurationUnit.SECONDS -> inWholeSeconds.toFloat()
        DurationUnit.MINUTES -> inWholeMinutes.toFloat() * 60
        DurationUnit.HOURS -> inWholeHours.toFloat() * 3600
        DurationUnit.DAYS -> inWholeDays.toFloat() * 86_400
    }

    companion object {
        val TickWidth = 1.5.dp
        val SpaceWidth = 4.dp

        fun itemWidth(scale: Float) = TickWidth + SpaceWidth * scale

        val Saver: Saver<ClipSelectorState, Any> = listSaver(
            save = {
                listOf(
                    it.listState.firstVisibleItemIndex,
                    it.listState.firstVisibleItemScrollOffset,
                    it.scale,
                    it.secondsPerTick,
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
                    secondsPerTick = it[3] as Int,
                    itemWidth = it[4] as Float,
                    startOffset = it[5] as Float,
                    endOffset = it[6] as Float,
                )
            },
        )
    }
}

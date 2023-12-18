package au.com.shiftyjelly.pocketcasts.wear.extensions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import com.google.android.horologist.annotations.ExperimentalHorologistApi
import com.google.android.horologist.compose.layout.ScalingLazyColumnDefaults
import com.google.android.horologist.compose.layout.ScalingLazyColumnState
import kotlin.math.sqrt
import androidx.wear.compose.foundation.lazy.ScalingLazyColumnDefaults as FoundationScalingLazyColumnDefaults

/**
 * This is being manually copied from horologist's 0.4.17 release (https://github.com/google/horologist/releases/tag/v0.4.17).
 * We should remove this once we update to that version.
 *
 */
@ExperimentalHorologistApi
fun ScalingLazyColumnDefaults.responsive(
    firstItemIsFullWidth: Boolean,
    verticalArrangement: Arrangement.Vertical =
        Arrangement.spacedBy(
            space = 4.dp,
            alignment = Alignment.Top
        ),
    horizontalPaddingPercent: Float = 0.052f
): ScalingLazyColumnState.Factory {
    fun calculateVerticalOffsetForChip(
        viewportDiameter: Float,
        horizontalPaddingPercent: Float
    ): Dp {
        val childViewHeight: Float = 52.dp.value
        val childViewWidth: Float = viewportDiameter * (1.0f - (2f * horizontalPaddingPercent))
        val radius = viewportDiameter / 2f
        return (
            radius -
                sqrt(
                    (radius - childViewHeight + childViewWidth * 0.5f) * (radius - childViewWidth * 0.5f)
                ) -
                childViewHeight * 0.5f
            ).dp
    }

    return object : ScalingLazyColumnState.Factory {
        @Composable
        override fun create(): ScalingLazyColumnState {
            val density = LocalDensity.current
            val configuration = LocalConfiguration.current
            val screenWidthDp = configuration.screenWidthDp.toFloat()
            val screenHeightDp = configuration.screenHeightDp.toFloat()

            return remember {
                val padding = screenWidthDp * horizontalPaddingPercent
                val topPaddingDp: Dp = if (firstItemIsFullWidth && configuration.isScreenRound) {
                    calculateVerticalOffsetForChip(screenWidthDp, horizontalPaddingPercent)
                } else {
                    32.dp
                }
                val bottomPaddingDp: Dp = if (configuration.isScreenRound) {
                    calculateVerticalOffsetForChip(screenWidthDp, horizontalPaddingPercent)
                } else {
                    0.dp
                }
                val contentPadding = PaddingValues(
                    start = padding.dp,
                    end = padding.dp,
                    top = topPaddingDp,
                    bottom = bottomPaddingDp
                )

                val sizeRatio = ((screenWidthDp - 192) / (233 - 192).toFloat()).coerceIn(0f, 1.5f)
                val presetRatio = 0f

                val minElementHeight = lerp(0.2f, 0.157f, sizeRatio)
                val maxElementHeight = lerp(0.6f, 0.472f, sizeRatio).coerceAtLeast(minElementHeight)
                val minTransitionArea = lerp(0.35f, lerp(0.35f, 0.393f, presetRatio), sizeRatio)
                val maxTransitionArea = lerp(0.55f, lerp(0.55f, 0.593f, presetRatio), sizeRatio)

                val scalingParams = FoundationScalingLazyColumnDefaults.scalingParams(
                    minElementHeight = minElementHeight,
                    maxElementHeight = maxElementHeight,
                    minTransitionArea = minTransitionArea,
                    maxTransitionArea = maxTransitionArea
                )

                val screenHeightPx =
                    with(density) { screenHeightDp.dp.roundToPx() }
                val topPaddingPx = with(density) { topPaddingDp.roundToPx() }
                val topScreenOffsetPx = screenHeightPx / 2 - topPaddingPx

                val initialScrollPosition = ScalingLazyColumnState.ScrollPosition(
                    index = 0,
                    offsetPx = topScreenOffsetPx
                )
                ScalingLazyColumnState(
                    initialScrollPosition = initialScrollPosition,
                    autoCentering = null,
                    anchorType = ScalingLazyListAnchorType.ItemStart,
                    rotaryMode = ScalingLazyColumnState.RotaryMode.Scroll,
                    verticalArrangement = verticalArrangement,
                    horizontalAlignment = Alignment.CenterHorizontally,
                    contentPadding = contentPadding,
                    scalingParams = scalingParams
                )
            }
        }
    }
}

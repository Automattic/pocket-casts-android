package au.com.shiftyjelly.pocketcasts.wear.ui.component.horologist

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameMillis
import au.com.shiftyjelly.pocketcasts.wear.ui.component.horologist.PlaybackProgressAnimation.PLAYBACK_PROGRESS_ANIMATION_SPEC
import com.google.android.horologist.media.model.TimestampProvider
import com.google.android.horologist.media.ui.state.LocalTimestampProvider
import com.google.android.horologist.media.ui.state.model.TrackPositionUiModel
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs

/**
 * State holder for the media progress indicator that supports both ongoing predictive progress and
 * animating progress.
 */
class ProgressStateHolderStyled(
    initial: Float,
    private val timestampProvider: TimestampProvider
) {
    private val actual = mutableStateOf(initial)
    private val animatable = Animatable(0f)
    val state = derivedStateOf { actual.value + animatable.value - animatable.targetValue }

    suspend fun setProgress(percent: Float, canAnimate: Boolean) = coroutineScope {
        val offset = percent - actual.value
        actual.value = percent
        if (!canAnimate || animatable.isRunning || abs(offset) < ANIMATION_THRESHOLD) {
            return@coroutineScope
        }
        launch(NonCancellable) {
            animatable.animateTo(offset, PLAYBACK_PROGRESS_ANIMATION_SPEC)
            animatable.snapTo(0f)
        }
    }

    suspend fun predictProgress(predictor: (Long) -> Float) = coroutineScope {
        val timestamp = timestampProvider.getTimestamp()
        val initialFrameTime = withFrameMillis { it }
        do {
            withFrameMillis {
                val frameTimeOffset = it - initialFrameTime
                actual.value = predictor(timestamp + frameTimeOffset)
            }
        } while (isActive)
    }

    companion object {
        // Never animate progress under this threshold
        private const val ANIMATION_THRESHOLD = 0.01f

        @Composable
        fun fromTrackPositionUiModel(trackPositionUiModel: TrackPositionUiModel): State<Float> {
            val timestampProvider = LocalTimestampProvider.current
            val percent = trackPositionUiModel.getCurrentPercent(timestampProvider.getTimestamp())
            val stateHolder = remember { ProgressStateHolderStyled(percent, timestampProvider) }
            LaunchedEffect(trackPositionUiModel) {
                stateHolder.setProgress(percent, trackPositionUiModel.shouldAnimate)
                if (trackPositionUiModel is TrackPositionUiModel.Predictive) {
                    stateHolder.predictProgress(trackPositionUiModel.predictor::predictPercent)
                }
            }
            return stateHolder.state
        }

        private fun TrackPositionUiModel.getCurrentPercent(timestamp: Long) = when (this) {
            is TrackPositionUiModel.Actual -> percent
            is TrackPositionUiModel.Predictive -> predictor.predictPercent(timestamp)
            else -> 0f
        }
    }
}

object PlaybackProgressAnimation {
    val PLAYBACK_PROGRESS_ANIMATION_SPEC =
        SpringSpec(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = 100f,
            // The default threshold is 0.01, or 1% of the overall progress range, which is quite
            // large and noticeable.
            visibilityThreshold = 1 / 1000f
        )
}

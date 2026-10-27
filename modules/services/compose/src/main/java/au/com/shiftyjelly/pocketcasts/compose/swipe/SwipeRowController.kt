package au.com.shiftyjelly.pocketcasts.compose.swipe

import android.widget.OverScroller
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal enum class SwipeDirection {
    LeftToRight,
    RightToLeft,
    None,
}

internal fun swipeButtonsCoefficient(count: Int) = when (count) {
    3 -> 2f / 3
    2 -> 1f
    else -> 2f
}

@Stable
internal class SwipeRowController(
    private val scope: CoroutineScope,
    private val scroller: OverScroller,
    private val buttonWidthPx: Float,
    private val swipedThresholdMarginPx: Float,
    private val minSwipedThresholdPx: Float,
    private val minFlingVelocity: Float,
    private val performHapticFeedback: () -> Unit,
    private val onSwipeAction: (SwipeRowAction) -> Unit,
) {
    private var rowWidth = 0

    var leadingActions = emptyList<SwipeRowAction>()
        private set

    var trailingActions = emptyList<SwipeRowAction>()
        private set

    var translation by mutableFloatStateOf(0f)
        private set

    var buttonsTranslation by mutableFloatStateOf(0f)
        private set

    var isLocked by mutableStateOf(false)
        private set

    var fadingSide by mutableStateOf<SwipeDirection?>(null)
        private set

    var fadeAlpha by mutableFloatStateOf(1f)
        private set

    val leadingIconOffset = Animatable(0f)

    val trailingIconOffset = Animatable(0f)

    private val iconTargets = floatArrayOf(Float.NaN, Float.NaN)

    private var isSwipeEnabled = true

    private var isSwipedThresholdReached = false

    private var isFlinging = false

    private var isGestureConsumed = false

    private var targetPosition = SwipeDirection.None

    private var swipeDirection = SwipeDirection.None

    private val dragEventDirections = ArrayDeque<Float>()

    private var scrollJob: Job? = null

    private var springJob: Job? = null

    private var swipedJob: Job? = null

    val isOpen get() = translation != 0f

    fun updateActions(leading: List<SwipeRowAction>, trailing: List<SwipeRowAction>) {
        if (!isLocked) {
            leadingActions = leading
            trailingActions = trailing
        }
    }

    fun updateSwipeEnabled(isEnabled: Boolean) {
        if (isSwipeEnabled == isEnabled) {
            return
        }
        isSwipeEnabled = isEnabled
        if (isEnabled) {
            updateLocked(false)
        } else {
            clearTranslation()
            updateLocked(true)
        }
    }

    fun onLayout(width: Int) {
        val isFirstLayout = rowWidth == 0
        rowWidth = width
        if (isFirstLayout) {
            setTranslation(0f, useHapticFeedback = false)
        }
    }

    fun onButtonClick(action: SwipeRowAction) {
        if (!isLocked) {
            updateLocked(true)
            springJob = scope.launch {
                animate(translation, 0f, animationSpec = spring(stiffness = 10_000f, visibilityThreshold = 0.75f)) { value, _ ->
                    translation = value
                    resolveButtonTranslations()
                }
                updateLocked(false)
            }
        }
        onSwipeAction(action)
    }

    fun onDragStarted() {
        isGestureConsumed = false
        if (isLocked) {
            return
        }
        isFlinging = false
        scroller.abortAnimation()
        dragEventDirections.clear()
    }

    fun onDrag(delta: Float) {
        if (isLocked || isGestureConsumed) {
            return
        }
        updateSwipeDirection(delta)
        if (dragEventDirections.size >= 10) {
            dragEventDirections.removeFirst()
        }
        dragEventDirections.addLast(translation.sign)
        setTranslation(translation + delta)
    }

    fun onDragStopped(velocity: Float) {
        if (isLocked || isGestureConsumed) {
            return
        }
        if (velocity.absoluteValue > minFlingVelocity) {
            fling(velocity)
        }
        if (scroller.isFinished) {
            settleSwipePosition()
        }
    }

    fun settle() {
        smoothScrollTo(0)
    }

    fun clearTranslation() {
        isGestureConsumed = false
        clearGesture()
        swipedJob?.cancel()
        swipedJob = null
        springJob?.cancel()
        iconTargets.fill(Float.NaN)
        isSwipedThresholdReached = false
        updateLocked(false)
        setTranslation(0f, useHapticFeedback = false)
    }

    private fun updateLocked(value: Boolean) {
        isLocked = value
        if (value) {
            clearGesture()
        }
    }

    private fun clearGesture() {
        scroller.forceFinished(true)
        scrollJob?.cancel()
        dragEventDirections.clear()
        isFlinging = false
        targetPosition = SwipeDirection.None
        swipeDirection = SwipeDirection.None
    }

    private fun setTranslation(value: Float, useHapticFeedback: Boolean = true) {
        if (isLocked || rowWidth == 0) {
            return
        }
        val min = if (trailingActions.isNotEmpty()) -rowWidth.toFloat() else 0f
        val max = if (leadingActions.isNotEmpty()) rowWidth.toFloat() else 0f
        translation = value.coerceIn(min, max)
        targetPosition = when {
            translation > 0f -> SwipeDirection.LeftToRight
            translation < 0f -> SwipeDirection.RightToLeft
            else -> SwipeDirection.None
        }
        resolveSwipedThreshold(useHapticFeedback)
        resolveButtonTranslations()
        resolveSwipedState()
    }

    private fun resolveButtonTranslations() {
        buttonsTranslation = translation
        resolveIconOffset(index = 0, leadingIconOffset, leadingActions.size)
        resolveIconOffset(index = 1, trailingIconOffset, trailingActions.size)
    }

    private fun resolveIconOffset(index: Int, offset: Animatable<Float, *>, count: Int) {
        val target = if (isSwipedThresholdReached) {
            translation - translation * swipeButtonsCoefficient(count) / 2
        } else {
            0f
        }
        if (iconTargets[index] != target) {
            iconTargets[index] = target
            scope.launch { offset.animateTo(target, spring(stiffness = 1_500f, visibilityThreshold = 0.75f)) }
        }
    }

    private fun resolveSwipedThreshold(useHapticFeedback: Boolean) {
        val isReached = translation.absoluteValue >= swipedThreshold(settledButtonsWidth())
        if (isSwipedThresholdReached != isReached) {
            isSwipedThresholdReached = isReached
            if (useHapticFeedback) {
                performHapticFeedback()
            }
        }
    }

    private fun resolveSwipedState() {
        if (isLocked || translation.absoluteValue < rowWidth) {
            return
        }
        updateLocked(true)
        isGestureConsumed = true
        val isLeading = translation > 0
        val primaryAction = (if (isLeading) leadingActions else trailingActions).firstOrNull()
        primaryAction?.let(onSwipeAction)
        swipedJob?.cancel()
        swipedJob = scope.launch {
            delay(100)
            try {
                fadingSide = if (isLeading) SwipeDirection.LeftToRight else SwipeDirection.RightToLeft
                translation = 0f
                animate(1f, 0f, animationSpec = spring(stiffness = 350f, visibilityThreshold = 0.75f / 256)) { value, _ ->
                    fadeAlpha = value
                }
                fadingSide = null
                fadeAlpha = 1f
                updateLocked(false)
                setTranslation(0f, useHapticFeedback = false)
            } finally {
                fadingSide = null
                fadeAlpha = 1f
            }
        }
    }

    private fun settleSwipePosition() {
        val buttonsWidth = settledButtonsWidth()
        val currentTranslation = translation.absoluteValue
        val finalPosition = when {
            currentTranslation < settledThreshold(buttonsWidth) -> 0f
            currentTranslation < swipedThreshold(buttonsWidth) -> buttonsWidth
            else -> rowWidth.toFloat()
        }
        val coefficient = when (targetPosition) {
            SwipeDirection.LeftToRight -> 1f
            SwipeDirection.RightToLeft -> -1f
            SwipeDirection.None -> 0f
        }
        smoothScrollTo((finalPosition * coefficient).roundToInt())
    }

    private fun settledThreshold(buttonsWidth: Float) = when (targetPosition) {
        SwipeDirection.LeftToRight -> if (swipeDirection == SwipeDirection.RightToLeft) buttonsWidth * 2 / 3 else buttonsWidth / 3
        SwipeDirection.RightToLeft -> if (swipeDirection == SwipeDirection.LeftToRight) buttonsWidth * 2 / 3 else buttonsWidth / 3
        SwipeDirection.None -> 0f
    }

    private fun swipedThreshold(buttonsWidth: Float) = (buttonsWidth + swipedThresholdMarginPx).coerceAtLeast(minSwipedThresholdPx)

    private fun settledButtonsWidth() = buttonWidthPx * when (targetPosition) {
        SwipeDirection.LeftToRight -> leadingActions.size
        SwipeDirection.RightToLeft -> trailingActions.size
        SwipeDirection.None -> 0
    }

    private fun fling(velocity: Float) {
        updateSwipeDirection(velocity)
        val swipedThreshold = swipedThreshold(settledButtonsWidth())
        val (minX, maxX) = when (swipeDirection) {
            SwipeDirection.LeftToRight -> -rowWidth to if (translation >= swipedThreshold) rowWidth else swipedThreshold.roundToInt() - 1
            SwipeDirection.RightToLeft -> (if (translation.absoluteValue >= swipedThreshold) -rowWidth else -swipedThreshold.roundToInt() + 1) to rowWidth
            SwipeDirection.None -> -rowWidth to rowWidth
        }
        scroller.fling(translation.toInt(), 0, velocity.roundToInt(), 0, minX, maxX, 0, 0)
        startScrollLoop()
    }

    private fun smoothScrollTo(target: Int) {
        scroller.abortAnimation()
        val start = translation.roundToInt()
        val dx = target - start
        if (dx == 0) {
            return
        }
        scroller.startScroll(start, 0, dx, 0)
        startScrollLoop()
    }

    private fun startScrollLoop() {
        if (scrollJob?.isActive == true) {
            return
        }
        scrollJob = scope.launch {
            do {
                withFrameNanos { }
            } while (computeScroll())
        }
    }

    private fun computeScroll(): Boolean {
        if (isLocked) {
            return false
        }
        if (scroller.computeScrollOffset()) {
            isFlinging = true
            val targetDirection = scroller.finalX.toFloat().sign
            if (targetDirection == 0f || dragEventDirections.all { it == targetDirection || it == 0f }) {
                setTranslation(scroller.currX.toFloat())
            } else {
                scroller.abortAnimation()
                smoothScrollTo(0)
            }
            return true
        } else if (isFlinging) {
            isFlinging = false
            settleSwipePosition()
            return !scroller.isFinished
        }
        return false
    }

    private fun updateSwipeDirection(distanceOrVelocity: Float) {
        swipeDirection = when (distanceOrVelocity.sign) {
            -1f -> SwipeDirection.RightToLeft
            1f -> SwipeDirection.LeftToRight
            else -> swipeDirection
        }
    }
}

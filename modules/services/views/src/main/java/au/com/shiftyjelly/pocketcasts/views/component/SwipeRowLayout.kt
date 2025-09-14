package au.com.shiftyjelly.pocketcasts.views.component

import android.annotation.SuppressLint
import android.content.Context
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.HapticFeedbackConstants
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.OverScroller
import androidx.core.view.doOnLayout
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringForce
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.views.component.SwipeDirection.LeftToRight
import au.com.shiftyjelly.pocketcasts.views.component.SwipeDirection.None
import au.com.shiftyjelly.pocketcasts.views.component.SwipeDirection.RightToLeft
import au.com.shiftyjelly.pocketcasts.views.databinding.SwipeRowLayoutBinding
import au.com.shiftyjelly.pocketcasts.views.extensions.cancelSpring
import au.com.shiftyjelly.pocketcasts.views.extensions.doOnEnd
import au.com.shiftyjelly.pocketcasts.views.extensions.doOnUpdate
import au.com.shiftyjelly.pocketcasts.views.extensions.spring
import au.com.shiftyjelly.pocketcasts.views.helper.DelayedAction
import au.com.shiftyjelly.pocketcasts.views.helper.runDelayedAction
import kotlin.math.abs
import kotlin.math.absoluteValue
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlin.time.Duration.Companion.milliseconds

class SwipeRowLayout<T : SwipeButton.UiState> @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
) : FrameLayout(context, attrs) {
    private val binding = SwipeRowLayoutBinding.inflate(LayoutInflater.from(context), this)

    private val swipeButtons = SwipeButtons<T>(binding)

    private val swipeableView by lazy(mode = LazyThreadSafetyMode.NONE) {
        val swipeButtonsCount = swipeButtons.count()
        check(childCount == swipeButtonsCount + 1) { "SwipeRowLayout must have exactly one direct child" }
        getChildAt(swipeButtonsCount)
    }

    private val swipeHandler by lazy(mode = LazyThreadSafetyMode.NONE) {
        SwipeGestureHandler(
            parent = this,
            child = swipeableView,
            setTranslation = ::setTranslation,
            settleSwipePosition = ::settleSwipePosition,
        )
    }

    private var isLocked = false
        set(value) {
            field = value
            if (value) {
                swipeHandler.clear()
            }
            swipeButtons.forEach { button ->
                button.isEnabled = !value
                button.isLocked = value
            }
        }

    private var isSwipedThresholdReached = false

    private val swipedThresholdMargin = 64.dpToPx(context).toFloat()

    private val minSwipedThreshold = 240.dpToPx(context).toFloat()

    private var swipedAction: DelayedAction? = null

    init {
        overScrollMode = OVER_SCROLL_NEVER
        val scrollToZeroListener: (T) -> Unit = {
            if (!isLocked) {
                isLocked = true
                swipeableView
                    .spring(DynamicAnimation.TRANSLATION_X, stiffness = SpringForce.STIFFNESS_HIGH)
                    .doOnUpdate { _, _, _ -> resolveButtonTranslations() }
                    .doOnEnd { _, isCancelled, _, _ ->
                        if (!isCancelled) {
                            isLocked = false
                        }
                    }
                    .animateToFinalPosition(0f)
            }
        }
        swipeButtons.forEach { button ->
            button.addOnSwipeActionListener(scrollToZeroListener)
        }
        doOnLayout { setTranslation(0f, useHapticFeedback = false) }
    }

    fun setLtr1State(state: T?) {
        swipeButtons.ltr1.setButtonState(state)
    }

    fun setLtr2State(state: T?) {
        swipeButtons.ltr2.setButtonState(state)
    }

    fun setLtr3State(state: T?) {
        swipeButtons.ltr3.setButtonState(state)
    }

    fun setRtl1State(state: T?) {
        swipeButtons.rtl1.setButtonState(state)
    }

    fun setRtl2State(state: T?) {
        swipeButtons.rtl2.setButtonState(state)
    }

    fun setRtl3State(state: T?) {
        swipeButtons.rtl3.setButtonState(state)
    }

    private fun SwipeButton<T>.setButtonState(state: T?) {
        setUiState(state)
        isVisible = isVisible && uiState != null
    }

    fun addOnSwipeActionListener(listener: (T) -> Unit) {
        swipeButtons.forEach { it.addOnSwipeActionListener(listener) }
    }

    fun removeOnSwipeActionListener(listener: (T) -> Unit) {
        swipeButtons.forEach { it.removeOnSwipeActionListener(listener) }
    }

    fun clearTranslation() {
        swipeHandler.clear()
        swipedAction?.cancel()
        swipedAction = null
        swipeButtons.ltr1.cancelSpring(DynamicAnimation.ALPHA)
        swipeButtons.rtl1.cancelSpring(DynamicAnimation.ALPHA)
        swipeableView.cancelSpring(DynamicAnimation.TRANSLATION_X)
        isSwipedThresholdReached = false
        isLocked = false
        setTranslation(0f, useHapticFeedback = false)
    }

    fun lock() {
        isLocked = true
    }

    fun unlock() {
        isLocked = false
    }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (isLocked) {
            return false
        }
        return swipeHandler.handleInterceptedTouchEvent(ev)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (isLocked) {
            return false
        }
        return swipeHandler.handleTouchEvent(event) || super.onTouchEvent(event)
    }

    override fun computeScroll() {
        if (isLocked) {
            return
        }
        swipeHandler.handleScrollComputation()
    }

    private fun setTranslation(value: Float, useHapticFeedback: Boolean = true) {
        if (isLocked || width == 0) {
            return
        }
        swipeableView.translationX = value.coerceIn(minTranslationValue(), maxTranslationValue())
        swipeHandler.refreshTargetPosition()
        resolveButtonTranslations()
        resolveSwipedThreshold(useHapticFeedback)
        resolveSwipedState()
    }

    private fun resolveButtonTranslations() {
        val baseTranslation = width.toFloat()
        val translation = swipeableView.translationX
        val rtlCoefficient = if (swipeButtons.rtl3Active == null) 0.5f else (2f / 3)
        val ltrCoefficient = if (swipeButtons.ltr3Active == null) 0.5f else (2f / 3)

        if (swipeButtons.rtl1Active == null) {
            swipeButtons.rtl1.translationX = baseTranslation
        } else {
            swipeButtons.rtl1Active?.translationX = baseTranslation + translation
        }
        if (swipeButtons.rtl2Active == null) {
            swipeButtons.rtl2.translationX = baseTranslation
        } else {
            swipeButtons.rtl2Active?.translationX = baseTranslation + translation * rtlCoefficient
        }
        if (swipeButtons.rtl3Active == null) {
            swipeButtons.rtl3.translationX = baseTranslation
        } else {
            swipeButtons.rtl3Active?.translationX = baseTranslation + translation * rtlCoefficient / 2
        }

        if (swipeButtons.ltr1Active == null) {
            swipeButtons.ltr1.translationX = -baseTranslation
        } else {
            swipeButtons.ltr1Active?.translationX = -baseTranslation + translation
        }
        if (swipeButtons.ltr2Active == null) {
            swipeButtons.ltr2.translationX = -baseTranslation
        } else {
            swipeButtons.ltr2Active?.translationX = -baseTranslation + translation * ltrCoefficient
        }
        if (swipeButtons.ltr3Active == null) {
            swipeButtons.ltr3.translationX = -baseTranslation
        } else {
            swipeButtons.ltr3Active?.translationX = -baseTranslation + translation * ltrCoefficient / 2
        }
    }

    private fun resolveSwipedThreshold(useHapticFeedback: Boolean) {
        val translation = swipeableView.translationX.absoluteValue
        val isSwipedThresholdReached = translation >= swipedThreshold(settledButtonsWidth())

        swipeButtons.rtl2Active?.isGone = isSwipedThresholdReached
        swipeButtons.rtl3Active?.isGone = isSwipedThresholdReached
        swipeButtons.ltr2Active?.isGone = isSwipedThresholdReached
        swipeButtons.ltr3Active?.isGone = isSwipedThresholdReached

        if (this.isSwipedThresholdReached != isSwipedThresholdReached) {
            this.isSwipedThresholdReached = isSwipedThresholdReached
            if (useHapticFeedback) {
                swipeableView.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK)
            }
        }
    }

    private fun resolveSwipedState() {
        val translation = swipeableView.translationX
        if (!isLocked && translation.absoluteValue >= width) {
            isLocked = true

            val button = when {
                translation > 0 -> swipeButtons.ltr1
                translation < 0 -> swipeButtons.rtl1
                else -> null
            }

            if (button != null) {
                button.callOnSwipeActionListeners()
                swipedAction?.cancel()
                swipedAction = runDelayedAction(300.milliseconds) {
                    button.elevation = 1f
                    swipeableView.translationX = 0f

                    button
                        .spring(DynamicAnimation.ALPHA)
                        .doOnEnd { _, isCancelled, _, _ ->
                            button.elevation = 0f
                            button.alpha = 1f
                            if (!isCancelled) {
                                isLocked = false
                                setTranslation(0f, useHapticFeedback = false)
                            }
                        }
                        .animateToFinalPosition(0f)
                }
            } else {
                isLocked = false
            }
        }
    }

    private fun settleSwipePosition() {
        val buttonsWidth = settledButtonsWidth()
        val currentTranslation = swipeableView.translationX.absoluteValue
        val finalPosition = when {
            currentTranslation < settledThreshold(buttonsWidth) -> 0f
            currentTranslation < swipedThreshold(buttonsWidth) -> buttonsWidth
            else -> width.toFloat()
        }
        val coefficient = when (swipeHandler.targetPosition) {
            LeftToRight -> 1f
            RightToLeft -> -1f
            None -> 0f
        }
        swipeHandler.smoothScrollTo((finalPosition * coefficient).roundToInt())
    }

    private fun settledThreshold(buttonsWidth: Float): Float {
        return when (swipeHandler.targetPosition) {
            LeftToRight -> when (swipeHandler.swipeDirection) {
                LeftToRight, None -> buttonsWidth / 3
                RightToLeft -> buttonsWidth * 2 / 3
            }

            RightToLeft -> when (swipeHandler.swipeDirection) {
                LeftToRight -> buttonsWidth * 2 / 3
                RightToLeft, None -> buttonsWidth / 3
            }

            None -> 0f
        }
    }

    private fun swipedThreshold(buttonsWidth: Float): Float {
        return (buttonsWidth + swipedThresholdMargin).coerceAtLeast(minSwipedThreshold)
    }

    private fun settledButtonsWidth(): Float {
        val buttons = when (swipeHandler.targetPosition) {
            LeftToRight -> swipeButtons.activeLtrs
            RightToLeft -> swipeButtons.activeRtls
            None -> emptyList()
        }
        return buttons.sumOf(SwipeButton<T>::settledItemWidth).toFloat()
    }

    private fun minTranslationValue(): Float {
        return if (swipeButtons.activeRtls.isNotEmpty()) {
            -width.toFloat()
        } else {
            0f
        }
    }

    private fun maxTranslationValue(): Float {
        return if (swipeButtons.activeLtrs.isNotEmpty()) {
            width.toFloat()
        } else {
            0f
        }
    }
}

private enum class SwipeDirection {
    LeftToRight,
    RightToLeft,
    None,
}

private class SwipeButtons<T : SwipeButton.UiState>(
    private val binding: SwipeRowLayoutBinding,
) : Iterable<SwipeButton<T>> {
    val ltr1 get() = binding.leftToRightButton1.typed()
    val ltr1Active get() = ltr1.active()

    val ltr2 get() = binding.leftToRightButton2.typed()
    val ltr2Active get() = ltr2.active()

    val ltr3 get() = binding.leftToRightButton3.typed()
    val ltr3Active get() = ltr3.active()

    val rtl1 get() = binding.rightToLeftButton1.typed()
    val rtl1Active get() = rtl1.active()

    val rtl2 get() = binding.rightToLeftButton2.typed()
    val rtl2Active get() = rtl2.active()

    val rtl3 get() = binding.rightToLeftButton3.typed()
    val rtl3Active get() = rtl3.active()

    val activeLtrs get() = listOfNotNull(ltr1Active, ltr2Active, ltr3Active)
    val activeRtls get() = listOfNotNull(rtl1Active, rtl2Active, rtl3Active)

    override fun iterator(): Iterator<SwipeButton<T>> {
        return iterator {
            yield(ltr1)
            yield(ltr2)
            yield(ltr3)
            yield(rtl1)
            yield(rtl2)
            yield(rtl3)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun SwipeButton<*>.typed() = this as SwipeButton<T>

    private fun SwipeButton<T>.active() = takeIf { it.uiState != null }
}

private class SwipeGestureHandler(
    private val parent: ViewGroup,
    private val child: View,
    private val setTranslation: (Float) -> Unit,
    private val settleSwipePosition: () -> Unit,
) {
    private val touchSlop = ViewConfiguration.get(child.context).scaledTouchSlop

    private var isDragging = false

    private var isFlinging = false

    private var shouldDispatchDownEvent = true

    private var downX = 0f

    private var downY = 0f

    private val dragEventDirections = ArrayDeque<Float>()

    var targetPosition = None
        private set

    var swipeDirection = None
        private set

    private val scroller = OverScroller(child.context).apply {
        setFriction(ViewConfiguration.getScrollFriction() * 5)
    }

    private val gestureDetector = GestureDetector(
        child.context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean {
                scroller.abortAnimation()
                dragEventDirections.clear()
                return false
            }

            override fun onScroll(e1: MotionEvent?, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
                updateSwipeDirection(-distanceX)
                if (dragEventDirections.size >= 10) {
                    dragEventDirections.removeFirst()
                }
                val currentTranslation = child.translationX
                dragEventDirections.addLast(currentTranslation.sign)
                setTranslation(currentTranslation - distanceX)
                return true
            }

            override fun onFling(e1: MotionEvent?, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
                updateSwipeDirection(velocityX)
                scroller.fling(
                    child.translationX.toInt(),
                    0,
                    velocityX.roundToInt(),
                    0,
                    -child.width,
                    child.width,
                    0,
                    0,
                )
                parent.postInvalidateOnAnimation()
                return true
            }
        },
    )

    fun clear() {
        scroller.forceFinished(true)
        dragEventDirections.clear()
        isDragging = false
        isFlinging = false
        shouldDispatchDownEvent = true
        downX = 0f
        downY = 0f
        targetPosition = None
        swipeDirection = None
    }

    fun handleInterceptedTouchEvent(event: MotionEvent): Boolean {
        return when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                parent.requestDisallowInterceptTouchEvent(false)
                isDragging = false
                isFlinging = false
                shouldDispatchDownEvent = true
                downX = event.x
                downY = event.y
                false
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - downX
                val dy = event.y - downY
                if (!isDragging && abs(dx) > touchSlop && abs(dx) > abs(dy)) {
                    parent.requestDisallowInterceptTouchEvent(true)
                    isDragging = true
                    true
                } else {
                    false
                }
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                parent.requestDisallowInterceptTouchEvent(false)
                isDragging = false
                false
            }

            else -> false
        }
    }

    fun handleTouchEvent(event: MotionEvent): Boolean {
        val shouldHandle = isDragging || !child.hasOnClickListeners() || !scroller.isFinished
        return if (shouldHandle) {
            if (shouldDispatchDownEvent) {
                val down = MotionEvent.obtain(
                    event.downTime,
                    event.eventTime,
                    MotionEvent.ACTION_DOWN,
                    event.x,
                    event.y,
                    event.metaState,
                )
                gestureDetector.onTouchEvent(down)
                down.recycle()
                shouldDispatchDownEvent = false
            }
            gestureDetector.onTouchEvent(event)

            if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
                if (scroller.isFinished) {
                    settleSwipePosition()
                }
                isDragging = false
                shouldDispatchDownEvent = true
            }
            true
        } else {
            false
        }
    }

    fun handleScrollComputation() {
        if (scroller.computeScrollOffset()) {
            isFlinging = true
            val targetDirection = scroller.finalX.toFloat().sign
            if (targetDirection == 0f || dragEventDirections.all { it == targetDirection || it == 0f }) {
                setTranslation(scroller.currX.toFloat())
                parent.postInvalidateOnAnimation()
            } else {
                scroller.abortAnimation()
                smoothScrollTo(0)
            }
        } else if (isFlinging) {
            isFlinging = false
            settleSwipePosition()
        }
    }

    fun smoothScrollTo(target: Int) {
        scroller.abortAnimation()
        val start = child.translationX.roundToInt()
        val dx = target - start
        if (dx == 0) {
            return
        }
        scroller.startScroll(start, 0, dx, 0)
        parent.postInvalidateOnAnimation()
    }

    fun refreshTargetPosition() {
        targetPosition = when {
            child.translationX > 0f -> LeftToRight
            child.translationX < 0f -> RightToLeft
            else -> None
        }
    }

    private fun updateSwipeDirection(distanceOrVelocity: Float) {
        swipeDirection = when (distanceOrVelocity.sign) {
            -1f -> RightToLeft
            1f -> LeftToRight
            else -> swipeDirection
        }
    }
}

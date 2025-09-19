package au.com.shiftyjelly.pocketcasts.views.swipe

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
import androidx.dynamicanimation.animation.DynamicAnimation
import androidx.dynamicanimation.animation.SpringForce
import au.com.shiftyjelly.pocketcasts.utils.extensions.dpToPx
import au.com.shiftyjelly.pocketcasts.views.databinding.SwipeRowLayoutBinding
import au.com.shiftyjelly.pocketcasts.views.extensions.cancelSpring
import au.com.shiftyjelly.pocketcasts.views.extensions.doOnEnd
import au.com.shiftyjelly.pocketcasts.views.extensions.doOnUpdate
import au.com.shiftyjelly.pocketcasts.views.extensions.spring
import au.com.shiftyjelly.pocketcasts.views.helper.DelayedAction
import au.com.shiftyjelly.pocketcasts.views.helper.runDelayedAction
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeDirection.LeftToRight
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeDirection.None
import au.com.shiftyjelly.pocketcasts.views.swipe.SwipeDirection.RightToLeft
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
            getSwipedThreshold = { swipedThreshold(settledButtonsWidth()) },
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

    private val swipedThresholdMargin = 24.dpToPx(context).toFloat()

    private val minSwipedThreshold = 200.dpToPx(context).toFloat()

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
        swipeButtons.ltrSection.button1.setUiState(state)
    }

    fun setLtr2State(state: T?) {
        swipeButtons.ltrSection.button2.setUiState(state)
    }

    fun setLtr3State(state: T?) {
        swipeButtons.ltrSection.button3.setUiState(state)
    }

    fun setRtl1State(state: T?) {
        swipeButtons.rtlSection.button1.setUiState(state)
    }

    fun setRtl2State(state: T?) {
        swipeButtons.rtlSection.button2.setUiState(state)
    }

    fun setRtl3State(state: T?) {
        swipeButtons.rtlSection.button3.setUiState(state)
    }

    fun addOnSwipeActionListener(listener: (T) -> Unit) {
        swipeButtons.forEach { it.addOnSwipeActionListener(listener) }
    }

    fun removeOnSwipeActionListener(listener: (T) -> Unit) {
        swipeButtons.forEach { it.removeOnSwipeActionListener(listener) }
    }

    fun settle() {
        swipeHandler.smoothScrollTo(0)
    }

    fun clearTranslation() {
        swipeHandler.clear()
        swipedAction?.cancel()
        swipedAction = null
        cancelSprings()
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
        resolveSwipedThreshold(useHapticFeedback)
        resolveButtonTranslations()
        resolveSwipedState()
    }

    private fun resolveButtonTranslations() {
        val baseOffset = width.toFloat()
        resolveButtonTranslations(swipeButtons.rtlSection, baseOffset)
        resolveButtonTranslations(swipeButtons.ltrSection, -baseOffset)
    }

    private fun resolveButtonTranslations(
        section: SwipeButtons.Section<T>,
        baseOffset: Float,
    ) {
        val translation = swipeableView.translationX
        val coefficient = when (section.allActive.size) {
            3 -> 2f / 3
            2 -> 1f
            else -> 2f
        }
        section.button1.outerContainer.translationX = baseOffset + translation * coefficient / 2
        section.button2.translationX = baseOffset + translation * coefficient
        section.button3.translationX = baseOffset + translation

        val thresholdPosition = if (isSwipedThresholdReached) {
            translation - translation * coefficient / 2
        } else {
            0f
        }
        section.button1.innerContainer
            .spring(DynamicAnimation.TRANSLATION_X, stiffness = SpringForce.STIFFNESS_MEDIUM)
            .animateToFinalPosition(thresholdPosition)
    }

    private fun resolveSwipedThreshold(useHapticFeedback: Boolean) {
        val translation = swipeableView.translationX.absoluteValue
        val isSwipedThresholdReached = translation >= swipedThreshold(settledButtonsWidth())

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

            val section = when {
                translation > 0 -> swipeButtons.ltrSection
                translation < 0 -> swipeButtons.rtlSection
                else -> null
            }

            if (section != null) {
                val primaryButton = section.button1
                primaryButton.callOnSwipeActionListeners()
                swipedAction?.cancel()
                swipedAction = runDelayedAction(
                    350.milliseconds,
                    action = {
                        primaryButton.elevation = 1f
                        swipeableView.translationX = 0f
                        section.button2.translationX = width.toFloat()
                        section.button3.translationX = width.toFloat()

                        primaryButton
                            .spring(DynamicAnimation.ALPHA)
                            .doOnEnd { _, isCancelled, _, _ ->
                                primaryButton.elevation = 0f
                                primaryButton.alpha = 1f
                                if (!isCancelled) {
                                    isLocked = false
                                    setTranslation(0f, useHapticFeedback = false)
                                }
                            }
                            .animateToFinalPosition(0f)
                    },
                    onDetach = {
                        primaryButton.elevation = 0f
                        clearTranslation()
                    },
                )
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
            LeftToRight -> swipeButtons.ltrSection.allActive
            RightToLeft -> swipeButtons.rtlSection.allActive
            None -> emptyList()
        }
        return buttons.sumOf(SwipeButton<T>::settledWidth).toFloat()
    }

    private fun minTranslationValue(): Float {
        return if (swipeButtons.rtlSection.allActive.isNotEmpty()) {
            -width.toFloat()
        } else {
            0f
        }
    }

    private fun maxTranslationValue(): Float {
        return if (swipeButtons.ltrSection.allActive.isNotEmpty()) {
            width.toFloat()
        } else {
            0f
        }
    }

    private fun cancelSprings() {
        swipeableView.cancelSpring(DynamicAnimation.TRANSLATION_X)
        swipeButtons.forEach { button ->
            button.cancelSpring(DynamicAnimation.ALPHA)
            button.innerContainer.cancelSpring(DynamicAnimation.TRANSLATION_X)
        }
    }
}

private enum class SwipeDirection {
    LeftToRight,
    RightToLeft,
    None,
}

private class SwipeButtons<T : SwipeButton.UiState>(
    binding: SwipeRowLayoutBinding,
) : Iterable<SwipeButton<T>> {
    val ltrSection = Section(
        button1 = binding.leftToRightButton1.typed(),
        button2 = binding.leftToRightButton2.typed(),
        button3 = binding.leftToRightButton3.typed(),
    )

    val rtlSection = Section(
        button1 = binding.rightToLeftButton1.typed(),
        button2 = binding.rightToLeftButton2.typed(),
        button3 = binding.rightToLeftButton3.typed(),
    )

    override fun iterator(): Iterator<SwipeButton<T>> {
        return iterator {
            yieldAll(ltrSection)
            yieldAll(rtlSection)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun SwipeButton<*>.typed() = this as SwipeButton<T>

    class Section<T : SwipeButton.UiState>(
        val button1: SwipeButton<T>,
        val button2: SwipeButton<T>,
        val button3: SwipeButton<T>,
    ) : Iterable<SwipeButton<T>> {
        val allActive get() = listOfNotNull(button1.active(), button2.active(), button3.active())

        override fun iterator(): Iterator<SwipeButton<T>> {
            return iterator {
                yield(button1)
                yield(button2)
                yield(button3)
            }
        }

        private fun SwipeButton<T>.active() = takeIf { it.uiState != null }
    }
}

private class SwipeGestureHandler(
    private val parent: ViewGroup,
    private val child: View,
    private val setTranslation: (Float) -> Unit,
    private val getSwipedThreshold: () -> Float,
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
                val swipedThreshold = getSwipedThreshold()
                val (minX, maxX) = when (swipeDirection) {
                    LeftToRight -> -child.width to if (child.translationX >= swipedThreshold) {
                        child.width
                    } else {
                        swipedThreshold.roundToInt() - 1
                    }

                    RightToLeft -> if (child.translationX.absoluteValue >= swipedThreshold) {
                        -child.width
                    } else {
                        -swipedThreshold.roundToInt() + 1
                    } to child.width

                    None -> -child.width to child.width
                }
                scroller.fling(
                    child.translationX.toInt(),
                    0,
                    velocityX.roundToInt(),
                    0,
                    minX,
                    maxX,
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

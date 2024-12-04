@file:OptIn(ExperimentalFoundationApi::class)

package au.com.shiftyjelly.pocketcasts.compose.components

import android.graphics.Matrix
import androidx.compose.animation.SplineBasedFloatDecayAnimationSpec
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.FloatDecayAnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.VectorConverter
import androidx.compose.animation.core.VisibilityThreshold
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.animateDecay
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.MutatePriority
import androidx.compose.foundation.MutatorMutex
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.layout.LazyLayout
import androidx.compose.foundation.lazy.layout.LazyLayoutPrefetchState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.isSpecified
import androidx.compose.ui.graphics.toAndroidRectF
import androidx.compose.ui.graphics.toComposeRect
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import androidx.compose.ui.input.pointer.util.VelocityTracker
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.OnGloballyPositionedModifier
import androidx.compose.ui.layout.Remeasurement
import androidx.compose.ui.layout.RemeasurementModifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.lerp
import kotlin.math.abs
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch

@Composable
fun LazyStack(
    modifier: Modifier = Modifier,
    state: LazyStackState = rememberLazyStackState(),
    contentPadding: PaddingValues = PaddingValues(0.dp),
    content: LazyStackScope.() -> Unit,
) {
    val pageProviderLambda = rememberLazyStackPageProviderLambda(state, content)
    val measurePolicy = rememberLazyStackMeasurePolicy(state, contentPadding, pageProviderLambda)
    LazyLayout(
        itemProvider = pageProviderLambda,
        measurePolicy = measurePolicy,
        modifier = Modifier
            .then(state.modifier(rememberCoroutineScope()))
            .then(modifier),
    )
}

data class LazyStackPhysics(
    val flingGestureThreshold: Velocity,
    val flingAnimationSpec: (Density) -> FloatDecayAnimationSpec,
    val horizontalDragThreshold: (componentWidth: Float) -> Float,
    val verticalDragThreshold: (componentHeight: Float) -> Float,
    val dragAnimationSpec: AnimationSpec<Offset>,
    val maxRotation: Float,
    val topPageRotation: (startDragPosition: Offset, offset: Offset, componenetSize: Size) -> Float,
    val bottomPageScale: (Offset, Size) -> Float,
) {
    companion object {
        val Default = LazyStackPhysics(
            flingGestureThreshold = Velocity(3000f, 3000f),
            flingAnimationSpec = ::SplineBasedFloatDecayAnimationSpec,
            horizontalDragThreshold = { width -> width * 0.3f },
            verticalDragThreshold = { height -> height * 0.2f },
            dragAnimationSpec = spring(
                stiffness = Spring.StiffnessMediumLow,
                visibilityThreshold = Offset.VisibilityThreshold,
            ),
            maxRotation = 10f,
            topPageRotation = { startOffset, offset, size ->
                val percentage = (1.75f * offset.x / size.width).coerceIn(-1f, 1f)
                val direction = if (startOffset.y < size.height * 0.75) 1f else -1f
                lerp(start = 0f, stop = 10f, fraction = percentage * direction)
            },
            bottomPageScale = { offset, size ->
                val fractionX = abs(offset.x) / size.width
                val fractionY = 1.2f * abs(offset.y) / size.height
                val largerFraction = maxOf(fractionX, fractionY)
                val scale = 0.3f * largerFraction + 0.85f
                scale.coerceIn(0.85f, 1f)
            },
        )
    }
}

enum class SwipeDirection {
    Left, Right, Top, Bottom
}

@Composable
fun rememberLazyStackState(
    initialPage: Int = 0,
    pagePrefetchCount: Int = 0,
    physics: LazyStackPhysics = LazyStackPhysics.Default,
): LazyStackState {
    val density = LocalDensity.current
    val flingAnimationSpec = remember(density.density, physics) {
        physics.flingAnimationSpec(density)
    }
    val stateSaver = remember(pagePrefetchCount, physics, flingAnimationSpec) {
        LazyStackState.saver(pagePrefetchCount, physics, flingAnimationSpec)
    }
    return rememberSaveable(saver = stateSaver) {
        LazyStackState(initialPage, pagePrefetchCount, physics, flingAnimationSpec)
    }
}

@Stable
class LazyStackState internal constructor(
    initialPage: Int,
    pagePrefetchCount: Int,
    private val physics: LazyStackPhysics,
    flingAnimationSpec: FloatDecayAnimationSpec,
) {
    val currentPageIndex get() = pageTracker.currentPageIndex
    val pageCount get() = pageTracker.pageCount

    internal val pagePrefetchCount get() = pageTracker.pagePrefetchCount
    internal val offset get() = swipeController.currentOffset()
    internal val rotation get() = swipeController.currentRotation()
    internal val scale get() = swipeController.currentScale()

    private val pageTracker = PageTracker(initialPage, pagePrefetchCount)
    private val swipeController = SwipeController(pageTracker, physics, flingAnimationSpec)

    fun modifier(coroutineScope: CoroutineScope) = Modifier
        .then(pageTracker.remeasurementModifer)
        .then(swipeController.layoutModifier)
        .detectSwipeGestures(swipeController, coroutineScope)

    suspend fun swipeToNextPage(
        direction: SwipeDirection,
        animationSpec: AnimationSpec<Offset> = DefaultSwipeAnimationSpec,
    ) {
        if (currentPageIndex < pageCount) {
            swipeController.swipeToNextPage(direction, animationSpec)
        }
    }

    suspend fun swipeToPreviousPage(
        direction: SwipeDirection,
        animationSpec: AnimationSpec<Offset> = DefaultSwipeAnimationSpec,
    ) {
        if (currentPageIndex > 0) {
            swipeController.swipeToPreviousPage(direction, animationSpec)
        }
    }

    internal fun matchPageIndexWithKey(pageProvider: LazyStackPageProvider, pageIndex: Int): Int {
        return pageTracker.matchPageIndexWithKey(pageProvider, pageIndex)
    }

    internal fun updatePremeasureConstraints(constraints: Constraints) {
        pageTracker.updatePremeasureConstraints(constraints)
    }

    internal fun applyMeasureResult(result: LazyStackMeasureResult) {
        pageTracker.applyMeasureResult(result)
        swipeController.applyMeasureResult(result)
    }

    internal companion object {
        fun saver(
            pagePrefetchCount: Int,
            physics: LazyStackPhysics,
            flingAnimationSpec: FloatDecayAnimationSpec,
        ): Saver<LazyStackState, Int> = Saver<LazyStackState, Int>(
            save = { it.currentPageIndex },
            restore = {
                LazyStackState(initialPage = it, pagePrefetchCount, physics, flingAnimationSpec)
            },
        )

        val DefaultSwipeAnimationSpec = tween<Offset>(600)
    }
}

private class PageTracker(
    initialPage: Int,
    internal val pagePrefetchCount: Int,
) {
    internal var currentPageIndex by mutableIntStateOf(initialPage)
        private set
    internal var pageCount by mutableIntStateOf(0)
        private set
    private var currentPageKey by mutableStateOf<Any?>(null)
    private val prefetchState = LazyLayoutPrefetchState()
    private var premeaseureConstraints = Constraints()
    private var remeasurement by mutableStateOf<Remeasurement?>(null)
    internal val remeasurementModifer = object : RemeasurementModifier {
        override fun onRemeasurementAvailable(remeasurement: Remeasurement) {
            this@PageTracker.remeasurement = remeasurement
        }
    }

    internal suspend fun switchToPage(index: Int) {
        val newIndex = index.coerceIn(0, pageCount)
        currentPageIndex = newIndex
        currentPageKey = null
    }

    internal fun schedulePrefetch() {
        prefetchState.schedulePrefetch(currentPageIndex + pagePrefetchCount + 1, premeaseureConstraints)
        remeasurement?.forceRemeasure()
    }

    internal fun matchPageIndexWithKey(
        provider: LazyStackPageProvider,
        pageIndex: Int,
    ): Int {
        val newIndex = provider.findIndexByKey(currentPageKey, pageIndex)
        if (pageIndex != newIndex) {
            currentPageIndex = newIndex
            currentPageKey = provider.getKey(newIndex)
        }
        return newIndex
    }

    internal fun updatePremeasureConstraints(constraints: Constraints) {
        premeaseureConstraints = constraints
    }

    internal fun applyMeasureResult(result: LazyStackMeasureResult) {
        currentPageKey = result.firstVisiblePageKey
        pageCount = result.pageCount
        result.firstVisiblePageIndex?.let { currentPageIndex = it }
    }
}

private class SwipeController(
    private val pageTracker: PageTracker,
    private val physics: LazyStackPhysics,
    flingAnimationSpec: FloatDecayAnimationSpec,
) {
    private var offset by mutableStateOf(Offset.Zero)
    private var rotation by mutableFloatStateOf(0f)
    private var scale by mutableFloatStateOf(0f)
    private var startOffset by mutableStateOf(Offset.Zero)
    private var containerSize by mutableStateOf<Size?>(null)
    private var currentPageSize by mutableStateOf<Size?>(null)
    private var currentPageSwipeDirections by mutableStateOf<Set<SwipeDirection>?>(null)
    private var currentPageOnSwipedOut by mutableStateOf<((SwipeDirection) -> Unit)?>(null)
    private var currentPageOnSwipedIn by mutableStateOf<((SwipeDirection) -> Unit)?>(null)
    private val rotationMatrix = Matrix()

    private val swipeScope = SwipeScope(physics.dragAnimationSpec, flingAnimationSpec) { delta ->
        offset += delta
        val size = containerSize?.takeIf { it.isSpecified && it.width > 0f && it.height > 0 }
        rotation = size?.let { physics.topPageRotation(startOffset, offset, it) } ?: 0f
        scale = size?.let { physics.bottomPageScale(offset, it) } ?: 1f
    }

    private val swipeAnimationFactory = SwipeAnimationFactory(
        physics,
        ::currentOffset,
        ::containerRect,
        ::currentPageRect,
        ::rotatedPageRect,
    )

    internal val layoutModifier = AwaitFirstLayoutModifier()

    internal suspend fun swipeToNextPage(
        direction: SwipeDirection,
        animationSpec: AnimationSpec<Offset>? = null,
    ) {
        layoutModifier.await()
        swipeAnimationFactory.createDrag(direction, animationSpec).swipePage(swipeScope)
        switchToNextPage(direction)
    }

    internal suspend fun swipeToPreviousPage(
        direction: SwipeDirection,
        animationSpec: AnimationSpec<Offset>? = null,
    ) {
        layoutModifier.await()
        pageTracker.switchToPage(pageTracker.currentPageIndex - 1)

        swipeAnimationFactory.createDrag(direction, animationSpec).swipeBack(swipeScope)
        currentPageOnSwipedIn?.invoke(direction)
        swipeScope.snapBy(-offset)

        pageTracker.schedulePrefetch()
    }

    private suspend fun switchToNextPage(direction: SwipeDirection) {
        currentPageOnSwipedOut?.invoke(direction)
        pageTracker.switchToPage(pageTracker.currentPageIndex + 1)
        swipeScope.snapBy(-offset)
        pageTracker.schedulePrefetch()
    }

    internal fun startDrag(initialOffset: Offset) {
        startOffset = initialOffset
    }

    internal suspend fun dragBy(
        delta: Offset,
        priority: MutatePriority = MutatePriority.Default,
    ) {
        val directions = currentPageSwipeDirections
        if (directions.isNullOrEmpty()) {
            return
        }
        val nextOffset = offset + delta
        val allowedDelta = delta.copy(
            x = when {
                SwipeDirection.Left !in directions && nextOffset.x < 0f -> 0f
                SwipeDirection.Right !in directions && nextOffset.x > 0f -> 0f
                else -> delta.x
            },
            y = when {
                SwipeDirection.Top !in directions && nextOffset.y < 0f -> 0f
                SwipeDirection.Bottom !in directions && nextOffset.y > 0f -> 0f
                else -> delta.y
            },
        )
        swipeScope.snapBy(allowedDelta, priority)
    }

    internal suspend fun finishDrag(
        velocity: Velocity,
        priority: MutatePriority = MutatePriority.Default,
    ) {
        val swipeAnimation = swipeAnimationFactory.create(velocity)
        val directions = currentPageSwipeDirections ?: emptySet()
        if (swipeAnimation.direction == null || swipeAnimation.direction in directions) {
            val swipedDirection = swipeAnimation.swipePage(
                swipeScope = swipeScope,
                priority = priority,
            )
            if (swipedDirection != null) {
                switchToNextPage(swipedDirection)
            }
        }
        startOffset = Offset.Zero
    }

    internal suspend fun cancelDrag(
        priority: MutatePriority = MutatePriority.Default,
    ) {
        swipeScope.drag(
            from = offset,
            to = Offset.Zero,
            priority = priority,
        )
        startOffset = Offset.Zero
    }

    internal fun currentOffset(): Offset {
        return offset
    }

    internal fun currentRotation(): Float {
        return rotation
    }

    internal fun currentScale(): Float {
        return scale
    }

    private fun containerRect(): Rect {
        return Rect(Offset.Zero, containerSize ?: Size.Zero)
    }

    private fun currentPageRect(): Rect {
        val baseRect = Rect(offset, currentPageSize ?: containerSize ?: Size.Zero)
        val androidRect = baseRect.toAndroidRectF()
        rotationMatrix.setRotate(rotation, baseRect.center.x, baseRect.center.y)
        rotationMatrix.mapRect(androidRect)
        return androidRect.toComposeRect()
    }

    private fun rotatedPageRect(): Rect {
        val baseRect = Rect(Offset.Zero, currentPageSize ?: containerSize ?: Size.Zero)
        val androidRect = baseRect.toAndroidRectF()
        rotationMatrix.setRotate(physics.maxRotation, baseRect.center.x, baseRect.center.y)
        rotationMatrix.mapRect(androidRect)
        return androidRect.toComposeRect()
    }

    internal fun applyMeasureResult(result: LazyStackMeasureResult) {
        containerSize = result.containerSize
        currentPageSize = result.pageSize
        currentPageSwipeDirections = result.directionsForPage
        currentPageOnSwipedOut = result.onSwipedOut
        currentPageOnSwipedIn = result.onSwipedIn
    }
}

private class SwipeScope(
    private val dragAnimationSpec: AnimationSpec<Offset>,
    private val flingAnimationSpec: FloatDecayAnimationSpec,
    private val consumeDragDelta: (Offset) -> Unit,
) {
    internal suspend fun snapBy(
        delta: Offset,
        priority: MutatePriority = MutatePriority.Default,
    ) {
        drag(priority) {
            consumeDragDelta(delta.toSafeOffset())
        }
    }

    internal suspend fun drag(
        from: Offset,
        to: Offset,
        priority: MutatePriority = MutatePriority.Default,
        animationSpec: AnimationSpec<Offset>? = null,
    ) {
        drag(priority) {
            var deltaValue = from
            animate(
                initialValue = from,
                targetValue = to,
                animationSpec = animationSpec ?: dragAnimationSpec,
                typeConverter = Offset.VectorConverter,
            ) { value, _ ->
                val offset = (value - deltaValue).toSafeOffset()
                consumeDragDelta(offset)
                deltaValue = value
            }
        }
    }

    internal suspend fun flingFlow(
        from: Offset,
        velocity: Velocity,
        priority: MutatePriority = MutatePriority.Default,
    ) = channelFlow<Offset> {
        drag(priority) {
            var currentOffsetX = from.x
            var currentOffsetY = from.y
            launch {
                var deltaValue = from.x
                animateDecay(
                    initialValue = from.x,
                    initialVelocity = velocity.x,
                    animationSpec = flingAnimationSpec,
                ) { value, _ ->
                    currentOffsetX = value
                    val offset = Offset(x = value - deltaValue, y = 0f).toSafeOffset()
                    consumeDragDelta(offset)
                    deltaValue = value
                    trySend(Offset(currentOffsetX, currentOffsetY).toSafeOffset())
                }
            }
            launch {
                var deltaValue = from.y
                animateDecay(
                    initialValue = from.y,
                    initialVelocity = velocity.y,
                    animationSpec = flingAnimationSpec,
                ) { value, _ ->
                    currentOffsetY = value
                    val offset = Offset(x = 0f, y = value - deltaValue).toSafeOffset()
                    consumeDragDelta(offset)
                    deltaValue = value
                    trySend(Offset(currentOffsetX, currentOffsetY).toSafeOffset())
                }
            }
        }
    }

    private fun Offset.toSafeOffset() = Offset(
        x = if (x.isFinite()) x else 0f,
        y = if (y.isFinite()) y else 0f,
    )

    private val mutex = MutatorMutex()

    private suspend fun drag(
        priority: MutatePriority = MutatePriority.Default,
        block: suspend SwipeScope.() -> Unit,
    ) {
        mutex.mutateWith(this, priority, block)
    }
}

private class SwipeAnimationFactory(
    private val physics: LazyStackPhysics,
    private val currentOffset: () -> Offset,
    private val containerRect: () -> Rect,
    private val currentPageRect: () -> Rect,
    private val rotatedPageRect: () -> Rect,
) {

    internal fun create(velocity: Velocity): SwipeAnimation {
        val thresholdOffset = currentOffset()
        val thresholdRect = containerRect()
        val horizontalThreshold = abs(physics.horizontalDragThreshold(thresholdRect.width))
        val verticalThreshold = abs(physics.verticalDragThreshold(thresholdRect.height))

        return when {
            velocity.x < 0f && abs(velocity.x) > abs(physics.flingGestureThreshold.x) -> {
                createFling(SwipeDirection.Left, velocity)
            }

            velocity.x > 0f && abs(velocity.x) > abs(physics.flingGestureThreshold.x) -> {
                createFling(SwipeDirection.Right, velocity)
            }

            velocity.y < 0f && abs(velocity.y) > abs(physics.flingGestureThreshold.y) -> {
                createFling(SwipeDirection.Top, velocity)
            }

            velocity.y > 0f && abs(velocity.y) > abs(physics.flingGestureThreshold.y) -> {
                createFling(SwipeDirection.Bottom, velocity)
            }

            thresholdOffset.x < 0f && abs(thresholdOffset.x) > horizontalThreshold -> {
                createDrag(SwipeDirection.Left)
            }

            thresholdOffset.x > 0f && thresholdOffset.x > horizontalThreshold -> {
                createDrag(SwipeDirection.Right)
            }

            thresholdOffset.y < 0f && abs(thresholdOffset.y) > verticalThreshold -> {
                createDrag(SwipeDirection.Top)
            }

            thresholdOffset.y > 0f && thresholdOffset.y > verticalThreshold -> {
                createDrag(SwipeDirection.Bottom)
            }

            else -> {
                SwipeAnimation.Center(currentOffset)
            }
        }
    }

    internal fun createFling(
        direction: SwipeDirection,
        velocity: Velocity,
    ): SwipeAnimation.Fling {
        return SwipeAnimation.Fling(direction, velocity, currentOffset, containerRect, currentPageRect)
    }

    internal fun createDrag(
        direction: SwipeDirection,
        animationSpec: AnimationSpec<Offset>? = null,
    ): SwipeAnimation.Drag {
        return SwipeAnimation.Drag(direction, animationSpec, currentOffset, containerRect, rotatedPageRect)
    }
}

private sealed interface SwipeAnimation {
    val direction: SwipeDirection?

    suspend fun swipePage(
        swipeScope: SwipeScope,
        priority: MutatePriority = MutatePriority.Default,
    ): SwipeDirection?

    class Drag(
        override val direction: SwipeDirection,
        private val animationSpec: AnimationSpec<Offset>?,
        private val offset: () -> Offset,
        private val containerRect: () -> Rect,
        private val rotatedPageRect: () -> Rect,
    ) : SwipeAnimation {
        override suspend fun swipePage(
            swipeScope: SwipeScope,
            priority: MutatePriority,
        ): SwipeDirection {
            drag(swipeScope)
            return direction
        }

        suspend fun swipeBack(swipeScope: SwipeScope) {
            val currentOffset = offset()
            val snapOffset = dragOffset(currentOffset)
            swipeScope.snapBy(snapOffset - currentOffset, MutatePriority.PreventUserInput)
            swipeScope.drag(
                from = snapOffset,
                to = Offset.Zero,
                priority = MutatePriority.PreventUserInput,
                animationSpec = animationSpec,
            )
        }

        private suspend fun drag(
            swipeScope: SwipeScope,
        ) {
            val currentOffset = offset()
            val targetOffset = dragOffset(currentOffset)
            swipeScope.drag(
                from = currentOffset,
                to = targetOffset,
                priority = MutatePriority.PreventUserInput,
                animationSpec = animationSpec,
            )
        }

        private fun dragOffset(currentOffset: Offset): Offset {
            val containerRect = containerRect()
            val rotatedPageRect = rotatedPageRect()

            val widthDiff = abs(containerRect.width - rotatedPageRect.width) / 2
            val heightDiff = abs(containerRect.width - rotatedPageRect.width) / 2

            return when (direction) {
                SwipeDirection.Left -> Offset(
                    x = (-containerRect.width - widthDiff),
                    y = currentOffset.y,
                )

                SwipeDirection.Right -> Offset(
                    x = (containerRect.width + widthDiff),
                    y = currentOffset.y,
                )

                SwipeDirection.Top -> Offset(
                    x = currentOffset.x,
                    y = (-containerRect.height - heightDiff),
                )

                SwipeDirection.Bottom -> Offset(
                    x = currentOffset.x,
                    y = (containerRect.height + heightDiff),
                )
            }
        }
    }

    class Fling(
        override val direction: SwipeDirection,
        private val velocity: Velocity,
        private val offset: () -> Offset,
        private val containerRect: () -> Rect,
        private val currentPageRect: () -> Rect,
    ) : SwipeAnimation {
        override suspend fun swipePage(
            swipeScope: SwipeScope,
            priority: MutatePriority,
        ): SwipeDirection? {
            fling(swipeScope, priority)
            return if (!isInBounds()) {
                direction
            } else {
                center(swipeScope, priority)
                null
            }
        }

        private suspend fun fling(
            swipeScope: SwipeScope,
            priority: MutatePriority,
        ) {
            val flingOffsets = swipeScope.flingFlow(
                from = offset(),
                velocity = velocity,
                priority = priority,
            )
            flingOffsets.takeWhile { isInBounds() }.collect()
        }

        private fun isInBounds(): Boolean {
            val rect = containerRect()
            val pageRect = currentPageRect()
            return pageRect.overlaps(rect)
        }

        private suspend fun center(
            swipeScope: SwipeScope,
            priority: MutatePriority,
        ) {
            swipeScope.drag(
                from = offset(),
                to = Offset.Zero,
                priority = priority,
            )
        }
    }

    class Center(
        private val currentOffset: () -> Offset,
    ) : SwipeAnimation {
        override val direction = null

        override suspend fun swipePage(
            swipeScope: SwipeScope,
            priority: MutatePriority,
        ): SwipeDirection? {
            swipeScope.drag(
                from = currentOffset(),
                to = Offset.Zero,
                priority = priority,
            )
            return null
        }
    }
}

private fun Modifier.detectSwipeGestures(
    controller: SwipeController,
    scope: CoroutineScope,
) = pointerInput(Unit) {
    val velocityTracker = VelocityTracker()

    detectDragGestures(
        onDragStart = { startPosition ->
            controller.startDrag(startPosition)
            velocityTracker.addPosition(System.currentTimeMillis(), controller.currentOffset())
        },
        onDrag = { change, dragAmount ->
            if (change.positionChanged()) {
                change.consume()
            }
            scope.launch {
                controller.dragBy(dragAmount, MutatePriority.UserInput)
                velocityTracker.addPosition(System.currentTimeMillis(), controller.currentOffset())
            }
        },
        onDragEnd = {
            val velocity = velocityTracker.calculateVelocity()
            velocityTracker.resetTracking()
            scope.launch {
                controller.finishDrag(velocity, MutatePriority.UserInput)
            }
        },
        onDragCancel = {
            velocityTracker.resetTracking()
            scope.launch {
                controller.cancelDrag(MutatePriority.UserInput)
            }
        },
    )
}

private class AwaitFirstLayoutModifier : OnGloballyPositionedModifier {
    private val deferred = CompletableDeferred<Unit>()

    suspend fun await() = deferred.await()

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        if (!deferred.isCompleted) {
            deferred.complete(Unit)
        }
    }
}

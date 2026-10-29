package au.com.shiftyjelly.pocketcasts.compose.swipe

import android.view.ViewConfiguration
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.exponentialDecay
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.snapping.SnapLayoutInfoProvider
import androidx.compose.foundation.gestures.snapping.snapFlingBehavior
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.Icon
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.RippleConfiguration
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.customActions
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import kotlin.math.absoluteValue
import kotlin.math.sign
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.filter
import androidx.compose.material.MaterialTheme as Material

private val ActionIconSize = 24.dp
private val ActionHorizontalPadding = 48.dp
private val FullSwipeMinThreshold = 200.dp
private val FullSwipeThresholdMargin = 24.dp

internal const val FULL_SWIPE_ANCHOR_MULTIPLIER = 2

private const val MAX_ACTIONS_PER_SIDE = 3

private val FullSwipeAnchors = setOf(SwipeRowAnchor.FullLeading, SwipeRowAnchor.FullTrailing)

private val SwipeRowState.settledOffset
    get() = draggableState.offset.let { offset -> if (offset.isNaN()) 0f else offset }

internal fun swipeRowAnchors(
    rowWidthPx: Float,
    leadingWidthPx: Float?,
    trailingWidthPx: Float?,
    isLeadingFullSwipeEnabled: Boolean,
    isTrailingFullSwipeEnabled: Boolean,
) = DraggableAnchors {
    SwipeRowAnchor.Resting at 0f
    if (rowWidthPx > 0f) {
        if (leadingWidthPx != null) {
            SwipeRowAnchor.Leading at leadingWidthPx
            if (isLeadingFullSwipeEnabled) {
                // Multiplied due to https://issuetracker.google.com/issues/367660226
                SwipeRowAnchor.FullLeading at rowWidthPx * FULL_SWIPE_ANCHOR_MULTIPLIER
            }
        }
        if (trailingWidthPx != null) {
            SwipeRowAnchor.Trailing at -trailingWidthPx
            if (isTrailingFullSwipeEnabled) {
                // Multiplied due to https://issuetracker.google.com/issues/367660226
                SwipeRowAnchor.FullTrailing at -rowWidthPx * FULL_SWIPE_ANCHOR_MULTIPLIER
            }
        }
    }
}

internal fun swipeRowFullSwipeThreshold(
    revealedWidthPx: Float,
    marginPx: Float,
    minThresholdPx: Float,
) = (revealedWidthPx + marginPx).coerceAtLeast(minThresholdPx)

internal data class SwipeRowSide(
    val revealedWidthPx: Float,
    val fullSwipeThresholdPx: Float?,
)

internal fun swipeRowTarget(
    offset: Float,
    velocity: Float,
    lastDragDirection: Float,
    minFlingVelocity: Float,
    leading: SwipeRowSide,
    trailing: SwipeRowSide,
): SwipeRowAnchor {
    val isLeading = offset > 0f
    val side = if (isLeading) leading else trailing
    if (offset == 0f || side.revealedWidthPx == 0f) {
        return SwipeRowAnchor.Resting
    }
    val distance = offset.absoluteValue
    val direction = if (velocity != 0f) velocity.sign else lastDragDirection
    val isClosing = if (isLeading) direction < 0f else direction > 0f
    val isFling = velocity.absoluteValue >= minFlingVelocity
    val openThreshold = if (isClosing) side.revealedWidthPx * 2 / 3 else side.revealedWidthPx / 3
    val revealed = if (isLeading) SwipeRowAnchor.Leading else SwipeRowAnchor.Trailing
    val full = if (isLeading) SwipeRowAnchor.FullLeading else SwipeRowAnchor.FullTrailing
    return when {
        isFling && isClosing -> SwipeRowAnchor.Resting
        side.fullSwipeThresholdPx != null && distance >= side.fullSwipeThresholdPx -> full
        isFling -> revealed
        distance < openThreshold -> SwipeRowAnchor.Resting
        else -> revealed
    }
}

internal fun swipeRowActionEdge(
    index: Int,
    count: Int,
    revealedWidthPx: Float,
) = revealedWidthPx * (index + 1) / count

@Composable
fun SwipeRow(
    state: SwipeRowState,
    modifier: Modifier = Modifier,
    leadingAction: SwipeRowAction? = null,
    trailingAction: SwipeRowAction? = null,
    isSwipeEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    SwipeRow(
        state = state,
        leadingActions = listOfNotNull(leadingAction),
        trailingActions = listOfNotNull(trailingAction),
        modifier = modifier,
        isSwipeEnabled = isSwipeEnabled,
        content = content,
    )
}

@Composable
fun SwipeRow(
    state: SwipeRowState,
    leadingActions: List<SwipeRowAction>,
    trailingActions: List<SwipeRowAction>,
    modifier: Modifier = Modifier,
    isSwipeEnabled: Boolean = true,
    actionIcon: @Composable (SwipeRowAction) -> Unit = { action -> SwipeRowActionIcon(action) },
    content: @Composable () -> Unit,
) {
    val shownLeadingActions = leadingActions.take(MAX_ACTIONS_PER_SIDE)
    val shownTrailingActions = trailingActions.take(MAX_ACTIONS_PER_SIDE)
    val density = LocalDensity.current
    val actionWidthPx = with(density) { (ActionIconSize + ActionHorizontalPadding).toPx() }
    val fullSwipeThresholdMarginPx = with(density) { FullSwipeThresholdMargin.toPx() }
    val fullSwipeMinThresholdPx = with(density) { FullSwipeMinThreshold.toPx() }
    val fullSwipeThresholdPx = remember(fullSwipeThresholdMarginPx, fullSwipeMinThresholdPx) {
        { revealedWidthPx: Float -> swipeRowFullSwipeThreshold(revealedWidthPx, fullSwipeThresholdMarginPx, fullSwipeMinThresholdPx) }
    }
    var rowWidthPx by remember { mutableIntStateOf(0) }

    val leadingWidthPx = actionWidthPx * shownLeadingActions.size
    val trailingWidthPx = actionWidthPx * shownTrailingActions.size
    val isLeadingFullSwipeEnabled = shownLeadingActions.firstOrNull()?.isFullSwipeEnabled == true
    val isTrailingFullSwipeEnabled = shownTrailingActions.firstOrNull()?.isFullSwipeEnabled == true
    val anchors = remember(rowWidthPx, leadingWidthPx, trailingWidthPx, isLeadingFullSwipeEnabled, isTrailingFullSwipeEnabled) {
        swipeRowAnchors(
            rowWidthPx = rowWidthPx.toFloat(),
            leadingWidthPx = leadingWidthPx.takeIf { it > 0f },
            trailingWidthPx = trailingWidthPx.takeIf { it > 0f },
            isLeadingFullSwipeEnabled = isLeadingFullSwipeEnabled,
            isTrailingFullSwipeEnabled = isTrailingFullSwipeEnabled,
        )
    }
    SideEffect {
        state.draggableState.updateAnchors(anchors)
    }

    val leadingSide by rememberUpdatedState(
        SwipeRowSide(leadingWidthPx, fullSwipeThresholdPx(leadingWidthPx).takeIf { isLeadingFullSwipeEnabled }),
    )
    val trailingSide by rememberUpdatedState(
        SwipeRowSide(trailingWidthPx, fullSwipeThresholdPx(trailingWidthPx).takeIf { isTrailingFullSwipeEnabled }),
    )
    val isFullSwipeThresholdReached by remember(state) {
        derivedStateOf {
            val offset = state.settledOffset
            val threshold = (if (offset > 0f) leadingSide else trailingSide).fullSwipeThresholdPx
            threshold != null && offset.absoluteValue >= threshold
        }
    }

    val lastDragDirection = remember(state) { mutableFloatStateOf(0f) }
    LaunchedEffect(state) {
        var previousOffset = state.settledOffset
        snapshotFlow { state.settledOffset }.collect { offset ->
            if (offset != previousOffset) {
                lastDragDirection.floatValue = (offset - previousOffset).sign
                previousOffset = offset
            }
        }
    }
    val minFlingVelocity = ViewConfiguration.get(LocalContext.current).scaledMinimumFlingVelocity.toFloat()
    val flingBehavior = remember(state, minFlingVelocity) {
        snapFlingBehavior(
            snapLayoutInfoProvider = object : SnapLayoutInfoProvider {
                override fun calculateApproachOffset(velocity: Float, decayOffset: Float) = 0f

                override fun calculateSnapOffset(velocity: Float): Float {
                    val offset = state.draggableState.requireOffset()
                    val target = swipeRowTarget(offset, velocity, lastDragDirection.floatValue, minFlingVelocity, leadingSide, trailingSide)
                    return state.draggableState.anchors.positionOf(target) - offset
                }
            },
            decayAnimationSpec = exponentialDecay(),
            snapAnimationSpec = AnchoredDraggableDefaults.SnapAnimationSpec,
        )
    }
    val hapticFeedback = LocalHapticFeedback.current
    LaunchedEffect(state) {
        snapshotFlow { isFullSwipeThresholdReached }
            .drop(1)
            .filter { state.draggableState.settledValue !in FullSwipeAnchors }
            .collect { hapticFeedback.performHapticFeedback(HapticFeedbackType.ContextClick) }
    }

    LaunchedEffect(state, isSwipeEnabled) {
        if (!isSwipeEnabled) {
            state.settleAndWait()
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val view = LocalView.current
    LaunchedEffect(interactionSource, view) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is DragInteraction.Start -> view.parent?.requestDisallowInterceptTouchEvent(true)
                is DragInteraction.Stop, is DragInteraction.Cancel -> view.parent?.requestDisallowInterceptTouchEvent(false)
                else -> Unit
            }
        }
    }

    val latestLeadingActions by rememberUpdatedState(shownLeadingActions)
    val latestTrailingActions by rememberUpdatedState(shownTrailingActions)
    LaunchedEffect(state) {
        snapshotFlow { state.draggableState.settledValue }
            .drop(1)
            .collect { anchor ->
                when (anchor) {
                    SwipeRowAnchor.FullLeading -> latestLeadingActions.firstOrNull()?.onClick?.invoke(state)
                    SwipeRowAnchor.FullTrailing -> latestTrailingActions.firstOrNull()?.onClick?.invoke(state)
                    else -> Unit
                }
            }
    }

    val primaryExpansion by animateFloatAsState(
        targetValue = if (isFullSwipeThresholdReached) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMedium),
    )

    Box(
        modifier = modifier
            .onSizeChanged { rowWidthPx = it.width }
            .anchoredDraggable(
                state = state.draggableState,
                orientation = Orientation.Horizontal,
                enabled = isSwipeEnabled,
                interactionSource = interactionSource,
                flingBehavior = flingBehavior,
            ),
    ) {
        CompositionLocalProvider(LocalRippleConfiguration provides ActionRippleConfiguration) {
            SwipeRowActionSlabs(
                actions = shownLeadingActions,
                state = state,
                rowWidthPx = rowWidthPx.toFloat(),
                primaryExpansion = { primaryExpansion },
                isLeading = true,
                actionIcon = actionIcon,
            )
            SwipeRowActionSlabs(
                actions = shownTrailingActions,
                state = state,
                rowWidthPx = rowWidthPx.toFloat(),
                primaryExpansion = { primaryExpansion },
                isLeading = false,
                actionIcon = actionIcon,
            )
        }

        val actions = shownLeadingActions + shownTrailingActions
        Box(
            modifier = Modifier
                .graphicsLayer { translationX = state.settledOffset }
                .semantics {
                    customActions = actions.map { action ->
                        CustomAccessibilityAction(action.contentDescription) {
                            action.onClick(state)
                            true
                        }
                    }
                },
        ) {
            content()
            if (state.isOpen) {
                Box(
                    modifier = Modifier
                        .matchParentSize()
                        .pointerInput(state) {
                            detectTapGestures { state.settle() }
                        },
                )
            }
        }
    }
}

@Composable
private fun BoxScope.SwipeRowActionSlabs(
    actions: List<SwipeRowAction>,
    state: SwipeRowState,
    rowWidthPx: Float,
    primaryExpansion: () -> Float,
    isLeading: Boolean,
    actionIcon: @Composable (SwipeRowAction) -> Unit,
) {
    for (index in actions.indices.reversed()) {
        val action = actions[index]
        Box(
            contentAlignment = if (isLeading) AbsoluteAlignment.CenterRight else AbsoluteAlignment.CenterLeft,
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer {
                    val offset = state.settledOffset
                    val isRevealed = if (isLeading) offset > 0f else offset < 0f
                    val revealedWidthPx = if (isRevealed) offset.absoluteValue.coerceAtMost(rowWidthPx) else 0f
                    val baseEdge = swipeRowActionEdge(index, actions.size, revealedWidthPx)
                    val edge = if (index == 0) baseEdge + (revealedWidthPx - baseEdge) * primaryExpansion() else baseEdge
                    translationX = if (isLeading) edge - rowWidthPx else rowWidthPx - edge
                    clip = true
                }
                .background(action.backgroundColor)
                .clickable(
                    enabled = state.isOpen,
                    onClick = { action.onClick(state) },
                )
                .semantics { contentDescription = action.contentDescription },
        ) {
            actionIcon(action)
        }
    }
}

@Composable
private fun SwipeRowActionIcon(action: SwipeRowAction) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .width(ActionIconSize + ActionHorizontalPadding)
            .fillMaxHeight(),
    ) {
        Icon(
            painter = painterResource(action.iconId),
            contentDescription = null,
            tint = action.iconTint,
            modifier = Modifier.size(ActionIconSize),
        )
    }
}

private val ActionRippleConfiguration = RippleConfiguration(
    color = Color.White,
    rippleAlpha = RippleAlpha(
        draggedAlpha = 0.15f,
        focusedAlpha = 0.15f,
        hoveredAlpha = 0.2f,
        pressedAlpha = 0.4f,
    ),
)

@Preview
@Composable
private fun SwipeRowPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        SwipeRow(
            state = rememberSwipeRowState(),
            leadingAction = SwipeRowActionDefaults.share(onClick = {}),
            trailingAction = SwipeRowActionDefaults.delete(onClick = {}),
        ) {
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .background(Material.theme.colors.primaryUi01),
            ) {
                TextH40(
                    text = "Swipe me",
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}

@Preview
@Composable
private fun MultiActionSwipeRowPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        SwipeRow(
            state = rememberSwipeRowState(),
            leadingActions = listOf(SwipeRowActionDefaults.share(onClick = {}), SwipeRowActionDefaults.delete(onClick = {})),
            trailingActions = listOf(
                SwipeRowActionDefaults.delete(onClick = {}),
                SwipeRowActionDefaults.share(onClick = {}),
                SwipeRowActionDefaults.delete(onClick = {}),
            ),
        ) {
            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(72.dp)
                    .background(Material.theme.colors.primaryUi01),
            ) {
                TextH40(
                    text = "Swipe me",
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}

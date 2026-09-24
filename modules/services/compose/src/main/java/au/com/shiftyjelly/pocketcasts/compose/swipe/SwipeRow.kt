package au.com.shiftyjelly.pocketcasts.compose.swipe

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.AnchoredDraggableDefaults
import androidx.compose.foundation.gestures.DraggableAnchors
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.anchoredDraggable
import androidx.compose.foundation.gestures.detectTapGestures
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
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.CustomAccessibilityAction
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
import kotlinx.coroutines.flow.drop
import androidx.compose.material.MaterialTheme as Material

private val ActionIconSize = 24.dp
private val ActionHorizontalPadding = 48.dp
private val FullSwipeMinThreshold = 200.dp
private val FullSwipeThresholdMargin = 24.dp

internal const val FULL_SWIPE_ANCHOR_MULTIPLIER = 2

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

internal fun swipeRowPositionalThreshold(
    distance: Float,
    actionWidthPx: Float,
    fullSwipeThresholdPx: Float,
) = if (distance > actionWidthPx) fullSwipeThresholdPx - actionWidthPx else distance / 2f

@Composable
fun SwipeRow(
    state: SwipeRowState,
    modifier: Modifier = Modifier,
    leadingAction: SwipeRowAction? = null,
    trailingAction: SwipeRowAction? = null,
    isSwipeEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val density = LocalDensity.current
    val actionWidthPx = with(density) { (ActionIconSize + ActionHorizontalPadding).toPx() }
    val fullSwipeThresholdPx = with(density) {
        maxOf(actionWidthPx + FullSwipeThresholdMargin.toPx(), FullSwipeMinThreshold.toPx())
    }
    val flingBehavior = AnchoredDraggableDefaults.flingBehavior(
        state = state.draggableState,
        positionalThreshold = remember(actionWidthPx, fullSwipeThresholdPx) {
            { distance -> swipeRowPositionalThreshold(distance, actionWidthPx, fullSwipeThresholdPx) }
        },
    )
    var rowWidthPx by remember { mutableIntStateOf(0) }

    val hasLeadingAction = leadingAction != null
    val hasTrailingAction = trailingAction != null
    val isLeadingFullSwipeEnabled = leadingAction?.isFullSwipeEnabled == true
    val isTrailingFullSwipeEnabled = trailingAction?.isFullSwipeEnabled == true
    val anchors = remember(rowWidthPx, actionWidthPx, hasLeadingAction, hasTrailingAction, isLeadingFullSwipeEnabled, isTrailingFullSwipeEnabled) {
        swipeRowAnchors(
            rowWidthPx = rowWidthPx.toFloat(),
            leadingWidthPx = actionWidthPx.takeIf { hasLeadingAction },
            trailingWidthPx = actionWidthPx.takeIf { hasTrailingAction },
            isLeadingFullSwipeEnabled = isLeadingFullSwipeEnabled,
            isTrailingFullSwipeEnabled = isTrailingFullSwipeEnabled,
        )
    }
    SideEffect {
        state.draggableState.updateAnchors(anchors)
    }

    val hapticFeedback = LocalHapticFeedback.current
    LaunchedEffect(state) {
        snapshotFlow { state.draggableState.targetValue }
            .drop(1)
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

    val latestLeadingAction by rememberUpdatedState(leadingAction)
    val latestTrailingAction by rememberUpdatedState(trailingAction)
    LaunchedEffect(state) {
        snapshotFlow { state.draggableState.settledValue }
            .drop(1)
            .collect { anchor ->
                when (anchor) {
                    SwipeRowAnchor.FullLeading -> latestLeadingAction?.onClick?.invoke(state)
                    SwipeRowAnchor.FullTrailing -> latestTrailingAction?.onClick?.invoke(state)
                    else -> Unit
                }
            }
    }

    Box(
        modifier = modifier.onSizeChanged { rowWidthPx = it.width },
    ) {
        CompositionLocalProvider(LocalRippleConfiguration provides ActionRippleConfiguration) {
            if (leadingAction != null) {
                SwipeRowActionSlab(
                    action = leadingAction,
                    state = state,
                    rowWidthPx = rowWidthPx.toFloat(),
                    actionWidthPx = actionWidthPx,
                    isLeading = true,
                )
            }
            if (trailingAction != null) {
                SwipeRowActionSlab(
                    action = trailingAction,
                    state = state,
                    rowWidthPx = rowWidthPx.toFloat(),
                    actionWidthPx = actionWidthPx,
                    isLeading = false,
                )
            }
        }

        val actions = listOfNotNull(leadingAction, trailingAction)
        Box(
            modifier = Modifier
                .graphicsLayer { translationX = state.settledOffset }
                .anchoredDraggable(
                    state = state.draggableState,
                    orientation = Orientation.Horizontal,
                    enabled = isSwipeEnabled,
                    interactionSource = interactionSource,
                    flingBehavior = flingBehavior,
                )
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
private fun BoxScope.SwipeRowActionSlab(
    action: SwipeRowAction,
    state: SwipeRowState,
    rowWidthPx: Float,
    actionWidthPx: Float,
    isLeading: Boolean,
) {
    Box(
        contentAlignment = if (isLeading) AbsoluteAlignment.CenterRight else AbsoluteAlignment.CenterLeft,
        modifier = Modifier
            .matchParentSize()
            .graphicsLayer {
                val offset = state.settledOffset
                translationX = if (isLeading) (offset - rowWidthPx).coerceAtMost(0f) else (offset + rowWidthPx).coerceAtLeast(0f)
                clip = true
            }
            .background(action.backgroundColor)
            .clickable(
                enabled = state.isOpen,
                onClick = { action.onClick(state) },
            ),
    ) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .width(ActionIconSize + ActionHorizontalPadding)
                .fillMaxHeight()
                .graphicsLayer {
                    val revealed = state.settledOffset.absoluteValue.coerceAtMost(actionWidthPx)
                    val shift = (actionWidthPx - revealed) / 2f
                    translationX = if (isLeading) shift else -shift
                },
        ) {
            Icon(
                painter = painterResource(action.iconId),
                contentDescription = action.contentDescription,
                tint = action.iconTint,
                modifier = Modifier.size(ActionIconSize),
            )
        }
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

package au.com.shiftyjelly.pocketcasts.compose.swipe

import android.view.HapticFeedbackConstants
import android.view.ViewConfiguration
import android.widget.OverScroller
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.DragInteraction
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.LocalRippleConfiguration
import androidx.compose.material.MaterialTheme
import androidx.compose.material.RippleConfiguration
import androidx.compose.material.ripple.RippleAlpha
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.AbsoluteAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChangeIgnoreConsumed
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewParameter
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastAny
import androidx.compose.ui.zIndex
import au.com.shiftyjelly.pocketcasts.compose.AppThemeWithBackground
import au.com.shiftyjelly.pocketcasts.compose.components.TextH40
import au.com.shiftyjelly.pocketcasts.compose.preview.ThemePreviewParameterProvider
import au.com.shiftyjelly.pocketcasts.compose.theme
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme

private val SwipeButtonWidth = 72.dp
private val SwipeIconSize = 24.dp
private val SwipeIconVerticalPadding = 12.dp

@Composable
fun SwipeRow(
    state: SwipeRowState,
    leadingActions: List<SwipeRowAction>,
    trailingActions: List<SwipeRowAction>,
    modifier: Modifier = Modifier,
    isSwipeEnabled: Boolean = true,
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val controller = remember(state, density) {
        SwipeRowController(
            scope = scope,
            scroller = OverScroller(context).apply { setFriction(ViewConfiguration.getScrollFriction() * 5) },
            buttonWidthPx = with(density) { SwipeButtonWidth.roundToPx().toFloat() },
            swipedThresholdMarginPx = (24 * density.density).toInt().toFloat(),
            minSwipedThresholdPx = (200 * density.density).toInt().toFloat(),
            minFlingVelocity = ViewConfiguration.get(context).scaledMinimumFlingVelocity.toFloat(),
            performHapticFeedback = { view.performHapticFeedback(HapticFeedbackConstants.CONTEXT_CLICK) },
            onSwipeAction = { action -> action.onClick(state) },
        )
    }
    DisposableEffect(state, controller) {
        state.controller = controller
        onDispose {
            if (state.controller === controller) {
                state.controller = null
            }
        }
    }
    val shownLeadingActions = if (controller.isLocked) controller.leadingActions else leadingActions.take(3)
    val shownTrailingActions = if (controller.isLocked) controller.trailingActions else trailingActions.take(3)
    SideEffect {
        controller.updateActions(shownLeadingActions, shownTrailingActions)
    }
    LaunchedEffect(controller, isSwipeEnabled) {
        controller.updateSwipeEnabled(isSwipeEnabled)
    }

    val interactionSource = remember { MutableInteractionSource() }
    LaunchedEffect(interactionSource, view) {
        interactionSource.interactions.collect { interaction ->
            when (interaction) {
                is DragInteraction.Start -> view.parent?.requestDisallowInterceptTouchEvent(true)
                is DragInteraction.Stop, is DragInteraction.Cancel -> view.parent?.requestDisallowInterceptTouchEvent(false)
                else -> Unit
            }
        }
    }

    Box(
        modifier = modifier
            .clipToBounds()
            .onSizeChanged { controller.onLayout(it.width) }
            .draggable(
                state = rememberDraggableState(controller::onDrag),
                orientation = Orientation.Horizontal,
                enabled = isSwipeEnabled,
                interactionSource = interactionSource,
                onDragStarted = { controller.onDragStarted() },
                onDragStopped = { velocity -> controller.onDragStopped(velocity) },
            ),
    ) {
        SwipeButtons(shownLeadingActions, SwipeDirection.LeftToRight, controller)
        SwipeButtons(shownTrailingActions, SwipeDirection.RightToLeft, controller)
        Box(
            modifier = Modifier
                .graphicsLayer { translationX = controller.translation }
                .pointerInput(controller) {
                    awaitEachGesture {
                        awaitFirstDown(requireUnconsumed = false, pass = PointerEventPass.Final)
                        var isDragged = false
                        do {
                            val event = awaitPointerEvent(PointerEventPass.Final)
                            isDragged = isDragged || event.changes.fastAny { it.isConsumed && it.positionChangeIgnoreConsumed() != Offset.Zero }
                        } while (event.changes.fastAny { it.pressed })
                        if (!isDragged) {
                            controller.settle()
                        }
                    }
                },
        ) {
            content()
        }
    }
}

@Composable
private fun BoxScope.SwipeButtons(
    actions: List<SwipeRowAction>,
    side: SwipeDirection,
    controller: SwipeRowController,
) {
    val isLeading = side == SwipeDirection.LeftToRight
    val coefficient = swipeButtonsCoefficient(actions.size)
    val iconOffset = if (isLeading) controller.leadingIconOffset else controller.trailingIconOffset
    val isFading = controller.fadingSide == side
    for (index in actions.indices.reversed()) {
        val action = actions[index]
        CompositionLocalProvider(LocalRippleConfiguration provides RippleConfiguration(color = action.iconTint, rippleAlpha = SwipeButtonRippleAlpha)) {
            Box(
                contentAlignment = if (isLeading) AbsoluteAlignment.CenterRight else AbsoluteAlignment.CenterLeft,
                modifier = Modifier
                    .matchParentSize()
                    .zIndex(if (index == 0 && isFading) 1f else 0f)
                    .graphicsLayer {
                        val width = size.width
                        val offset = if (isLeading) -width else width
                        val buttonsTranslation = controller.buttonsTranslation
                        translationX = when {
                            index > 0 && isFading -> width
                            index == 0 -> offset + buttonsTranslation * coefficient / 2
                            index == 1 -> offset + buttonsTranslation * coefficient
                            else -> offset + buttonsTranslation
                        }
                        alpha = if (index == 0 && isFading) controller.fadeAlpha else 1f
                    }
                    .graphicsLayer { translationX = if (index == 0) iconOffset.value else 0f }
                    .background(action.backgroundColor)
                    .clickable { controller.onButtonClick(action) }
                    .semantics { contentDescription = action.contentDescription },
            ) {
                Icon(
                    painter = painterResource(action.iconId),
                    contentDescription = null,
                    tint = action.iconTint,
                    modifier = Modifier
                        .padding(horizontal = (SwipeButtonWidth - SwipeIconSize) / 2, vertical = SwipeIconVerticalPadding)
                        .size(SwipeIconSize),
                )
            }
        }
    }
}

private val SwipeButtonRippleAlpha = RippleAlpha(
    draggedAlpha = 78 / 255f,
    focusedAlpha = 78 / 255f,
    hoveredAlpha = 78 / 255f,
    pressedAlpha = 78 / 255f,
)

@Preview
@Composable
private fun MultiActionSwipeRowPreview(
    @PreviewParameter(ThemePreviewParameterProvider::class) themeType: Theme.ThemeType,
) {
    AppThemeWithBackground(themeType) {
        SwipeRow(
            state = rememberSwipeRowState(),
            leadingActions = listOf(SwipeRowActionDefaults.share(onClick = {})),
            trailingActions = listOf(SwipeRowActionDefaults.delete(onClick = {}), SwipeRowActionDefaults.share(onClick = {})),
        ) {
            TextH40(
                text = "Swipe me",
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.theme.colors.primaryUi01)
                    .padding(16.dp),
            )
        }
    }
}

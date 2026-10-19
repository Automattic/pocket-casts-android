package au.com.shiftyjelly.pocketcasts.chat.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Snackbar
import androidx.compose.material.SnackbarData
import androidx.compose.material.SnackbarHostState
import androidx.compose.material.Text
import androidx.compose.material.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import au.com.shiftyjelly.pocketcasts.compose.components.ThemedSnackbarHost
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sign
import kotlinx.coroutines.launch

@Composable
internal fun ChatSnackbarHost(
    hostState: SnackbarHostState,
    theme: ChatTheme,
    onDragChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    ThemedSnackbarHost(
        hostState = hostState,
        snackbar = { data, _ ->
            SwipeToDismissToast(onDismiss = data::dismiss, onDragChange = onDragChange) {
                ChatSnackbar(data = data, theme = theme)
            }
        },
        modifier = modifier,
    )
}

@Composable
private fun ChatSnackbar(
    data: SnackbarData,
    theme: ChatTheme,
) {
    Snackbar(
        backgroundColor = Color.White,
        contentColor = theme.onWhiteText,
        action = data.actionLabel?.let { label ->
            {
                TextButton(onClick = data::performAction) {
                    Text(
                        text = label,
                        color = theme.toastAction,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        letterSpacing = 0.sp,
                        fontWeight = FontWeight.SemiBold,
                    )
                }
            }
        },
        elevation = 0.dp,
        modifier = if (data.actionLabel != null) Modifier.heightIn(min = 56.dp) else Modifier,
    ) {
        Text(
            text = data.message,
            fontSize = 13.sp,
            lineHeight = 18.sp,
            letterSpacing = 0.sp,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

@Composable
private fun SwipeToDismissToast(
    onDismiss: () -> Unit,
    onDragChange: (Boolean) -> Unit,
    content: @Composable () -> Unit,
) {
    val offsetX = remember { Animatable(0f) }
    val currentOnDragChange by rememberUpdatedState(onDragChange)
    DisposableEffect(Unit) {
        onDispose { currentOnDragChange(false) }
    }
    var width by remember { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()
    val dismissVelocity = with(LocalDensity.current) { DismissVelocity.toPx() }
    Box(
        modifier = Modifier
            .padding(start = 16.dp, end = 16.dp, bottom = 16.dp)
            .onSizeChanged { width = it.width }
            .offset { IntOffset(offsetX.value.roundToInt(), 0) }
            .graphicsLayer { alpha = 1f - (abs(offsetX.value) / width.coerceAtLeast(1)).coerceIn(0f, 1f) }
            .draggable(
                orientation = Orientation.Horizontal,
                state = rememberDraggableState { delta ->
                    scope.launch { offsetX.snapTo(offsetX.value + delta) }
                },
                onDragStarted = { onDragChange(true) },
                onDragStopped = { velocity ->
                    onDragChange(false)
                    val isFarEnough = width > 0 && abs(offsetX.value) > width * DISMISS_DISTANCE_FRACTION
                    val isFlungAway = abs(velocity) > dismissVelocity && sign(velocity) != -sign(offsetX.value)
                    val isFlungBack = abs(velocity) > dismissVelocity && sign(velocity) == -sign(offsetX.value)
                    if (isFlungAway || (isFarEnough && !isFlungBack)) {
                        val direction = if (isFlungAway) sign(velocity) else sign(offsetX.value)
                        offsetX.animateTo(direction * width)
                        onDismiss()
                    } else {
                        offsetX.animateTo(0f)
                    }
                },
            ),
    ) {
        content()
    }
}

private const val DISMISS_DISTANCE_FRACTION = 0.4f
private val DismissVelocity = 1000.dp

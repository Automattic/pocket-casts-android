package au.com.shiftyjelly.pocketcasts.compose.extensions

import android.view.KeyEvent
import androidx.compose.animation.core.Spring.StiffnessMediumLow
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

private const val Black60 = 0x99000000

// From https://stackoverflow.com/a/71376469/1910286
fun Modifier.brush(brush: Brush) = this
    .graphicsLayer(alpha = 0.99f)
    .drawWithCache {
        onDrawWithContent {
            drawContent()
            drawRect(brush, blendMode = BlendMode.SrcAtop)
        }
    }

/**
 * When the user presses enter run the action.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.onEnter(onEnter: () -> Unit): Modifier =
    this.onPreviewKeyEvent {
        if (it.key == Key.Enter && it.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
            onEnter()
            true
        } else {
            false
        }
    }

/**
 * When the user presses tab move the focus to the next field.
 */
@OptIn(ExperimentalComposeUiApi::class)
fun Modifier.onTabMoveFocus(): Modifier = composed {
    val focusManager = LocalFocusManager.current
    this.onPreviewKeyEvent {
        if (it.key == Key.Tab && it.nativeKeyEvent.action == KeyEvent.ACTION_DOWN) {
            focusManager.moveFocus(FocusDirection.Down)
            true
        } else {
            false
        }
    }
}

fun Modifier.gradientBackground(
    baseColor: Color,
    colorStops: List<Color> = listOf(Color.Black, Color(Black60)),
    direction: FadeDirection = FadeDirection.TopToBottom,
) =
    graphicsLayer {
        /*
        https://rb.gy/iju6fn
        This is required to render to an offscreen buffer
        The Clear blend mode will not work without it */
        alpha = 0.99f
    }.drawWithContent {
        drawRect(color = baseColor)
        drawRect(
            brush = Brush.verticalGradient(
                colorStops,
                startY = if (direction == FadeDirection.BottomToTop) Float.POSITIVE_INFINITY else 0f,
                endY = if (direction == FadeDirection.BottomToTop) 0f else Float.POSITIVE_INFINITY,
            ),
            blendMode = BlendMode.DstIn,
        )
        drawContent()
    }

enum class FadeDirection {
    TopToBottom,
    BottomToTop,
}

fun Modifier.verticalScrollBar(
    scrollState: LazyListState,
    width: Dp = 4.dp,
    thumbColor: Color,
    contentPadding: PaddingValues = PaddingValues(0.dp),
) = composed {
    val density = LocalDensity.current
    val miScrollBarHeight = density.run { 50.dp.toPx() }
    val topPaddingPx = density.run { contentPadding.calculateTopPadding().toPx() }
    val bottomPaddingPx = density.run { contentPadding.calculateBottomPadding().toPx() }

    val thumbAlphaAnimated by animateFloatAsState(
        targetValue = if (scrollState.isScrollInProgress) 0.8f else 0f,
        animationSpec = tween(
            durationMillis = if (scrollState.isScrollInProgress) 150 else 1000,
            delayMillis = if (scrollState.isScrollInProgress) 0 else 500,
        ),
        label = "",
    )

    val firstIndex by animateFloatAsState(
        targetValue = scrollState.layoutInfo.visibleItemsInfo.firstOrNull()?.index?.toFloat() ?: 0f,
        animationSpec = spring(stiffness = StiffnessMediumLow),
        label = "",
    )

    val lastIndex by animateFloatAsState(
        targetValue = scrollState.layoutInfo.visibleItemsInfo.lastOrNull()?.index?.toFloat() ?: 0f,
        animationSpec = spring(stiffness = StiffnessMediumLow),
        label = "",
    )

    drawWithContent {
        drawContent()
        val itemsCount = scrollState.layoutInfo.totalItemsCount
        if (itemsCount > 0 && thumbAlphaAnimated > 0f) {
            val contentHeight = size.height - (topPaddingPx + bottomPaddingPx)
            val scrollbarTop = firstIndex / itemsCount * contentHeight + topPaddingPx
            val scrollBottom = (lastIndex + 1f) / itemsCount * contentHeight + bottomPaddingPx
            val scrollbarHeight = (scrollBottom - scrollbarTop).coerceAtLeast(miScrollBarHeight)
            drawRect(
                color = thumbColor,
                topLeft = Offset(size.width - width.toPx(), scrollbarTop),
                size = Size(width.toPx(), scrollbarHeight),
                alpha = thumbAlphaAnimated,
            )
        }
    }
}

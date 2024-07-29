package au.com.shiftyjelly.pocketcasts.compose.extensions

import android.view.KeyEvent
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalFocusManager

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

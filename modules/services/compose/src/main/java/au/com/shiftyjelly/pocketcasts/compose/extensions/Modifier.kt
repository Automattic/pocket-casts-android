package au.com.shiftyjelly.pocketcasts.compose.extensions

import android.view.KeyEvent
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.platform.LocalFocusManager

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

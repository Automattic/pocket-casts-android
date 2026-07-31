package au.com.shiftyjelly.pocketcasts.component

import android.view.KeyEvent.KEYCODE_DPAD_CENTER
import android.view.KeyEvent.KEYCODE_DPAD_LEFT
import android.view.KeyEvent.KEYCODE_DPAD_RIGHT
import android.view.KeyEvent.KEYCODE_ENTER
import android.view.KeyEvent.KEYCODE_NUMPAD_ENTER
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.nativeKeyCode
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.tv.material3.ButtonColors
import androidx.tv.material3.ButtonDefaults
import au.com.shiftyjelly.pocketcasts.theme.TvColors

@Stable
class TvTileButtonState(val buttonCount: Int) {
    var selectedIndex by mutableIntStateOf(0)
        private set

    var isFocused by mutableStateOf(false)
        private set

    private var consumedLastLeftRight = false

    fun onFocusChanged(focused: Boolean) {
        val wasFocused = isFocused
        isFocused = focused
        if (focused && !wasFocused) {
            selectedIndex = 0
        }
    }

    fun handleDpadDirection(keyCode: Int, isKeyDown: Boolean): Boolean {
        if (keyCode != KEYCODE_DPAD_LEFT && keyCode != KEYCODE_DPAD_RIGHT) return false

        if (isKeyDown) {
            consumedLastLeftRight = when (keyCode) {
                KEYCODE_DPAD_RIGHT -> {
                    if (selectedIndex < buttonCount - 1) {
                        selectedIndex++
                        true
                    } else {
                        false
                    }
                }

                KEYCODE_DPAD_LEFT -> {
                    if (selectedIndex > 0) {
                        selectedIndex--
                        true
                    } else {
                        false
                    }
                }

                else -> false
            }
            return consumedLastLeftRight
        } else {
            val consumed = consumedLastLeftRight
            consumedLastLeftRight = false
            return consumed
        }
    }

    fun isButtonSelected(index: Int): Boolean = isFocused && selectedIndex == index
}

@Composable
fun rememberTvTileButtonState(buttonCount: Int): TvTileButtonState {
    return remember(buttonCount) { TvTileButtonState(buttonCount) }
}

fun Modifier.tvTileButtonNavigation(
    state: TvTileButtonState,
    actions: List<() -> Unit>,
): Modifier = this
    .onFocusChanged { focusState ->
        state.onFocusChanged(focusState.hasFocus)
    }
    .onPreviewKeyEvent { keyEvent ->
        val keyCode = keyEvent.key.nativeKeyCode
        val isKeyDown = keyEvent.type == KeyEventType.KeyDown

        when (keyCode) {
            KEYCODE_DPAD_CENTER, KEYCODE_ENTER, KEYCODE_NUMPAD_ENTER -> {
                if (isKeyDown) {
                    actions.getOrNull(state.selectedIndex)?.invoke()
                }
                true
            }

            KEYCODE_DPAD_LEFT, KEYCODE_DPAD_RIGHT -> {
                state.handleDpadDirection(keyCode, isKeyDown)
            }

            else -> false
        }
    }

@Composable
fun tileButtonColors(isSelected: Boolean): ButtonColors = ButtonDefaults.colors(
    containerColor = if (isSelected) Color.White else TvColors.BgActive20,
    contentColor = if (isSelected) Color.Black else TvColors.TextSecondary,
    focusedContainerColor = TvColors.BgActive20,
    focusedContentColor = TvColors.TextSecondary,
)

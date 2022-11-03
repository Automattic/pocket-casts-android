package au.com.shiftyjelly.pocketcasts.compose.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.platform.LocalSoftwareKeyboardController

object Keyboard {

    @Composable
    @OptIn(ExperimentalComposeUiApi::class)
    fun hide() {
        LocalSoftwareKeyboardController.current?.hide()
    }

    @Composable
    @OptIn(ExperimentalComposeUiApi::class)
    fun show() {
        LocalSoftwareKeyboardController.current?.show()
    }
}

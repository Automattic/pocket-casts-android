package au.com.shiftyjelly.pocketcasts.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable

/**
 * Calls a function a single time. This can be useful for calling a
 * function when a composable is first shown even if there are
 * configuration changes. Compare to LaunchedEffect(Unit), which will
 * call the function again after configuration changes.
 *
 * @param onShown The function to call when the composable is first shown.
 */
@Composable
fun CallOnce(onShown: () -> Unit) {
    val shown = rememberSaveable { mutableStateOf(false) }
    if (!shown.value) {
        onShown()
    }
    shown.value = true
}

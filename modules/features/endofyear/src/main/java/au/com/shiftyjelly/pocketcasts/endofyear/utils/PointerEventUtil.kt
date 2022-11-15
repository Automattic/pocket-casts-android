package au.com.shiftyjelly.pocketcasts.endofyear.utils

import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.util.fastAll

suspend fun AwaitPointerEventScope.waitForUpInitial(): PointerInputChange {
    while (true) {
        /* PointerEventPass.Initial: Allows parent to consume aspects of PointerInputChange before children. */
        val event = awaitPointerEvent(PointerEventPass.Initial)
        if (event.changes.fastAll { it.changedToUp() }) {
            return event.changes[0]
        }
    }
}

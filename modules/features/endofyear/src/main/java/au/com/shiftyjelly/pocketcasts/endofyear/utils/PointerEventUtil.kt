package au.com.shiftyjelly.pocketcasts.endofyear.utils

import androidx.compose.ui.input.pointer.AwaitPointerEventScope
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.PointerInputChange
import androidx.compose.ui.input.pointer.changedToUp
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.util.fastAll
import androidx.compose.ui.util.fastAny
import kotlin.math.absoluteValue

suspend fun AwaitPointerEventScope.waitForUpOrCancelInitial(): PointerInputChange? {
    while (true) {
        /* PointerEventPass.Initial: Allows parent to consume aspects of PointerInputChange before children. */
        val event = awaitPointerEvent(PointerEventPass.Initial)
        if (event.changes.fastAll { it.changedToUp() }) {
            return event.changes[0]
        }

        // Check for cancel by vertical position consumption due to scroll.
        val consumeCheck = awaitPointerEvent(PointerEventPass.Final)
        if (consumeCheck.changes.fastAny { it.positionChange().y.absoluteValue > 10 }) {
            return null
        }
    }
}

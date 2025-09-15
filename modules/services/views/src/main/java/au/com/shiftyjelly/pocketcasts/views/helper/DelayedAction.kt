package au.com.shiftyjelly.pocketcasts.views.helper

import android.view.View
import androidx.core.view.doOnDetach
import kotlin.time.Duration

class DelayedAction(
    private val view: View,
    private val delay: Duration,
    private val action: Runnable,
    private val onDetach: () -> Unit,
) {
    fun run() {
        view.postDelayed(action, delay.inWholeMilliseconds)
        view.doOnDetach {
            cancel()
            onDetach()
        }
    }

    fun cancel() {
        view.removeCallbacks(action)
    }
}

fun View.runDelayedAction(
    delay: Duration,
    onDetach: () -> Unit = {},
    action: Runnable,
): DelayedAction {
    return DelayedAction(this, delay, action, onDetach).also(DelayedAction::run)
}

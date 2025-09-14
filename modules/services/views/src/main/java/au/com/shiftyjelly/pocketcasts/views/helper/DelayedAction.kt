package au.com.shiftyjelly.pocketcasts.views.helper

import android.view.View
import androidx.core.view.doOnDetach
import kotlin.time.Duration

class DelayedAction(
    private val view: View,
    private val delay: Duration,
    private val action: Runnable,
) {
    fun run() {
        view.postDelayed(action, delay.inWholeMilliseconds)
        view.doOnDetach { view.removeCallbacks(action) }
    }

    fun cancel() {
        view.removeCallbacks(action)
    }
}

fun View.runDelayedAction(delay: Duration, action: Runnable): DelayedAction {
    return DelayedAction(this, delay, action).also(DelayedAction::run)
}

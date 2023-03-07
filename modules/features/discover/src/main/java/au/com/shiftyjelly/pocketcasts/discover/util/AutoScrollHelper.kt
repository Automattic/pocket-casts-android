package au.com.shiftyjelly.pocketcasts.discover.util

import java.util.Timer
import java.util.TimerTask

private const val AUTO_SCROLL_INTERVAL = 3000L

class AutoScrollHelper(private val onAutoScrollCompleted: () -> Unit) {
    private var autoScrollTimer: Timer? = null
    private var autoScrollTimerTask: TimerTask? = null
    private var skipAutoScroll = false

    fun startAutoScrollTimer() {
        if (autoScrollTimerTask != null) return
        autoScrollTimerTask = object : TimerTask() {
            override fun run() {
                if (!skipAutoScroll) onAutoScrollCompleted()
                skipAutoScroll = false
            }
        }
        autoScrollTimer = Timer().apply {
            schedule(autoScrollTimerTask, 0, AUTO_SCROLL_INTERVAL)
        }
    }

    fun stopAutoScrollTimer() {
        autoScrollTimer?.cancel()
        autoScrollTimerTask?.cancel()
        autoScrollTimer = null
        autoScrollTimerTask = null
    }

    fun skipAutoScroll() {
        skipAutoScroll = true
    }
}

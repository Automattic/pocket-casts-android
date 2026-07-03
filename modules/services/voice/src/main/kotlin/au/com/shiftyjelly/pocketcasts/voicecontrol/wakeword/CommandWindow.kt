package au.com.shiftyjelly.pocketcasts.voicecontrol.wakeword

class CommandWindow(
    private val conversationTimeoutMs: Long = 30_000L,
) {
    private var isOpen = false
    private var lastActivityTimeMs: Long = 0L

    val isActive: Boolean
        get() {
            if (!isOpen) return false
            if (System.currentTimeMillis() - lastActivityTimeMs > conversationTimeoutMs) {
                isOpen = false
                return false
            }
            return true
        }

    fun onWakeWord(): Boolean {
        if (isOpen) {
            // Already open — refresh the window
            lastActivityTimeMs = System.currentTimeMillis()
            return true
        }
        isOpen = true
        lastActivityTimeMs = System.currentTimeMillis()
        return true
    }

    fun onActivity() {
        if (isOpen) {
            lastActivityTimeMs = System.currentTimeMillis()
        }
    }

    fun close() {
        isOpen = false
        lastActivityTimeMs = 0L
    }

    fun reset() {
        isOpen = false
        lastActivityTimeMs = 0L
    }
}

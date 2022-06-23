package au.com.shiftyjelly.pocketcasts.utils.extensions

fun Throwable?.anyMessageContains(text: String): Boolean {
    if (this == null) {
        return false
    }
    if (message?.contains(text) == true) {
        return true
    }

    var cause: Throwable? = this
    while (cause != null && cause.cause !== cause) {
        cause = cause.cause
        if (cause?.message?.contains(text) == true) {
            return true
        }
    }

    return false
}

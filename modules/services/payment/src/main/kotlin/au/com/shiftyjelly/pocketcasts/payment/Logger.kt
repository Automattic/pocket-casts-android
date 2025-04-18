package au.com.shiftyjelly.pocketcasts.payment

interface Logger {
    fun info(message: String)
    fun warning(message: String)
    fun error(message: String, exception: Throwable)
}

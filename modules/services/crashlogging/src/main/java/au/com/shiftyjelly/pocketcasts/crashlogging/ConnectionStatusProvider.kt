package au.com.shiftyjelly.pocketcasts.crashlogging

fun interface ConnectionStatusProvider {
    fun isConnected(): Boolean
}

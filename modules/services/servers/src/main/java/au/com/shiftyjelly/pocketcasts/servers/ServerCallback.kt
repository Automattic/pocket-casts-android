package au.com.shiftyjelly.pocketcasts.servers

interface ServerCallback<T> {

    fun callFailed(errorCode: Int, userMessage: String?, serverMessageId: String?, serverMessage: String?, throwable: Throwable?)

    fun dataReturned(result: T?)
}

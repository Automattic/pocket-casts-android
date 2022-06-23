package au.com.shiftyjelly.pocketcasts.servers

interface ServerCallback<T> {

    fun callFailed(errorCode: Int, userMessage: String?, userMessageId: Int?, serverMessage: String?, throwable: Throwable?)

    fun dataReturned(result: T?)
}

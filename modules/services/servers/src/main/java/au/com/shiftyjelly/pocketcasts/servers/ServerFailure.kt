package au.com.shiftyjelly.pocketcasts.servers

interface ServerFailure {
    fun onFailed(
        errorCode: Int,
        userMessage: String?,
        serverMessageId: String?,
        serverMessage: String?,
        throwable: Throwable?
    )
}

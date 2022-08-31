package au.com.shiftyjelly.pocketcasts.servers

interface PostCallback {
    fun onSuccess(data: String?, response: ServerResponse)
    fun onFailed(
        errorCode: Int,
        userMessage: String?,
        serverMessageId: String?,
        serverMessage: String?,
        throwable: Throwable?
    )
}

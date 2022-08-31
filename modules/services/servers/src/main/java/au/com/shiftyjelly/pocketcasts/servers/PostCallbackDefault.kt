package au.com.shiftyjelly.pocketcasts.servers

class PostCallbackDefault(private val callback: ServerCallback<String>) : PostCallback {

    override fun onSuccess(data: String?, response: ServerResponse) {
        callback.dataReturned(data)
    }

    override fun onFailed(
        errorCode: Int,
        userMessage: String?,
        serverMessageId: String?,
        serverMessage: String?,
        throwable: Throwable?
    ) {
        callback.callFailed(
            errorCode = errorCode,
            userMessage = userMessage,
            serverMessageId = serverMessageId,
            serverMessage = serverMessage,
            throwable = throwable
        )
    }
}

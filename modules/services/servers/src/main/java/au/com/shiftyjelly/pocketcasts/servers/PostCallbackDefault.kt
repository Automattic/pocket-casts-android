package au.com.shiftyjelly.pocketcasts.servers

class PostCallbackDefault(private val callback: ServerCallback<String>) : PostCallback {

    override fun onSuccess(data: String?, response: ServerResponse) {
        callback.dataReturned(data)
    }

    override fun onFailed(errorCode: Int, userMessage: String?, userMessageId: Int?, serverMessage: String?, throwable: Throwable?) {
        callback.callFailed(errorCode, userMessage, userMessageId, serverMessage, throwable)
    }
}

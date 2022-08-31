package au.com.shiftyjelly.pocketcasts.servers

data class ServerResponse(
    var message: String? = null,
    var serverMessageId: String? = null,
    var success: Boolean,
    var polling: Boolean = false,
    var data: String? = null,
    var errorCode: Int = ERROR_CODE_NO_ERROR_CODE,
    var token: String? = null
) {

    companion object {
        const val ERROR_CODE_NO_ERROR_CODE = 0
    }

    fun requiresPolling(): Boolean {
        return polling
    }
}

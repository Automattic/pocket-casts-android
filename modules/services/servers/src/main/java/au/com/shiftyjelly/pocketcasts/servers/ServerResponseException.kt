package au.com.shiftyjelly.pocketcasts.servers

class ServerResponseException(
    val errorCode: Int,
    val userMessage: String?,
    val serverMessageId: String?,
    val serverMessage: String?,
    cause: Throwable?,
) : RuntimeException(cause)

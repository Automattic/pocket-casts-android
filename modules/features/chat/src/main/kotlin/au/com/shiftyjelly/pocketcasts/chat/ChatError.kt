package au.com.shiftyjelly.pocketcasts.chat

sealed class ChatError {
    data object ServerError : ChatError()
    data object NetworkError : ChatError()
}

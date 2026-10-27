package au.com.shiftyjelly.pocketcasts.chat

import au.com.shiftyjelly.pocketcasts.repositories.chat.ChatMessage

internal data class ChatRatedAnswer(
    val question: String?,
    val answer: String,
)

internal fun List<ChatMessage>.ratedAnswer(answerUuid: String): ChatRatedAnswer? {
    val answerIndex = indexOfFirst { it.uuid == answerUuid }
    if (answerIndex < 0) return null
    val answer = drop(answerIndex)
        .takeWhile { it !is ChatMessage.User }
        .mapNotNull { message ->
            when (message) {
                is ChatMessage.Assistant -> message.text
                is ChatMessage.Quote -> message.text
                is ChatMessage.User -> null
            }
        }
        .joinToString(separator = "\n\n")
    val question = (getOrNull(answerIndex - 1) as? ChatMessage.User)?.text
    return ChatRatedAnswer(
        question = question?.truncatedForAnalytics(),
        answer = answer.truncatedForAnalytics(),
    )
}

internal fun String.truncatedForAnalytics(): String {
    if (length <= MAX_ANALYTICS_TEXT_LENGTH) return this
    val end = if (this[MAX_ANALYTICS_TEXT_LENGTH - 1].isHighSurrogate()) MAX_ANALYTICS_TEXT_LENGTH - 1 else MAX_ANALYTICS_TEXT_LENGTH
    return substring(0, end)
}

private const val MAX_ANALYTICS_TEXT_LENGTH = 1000

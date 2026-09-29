package au.com.shiftyjelly.pocketcasts.repositories.whatsnew

data class WhatsNewReadState(
    val readMessageIds: Set<String> = emptySet(),
    val seenMessageIds: Set<String> = emptySet(),
    val listedMessageIds: Set<String> = emptySet(),
    val respondedPollIds: Set<String> = emptySet(),
) {
    fun isRead(messageId: String) = messageId in readMessageIds

    fun isUnseen(messageId: String) = !isRead(messageId) && messageId !in seenMessageIds

    fun isListed(messageId: String) = messageId in listedMessageIds

    fun hasRespondedTo(pollId: String) = pollId in respondedPollIds
}

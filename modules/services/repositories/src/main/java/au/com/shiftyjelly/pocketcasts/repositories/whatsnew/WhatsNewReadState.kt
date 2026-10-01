package au.com.shiftyjelly.pocketcasts.repositories.whatsnew

import au.com.shiftyjelly.pocketcasts.servers.whatsnew.WhatsNewMessage
import java.time.Instant

data class WhatsNewReadState(
    val readMessageIds: Set<String> = emptySet(),
    val seenMessageIds: Set<String> = emptySet(),
    val listedMessageIds: Set<String> = emptySet(),
    val respondedPollIds: Set<String> = emptySet(),
    val feedStartDate: Instant? = null,
) {
    fun isRead(message: WhatsNewMessage): Boolean {
        if (message.id in readMessageIds) return true
        val feedStartDate = feedStartDate ?: return false
        return message.publishedAt.isBefore(feedStartDate)
    }

    fun isUnseen(message: WhatsNewMessage) = !isRead(message) && message.id !in seenMessageIds

    fun isUnlisted(message: WhatsNewMessage) = !isRead(message) && message.id !in listedMessageIds

    fun hasRespondedTo(pollId: String) = pollId in respondedPollIds
}

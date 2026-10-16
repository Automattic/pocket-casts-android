package au.com.shiftyjelly.pocketcasts.chat

import au.com.shiftyjelly.pocketcasts.models.to.EpisodeChatHistoryItem
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class ChatHistoryGroup {
    Today,
    Yesterday,
    PreviousSevenDays,
    Older,
}

data class ChatHistorySection(
    val group: ChatHistoryGroup,
    val items: List<EpisodeChatHistoryItem>,
)

internal fun List<EpisodeChatHistoryItem>.toHistorySections(now: Instant, zone: ZoneId): List<ChatHistorySection> {
    val today = now.atZone(zone).toLocalDate()
    return groupBy { item ->
        val day = item.lastMessageAt.toInstant().atZone(zone).toLocalDate()
        when (ChronoUnit.DAYS.between(day, today)) {
            in Long.MIN_VALUE..0L -> ChatHistoryGroup.Today
            1L -> ChatHistoryGroup.Yesterday
            in 2L..7L -> ChatHistoryGroup.PreviousSevenDays
            else -> ChatHistoryGroup.Older
        }
    }.toSortedMap().map { (group, items) -> ChatHistorySection(group, items) }
}

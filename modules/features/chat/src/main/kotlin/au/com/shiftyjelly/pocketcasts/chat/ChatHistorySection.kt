package au.com.shiftyjelly.pocketcasts.chat

import au.com.shiftyjelly.pocketcasts.models.to.EpisodeChatHistoryItem
import com.automattic.eventhorizon.EpisodeChatHistorySectionType
import java.time.Instant
import java.time.ZoneId
import java.time.temporal.ChronoUnit

enum class ChatHistoryGroup(val analyticsValue: EpisodeChatHistorySectionType) {
    Today(EpisodeChatHistorySectionType.Today),
    Yesterday(EpisodeChatHistorySectionType.Yesterday),
    PreviousSevenDays(EpisodeChatHistorySectionType.Previous7Days),
    Older(EpisodeChatHistorySectionType.Older),
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

internal fun List<ChatHistorySection>.rowOf(episodeUuid: String): Pair<ChatHistoryGroup, Long>? {
    var rowsBefore = 0L
    for (section in this) {
        val index = section.items.indexOfFirst { it.episodeUuid == episodeUuid }
        if (index >= 0) return section.group to rowsBefore + index + 1
        rowsBefore += section.items.size
    }
    return null
}

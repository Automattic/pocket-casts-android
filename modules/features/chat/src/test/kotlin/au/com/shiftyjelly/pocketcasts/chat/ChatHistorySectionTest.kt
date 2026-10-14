package au.com.shiftyjelly.pocketcasts.chat

import au.com.shiftyjelly.pocketcasts.models.to.EpisodeChatHistoryItem
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Test

class ChatHistorySectionTest {
    private val zone = ZoneId.of("Australia/Adelaide")
    private val now = LocalDateTime.of(2026, 10, 14, 9, 0).atZone(zone).toInstant()

    @Test
    fun `chats are grouped by the local day of their latest message`() {
        val items = listOf(
            item("today", LocalDateTime.of(2026, 10, 14, 0, 5)),
            item("yesterday", LocalDateTime.of(2026, 10, 13, 23, 55)),
            item("two-days", LocalDateTime.of(2026, 10, 12, 12, 0)),
            item("seven-days", LocalDateTime.of(2026, 10, 7, 1, 0)),
            item("eight-days", LocalDateTime.of(2026, 10, 6, 23, 0)),
        )

        val sections = items.toHistorySections(now, zone)

        assertEquals(
            listOf(
                ChatHistorySection(ChatHistoryGroup.Today, listOf(items[0])),
                ChatHistorySection(ChatHistoryGroup.Yesterday, listOf(items[1])),
                ChatHistorySection(ChatHistoryGroup.PreviousSevenDays, listOf(items[2], items[3])),
                ChatHistorySection(ChatHistoryGroup.Older, listOf(items[4])),
            ),
            sections,
        )
    }

    @Test
    fun `a message from the future counts as today`() {
        val item = item("future", LocalDateTime.of(2026, 10, 15, 8, 0))

        assertEquals(listOf(ChatHistorySection(ChatHistoryGroup.Today, listOf(item))), listOf(item).toHistorySections(now, zone))
    }

    @Test
    fun `no chats means no sections`() {
        assertEquals(emptyList<ChatHistorySection>(), emptyList<EpisodeChatHistoryItem>().toHistorySections(now, zone))
    }

    private fun item(uuid: String, lastMessageAt: LocalDateTime) = EpisodeChatHistoryItem(
        episodeUuid = uuid,
        episodeTitle = uuid,
        podcastUuid = "podcast-uuid",
        podcastTitle = "Podcast",
        episodeDuration = 0.0,
        lastMessageAt = Date.from(lastMessageAt.atZone(zone).toInstant()),
    )
}

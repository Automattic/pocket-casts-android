package au.com.shiftyjelly.pocketcasts.repositories.history.upnext

import au.com.shiftyjelly.pocketcasts.models.entity.UpNextHistoryEntry
import java.time.Instant
import java.util.Date

interface UpNextHistoryManager {
    suspend fun snapshotUpNext(date: Date = Date.from(Instant.now()))
    suspend fun findAllHistoryEntries(): List<UpNextHistoryEntry>
    suspend fun findEpisodeUuidsForDate(date: Date): List<String>
}

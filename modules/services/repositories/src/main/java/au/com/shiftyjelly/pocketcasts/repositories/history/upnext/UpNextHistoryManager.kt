package au.com.shiftyjelly.pocketcasts.repositories.history.upnext

import au.com.shiftyjelly.pocketcasts.models.entity.UpNextHistoryEntry
import java.util.Date

interface UpNextHistoryManager {
    suspend fun snapshotUpNext()
    suspend fun findAllHistoryEntries(): List<UpNextHistoryEntry>
    suspend fun findEpisodeUuidsForDate(date: Date): List<String>
}

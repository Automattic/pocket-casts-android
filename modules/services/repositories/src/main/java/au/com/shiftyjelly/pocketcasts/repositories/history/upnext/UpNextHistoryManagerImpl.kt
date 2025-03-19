package au.com.shiftyjelly.pocketcasts.repositories.history.upnext

import au.com.shiftyjelly.pocketcasts.models.db.dao.UpNextHistoryDao
import au.com.shiftyjelly.pocketcasts.utils.log.LogBuffer
import java.time.Duration
import java.time.Instant
import java.util.Date
import javax.inject.Inject
import timber.log.Timber

class UpNextHistoryManagerImpl @Inject constructor(
    private val upNextHistoryDao: UpNextHistoryDao,
) : UpNextHistoryManager {
    private val periodOfSnapshot: Duration = Duration.ofDays(14)

    override suspend fun snapshotUpNext() {
        try {
            val now = Instant.now()
            upNextHistoryDao.insertHistoryForDate(Date.from(now))
            upNextHistoryDao.deleteHistoryOnOrBeforeDate(Date.from(now.minus(periodOfSnapshot)))
        } catch (e: Exception) {
            val message = "Could not snapshot up next."
            Timber.e(e, message)
            LogBuffer.e(LogBuffer.TAG_INVALID_STATE, e, message)
        }
    }

    override suspend fun findAllHistoryEntries() =
        upNextHistoryDao.findAllHistoryEntries()

    override suspend fun findEpisodeUuidsForDate(date: Date) =
        upNextHistoryDao.findEpisodeUuidsForDate(date)
}

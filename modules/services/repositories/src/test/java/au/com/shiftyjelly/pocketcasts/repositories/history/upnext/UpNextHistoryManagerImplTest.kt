package au.com.shiftyjelly.pocketcasts.repositories.history.upnext

import au.com.shiftyjelly.pocketcasts.models.db.dao.UpNextHistoryDao
import java.time.Duration
import java.time.Instant
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.eq
import org.mockito.kotlin.verify

@ExperimentalCoroutinesApi
class UpNextHistoryManagerImplTest {
    private lateinit var upNextHistoryDao: UpNextHistoryDao
    private lateinit var upNextHistoryManager: UpNextHistoryManagerImpl
    private val periodOfSnapshot: Duration = Duration.ofDays(14)

    @Before
    fun setup() {
        upNextHistoryDao = mock()
        upNextHistoryManager = UpNextHistoryManagerImpl(upNextHistoryDao)
    }

    @Test
    fun `snapshot up next - insertion and deletion dates are correct`() = runTest {
        val now = Instant.now()
        val expectedDate = Date.from(now)
        val expectedDeletionDate = Date.from(now.minus(periodOfSnapshot))

        upNextHistoryManager.snapshotUpNext(Date.from(now))

        verify(upNextHistoryDao).insertHistoryForDate(eq(expectedDate))
        verify(upNextHistoryDao).deleteHistoryOnOrBeforeDate(eq(expectedDeletionDate))
    }
}

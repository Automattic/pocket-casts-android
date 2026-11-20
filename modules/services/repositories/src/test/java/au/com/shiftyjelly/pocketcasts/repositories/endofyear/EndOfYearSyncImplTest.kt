package au.com.shiftyjelly.pocketcasts.repositories.endofyear

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.podcast.HistoryManager
import au.com.shiftyjelly.pocketcasts.repositories.ratings.RatingsManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.servers.sync.history.HistoryYearResponse
import java.time.Clock
import java.time.Instant
import java.time.Year
import java.time.ZoneOffset
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class EndOfYearSyncImplTest {
    private val now = Instant.parse("2026-12-01T10:00:00Z")

    private val syncManager = mock<SyncManager> { on { isLoggedIn() } doReturn true }
    private val endOfYearManager = mock<EndOfYearManager>()
    private val lastSyncYear = mock<UserSetting<Int>> { on { value } doReturn 0 }
    private val lastSyncTimestamp = mock<UserSetting<Instant>> { on { value } doReturn Instant.EPOCH }
    private val settings = mock<Settings> {
        on { lastEoySyncYear } doReturn lastSyncYear
        on { lastEoySyncTimestamp } doReturn lastSyncTimestamp
        // Synced within the day, so the ratings are taken as they are.
        on { getLastModified() } doReturn now.toString()
    }

    private val sync = EndOfYearSyncImpl(
        syncManager = syncManager,
        historyManager = mock<HistoryManager>(),
        ratingsManager = mock<RatingsManager>(),
        endOfYearManager = endOfYearManager,
        settings = settings,
        clock = Clock.fixed(now, ZoneOffset.UTC),
    )

    @Test
    fun `syncs the history of the year and the year before`() = runTest {
        whenever(syncManager.historyYear(any(), any())).thenReturn(HistoryYearResponse(count = 0, history = null))
        whenever(endOfYearManager.getPlayedEpisodeCount(any())).thenReturn(0)

        assertTrue(sync.sync(Year.of(2026)))

        verify(syncManager).historyYear(year = 2026, count = true)
        verify(syncManager).historyYear(year = 2025, count = true)
        verify(lastSyncYear).set(eq(2026), eq(false), any(), any())
    }

    @Test
    fun `fails when the year before fails`() = runTest {
        whenever(syncManager.historyYear(year = 2026, count = true)).thenReturn(HistoryYearResponse(count = 0, history = null))
        whenever(syncManager.historyYear(year = 2025, count = true)).thenReturn(HistoryYearResponse(count = null, history = null))
        whenever(endOfYearManager.getPlayedEpisodeCount(any())).thenReturn(0)

        assertFalse(sync.sync(Year.of(2026)))

        verify(lastSyncYear, never()).set(any(), any(), any(), any())
    }
}

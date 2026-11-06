package au.com.shiftyjelly.pocketcasts.endofyear.playback2026

import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearManager
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearSync
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.PlaybackStats
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import java.time.Year
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.inOrder
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class Playback2026ViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val stats = PlaybackStats(
        podcasts = emptyList(),
        monthlySeconds = List(12) { 60.0 },
        lastYearSeconds = 0.0,
        startedEpisodeCount = 0,
        completedEpisodeCount = 0,
    )

    private val endOfYearSync = mock<EndOfYearSync>()
    private val endOfYearManager = mock<EndOfYearManager>()

    @Test
    fun `sync history before reading stats`() = runTest {
        whenever(endOfYearSync.sync(Year.of(2026))).thenReturn(true)
        whenever(endOfYearManager.getPlaybackStats(Year.of(2026))).thenReturn(stats)

        val viewModel = Playback2026ViewModel(endOfYearSync, endOfYearManager)

        assertEquals(stats.toPlaybackPayload(Year.of(2026)), viewModel.payload.value)
        inOrder(endOfYearSync, endOfYearManager) {
            verify(endOfYearSync).sync(Year.of(2026))
            verify(endOfYearManager).getPlaybackStats(Year.of(2026))
        }
    }

    @Test
    fun `use local stats when sync fails`() = runTest {
        whenever(endOfYearSync.sync(Year.of(2026))).thenReturn(false)
        whenever(endOfYearManager.getPlaybackStats(Year.of(2026))).thenReturn(stats)

        val viewModel = Playback2026ViewModel(endOfYearSync, endOfYearManager)

        assertEquals(stats.toPlaybackPayload(Year.of(2026)), viewModel.payload.value)
    }

    @Test
    fun `no payload while syncing`() = runTest {
        val sync = CompletableDeferred<Boolean>()
        val endOfYearSync = object : EndOfYearSync {
            override suspend fun sync(year: Year) = sync.await()
            override suspend fun reset() = Unit
        }
        whenever(endOfYearManager.getPlaybackStats(Year.of(2026))).thenReturn(stats)

        val viewModel = Playback2026ViewModel(endOfYearSync, endOfYearManager)
        assertNull(viewModel.payload.value)

        sync.complete(true)
        assertEquals(stats.toPlaybackPayload(Year.of(2026)), viewModel.payload.value)
    }
}

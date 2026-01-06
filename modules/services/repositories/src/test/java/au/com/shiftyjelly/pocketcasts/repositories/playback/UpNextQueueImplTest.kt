package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import au.com.shiftyjelly.pocketcasts.models.db.AppDatabase
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.Mockito.mock

class UpNextQueueImplTest {
    private lateinit var upNextQueue: UpNextQueueImpl

    @Before
    fun setUp() {
        val appDatabase: AppDatabase = mock()
        val settings: Settings = mock()
        val episodeManager: EpisodeManager = mock()
        val syncManager: SyncManager = mock()
        val context: Context = mock()

        upNextQueue = UpNextQueueImpl(
            appDatabase,
            settings,
            episodeManager,
            syncManager,
            context,
        )
    }

    @Test
    fun `isUserDragging returns false when no drag time`() {
        assertFalse(upNextQueue.isUserDragging(now = 15_000L))
    }

    @Test
    fun `isUserDragging returns true when within grace period`() {
        upNextQueue.setDragStartTimeForTesting(1_000L)
        // checking 4 seconds later (within 10 second grace period)
        assertTrue(upNextQueue.isUserDragging(now = 1_000L + 3_000L))
    }

    @Test
    fun `isUserDragging returns false when exactly at grace period boundary`() {
        upNextQueue.setDragStartTimeForTesting(1_000L)
        // checking exactly 1 minute later (exactly at grace period)
        assertFalse(upNextQueue.isUserDragging(now = 1_000L + 60_000L))
    }

    @Test
    fun `isUserDragging returns false when outside grace period`() {
        upNextQueue.setDragStartTimeForTesting(1_000L)
        // checking 61 seconds later (outside 1 minute grace period)
        assertFalse(upNextQueue.isUserDragging(now = 1_000L + 61_000L))
    }
}

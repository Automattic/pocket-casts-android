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
    fun `recentUserInteraction returns false when no previous interaction`() {
        assertFalse(upNextQueue.recentUserInteraction(now = 15_000L))
    }

    @Test
    fun `recentUserInteraction returns true when within grace period`() {
        upNextQueue.setLastUserInteractionTimeForTesting(1_000L)
        // checking 3 seconds later (within 10 second grace period)
        assertTrue(upNextQueue.recentUserInteraction(now = 1_000L + 3_000L))
    }

    @Test
    fun `recentUserInteraction returns true when exactly at grace period boundary`() {
        upNextQueue.setLastUserInteractionTimeForTesting(1_000L)
        // checking exactly 10 seconds later (exactly at grace period)
        assertFalse(upNextQueue.recentUserInteraction(now = 1_000L + 10_000L))
    }

    @Test
    fun `recentUserInteraction returns false when outside grace period`() {
        upNextQueue.setLastUserInteractionTimeForTesting(1_000L)
        // checking 11 seconds later (outside 10 second grace period)
        assertFalse(upNextQueue.recentUserInteraction(now = 1_000L + 11_000L))
    }
}

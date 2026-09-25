package au.com.shiftyjelly.pocketcasts.repositories.podcast

import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.HistorySyncChange
import au.com.shiftyjelly.pocketcasts.models.to.HistorySyncResponse
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import java.util.Date
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.delay
import kotlinx.coroutines.rx2.rxSingle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@Config(manifest = Config.NONE)
@RunWith(RobolectricTestRunner::class)
class HistoryManagerTest {

    private val settings = mock<Settings>()

    // HISTORY-01: history sync must update only the interaction columns, never a full-row @Update
    // that could revert playedUpTo / download state written concurrently by playback.
    @Test
    fun `add updates only the interaction columns, not the whole row`() = runTest {
        val episodeManager = episodeManager(storedInteractionDate = 50L)

        historyManager(episodeManager).processServerResponse(
            response = response(action = HistoryManager.ACTION_ADD),
            updateServerModified = false,
        )

        verifyBlocking(episodeManager) {
            updatePlaybackInteraction(EPISODE_UUID, INTERACTION_DATE, PodcastEpisode.LAST_PLAYBACK_INTERACTION_SYNCED)
        }
        verify(episodeManager, never()).updateBlocking(anyOrNull())
    }

    @Test
    fun `delete clears the interaction date without a whole-row update`() = runTest {
        val episodeManager = episodeManager(storedInteractionDate = 50L)

        historyManager(episodeManager).processServerResponse(
            response = response(action = HistoryManager.ACTION_DELETE),
            updateServerModified = false,
        )

        verifyBlocking(episodeManager) {
            updatePlaybackInteraction(EPISODE_UUID, 0L, PodcastEpisode.LAST_PLAYBACK_INTERACTION_SYNCED)
        }
        verify(episodeManager, never()).updateBlocking(anyOrNull())
    }

    @Test
    fun `add does not write when the stored interaction is already newer`() = runTest {
        val episodeManager = episodeManager(storedInteractionDate = INTERACTION_DATE + 1)

        historyManager(episodeManager).processServerResponse(
            response = response(action = HistoryManager.ACTION_ADD),
            updateServerModified = false,
        )

        verifyBlocking(episodeManager, never()) {
            updatePlaybackInteraction(any(), any(), any())
        }
        verify(episodeManager, never()).updateBlocking(anyOrNull())
    }

    @Test
    fun `missing podcasts are added at most five at a time`() = runTest {
        val missingPodcastUuids = (1..12).map { index -> "missing-podcast-$index" }
        val activeAdds = AtomicInteger()
        val maxActiveAdds = AtomicInteger()
        val podcastManager = mock<PodcastManager> {
            on { findSubscribedUuids() } doReturn emptyList()
            on { addPodcastRxSingle(any(), any(), any(), any()) } doAnswer { invocation ->
                rxSingle {
                    maxActiveAdds.accumulateAndGet(activeAdds.incrementAndGet(), ::maxOf)
                    delay(20)
                    activeAdds.decrementAndGet()
                    Podcast(uuid = invocation.getArgument(0))
                }
            }
        }

        HistoryManager(podcastManager, episodeManager(storedInteractionDate = 50L), settings).processServerResponse(
            response = response(action = HistoryManager.ACTION_ADD, podcastUuids = missingPodcastUuids),
            updateServerModified = false,
        )

        missingPodcastUuids.forEach { podcastUuid ->
            verify(podcastManager).addPodcastRxSingle(podcastUuid, sync = false, subscribed = false, shouldAutoDownload = false)
        }
        assertTrue(maxActiveAdds.get() in 1..HistoryManager.ADD_PODCAST_CONCURRENCY)
    }

    @Test
    fun `a podcast that fails to add does not stop the history being processed`() = runTest {
        val episodeManager = episodeManager(storedInteractionDate = 50L)
        val podcastManager = mock<PodcastManager> {
            on { findSubscribedUuids() } doReturn emptyList()
            on { addPodcastRxSingle(any(), any(), any(), any()) } doReturn rxSingle<Podcast> { throw IllegalStateException("boom") }
        }

        HistoryManager(podcastManager, episodeManager, settings).processServerResponse(
            response = response(action = HistoryManager.ACTION_ADD),
            updateServerModified = true,
        )

        verifyBlocking(episodeManager) {
            updatePlaybackInteraction(EPISODE_UUID, INTERACTION_DATE, PodcastEpisode.LAST_PLAYBACK_INTERACTION_SYNCED)
        }
        verify(settings).setHistoryServerModified(100L)
    }

    private fun historyManager(episodeManager: EpisodeManager): HistoryManager {
        val podcastManager = mock<PodcastManager> {
            on { findSubscribedUuids() } doReturn listOf(PODCAST_UUID)
        }
        return HistoryManager(podcastManager, episodeManager, settings)
    }

    private fun episodeManager(storedInteractionDate: Long?): EpisodeManager {
        val episode = PodcastEpisode(
            uuid = EPISODE_UUID,
            podcastUuid = PODCAST_UUID,
            publishedDate = Date(),
            lastPlaybackInteraction = storedInteractionDate,
        )
        return mock {
            on { findByUuid(EPISODE_UUID) } doReturn episode
        }
    }

    private fun response(action: Int, podcastUuids: List<String> = listOf(PODCAST_UUID)) = HistorySyncResponse(
        serverModified = 100L,
        lastCleared = 0L,
        changes = podcastUuids.map { podcastUuid ->
            HistorySyncChange(
                action = action,
                episode = EPISODE_UUID,
                modifiedAt = INTERACTION_DATE.toString(),
                podcast = podcastUuid,
            )
        },
    )

    companion object {
        private const val EPISODE_UUID = "episode-uuid"
        private const val PODCAST_UUID = "podcast-uuid"
        private const val INTERACTION_DATE = 100L
    }
}

package au.com.shiftyjelly.pocketcasts.views.viewmodels

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.Mockito.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class PodcastSelectViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Test
    fun `should load selectable podcasts`() = runTest {
        val podcastManager: PodcastManager = mock()
        val analyticsTracker: AnalyticsTracker = mock()

        val uuid1 = "uuid1"
        val uuid2 = "uuid2"

        val podcast1 = Podcast(uuid = uuid1, title = "Apple")
        val podcast2 = Podcast(uuid = uuid2, title = "Zebra")

        whenever(podcastManager.findSubscribedBlocking()).thenReturn(listOf(podcast1, podcast2))

        val viewModel = PodcastSelectViewModel(podcastManager, analyticsTracker)

        viewModel.selectablePodcasts.test {
            viewModel.loadSelectablePodcasts(listOf(uuid1))

            skipItems(1)

            viewModel.loadSelectablePodcasts(listOf(uuid1))
            advanceUntilIdle()

            val result = awaitItem()

            assertEquals(2, result.size)

            assertEquals("Apple", result[0].podcast.title)
            assertTrue(result[0].selected)

            assertEquals("Zebra", result[1].podcast.title)
            assertFalse(result[1].selected)
        }
    }
}

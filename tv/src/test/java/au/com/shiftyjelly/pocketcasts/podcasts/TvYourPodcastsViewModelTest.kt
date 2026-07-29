package au.com.shiftyjelly.pocketcasts.podcasts

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

@OptIn(ExperimentalCoroutinesApi::class)
class TvYourPodcastsViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val subscribed = MutableSharedFlow<List<Podcast>>(replay = 1)
    private val podcastManager = mock<PodcastManager> {
        on { findSubscribedFlow() } doReturn subscribed
    }

    @Test
    fun `no subscriptions map to the empty state`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            assertEquals(TvYourPodcastsUiState.Loading, awaitItem())

            subscribed.emit(emptyList())

            assertEquals(TvYourPodcastsUiState.Empty, awaitItem())
        }
    }

    @Test
    fun `subscriptions are exposed sorted by title`() = runTest {
        val viewModel = createViewModel()
        val zebra = podcast("zebra", "Zebra Cast")
        val apple = podcast("apple", "Apple Cast")
        val theBeat = podcast("beat", "The Beat")

        viewModel.uiState.test {
            assertEquals(TvYourPodcastsUiState.Loading, awaitItem())

            subscribed.emit(listOf(zebra, apple, theBeat))

            assertEquals(TvYourPodcastsUiState.Loaded(listOf(apple, theBeat, zebra)), awaitItem())
        }
    }

    @Test
    fun `showing the tab refreshes podcasts`() = runTest {
        val viewModel = createViewModel()

        viewModel.onShown()

        verify(podcastManager).refreshPodcasts(any())
    }

    private fun createViewModel() = TvYourPodcastsViewModel(
        podcastManager = podcastManager,
    )

    private fun podcast(uuid: String, title: String) = Podcast(uuid = uuid, title = title)
}

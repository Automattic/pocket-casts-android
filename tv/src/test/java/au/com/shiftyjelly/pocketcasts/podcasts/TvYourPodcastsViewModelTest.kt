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
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

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
    fun `subscriptions preserve the order provided by the dao`() = runTest {
        val viewModel = createViewModel()
        val apple = podcast("apple", "Apple Cast")
        val theBeat = podcast("beat", "The Beat")
        val zebra = podcast("zebra", "Zebra Cast")

        viewModel.uiState.test {
            assertEquals(TvYourPodcastsUiState.Loading, awaitItem())

            subscribed.emit(listOf(apple, theBeat, zebra))

            assertEquals(TvYourPodcastsUiState.Loaded(listOf(apple, theBeat, zebra)), awaitItem())
        }
    }

    @Test
    fun `unsubscribing the last podcast falls back to the empty state`() = runTest {
        val viewModel = createViewModel()
        val apple = podcast("apple", "Apple Cast")

        viewModel.uiState.test {
            assertEquals(TvYourPodcastsUiState.Loading, awaitItem())

            subscribed.emit(listOf(apple))
            assertEquals(TvYourPodcastsUiState.Loaded(listOf(apple)), awaitItem())

            subscribed.emit(emptyList())
            assertEquals(TvYourPodcastsUiState.Empty, awaitItem())
        }
    }

    private fun createViewModel() = TvYourPodcastsViewModel(
        podcastManager = podcastManager,
    )

    private fun podcast(uuid: String, title: String) = Podcast(uuid = uuid, title = title)
}

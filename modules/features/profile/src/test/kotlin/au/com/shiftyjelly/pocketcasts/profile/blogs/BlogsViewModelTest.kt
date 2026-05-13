package au.com.shiftyjelly.pocketcasts.profile.blogs

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import com.automattic.eventhorizon.BlogsListPodcastTappedEvent
import com.automattic.eventhorizon.EventHorizon
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify

class BlogsViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val podcastsFlow = MutableStateFlow<List<Podcast>>(emptyList())
    private val bottomInsetFlow = MutableStateFlow(0)

    private val podcastManager = mock<PodcastManager> {
        on { observeSubscribedWebFeedPodcasts() } doReturn podcastsFlow
    }
    private val settings = mock<Settings> {
        on { bottomInset } doReturn bottomInsetFlow
    }
    private val eventHorizon = mock<EventHorizon>()

    private lateinit var viewModel: BlogsViewModel

    @Before
    fun setUp() {
        viewModel = BlogsViewModel(podcastManager, settings, eventHorizon)
    }

    @Test
    fun `blogPodcasts initial value is null before any subscription`() {
        assertEquals(null, viewModel.blogPodcasts.value)
    }

    @Test
    fun `bottomInset initial value is 0 before any subscription`() {
        assertEquals(0, viewModel.bottomInset.value)
    }

    @Test
    fun `blogPodcasts mirrors emissions from podcastManager`() = runTest {
        val initial = listOf(podcast("uuid-1"))
        val updated = listOf(podcast("uuid-1"), podcast("uuid-2"))
        podcastsFlow.value = initial

        viewModel.blogPodcasts.test {
            assertEquals(initial, expectMostRecentItem())

            podcastsFlow.value = updated
            assertEquals(updated, awaitItem())
        }
    }

    @Test
    fun `bottomInset mirrors emissions from settings`() = runTest {
        bottomInsetFlow.value = 80

        viewModel.bottomInset.test {
            assertEquals(80, expectMostRecentItem())

            bottomInsetFlow.value = 200
            assertEquals(200, awaitItem())
        }
    }

    @Test
    fun `onPodcastTapped tracks BlogsListPodcastTappedEvent with the podcast uuid`() {
        viewModel.onPodcastTapped("uuid-42")

        verify(eventHorizon).track(BlogsListPodcastTappedEvent(uuid = "uuid-42"))
    }

    private fun podcast(uuid: String) = Podcast(uuid = uuid, title = "Title $uuid")
}

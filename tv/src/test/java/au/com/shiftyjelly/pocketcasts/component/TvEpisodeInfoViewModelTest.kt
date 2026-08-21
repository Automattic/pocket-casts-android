package au.com.shiftyjelly.pocketcasts.component

import au.com.shiftyjelly.pocketcasts.component.TvEpisodeInfoViewModel.ShowNotes
import au.com.shiftyjelly.pocketcasts.component.TvEpisodeInfoViewModel.UiState
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.shownotes.ShowNotesManager
import au.com.shiftyjelly.pocketcasts.servers.shownotes.ShowNotesState
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import com.automattic.eventhorizon.EpisodeDetailShownEvent
import com.automattic.eventhorizon.EpisodeViewSourceType
import com.automattic.eventhorizon.EventHorizon
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class TvEpisodeInfoViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val podcastManager = mock<PodcastManager>()
    private val showNotesManager = mock<ShowNotesManager>()
    private val eventHorizon = mock<EventHorizon>()

    @Test
    fun `initial state is null`() = runTest {
        assertNull(createViewModel().uiState.value)
    }

    @Test
    fun `tracking shown records the detail shown event with the source`() = runTest {
        createViewModel().trackDetailShown(EpisodeViewSourceType.PodcastScreen)

        verify(eventHorizon).track(EpisodeDetailShownEvent(source = EpisodeViewSourceType.PodcastScreen))
    }

    @Test
    fun `load resolves the podcast title and show notes`() = runTest {
        stubTitle("Buzzcast")
        stubShowNotes(ShowNotesState.Loaded("<p>notes</p>"))
        val viewModel = createViewModel()

        viewModel.load(podcastUuid = "podcast", episodeUuid = "episode")

        assertEquals(
            UiState("episode", "Buzzcast", ShowNotes.Loaded("<p>notes</p>")),
            viewModel.uiState.value,
        )
    }

    @Test
    fun `an unknown podcast leaves the title null`() = runTest {
        stubShowNotes(ShowNotesState.Loaded("notes"))
        val viewModel = createViewModel()

        viewModel.load(podcastUuid = "podcast", episodeUuid = "episode")

        assertNull(viewModel.uiState.value?.podcastTitle)
    }

    @Test
    fun `missing show notes map to Unavailable`() = runTest {
        stubShowNotes(ShowNotesState.NotFound)
        val viewModel = createViewModel()

        viewModel.load(podcastUuid = "podcast", episodeUuid = "episode")

        assertEquals(ShowNotes.Unavailable, viewModel.uiState.value?.showNotes)
    }

    @Test
    fun `errored show notes map to Unavailable`() = runTest {
        stubShowNotes(ShowNotesState.Error(RuntimeException("boom")))
        val viewModel = createViewModel()

        viewModel.load(podcastUuid = "podcast", episodeUuid = "episode")

        assertEquals(ShowNotes.Unavailable, viewModel.uiState.value?.showNotes)
    }

    @Test
    fun `blank show notes map to Unavailable and can be retried`() = runTest {
        stubShowNotes(ShowNotesState.Loaded("   "))
        val viewModel = createViewModel()

        viewModel.load(podcastUuid = "podcast", episodeUuid = "episode")
        assertEquals(ShowNotes.Unavailable, viewModel.uiState.value?.showNotes)

        stubShowNotes(ShowNotesState.Loaded("<p>notes</p>"))
        viewModel.load(podcastUuid = "podcast", episodeUuid = "episode")

        assertEquals(ShowNotes.Loaded("<p>notes</p>"), viewModel.uiState.value?.showNotes)
        verifyBlocking(showNotesManager, times(2)) { loadShowNotes("podcast", "episode") }
    }

    @Test
    fun `loading the same episode again does not reload`() = runTest {
        stubShowNotes(ShowNotesState.Loaded("notes"))
        val viewModel = createViewModel()

        viewModel.load(podcastUuid = "podcast", episodeUuid = "episode")
        viewModel.load(podcastUuid = "podcast", episodeUuid = "episode")

        verifyBlocking(showNotesManager, times(1)) { loadShowNotes("podcast", "episode") }
    }

    @Test
    fun `loading a different episode reloads and replaces the state`() = runTest {
        stubTitle("Buzzcast")
        stubShowNotes(ShowNotesState.Loaded("notes"))
        val viewModel = createViewModel()

        viewModel.load(podcastUuid = "podcast", episodeUuid = "first")
        viewModel.load(podcastUuid = "podcast", episodeUuid = "second")

        assertEquals("second", viewModel.uiState.value?.episodeUuid)
        verifyBlocking(showNotesManager) { loadShowNotes("podcast", "first") }
        verifyBlocking(showNotesManager) { loadShowNotes("podcast", "second") }
    }

    @Test
    fun `a late failing load for a previous episode does not clear the current marker`() = runTest {
        stubTitle("Buzzcast")
        val firstNotes = CompletableDeferred<ShowNotesState>()
        whenever(showNotesManager.loadShowNotes(any(), eq("first"))).doSuspendableAnswer { firstNotes.await() }
        whenever(showNotesManager.loadShowNotes(any(), eq("second"))).doSuspendableAnswer { ShowNotesState.Loaded("notes") }
        val viewModel = createViewModel()

        viewModel.load(podcastUuid = "podcast", episodeUuid = "first")
        viewModel.load(podcastUuid = "podcast", episodeUuid = "second")
        firstNotes.complete(ShowNotesState.NotFound)

        viewModel.load(podcastUuid = "podcast", episodeUuid = "second")

        verifyBlocking(showNotesManager, times(1)) { loadShowNotes("podcast", "second") }
    }

    private suspend fun stubTitle(title: String) {
        whenever(podcastManager.findPodcastByUuid(any())).doSuspendableAnswer { Podcast(uuid = "podcast", title = title) }
    }

    private suspend fun stubShowNotes(state: ShowNotesState) {
        whenever(showNotesManager.loadShowNotes(any(), any())).doSuspendableAnswer { state }
    }

    private fun createViewModel() = TvEpisodeInfoViewModel(
        podcastManager = podcastManager,
        showNotesManager = showNotesManager,
        eventHorizon = eventHorizon,
    )
}

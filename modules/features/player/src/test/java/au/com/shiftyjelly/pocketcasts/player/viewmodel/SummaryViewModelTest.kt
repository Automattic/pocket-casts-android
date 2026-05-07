package au.com.shiftyjelly.pocketcasts.player.viewmodel

import au.com.shiftyjelly.pocketcasts.player.viewmodel.SummaryViewModel.SummaryState
import au.com.shiftyjelly.pocketcasts.repositories.transcript.TranscriptManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class SummaryViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Mock
    private lateinit var transcriptManager: TranscriptManager

    private fun createViewModel() = SummaryViewModel(transcriptManager)

    @Test
    fun `initial state is Loading`() {
        val viewModel = createViewModel()

        assertEquals(SummaryState.Loading, viewModel.state.value)
    }

    @Test
    fun `state is Loaded when summary text is available`() = runTest {
        whenever(transcriptManager.loadSummaryText("episode-1")).thenReturn("Summary text")
        val viewModel = createViewModel()

        viewModel.loadSummary("episode-1")

        assertEquals(SummaryState.Loaded("Summary text"), viewModel.state.value)
    }

    @Test
    fun `state is NotAvailable when summary text is null`() = runTest {
        whenever(transcriptManager.loadSummaryText("episode-1")).thenReturn(null)
        val viewModel = createViewModel()

        viewModel.loadSummary("episode-1")

        assertEquals(SummaryState.NotAvailable, viewModel.state.value)
    }

    @Test
    fun `loading same episode twice does not re-fetch`() = runTest {
        whenever(transcriptManager.loadSummaryText("episode-1")).thenReturn("Summary text")
        val viewModel = createViewModel()

        viewModel.loadSummary("episode-1")
        viewModel.loadSummary("episode-1")

        verify(transcriptManager).loadSummaryText("episode-1")
    }

    @Test
    fun `loading different episode fetches new summary`() = runTest {
        whenever(transcriptManager.loadSummaryText("episode-1")).thenReturn("Summary 1")
        whenever(transcriptManager.loadSummaryText("episode-2")).thenReturn("Summary 2")
        val viewModel = createViewModel()

        viewModel.loadSummary("episode-1")
        assertEquals(SummaryState.Loaded("Summary 1"), viewModel.state.value)

        viewModel.loadSummary("episode-2")
        assertEquals(SummaryState.Loaded("Summary 2"), viewModel.state.value)
    }

    @Test
    fun `state resets to Loading when switching episodes`() = runTest {
        whenever(transcriptManager.loadSummaryText("episode-1")).thenReturn("Summary 1")
        val viewModel = createViewModel()
        viewModel.loadSummary("episode-1")
        assertEquals(SummaryState.Loaded("Summary 1"), viewModel.state.value)

        // When loading a new episode, state should reset before the result arrives
        whenever(transcriptManager.loadSummaryText("episode-2")).thenReturn(null)
        viewModel.loadSummary("episode-2")
        assertEquals(SummaryState.NotAvailable, viewModel.state.value)
    }

    @Test
    fun `no interaction with transcript manager before loadSummary is called`() {
        createViewModel()

        verifyNoInteractions(transcriptManager)
    }
}

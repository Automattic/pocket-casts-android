package au.com.shiftyjelly.pocketcasts.player.view.transcripts

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock

@OptIn(ExperimentalCoroutinesApi::class)
class TranscriptSearchViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineRule = MainCoroutineRule(testDispatcher)
    private lateinit var viewModel: TranscriptSearchViewModel

    @Before
    fun setUp() {
        viewModel = TranscriptSearchViewModel(
            analyticsTracker = mock(),
            defaultDispatcher = testDispatcher,
        )
        viewModel.setSearchInput(searchSourceText = "text text text", "", "")
    }

    @Test
    fun `search is performed when search query changes`() = runTest {
        viewModel.onSearchQueryChanged("text")
        advanceUntilIdle()

        viewModel.searchState.test {
            assertEquals(
                TranscriptSearchViewModel.SearchUiState(
                    searchTerm = "text",
                    searchResultIndices = listOf(0, 5, 10),
                    currentSearchIndex = 0,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `search index is updated when searching next`() = runTest {
        viewModel.onSearchQueryChanged("text")
        advanceUntilIdle()
        viewModel.onSearchNext()

        viewModel.searchState.test {
            assertEquals(1, awaitItem().currentSearchIndex)
        }
    }

    @Test
    fun `search index is updated when searching previous`() = runTest {
        viewModel.onSearchQueryChanged("text")
        advanceUntilIdle()
        viewModel.onSearchPrevious()

        viewModel.searchState.test {
            assertEquals(2, awaitItem().currentSearchIndex)
        }
    }

    @Test
    fun `current search index is reset to zero on new search`() = runTest {
        viewModel.onSearchQueryChanged("text")
        advanceUntilIdle()
        viewModel.onSearchNext()

        viewModel.searchState.test {
            assertEquals(1, awaitItem().currentSearchIndex)

            viewModel.onSearchQueryChanged("tex")
            assertEquals(0, awaitItem().currentSearchIndex)
        }
    }

    @Test
    fun `search state is reset when search is done`() = runTest {
        viewModel.onSearchQueryChanged("text")
        advanceUntilIdle()

        viewModel.searchState.test {
            skipItems(1)

            viewModel.onSearchDone()

            assertEquals(
                TranscriptSearchViewModel.SearchUiState(
                    searchTerm = "",
                    searchResultIndices = emptyList(),
                    currentSearchIndex = 0,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `search state is reset when search is cleared`() = runTest {
        viewModel.onSearchQueryChanged("text")
        advanceUntilIdle()

        viewModel.searchState.test {
            skipItems(1)

            viewModel.onSearchCleared()

            assertEquals(
                TranscriptSearchViewModel.SearchUiState(
                    searchTerm = "",
                    searchResultIndices = emptyList(),
                    currentSearchIndex = 0,
                ),
                awaitItem(),
            )
        }
    }

    @Test
    fun `searchOccurrencesText returns correct format when searchResultIndices is not empty`() {
        val searchUiState = TranscriptSearchViewModel.SearchUiState(
            searchTerm = "text",
            searchResultIndices = listOf(0, 1, 2),
            currentSearchIndex = 1,
        )

        assertEquals("2/3", searchUiState.searchOccurrencesText)
    }

    @Test
    fun `searchOccurrencesText returns zero when searchResultIndices is empty`() {
        val searchUiState = TranscriptSearchViewModel.SearchUiState(
            searchTerm = "text",
            searchResultIndices = emptyList(),
            currentSearchIndex = 0,
        )

        assertEquals("0", searchUiState.searchOccurrencesText)
    }

    @Test
    fun `prev next buttons enabled when search results found`() {
        val searchUiState = TranscriptSearchViewModel.SearchUiState(
            searchTerm = "text",
            searchResultIndices = listOf(1),
            currentSearchIndex = 0,
        )

        assertTrue(searchUiState.prevNextArrowButtonsEnabled)
    }

    @Test
    fun `prev next buttons disabled when search results not found`() {
        val searchUiState = TranscriptSearchViewModel.SearchUiState(
            searchTerm = "text",
            searchResultIndices = emptyList(),
            currentSearchIndex = 0,
        )

        assertFalse(searchUiState.prevNextArrowButtonsEnabled)
    }
}

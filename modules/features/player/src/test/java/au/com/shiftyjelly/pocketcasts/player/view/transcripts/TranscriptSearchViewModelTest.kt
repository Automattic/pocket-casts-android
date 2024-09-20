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
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doNothing
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class TranscriptSearchViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineRule = MainCoroutineRule(testDispatcher)
    private lateinit var viewModel: TranscriptSearchViewModel
    private val kmpSearch: KMPSearch = mock()
    private val searchResultIndices = listOf(1, 2, 3)
    private val searchTerm = "test"

    @Before
    fun setUp() {
        doNothing().whenever(kmpSearch).setPattern(anyOrNull())
        whenever(kmpSearch.search(any())).thenReturn(searchResultIndices)
        viewModel = TranscriptSearchViewModel(
            kmpSearch = kmpSearch,
            analyticsTracker = mock(),
            defaultDispatcher = testDispatcher,
        )
    }

    @Test
    fun `search is performed when search query changes`() = runTest {
        viewModel.onSearchQueryChanged(searchTerm)
        advanceUntilIdle()

        viewModel.searchState.test {
            verify(kmpSearch).setPattern(searchTerm)
            verify(kmpSearch).search(any())
            assertTrue(
                awaitItem() == TranscriptSearchViewModel.SearchUiState(
                    searchTerm = searchTerm,
                    searchResultIndices = searchResultIndices,
                    currentSearchIndex = 0,
                ),
            )
        }
    }

    @Test
    fun `search index is updated when searching previous`() = runTest {
        viewModel.onSearchQueryChanged(searchTerm)
        advanceUntilIdle()

        viewModel.onSearchNext()

        viewModel.searchState.test {
            assertTrue((awaitItem().currentSearchIndex == 1))
        }
    }

    @Test
    fun `search index is updated when searching next`() = runTest {
        viewModel.onSearchQueryChanged(searchTerm)
        advanceUntilIdle()
        viewModel.onSearchNext()
        advanceUntilIdle()

        viewModel.onSearchPrevious()

        viewModel.searchState.test {
            assertTrue((awaitItem().currentSearchIndex == 0))
        }
    }

    @Test
    fun `current search index is reset to zero on new search`() = runTest {
        viewModel.onSearchQueryChanged("test1")
        advanceUntilIdle()
        viewModel.onSearchNext()
        viewModel.onSearchNext() // currentSearchIndex = 2

        viewModel.onSearchQueryChanged("test2")

        viewModel.searchState.test {
            assertTrue((awaitItem().currentSearchIndex == 2))
            assertTrue((awaitItem().currentSearchIndex == 0))
        }
    }

    @Test
    fun `search state is reset when search is done`() = runTest {
        viewModel.onSearchQueryChanged(searchTerm)
        advanceUntilIdle()

        viewModel.onSearchDone()

        viewModel.searchState.test {
            assertTrue(
                (
                    awaitItem() == TranscriptSearchViewModel.SearchUiState(
                        searchTerm = "",
                        searchResultIndices = emptyList(),
                        currentSearchIndex = 0,
                    )
                    ),
            )
        }
    }

    @Test
    fun `search state is reset when search is cleared`() = runTest {
        viewModel.onSearchQueryChanged(searchTerm)
        advanceUntilIdle()

        viewModel.onSearchCleared()

        viewModel.searchState.test {
            assertTrue(
                (
                    awaitItem() == TranscriptSearchViewModel.SearchUiState(
                        searchTerm = "",
                        searchResultIndices = emptyList(),
                        currentSearchIndex = 0,
                    )
                    ),
            )
        }
    }

    @Test
    fun `searchOccurrencesText returns correct format when searchResultIndices is not empty`() {
        val searchUiState = TranscriptSearchViewModel.SearchUiState(
            searchTerm = searchTerm,
            searchResultIndices = searchResultIndices,
            currentSearchIndex = 1,
        )

        assertEquals("2/3", searchUiState.searchOccurrencesText)
    }

    @Test
    fun `searchOccurrencesText returns zero when searchResultIndices is empty`() {
        val searchUiState = TranscriptSearchViewModel.SearchUiState(
            searchTerm = searchTerm,
            searchResultIndices = emptyList(),
            currentSearchIndex = 0,
        )

        assertEquals("0", searchUiState.searchOccurrencesText)
    }

    @Test
    fun `prev next buttons enabled when search results found`() {
        val searchUiState = TranscriptSearchViewModel.SearchUiState(
            searchTerm = searchTerm,
            searchResultIndices = listOf(1),
            currentSearchIndex = 0,
        )

        assertTrue(searchUiState.prevNextArrowButtonsEnabled)
    }

    @Test
    fun `prev next buttons disabled when search results not found`() {
        val searchUiState = TranscriptSearchViewModel.SearchUiState(
            searchTerm = searchTerm,
            searchResultIndices = emptyList(),
            currentSearchIndex = 0,
        )

        assertFalse(searchUiState.prevNextArrowButtonsEnabled)
    }
}

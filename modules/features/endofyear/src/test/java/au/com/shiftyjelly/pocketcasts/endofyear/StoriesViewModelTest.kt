package au.com.shiftyjelly.pocketcasts.endofyear

import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearManager
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.Story
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.utils.FileUtilWrapper
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

private val story1 = mock<Story>()
private val story2 = mock<Story>()

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class StoriesViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Mock
    private lateinit var fileUtilWrapper: FileUtilWrapper

    @Mock
    private lateinit var endOfYearManager: EndOfYearManager

    @Test
    fun `when vm starts, then progress is zero`() = runTest {
        val backgroundScope = CoroutineScope(coroutineContext + Job())
        try {
            val viewModel = initViewModel(listOf(story1, story2))

            assertEquals(viewModel.progress.value, 0f)
        } finally {
            backgroundScope.cancel()
        }
    }

    @Test
    fun `when vm starts, then loading is shown`() = runTest {
        val backgroundScope = CoroutineScope(coroutineContext + Job())
        try {
            backgroundScope.launch {
                val viewModel = initViewModel(listOf(story1, story2))

                assertEquals(viewModel.state.value is StoriesViewModel.State.Loading, true)
            }
        } finally {
            backgroundScope.cancel()
        }
    }

    @Test
    fun `when vm starts, then stories are loaded`() = runTest {
        initViewModel(emptyList())

        verify(endOfYearManager).loadStories()
    }

    @Test
    fun `given no stories found, when vm starts, then error is shown`() = runTest {
        val viewModel = initViewModel(emptyList())

        assertEquals(viewModel.state.value is StoriesViewModel.State.Error, true)
    }

    @Test
    fun `given stories found, when vm starts, then screen is loaded`() = runTest {
        val viewModel = initViewModel(listOf(story1, story2))

        assertEquals(viewModel.state.value is StoriesViewModel.State.Loaded, true)
    }

    @Test
    fun `when next is invoked, then next story is shown`() = runTest {
        val viewModel = initViewModel(listOf(story1, story2))

        viewModel.skipNext()

        val state = viewModel.state.value as StoriesViewModel.State.Loaded
        assertEquals(state.currentStory, story2)
    }

    @Test
    fun `when previous is invoked, then previous story is shown`() = runTest {
        val viewModel = initViewModel(listOf(story1, story2))
        viewModel.skipNext()

        viewModel.skipPrevious()

        val state = viewModel.state.value as StoriesViewModel.State.Loaded
        assertEquals(state.currentStory, story1)
    }

    @Test
    fun `when replay is invoked, then first story is shown`() = runTest {
        val story3 = mock<Story>()
        val viewModel = initViewModel(listOf(story1, story2, story3))
        viewModel.skipNext()
        viewModel.skipNext() // At last story

        viewModel.replay()

        val state = viewModel.state.value as StoriesViewModel.State.Loaded
        assertEquals(state.currentStory, story1)
    }

    private suspend fun initViewModel(mockStories: List<Story>): StoriesViewModel {
        whenever(endOfYearManager.loadStories()).thenReturn(mockStories)
        return StoriesViewModel(
            endOfYearManager = endOfYearManager,
            fileUtilWrapper = fileUtilWrapper,
            shareableTextProvider = mock(),
            analyticsTracker = mock(),
        )
    }
}

package au.com.shiftyjelly.pocketcasts.endofyear

import au.com.shiftyjelly.pocketcasts.endofyear.stories.Story
import au.com.shiftyjelly.pocketcasts.utils.seconds
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertTrue
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import kotlin.math.roundToInt

private val story1 = mock<Story>()
private val story2 = mock<Story>()

@RunWith(MockitoJUnitRunner::class)
class StoriesViewModelTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when vm starts, then loading is shown`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher())
        val viewModel = StoriesViewModel(MockStoriesDataSource(listOf(story1, story2)))

        assertEquals(viewModel.state.value is StoriesViewModel.State.Loading, true)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when vm starts, then progress is zero`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher())
        val viewModel = StoriesViewModel(MockStoriesDataSource(listOf(story1, story2)))

        assertEquals(viewModel.progress.value, 0f)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when vm starts, then stories are loaded`() = runTest {
        val dataSource = mock<StoriesDataSource>()
        StoriesViewModel(dataSource)

        verify(dataSource).loadStories()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when stories are found, then progress increments`() = runTest {
        val storyLengthInMs = 2.seconds()
        whenever(story1.storyLength).thenReturn(storyLengthInMs)
        whenever(story2.storyLength).thenReturn(storyLengthInMs)
        val viewModel = StoriesViewModel(MockStoriesDataSource(listOf(story1, story2)))

        val progress = mutableListOf<Float>()
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.progress.collect {
                progress.add(it)
            }
        }

        assertTrue(progress.last() > 0f)
        collectJob.cancel()
    }

    @Test
    fun `given no stories found, when vm starts, then error is shown`() {
        val viewModel = StoriesViewModel(MockStoriesDataSource(emptyList()))

        assertEquals(viewModel.state.value is StoriesViewModel.State.Error, true)
    }

    @Test
    fun `given stories found, when vm starts, then screen is loaded`() {
        val viewModel = StoriesViewModel(MockStoriesDataSource(listOf(story1, story2)))

        assertEquals(viewModel.state.value is StoriesViewModel.State.Loaded, true)
    }

    @Test
    fun `when next is invoked, then next story is shown`() {
        val viewModel = StoriesViewModel(MockStoriesDataSource(listOf(story1, story2)))

        viewModel.skipNext()

        val state = viewModel.state.value as StoriesViewModel.State.Loaded
        assertEquals(state.currentStory, story2)
    }

    @Test
    fun `when previous is invoked, then previous story is shown`() {
        val viewModel = StoriesViewModel(MockStoriesDataSource(listOf(story1, story2)))
        viewModel.skipNext()

        viewModel.skipPrevious()

        val state = viewModel.state.value as StoriesViewModel.State.Loaded
        assertEquals(state.currentStory, story1)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when next is invoked, then story skips to correct position`() = runTest {
        whenever(story1.storyLength).thenReturn(5.seconds())
        whenever(story2.storyLength).thenReturn(10.seconds())
        val viewModel = StoriesViewModel(MockStoriesDataSource(listOf(story1, story2)))
        val progress = mutableListOf<Float>()
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.progress.collect {
                progress.add(it)
            }
        }

        viewModel.skipNext()

        assertEquals(0.34f, (progress.last() * 100f).roundToInt() / 100f)
        collectJob.cancel()
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `when prev is invoked, then story skips to correct position`() = runTest {
        val story3 = mock<Story>()
        whenever(story1.storyLength).thenReturn(5.seconds())
        whenever(story2.storyLength).thenReturn(10.seconds())
        whenever(story3.storyLength).thenReturn(5.seconds())
        val viewModel = StoriesViewModel(MockStoriesDataSource(listOf(story1, story2, story3)))
        viewModel.skipNext()
        viewModel.skipNext() // At last story
        val progress = mutableListOf<Float>()
        val collectJob = launch(UnconfinedTestDispatcher()) {
            viewModel.progress.collect {
                progress.add(it)
            }
        }

        viewModel.skipPrevious() // Skip to middle story

        assertEquals(0.25f, (progress.last() * 100f).roundToInt() / 100f)
        collectJob.cancel()
    }

    class MockStoriesDataSource(private val mockStories: List<Story>) : StoriesDataSource() {
        override val stories = mutableListOf<Story>()

        override suspend fun loadStories(): Flow<List<Story>> {
            stories.addAll(mockStories)
            return flowOf(stories)
        }

        override fun storyAt(index: Int): Story? {
            return try {
                stories[index]
            } catch (e: IndexOutOfBoundsException) {
                null
            }
        }
    }
}

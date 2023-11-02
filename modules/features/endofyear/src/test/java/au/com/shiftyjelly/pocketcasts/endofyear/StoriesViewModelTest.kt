package au.com.shiftyjelly.pocketcasts.endofyear

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearManager
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.Story
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.utils.FileUtilWrapper
import au.com.shiftyjelly.pocketcasts.utils.featureflag.UserTier
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(MockitoJUnitRunner::class)
class StoriesViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Mock
    private lateinit var fileUtilWrapper: FileUtilWrapper

    @Mock
    private lateinit var endOfYearManager: EndOfYearManager

    @Mock
    private lateinit var settings: Settings

    @Mock
    private lateinit var plusStory1: Story

    @Mock
    private lateinit var plusStory2: Story

    @Mock
    private lateinit var plusStory3: Story

    @Mock
    private lateinit var freeStory1: Story

    @Mock
    private lateinit var freeStory2: Story

    @Before
    fun setup() {
        whenever(settings.userTier).thenReturn(UserTier.Free)
        whenever(plusStory1.plusOnly).thenReturn(true)
        whenever(plusStory2.plusOnly).thenReturn(true)
        whenever(plusStory3.plusOnly).thenReturn(true)
        whenever(freeStory1.plusOnly).thenReturn(false)
        whenever(freeStory2.plusOnly).thenReturn(false)
    }

    @Test
    fun `when vm starts, then progress is zero`() = runTest {
        val backgroundScope = CoroutineScope(coroutineContext + Job())
        try {
            val viewModel = initViewModel(listOf(freeStory1, freeStory2))

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
                val viewModel = initViewModel(listOf(freeStory1, freeStory2))

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
        val viewModel = initViewModel(listOf(freeStory1, freeStory2))

        assertEquals(viewModel.state.value is StoriesViewModel.State.Loaded, true)
    }

    @Test
    fun `when next is invoked, then next story is shown`() = runTest {
        val viewModel = initViewModel(listOf(freeStory1, freeStory2))

        viewModel.skipNext()

        val state = viewModel.state.value as StoriesViewModel.State.Loaded
        assertEquals(state.currentStory, freeStory2)
    }

    @Test
    fun `when previous is invoked, then previous story is shown`() = runTest {
        val viewModel = initViewModel(listOf(freeStory1, freeStory2))
        viewModel.skipNext()

        viewModel.skipPrevious()

        val state = viewModel.state.value as StoriesViewModel.State.Loaded
        assertEquals(state.currentStory, freeStory1)
    }

    @Test
    fun `when replay is invoked, then first story is shown`() = runTest {
        val story3 = mock<Story>()
        val viewModel = initViewModel(listOf(freeStory1, freeStory2, story3))
        viewModel.skipNext()
        viewModel.skipNext() // At last story

        viewModel.replay()

        val state = viewModel.state.value as StoriesViewModel.State.Loaded
        assertEquals(state.currentStory, freeStory1)
    }

    /* Plus Stories */
    @Test
    fun `given free user at plus story, when next is invoked, then free story is shown skipping in between plus stories`() = runTest {
        whenever(settings.userTier).thenReturn(UserTier.Free)
        val viewModel = initViewModel(listOf(plusStory1, plusStory2, plusStory3, freeStory1))

        viewModel.skipNext()

        val state = viewModel.state.value as StoriesViewModel.State.Loaded
        assertEquals(state.currentStory, freeStory1) // plusStory2, plusStory3 skipped
    }

    @Test
    fun `given free user at free story next to plus, when previous is invoked, then first plus story is shown`() = runTest {
        whenever(settings.userTier).thenReturn(UserTier.Free)
        val viewModel = initViewModel(listOf(plusStory1, plusStory2, plusStory3, freeStory1))
        viewModel.skipNext() // at free story

        viewModel.skipPrevious()

        val state = viewModel.state.value as StoriesViewModel.State.Loaded
        assertEquals(state.currentStory, plusStory1) // plusStory2, plusStory3 skipped
    }

    @Test
    fun `given paid user with next plus story, when next is invoked, then next plus story is shown`() = runTest {
        whenever(settings.userTier).thenReturn(UserTier.Plus)
        val viewModel = initViewModel(listOf(plusStory1, plusStory2, plusStory3, freeStory1))

        viewModel.skipNext()

        val state = viewModel.state.value as StoriesViewModel.State.Loaded
        assertEquals(state.currentStory, plusStory2) // plusStory2 is not skipped
    }

    @Test
    fun `given paid user at free story next to plus, when previous is invoked, then previous plus story is shown`() = runTest {
        whenever(settings.userTier).thenReturn(UserTier.Plus)
        val viewModel = initViewModel(listOf(plusStory1, plusStory2, plusStory3, freeStory1))
        viewModel.skipNext()
        viewModel.skipNext()
        viewModel.skipNext() // at free story next to plus

        viewModel.skipPrevious()

        val state = viewModel.state.value as StoriesViewModel.State.Loaded
        assertEquals(state.currentStory, plusStory3) // plusStory3 is not skipped
    }

    private suspend fun initViewModel(mockStories: List<Story>): StoriesViewModel {
        whenever(endOfYearManager.loadStories()).thenReturn(mockStories)
        return StoriesViewModel(
            endOfYearManager = endOfYearManager,
            fileUtilWrapper = fileUtilWrapper,
            shareableTextProvider = mock(),
            analyticsTracker = mock(),
            settings = settings,
        )
    }
}

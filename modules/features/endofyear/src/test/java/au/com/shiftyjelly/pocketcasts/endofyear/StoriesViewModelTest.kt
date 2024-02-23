package au.com.shiftyjelly.pocketcasts.endofyear

import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.EndOfYearManager
import au.com.shiftyjelly.pocketcasts.repositories.endofyear.stories.Story
import au.com.shiftyjelly.pocketcasts.repositories.subscription.FreeTrial
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.utils.FileUtilWrapper
import au.com.shiftyjelly.pocketcasts.utils.featureflag.UserTier
import java.util.Date
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.stub
import org.mockito.kotlin.times
import org.mockito.kotlin.verifyBlocking
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

    @Mock
    private lateinit var subscriptionManager: SubscriptionManager

    @Mock
    private lateinit var userSetting: UserSetting<SubscriptionStatus?>

    private lateinit var cachedSubscriptionStatusFlow: MutableStateFlow<SubscriptionStatus>

    private val cachedSubscriptionStatus = SubscriptionStatus.Free()

    @Before
    fun setup() {
        cachedSubscriptionStatusFlow = MutableStateFlow(cachedSubscriptionStatus)
        whenever(settings.userTier).thenReturn(UserTier.Free)
        whenever(plusStory1.plusOnly).thenReturn(true)
        whenever(plusStory2.plusOnly).thenReturn(true)
        whenever(plusStory3.plusOnly).thenReturn(true)
        whenever(freeStory1.plusOnly).thenReturn(false)
        whenever(freeStory2.plusOnly).thenReturn(false)
        whenever(plusStory1.storyLength).thenReturn(1)
        whenever(plusStory2.storyLength).thenReturn(1)
        whenever(plusStory3.storyLength).thenReturn(1)
    }

    @Test
    fun `when vm starts, then progress is zero`() {
        val viewModel = initViewModel(listOf(freeStory1, freeStory2))

        assertEquals(viewModel.progress.value, 0f)
    }

    @Test
    fun `when vm starts, then stories are loaded`() {
        initViewModel(emptyList())

        verifyBlocking(endOfYearManager) { loadStories() }
    }

    @Test
    fun `given no stories found, when vm starts, then error is shown`() {
        val viewModel = initViewModel(emptyList())

        assertEquals(viewModel.state.value is StoriesViewModel.State.Error, true)
    }

    @Test
    fun `given stories found, when vm starts, then screen is loaded`() {
        val viewModel = initViewModel(listOf(freeStory1, freeStory2))

        assertEquals(viewModel.state.value is StoriesViewModel.State.Loaded, true)
    }

    @Test
    fun `when next is invoked, then next story is shown`() {
        val viewModel = initViewModel(listOf(freeStory1, freeStory2))

        viewModel.skipNext()

        val state = viewModel.state.value as StoriesViewModel.State.Loaded
        assertEquals(state.currentStory, freeStory2)
    }

    @Test
    fun `when previous is invoked, then previous story is shown`() {
        val viewModel = initViewModel(listOf(freeStory1, freeStory2))
        viewModel.skipNext()

        viewModel.skipPrevious()

        val state = viewModel.state.value as StoriesViewModel.State.Loaded
        assertEquals(state.currentStory, freeStory1)
    }

    @Test
    fun `when replay is invoked, then first story is shown`() {
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
    fun `given free user at plus story, when next is invoked, then free story is shown skipping in between plus stories`() {
        whenever(settings.userTier).thenReturn(UserTier.Free)
        val viewModel = initViewModel(listOf(plusStory1, plusStory2, plusStory3, freeStory1))

        viewModel.skipNext()

        val state = viewModel.state.value as StoriesViewModel.State.Loaded
        assertEquals(state.currentStory, freeStory1) // plusStory2, plusStory3 skipped
    }

    @Test
    fun `given free user at free story next to plus, when previous is invoked, then first plus story is shown`() {
        whenever(settings.userTier).thenReturn(UserTier.Free)
        val viewModel = initViewModel(listOf(plusStory1, plusStory2, plusStory3, freeStory1))
        viewModel.skipNext() // at free story

        viewModel.skipPrevious()

        val state = viewModel.state.value as StoriesViewModel.State.Loaded
        assertEquals(state.currentStory, plusStory1) // plusStory2, plusStory3 skipped
    }

    @Test
    fun `given paid user with next plus story, when next is invoked, then next plus story is shown`() {
        whenever(settings.userTier).thenReturn(UserTier.Plus)
        val viewModel = initViewModel(listOf(plusStory1, plusStory2, plusStory3, freeStory1))

        viewModel.skipNext()

        val state = viewModel.state.value as StoriesViewModel.State.Loaded
        assertEquals(state.currentStory, plusStory2) // plusStory2 is not skipped
    }

    @Test
    fun `given paid user at free story next to plus, when previous is invoked, then previous plus story is shown`() {
        whenever(settings.userTier).thenReturn(UserTier.Plus)
        val viewModel = initViewModel(listOf(plusStory1, plusStory2, plusStory3, freeStory1))
        viewModel.skipNext()
        viewModel.skipNext()
        viewModel.skipNext() // at free story next to plus

        viewModel.skipPrevious()

        val state = viewModel.state.value as StoriesViewModel.State.Loaded
        assertEquals(state.currentStory, plusStory3) // plusStory3 is not skipped
    }

    /* Upsell */
    @Test
    fun `given free user, when plus story shown, then upsell shown`() {
        whenever(settings.userTier).thenReturn(UserTier.Free)
        val viewModel = initViewModel(listOf(plusStory1))

        val shouldShowUpsell = viewModel.shouldShowUpsell()

        assertTrue(shouldShowUpsell)
    }

    @Test
    fun `given paid user, when plus story shown, then upsell not shown`() {
        whenever(settings.userTier).thenReturn(UserTier.Plus)
        val viewModel = initViewModel(listOf(plusStory1))

        val shouldShowUpsell = viewModel.shouldShowUpsell()

        assertFalse(shouldShowUpsell)
    }

    /* Subscription updated */
    @Test
    fun `given subscription updated, then listening history and stories reloaded`() {
        whenever(settings.userTier)
            .thenReturn(UserTier.Free)
            .thenReturn(UserTier.Plus)
        initViewModel(listOf(plusStory1))

        cachedSubscriptionStatusFlow.value = cachedSubscriptionStatus.copy(expiry = Date())

        verifyBlocking(endOfYearManager, times(2)) { downloadListeningHistory(anyOrNull()) }
        verifyBlocking(endOfYearManager, times(2)) { loadStories() }
    }

    @Test
    fun `given subscription not updated, then listening history and stories not reloaded`() {
        whenever(settings.userTier)
            .thenReturn(UserTier.Free)
            .thenReturn(UserTier.Free)
        initViewModel(listOf(plusStory1))

        cachedSubscriptionStatusFlow.value = cachedSubscriptionStatus.copy(expiry = Date())

        verifyBlocking(endOfYearManager, times(1)) { downloadListeningHistory(anyOrNull()) }
        verifyBlocking(endOfYearManager, times(1)) { loadStories() }
    }

    private fun initViewModel(mockStories: List<Story>): StoriesViewModel {
        endOfYearManager.stub {
            onBlocking { loadStories() } doReturn mockStories
        }
        whenever(subscriptionManager.freeTrialForSubscriptionTierFlow(Subscription.SubscriptionTier.PLUS))
            .thenReturn(flowOf(FreeTrial(Subscription.SubscriptionTier.PLUS)))
        whenever(userSetting.flow).thenReturn(cachedSubscriptionStatusFlow)
        whenever(settings.cachedSubscriptionStatus).thenReturn(userSetting)

        return StoriesViewModel(
            endOfYearManager = endOfYearManager,
            fileUtilWrapper = fileUtilWrapper,
            shareableTextProvider = mock(),
            analyticsTracker = mock(),
            settings = settings,
            subscriptionManager = subscriptionManager,
        )
    }
}

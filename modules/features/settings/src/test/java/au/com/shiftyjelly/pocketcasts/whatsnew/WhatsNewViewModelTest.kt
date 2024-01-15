package au.com.shiftyjelly.pocketcasts.whatsnew

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkFeatureControl
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.subscription.FreeTrial
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.settings.whatsnew.WhatsNewViewModel
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.UserTier
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@RunWith(MockitoJUnitRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class WhatsNewViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Mock
    lateinit var settings: Settings

    @Mock
    lateinit var playbackManager: PlaybackManager

    @Mock
    lateinit var subscriptionManager: SubscriptionManager

    @Mock
    lateinit var bookmarkFeature: BookmarkFeatureControl

    private lateinit var viewModel: WhatsNewViewModel

    @Test
    fun `given plus user, then bookmarks are here title shown`() =
        runTest {
            whenever(settings.userTier).thenReturn(UserTier.Plus)

            initViewModel()

            viewModel.state.test {
                assertTrue((awaitItem() as WhatsNewViewModel.UiState.Loaded).feature.title == R.string.whats_new_bookmarks_title)
            }
        }

    @Test
    fun `given patron user, then bookmarks are here title shown`() =
        runTest {
            whenever(settings.userTier).thenReturn(UserTier.Patron)
            initViewModel()

            viewModel.state.test {
                assertTrue((awaitItem() as WhatsNewViewModel.UiState.Loaded).feature.title == R.string.whats_new_bookmarks_title)
            }
        }

    @Test
    fun `given free user, then bookmarks are here title shown`() =
        runTest {
            whenever(settings.userTier).thenReturn(UserTier.Free)

            initViewModel()

            viewModel.state.test {
                assertTrue((awaitItem() as WhatsNewViewModel.UiState.Loaded).feature.title == LR.string.whats_new_bookmarks_title)
            }
        }

    private fun initViewModel() {
        whenever(subscriptionManager.freeTrialForSubscriptionTierFlow(Subscription.SubscriptionTier.PLUS))
            .thenReturn(flowOf(FreeTrial(subscriptionTier = Subscription.SubscriptionTier.PLUS)))

        viewModel = WhatsNewViewModel(
            playbackManager = playbackManager,
            subscriptionManager = subscriptionManager,
            settings = settings,
            bookmarkFeature = bookmarkFeature,
        )
    }
}

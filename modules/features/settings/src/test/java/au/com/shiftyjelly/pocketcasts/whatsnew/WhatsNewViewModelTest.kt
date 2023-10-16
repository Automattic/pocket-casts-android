package au.com.shiftyjelly.pocketcasts.whatsnew

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.subscription.ProductDetailsState
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.settings.whatsnew.WhatsNewViewModel
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlagWrapper
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureTier
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureWrapper
import au.com.shiftyjelly.pocketcasts.utils.featureflag.ReleaseVersion
import au.com.shiftyjelly.pocketcasts.utils.featureflag.ReleaseVersionWrapper
import au.com.shiftyjelly.pocketcasts.utils.featureflag.UserTier
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.rx2.asFlowable
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
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
    lateinit var subscriptionManager: SubscriptionManager

    @Mock
    private lateinit var productDetailsState: ProductDetailsState.Loaded

    @Mock
    private lateinit var productDetails: ProductDetails

    private lateinit var viewModel: WhatsNewViewModel

    private val betaEarlyAccessRelease = ReleaseVersion(7, 50, null, 1)
    private val productionEarlyAccessRelease = ReleaseVersion(7, 50)
    private val betaFullAccessRelease = ReleaseVersion(7, 51, null, 1)
    private val productionFullAccessRelease = ReleaseVersion(7, 51)

    @Before
    fun setUp() {
        whenever(productDetailsState.productDetails).thenReturn(listOf(productDetails))
        whenever(subscriptionManager.observeProductDetails()).thenReturn(flowOf(productDetailsState).asFlowable())
        whenever(productDetails.productId).thenReturn("productId")
    }

    /* Title */
    @Test
    fun `given plus user, when beta early access release, then join beta testing title shown`() = runTest {
        whenever(settings.userTier).thenReturn(UserTier.Plus)

        initViewModel(
            currentRelease = betaEarlyAccessRelease,
            patronExclusiveAccessRelease = productionEarlyAccessRelease,
        )

        viewModel.state.test {
            assertTrue((awaitItem() as WhatsNewViewModel.UiState.Loaded).feature.title == LR.string.whats_new_boomarks_join_beta_testing_title)
        }
    }

    @Test
    fun `given patron user, when beta early access release, then join beta testing title shown`() = runTest {
        whenever(settings.userTier).thenReturn(UserTier.Patron)

        initViewModel(
            currentRelease = betaEarlyAccessRelease,
            patronExclusiveAccessRelease = productionEarlyAccessRelease,
        )

        viewModel.state.test {
            assertTrue((awaitItem() as WhatsNewViewModel.UiState.Loaded).feature.title == LR.string.whats_new_boomarks_join_beta_testing_title)
        }
    }

    @Test
    fun `given plus user, when prod early access release, then bookmarks are here title shown`() = runTest {
        whenever(settings.userTier).thenReturn(UserTier.Plus)

        initViewModel(
            currentRelease = productionEarlyAccessRelease,
            patronExclusiveAccessRelease = productionEarlyAccessRelease,
        )

        viewModel.state.test {
            assertTrue((awaitItem() as WhatsNewViewModel.UiState.Loaded).feature.title == R.string.whats_new_bookmarks_title)
        }
    }

    @Test
    fun `given patron user, when prod early access release, then bookmarks are here title shown`() = runTest {
        whenever(settings.userTier).thenReturn(UserTier.Patron)
        initViewModel(
            currentRelease = productionEarlyAccessRelease,
            patronExclusiveAccessRelease = productionEarlyAccessRelease,
        )

        viewModel.state.test {
            assertTrue((awaitItem() as WhatsNewViewModel.UiState.Loaded).feature.title == R.string.whats_new_bookmarks_title)
        }
    }

    @Test
    fun `given plus user, when beta full access release, then bookmarks are here title shown`() = runTest {
        whenever(settings.userTier).thenReturn(UserTier.Plus)

        initViewModel(
            currentRelease = betaFullAccessRelease,
            patronExclusiveAccessRelease = productionEarlyAccessRelease,
        )

        viewModel.state.test {
            assertTrue((awaitItem() as WhatsNewViewModel.UiState.Loaded).feature.title == R.string.whats_new_bookmarks_title)
        }
    }

    @Test
    fun `given patron user, when beta full access release, then bookmarks are here title shown`() = runTest {
        whenever(settings.userTier).thenReturn(UserTier.Patron)

        initViewModel(
            currentRelease = betaFullAccessRelease,
            patronExclusiveAccessRelease = productionEarlyAccessRelease,
        )

        viewModel.state.test {
            assertTrue((awaitItem() as WhatsNewViewModel.UiState.Loaded).feature.title == R.string.whats_new_bookmarks_title)
        }
    }

    @Test
    fun `given free user, when prod full access release, then bookmarks are here title shown`() = runTest {
        whenever(settings.userTier).thenReturn(UserTier.Free)

        initViewModel(
            currentRelease = productionFullAccessRelease,
            patronExclusiveAccessRelease = productionEarlyAccessRelease,
        )

        viewModel.state.test {
            assertTrue((awaitItem() as WhatsNewViewModel.UiState.Loaded).feature.title == LR.string.whats_new_bookmarks_title)
        }
    }

    @Test
    fun `given plus user, when prod full access release, then bookmarks are here title shown`() = runTest {
        whenever(settings.userTier).thenReturn(UserTier.Plus)

        initViewModel(
            currentRelease = productionFullAccessRelease,
            patronExclusiveAccessRelease = productionEarlyAccessRelease,
        )

        viewModel.state.test {
            assertTrue((awaitItem() as WhatsNewViewModel.UiState.Loaded).feature.title == R.string.whats_new_bookmarks_title)
        }
    }

    @Test
    fun `given patron user, when prod full access release, then bookmarks are here title shown`() = runTest {
        whenever(settings.userTier).thenReturn(UserTier.Patron)

        initViewModel(
            currentRelease = productionFullAccessRelease,
            patronExclusiveAccessRelease = productionEarlyAccessRelease,
        )

        viewModel.state.test {
            assertTrue((awaitItem() as WhatsNewViewModel.UiState.Loaded).feature.title == R.string.whats_new_bookmarks_title)
        }
    }

    private fun initViewModel(
        currentRelease: ReleaseVersion,
        patronExclusiveAccessRelease: ReleaseVersion?,
    ) {
        val releaseVersion = mock<ReleaseVersionWrapper>().apply {
            doReturn(currentRelease).whenever(this).currentReleaseVersion
        }
        val bookmarksFeature = mock<Feature>().apply {
            doReturn(FeatureTier.Plus(patronExclusiveAccessRelease)).whenever(this).tier
        }
        val feature = mock<FeatureWrapper>().apply {
            doReturn(bookmarksFeature).whenever(this).bookmarksFeature
        }

        val featureFlag = mock<FeatureFlagWrapper>()
        whenever(featureFlag.isEnabled(feature.bookmarksFeature)).thenReturn(true)

        viewModel = WhatsNewViewModel(
            subscriptionManager = subscriptionManager,
            settings = settings,
            releaseVersion = releaseVersion,
            feature = feature,
            featureFlag = featureFlag,
        )
    }
}

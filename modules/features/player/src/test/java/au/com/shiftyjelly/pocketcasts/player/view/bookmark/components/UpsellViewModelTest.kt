package au.com.shiftyjelly.pocketcasts.player.view.bookmark.components

import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.repositories.subscription.ProductDetailsState
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureTier
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureWrapper
import au.com.shiftyjelly.pocketcasts.utils.featureflag.ReleaseVersion
import au.com.shiftyjelly.pocketcasts.utils.featureflag.ReleaseVersionWrapper
import com.android.billingclient.api.ProductDetails
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.rx2.asFlowable
import org.junit.Assert.assertFalse
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

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class UpsellViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Mock
    private lateinit var subscriptionManager: SubscriptionManager

    @Mock
    private lateinit var productDetailsState: ProductDetailsState.Loaded

    @Mock
    private lateinit var productDetails: ProductDetails

    private lateinit var upsellViewModel: UpsellViewModel

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

    /* Early Access Availability */
    // Current Release                   | Beta         | Production
    // ----------------------------------|--------------|--------------
    // Beta Early Access Release         | Plus Users   | N/A
    // Production Early Access Release   | Plus Users   | Patron Users
    // Beta Full Access Release          | Plus Users   | Plus Users
    // Production Full Access Release    | Plus Users   | Plus Users
    @Test
    fun `given patron exclusive feature, when beta release, feature available to Plus`() {
        initViewModel(
            currentRelease = betaEarlyAccessRelease,
            patronExclusiveAccessRelease = productionEarlyAccessRelease,
        )

        val state = upsellViewModel.state.value as UpsellViewModel.UiState.Loaded
        assertTrue(state.tier == Subscription.SubscriptionTier.PLUS)
    }

    @Test
    fun `given patron exclusive feature, when production early access release, feature available to Patron`() {
        initViewModel(
            currentRelease = productionEarlyAccessRelease,
            patronExclusiveAccessRelease = productionEarlyAccessRelease,
        )

        val state = upsellViewModel.state.value as UpsellViewModel.UiState.Loaded
        assertTrue(state.tier == Subscription.SubscriptionTier.PATRON)
    }

    @Test
    fun `given not a patron exclusive feature, when production release, feature available to Plus`() {
        initViewModel(
            currentRelease = productionEarlyAccessRelease,
            patronExclusiveAccessRelease = null,
        )

        val state = upsellViewModel.state.value as UpsellViewModel.UiState.Loaded
        assertTrue(state.tier == Subscription.SubscriptionTier.PLUS)
    }

    @Test
    fun `given not a patron exclusive feature, when beta full access release, feature available to Plus`() {
        initViewModel(
            currentRelease = betaFullAccessRelease,
            patronExclusiveAccessRelease = productionEarlyAccessRelease,
        )

        val state = upsellViewModel.state.value as UpsellViewModel.UiState.Loaded
        assertTrue(state.tier == Subscription.SubscriptionTier.PLUS)
    }

    @Test
    fun `given not a patron exclusive feature, when production full access release, feature available to Plus`() {
        initViewModel(
            currentRelease = productionFullAccessRelease,
            patronExclusiveAccessRelease = productionEarlyAccessRelease,
        )

        val state = upsellViewModel.state.value as UpsellViewModel.UiState.Loaded
        assertTrue(state.tier == Subscription.SubscriptionTier.PLUS)
    }

    /* Early Access Message */
    @Test
    fun `given not a patron exclusive feature, when beta release, early access message not shown`() {
        initViewModel(
            currentRelease = betaEarlyAccessRelease,
            patronExclusiveAccessRelease = productionEarlyAccessRelease,
        )

        val state = upsellViewModel.state.value as UpsellViewModel.UiState.Loaded
        assertFalse(state.showEarlyAccessMessage)
    }

    @Test
    fun `given patron exclusive feature, when production early access release, early access message shown`() {
        val bookmarksFeatureMock = mock<Feature>().apply {
            doReturn(FeatureTier.Plus(productionEarlyAccessRelease)).whenever(this).tier
            doReturn(true).whenever(this).isCurrentlyExclusiveToPatron()
        }
        initViewModel(
            currentRelease = productionEarlyAccessRelease,
            patronExclusiveAccessRelease = productionEarlyAccessRelease,
            bookmarksFeatureMock = bookmarksFeatureMock,
        )

        val state = upsellViewModel.state.value as UpsellViewModel.UiState.Loaded
        assertTrue(state.showEarlyAccessMessage)
    }

    @Test
    fun `given not a patron exclusive feature, when beta full access release, early access message not shown`() {
        initViewModel(
            currentRelease = betaFullAccessRelease,
            patronExclusiveAccessRelease = productionEarlyAccessRelease,
        )

        val state = upsellViewModel.state.value as UpsellViewModel.UiState.Loaded
        assertFalse(state.showEarlyAccessMessage)
    }

    @Test
    fun `given not a patron exclusive feature, when production full access release, early access message not shown`() {
        initViewModel(
            currentRelease = productionFullAccessRelease,
            patronExclusiveAccessRelease = productionEarlyAccessRelease,
        )

        val state = upsellViewModel.state.value as UpsellViewModel.UiState.Loaded
        assertFalse(state.showEarlyAccessMessage)
    }

    private fun initViewModel(
        currentRelease: ReleaseVersion,
        patronExclusiveAccessRelease: ReleaseVersion?,
        bookmarksFeatureMock: Feature? = null,
    ) {
        val bookmarksFeature = bookmarksFeatureMock ?: mock<Feature>().apply {
            doReturn(FeatureTier.Plus(patronExclusiveAccessRelease)).whenever(this).tier
        }
        val releaseVersion = mock<ReleaseVersionWrapper>().apply {
            doReturn(currentRelease).whenever(this).currentReleaseVersion
        }
        val feature = mock<FeatureWrapper>().apply {
            doReturn(bookmarksFeature).whenever(this).bookmarksFeature
        }
        upsellViewModel = UpsellViewModel(
            analyticsTracker = mock(),
            subscriptionManager = subscriptionManager,
            feature = feature,
            releaseVersion = releaseVersion,
        )
    }
}

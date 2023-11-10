package au.com.shiftyjelly.pocketcasts.player.view.bookmark.components

import au.com.shiftyjelly.pocketcasts.models.type.Subscription.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.repositories.subscription.FreeTrial
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureTier
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureWrapper
import au.com.shiftyjelly.pocketcasts.utils.featureflag.ReleaseVersion
import au.com.shiftyjelly.pocketcasts.utils.featureflag.ReleaseVersionWrapper
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
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

    private lateinit var upsellViewModel: UpsellViewModel

    private val betaEarlyAccessRelease = ReleaseVersion(7, 50, null, 1)
    private val productionEarlyAccessRelease = ReleaseVersion(7, 50)
    private val betaFullAccessRelease = ReleaseVersion(7, 51, null, 1)
    private val productionFullAccessRelease = ReleaseVersion(7, 51)

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
        assertTrue(state.freeTrial.subscriptionTier == SubscriptionTier.PLUS)
    }

    @Test
    fun `given patron exclusive feature, when production early access release, feature available to Patron`() {
        initViewModel(
            currentRelease = productionEarlyAccessRelease,
            patronExclusiveAccessRelease = productionEarlyAccessRelease,
        )

        val state = upsellViewModel.state.value as UpsellViewModel.UiState.Loaded
        assertTrue(state.freeTrial.subscriptionTier == SubscriptionTier.PATRON)
    }

    @Test
    fun `given not a patron exclusive feature, when production release, feature available to Plus`() {
        initViewModel(
            currentRelease = productionEarlyAccessRelease,
            patronExclusiveAccessRelease = null,
        )

        val state = upsellViewModel.state.value as UpsellViewModel.UiState.Loaded
        assertTrue(state.freeTrial.subscriptionTier == SubscriptionTier.PLUS)
    }

    @Test
    fun `given not a patron exclusive feature, when beta full access release, feature available to Plus`() {
        initViewModel(
            currentRelease = betaFullAccessRelease,
            patronExclusiveAccessRelease = productionEarlyAccessRelease,
        )

        val state = upsellViewModel.state.value as UpsellViewModel.UiState.Loaded
        assertTrue(state.freeTrial.subscriptionTier == SubscriptionTier.PLUS)
    }

    @Test
    fun `given not a patron exclusive feature, when production full access release, feature available to Plus`() {
        initViewModel(
            currentRelease = productionFullAccessRelease,
            patronExclusiveAccessRelease = productionEarlyAccessRelease,
        )

        val state = upsellViewModel.state.value as UpsellViewModel.UiState.Loaded
        assertTrue(state.freeTrial.subscriptionTier == SubscriptionTier.PLUS)
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
        val releaseVersionMock = mock<ReleaseVersionWrapper>().apply {
            doReturn(productionEarlyAccessRelease).whenever(this).currentReleaseVersion
        }

        val bookmarksFeatureMock = mock<Feature>().apply {
            doReturn(FeatureTier.Plus(productionEarlyAccessRelease)).whenever(this).tier
            doReturn(true).whenever(this).isCurrentlyExclusiveToPatron(releaseVersionMock)
        }
        initViewModel(
            currentRelease = productionEarlyAccessRelease,
            patronExclusiveAccessRelease = productionEarlyAccessRelease,
            releaseVersionMock = releaseVersionMock,
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
        releaseVersionMock: ReleaseVersionWrapper? = null,
    ) {
        whenever(subscriptionManager.freeTrialForSubscriptionTierFlow(SubscriptionTier.PATRON))
            .thenReturn(flowOf(FreeTrial(subscriptionTier = SubscriptionTier.PATRON)))

        whenever(subscriptionManager.freeTrialForSubscriptionTierFlow(SubscriptionTier.PLUS))
            .thenReturn(flowOf(FreeTrial(subscriptionTier = SubscriptionTier.PLUS)))

        val bookmarksFeature = bookmarksFeatureMock ?: mock<Feature>().apply {
            doReturn(FeatureTier.Plus(patronExclusiveAccessRelease)).whenever(this).tier
        }
        val releaseVersion = releaseVersionMock ?: mock<ReleaseVersionWrapper>().apply {
            doReturn(currentRelease).whenever(this).currentReleaseVersion
        }
        val feature = mock<FeatureWrapper>().apply {
            doReturn(bookmarksFeature).whenever(this).bookmarksFeature
        }
        upsellViewModel = UpsellViewModel(
            analyticsTracker = mock(),
            feature = feature,
            releaseVersion = releaseVersion,
            subscriptionManager = subscriptionManager,
        )
    }
}

package au.com.shiftyjelly.pocketcasts.player.view.bookmark.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionMapper
import au.com.shiftyjelly.pocketcasts.repositories.subscription.ProductDetailsState
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.utils.featureflag.EarlyAccessState
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureTier
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureWrapper
import au.com.shiftyjelly.pocketcasts.utils.featureflag.ReleaseVersion.Companion.comparedToEarlyPatronAccess
import au.com.shiftyjelly.pocketcasts.utils.featureflag.ReleaseVersionWrapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import javax.inject.Inject

@HiltViewModel
class UpsellViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val subscriptionManager: SubscriptionManager,
    private val feature: FeatureWrapper,
    private val releaseVersion: ReleaseVersionWrapper,
) : ViewModel() {

    private val _state: MutableStateFlow<UiState> = MutableStateFlow(UiState.Loading)
    val state: StateFlow<UiState> = _state

    init {
        viewModelScope.launch {
            subscriptionManager
                .observeProductDetails()
                .asFlow()
                .stateIn(viewModelScope)
                .collect { productDetails ->
                    val subscriptions = when (productDetails) {
                        is ProductDetailsState.Error -> null
                        is ProductDetailsState.Loaded -> productDetails.productDetails.mapNotNull { productDetailsState ->
                            Subscription.fromProductDetails(
                                productDetails = productDetailsState,
                                isFreeTrialEligible = subscriptionManager.isFreeTrialEligible(
                                    SubscriptionMapper.mapProductIdToTier(productDetailsState.productId)
                                )
                            )
                        }
                    } ?: emptyList()
                    updateState(subscriptions)
                }
        }
    }

    private fun updateState(
        subscriptions: List<Subscription>,
    ) {
        val bookmarksFeature = feature.bookmarksFeature
        val patronExclusiveAccessRelease = (bookmarksFeature.tier as? FeatureTier.Plus)?.patronExclusiveAccessRelease

        val isReleaseCandidate = releaseVersion.currentReleaseVersion.releaseCandidate != null
        val relativeToEarlyPatronAccess = patronExclusiveAccessRelease?.let {
            releaseVersion.currentReleaseVersion.comparedToEarlyPatronAccess(it)
        }
        val availableForFeatureTier = when (relativeToEarlyPatronAccess) {
            null -> bookmarksFeature.tier
            EarlyAccessState.Before,
            EarlyAccessState.During -> if (isReleaseCandidate) bookmarksFeature.tier else FeatureTier.Patron
            EarlyAccessState.After -> bookmarksFeature.tier
        }
        val subscriptionTier = availableForFeatureTier.toSubscriptionTier()
        val updatedSubscriptions = subscriptions.filter { it.tier == subscriptionTier }

        // Check if subscription has a free trial
        val selectedSubscription = subscriptionManager.getDefaultSubscription(
            tier = subscriptionTier,
            subscriptions = updatedSubscriptions,
        )

        _state.update {
            UiState.Loaded(
                tier = subscriptionTier,
                hasFreeTrial = selectedSubscription?.trialPricingPhase != null,
                showEarlyAccessMessage = bookmarksFeature.isCurrentlyExclusiveToPatron(),
            )
        }
    }

    fun onClick(sourceView: SourceView) {
        analyticsTracker.track(
            AnalyticsEvent.BOOKMARKS_UPGRADE_BUTTON_TAPPED,
            mapOf("source" to sourceView.analyticsValue)
        )
    }

    private fun FeatureTier.toSubscriptionTier() = when (this) {
        FeatureTier.Patron -> SubscriptionTier.PATRON
        is FeatureTier.Plus -> SubscriptionTier.PLUS
        FeatureTier.Free -> SubscriptionTier.UNKNOWN
    }

    sealed class UiState {
        object Loading : UiState()
        data class Loaded(
            val tier: SubscriptionTier,
            val hasFreeTrial: Boolean,
            val showEarlyAccessMessage: Boolean,
        ) : UiState()
    }
}

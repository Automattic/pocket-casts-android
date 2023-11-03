package au.com.shiftyjelly.pocketcasts.player.view.bookmark.components

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.repositories.subscription.FreeTrial
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
import javax.inject.Inject

@HiltViewModel
class UpsellViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val subscriptionManager: SubscriptionManager,
    feature: FeatureWrapper,
    private val releaseVersion: ReleaseVersionWrapper,
) : ViewModel() {

    private val _state: MutableStateFlow<UiState> = MutableStateFlow(UiState.Loading)
    val state: StateFlow<UiState> = _state

    init {
        val bookmarksFeature = feature.bookmarksFeature
        val patronExclusiveAccessRelease = (bookmarksFeature.tier as? FeatureTier.Plus)?.patronExclusiveAccessRelease

        val isReleaseCandidate = releaseVersion.currentReleaseVersion.releaseCandidate != null
        val relativeToEarlyPatronAccess = patronExclusiveAccessRelease?.let {
            releaseVersion.currentReleaseVersion.comparedToEarlyPatronAccess(it)
        }
        val availableForFeatureTier = when (relativeToEarlyPatronAccess) {
            null -> bookmarksFeature.tier
            EarlyAccessState.Before,
            EarlyAccessState.During,
            -> if (isReleaseCandidate) bookmarksFeature.tier else FeatureTier.Patron

            EarlyAccessState.After -> bookmarksFeature.tier
        }
        val subscriptionTier = availableForFeatureTier.toSubscriptionTier()

        viewModelScope.launch {
            subscriptionManager.freeTrialForSubscriptionTierFlow(subscriptionTier)
                .stateIn(this)
                .collect { freeTrial ->
                    _state.update {
                        UiState.Loaded(
                            freeTrial = freeTrial,
                            showEarlyAccessMessage = bookmarksFeature.isCurrentlyExclusiveToPatron(releaseVersion),
                        )
                    }
                }
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
            val freeTrial: FreeTrial,
            val showEarlyAccessMessage: Boolean,
        ) : UiState()
    }
}

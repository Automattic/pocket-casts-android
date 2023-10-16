package au.com.shiftyjelly.pocketcasts.settings.whatsnew

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionMapper
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.subscription.ProductDetailsState
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.utils.featureflag.EarlyAccessState
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlagWrapper
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureTier
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureWrapper
import au.com.shiftyjelly.pocketcasts.utils.featureflag.ReleaseVersion.Companion.comparedToEarlyPatronAccess
import au.com.shiftyjelly.pocketcasts.utils.featureflag.ReleaseVersionWrapper
import au.com.shiftyjelly.pocketcasts.utils.featureflag.UserTier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class WhatsNewViewModel @Inject constructor(
    private val subscriptionManager: SubscriptionManager,
    private val settings: Settings,
    private val releaseVersion: ReleaseVersionWrapper,
    private val feature: FeatureWrapper,
    featureFlag: FeatureFlagWrapper,
) : ViewModel() {
    private val _state: MutableStateFlow<UiState> = MutableStateFlow(UiState.Loading)
    val state = _state.asStateFlow()

    private val _navigationState: MutableSharedFlow<NavigationState> = MutableSharedFlow()
    val navigationState = _navigationState.asSharedFlow()

    init {
        val isBookmarksEnabled = featureFlag.isEnabled(feature.bookmarksFeature)
        if (isBookmarksEnabled) {
            updateStateForBookmarks()
        } else {
            _state.value = UiState.Loaded(
                feature = WhatsNewFeature.AutoPlay,
                tier = UserTier.Free,
            )
        }
    }

    private fun updateStateForBookmarks() {
        val userTier = settings.userTier
        val isUserEntitled = feature.isUserEntitled(feature.bookmarksFeature, userTier)
        if (isUserEntitled) {
            _state.value = UiState.Loaded(
                feature = bookmarksFeature(isUserEntitled = true),
                tier = userTier,
            )
        } else {
            viewModelScope.launch {
                subscriptionManager
                    .observeProductDetails()
                    .asFlow()
                    .stateIn(viewModelScope)
                    .collect { productDetails ->
                        val subscriptions = when (productDetails) {
                            is ProductDetailsState.Error -> null
                            is ProductDetailsState.Loaded -> productDetails.productDetails.mapNotNull { productDetailsState ->
                                // Get subscriptions from product details to check if trial exists
                                Subscription.fromProductDetails(
                                    productDetails = productDetailsState,
                                    isFreeTrialEligible = subscriptionManager.isFreeTrialEligible(
                                        SubscriptionMapper.mapProductIdToTier(productDetailsState.productId)
                                    )
                                )
                            }
                        } ?: emptyList()

                        val availableForFeatureTier = if (feature.bookmarksFeature.isCurrentlyExclusiveToPatron(releaseVersion)) {
                            FeatureTier.Patron
                        } else {
                            feature.bookmarksFeature.tier
                        }
                        val subscriptionTier = availableForFeatureTier.toSubscriptionTier()
                        val trialExists = subscriptionManager.trialExists(
                            tier = subscriptionTier,
                            subscriptions = subscriptions,
                        )

                        _state.value = UiState.Loaded(
                            feature = bookmarksFeature(
                                trialExists = trialExists,
                                isUserEntitled = false,
                                subscriptionTier = subscriptionTier,
                            ),
                            tier = userTier,
                        )
                    }
            }
        }
    }

    private fun bookmarksFeature(
        trialExists: Boolean = false,
        isUserEntitled: Boolean,
        subscriptionTier: Subscription.SubscriptionTier? = null,
    ) = WhatsNewFeature.Bookmarks(
        title = titleResId(),
        message = if (isUserEntitled) LR.string.whats_new_bookmarks_body else LR.string.bookmarks_upsell_instructions,
        hasFreeTrial = trialExists,
        isUserEntitled = isUserEntitled,
        subscriptionTier = subscriptionTier,
    )

    private fun titleResId(): Int {
        val bookmarksFeature = feature.bookmarksFeature
        val patronExclusiveAccessRelease = (bookmarksFeature.tier as? FeatureTier.Plus)?.patronExclusiveAccessRelease

        val isReleaseCandidate = releaseVersion.currentReleaseVersion.releaseCandidate != null
        val relativeToEarlyPatronAccess = patronExclusiveAccessRelease?.let {
            releaseVersion.currentReleaseVersion.comparedToEarlyPatronAccess(it)
        }

        val showJoinBeta = when (relativeToEarlyPatronAccess) {
            EarlyAccessState.Before,
            EarlyAccessState.During -> isReleaseCandidate
            EarlyAccessState.After -> false
            null -> false
        }

        return if (showJoinBeta) {
            LR.string.whats_new_boomarks_join_beta_testing_title
        } else {
            LR.string.whats_new_bookmarks_title
        }
    }

    fun onConfirm() {
        viewModelScope.launch {
            val currentState = state.value as? UiState.Loaded ?: return@launch
            val target = when (currentState.feature) {
                is WhatsNewFeature.AutoPlay -> NavigationState.PlaybackSettings
                is WhatsNewFeature.Bookmarks -> if (currentState.feature.isUserEntitled) {
                    NavigationState.HeadphoneControlsSettings
                } else {
                    NavigationState.StartUpsellFlow
                }
            }
            _navigationState.emit(target)
        }
    }

    private fun FeatureTier.toSubscriptionTier() = when (this) {
        is FeatureTier.Patron -> Subscription.SubscriptionTier.PATRON
        is FeatureTier.Plus -> Subscription.SubscriptionTier.PLUS
        is FeatureTier.Free -> Subscription.SubscriptionTier.UNKNOWN
    }

    sealed class UiState {
        object Loading : UiState()
        data class Loaded(
            val feature: WhatsNewFeature,
            val tier: UserTier,
        ) : UiState()
    }

    sealed class WhatsNewFeature(
        @StringRes open val title: Int,
        @StringRes open val message: Int,
        @StringRes val confirmButtonTitle: Int,
        @StringRes val closeButtonTitle: Int? = null,
    ) {
        object AutoPlay : WhatsNewFeature(
            title = LR.string.whats_new_autoplay_title,
            message = LR.string.whats_new_autoplay_body,
            confirmButtonTitle = LR.string.whats_new_autoplay_enable_button,
            closeButtonTitle = LR.string.whats_new_autoplay_maybe_later_button,
        )

        data class Bookmarks(
            @StringRes override val title: Int,
            @StringRes override val message: Int,
            val hasFreeTrial: Boolean,
            val isUserEntitled: Boolean,
            val subscriptionTier: Subscription.SubscriptionTier? = null, // To show subscription when user is not entitled to the feature
        ) : WhatsNewFeature(
            title = title,
            message = message,
            confirmButtonTitle = LR.string.whats_new_bookmarks_try_now_button,
        )
    }

    sealed class NavigationState {
        object PlaybackSettings : NavigationState()
        object HeadphoneControlsSettings : NavigationState()
        object StartUpsellFlow : NavigationState()
    }
}

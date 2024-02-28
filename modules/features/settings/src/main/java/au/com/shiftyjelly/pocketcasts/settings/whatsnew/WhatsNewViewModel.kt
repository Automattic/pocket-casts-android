package au.com.shiftyjelly.pocketcasts.settings.whatsnew

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionFrequency
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
import au.com.shiftyjelly.pocketcasts.utils.earlyaccess.EarlyAccessStrings
import au.com.shiftyjelly.pocketcasts.utils.featureflag.BookmarkFeatureControl
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.featureflag.UserTier
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class WhatsNewViewModel @Inject constructor(
    private val playbackManager: PlaybackManager,
    private val subscriptionManager: SubscriptionManager,
    private val settings: Settings,
    private val bookmarkFeature: BookmarkFeatureControl,
) : ViewModel() {
    private val _state: MutableStateFlow<UiState> = MutableStateFlow(UiState.Loading)
    val state = _state.asStateFlow()

    private val _navigationState: MutableSharedFlow<NavigationState> = MutableSharedFlow()
    val navigationState = _navigationState.asSharedFlow()

    init {
        viewModelScope.launch {
            settings.cachedSubscriptionStatus.flow.stateIn(viewModelScope).collect { subscriptionStatus ->
                if (FeatureFlag.isEnabled(Feature.SLUMBER_STUDIOS_YEARLY_PROMO)) {
                    val previousUserTier = (_state.value as? UiState.Loaded)?.tier
                    // Close what's new if plan changed but user is not eligible for slumber studios promo
                    // This is done so that user doesn't subscribe from the what's new to yearly plan now and gets subscribed to two plans
                    if (previousUserTier != null &&
                        !isEligibleForSlumberStudiosPromo(subscriptionStatus)
                    ) {
                        _navigationState.emit(NavigationState.SlumberStudiosClose)
                    } else {
                        updateStateForSlumberStudiosPromo(subscriptionStatus)
                    }
                } else {
                    updateStateForBookmarks()
                }
            }
        }
    }

    private fun updateStateForSlumberStudiosPromo(subscriptionStatus: SubscriptionStatus?) {
        val userTier = settings.userTier
        if (isEligibleForSlumberStudiosPromo(subscriptionStatus)) {
            _state.value = UiState.Loaded(
                feature = WhatsNewFeature.SlumberStudiosPromo(
                    promoCode = settings.getSlumberStudiosPromoCode(),
                    message = LR.string.whats_new_slumber_studios_body,
                ),
                fullModel = true,
                tier = userTier,
            )
        } else {
            _state.value = UiState.Loaded(
                feature = WhatsNewFeature.SlumberStudiosPromo(
                    message = LR.string.whats_new_slumber_studios_body_free_user,
                    isUserEntitled = false,
                ),
                fullModel = true,
                tier = userTier,
            )
        }
    }

    private fun updateStateForBookmarks() {
        val userTier = settings.userTier
        val isUserEntitled = bookmarkFeature.isAvailable(userTier)
        if (isUserEntitled) {
            _state.value = UiState.Loaded(
                feature = bookmarksFeature(isUserEntitled = true),
                tier = userTier,
            )
        } else {
            viewModelScope.launch {
                val subscriptionTier = Subscription.SubscriptionTier.PLUS

                subscriptionManager
                    .freeTrialForSubscriptionTierFlow(subscriptionTier)
                    .stateIn(viewModelScope)
                    .collect { freeTrial ->
                        _state.value = UiState.Loaded(
                            feature = bookmarksFeature(
                                trialExists = freeTrial.exists,
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
    ): WhatsNewFeature.Bookmarks {
        val currentEpisode = playbackManager.getCurrentEpisode()
        return WhatsNewFeature.Bookmarks(
            title = EarlyAccessStrings.getAppropriateTextResource(LR.string.whats_new_bookmarks_title),
            message = if (isUserEntitled) LR.string.whats_new_bookmarks_body else LR.string.bookmarks_upsell_instructions,
            confirmButtonTitle = if (currentEpisode == null) LR.string.whats_new_bookmarks_enable_now_button else LR.string.whats_new_bookmarks_try_now_button,
            hasOffer = trialExists,
            isUserEntitled = isUserEntitled,
            subscriptionTier = subscriptionTier,
        )
    }

    fun onConfirm() {
        viewModelScope.launch {
            val currentState = state.value as? UiState.Loaded ?: return@launch
            val currentEpisode = playbackManager.getCurrentEpisode()
            val target = when (currentState.feature) {
                is WhatsNewFeature.Bookmarks -> if (currentState.feature.isUserEntitled) {
                    if (currentEpisode == null) {
                        NavigationState.HeadphoneControlsSettings
                    } else {
                        NavigationState.FullScreenPlayerScreen
                    }
                } else {
                    NavigationState.StartUpsellFlow(OnboardingUpgradeSource.BOOKMARKS)
                }

                is WhatsNewFeature.SlumberStudiosPromo -> if (currentState.feature.isUserEntitled) {
                    NavigationState.SlumberStudiosRedeemPromoCode
                } else {
                    NavigationState.StartUpsellFlow(
                        source = OnboardingUpgradeSource.SLUMBER_STUDIOS,
                        shouldCloseOnConfirm = false,
                    )
                }
            }
            _navigationState.emit(target)
        }
    }

    private fun isEligibleForSlumberStudiosPromo(subscriptionStatus: SubscriptionStatus?) =
        (subscriptionStatus as? SubscriptionStatus.Paid)?.let { paidSubscription ->
            paidSubscription.isLifetimePlus || paidSubscription.frequency == SubscriptionFrequency.YEARLY
        } ?: false

    sealed class UiState {
        data object Loading : UiState()
        data class Loaded(
            val feature: WhatsNewFeature,
            val tier: UserTier,
            val fullModel: Boolean = false,
        ) : UiState()
    }

    sealed class WhatsNewFeature(
        @StringRes open val title: Int,
        @StringRes open val message: Int,
        @StringRes open val confirmButtonTitle: Int,
        @StringRes val closeButtonTitle: Int? = null,
    ) {
        abstract val hasOffer: Boolean
        abstract val isUserEntitled: Boolean
        abstract val subscriptionTier: Subscription.SubscriptionTier? // To show subscription when user is not entitled to the feature

        data class Bookmarks(
            @StringRes override val title: Int,
            @StringRes override val message: Int,
            @StringRes override val confirmButtonTitle: Int,
            override val hasOffer: Boolean,
            override val isUserEntitled: Boolean,
            override val subscriptionTier: Subscription.SubscriptionTier? = null,
        ) : WhatsNewFeature(
            title = title,
            message = message,
            confirmButtonTitle = confirmButtonTitle,
        )

        data class SlumberStudiosPromo(
            val promoCode: String = "",
            @StringRes override val message: Int,
            override val hasOffer: Boolean = false,
            override val isUserEntitled: Boolean = true,
            override val subscriptionTier: Subscription.SubscriptionTier? = null,
        ) : WhatsNewFeature(
            title = LR.string.whats_new_slumber_studios_title,
            message = message,
            confirmButtonTitle = LR.string.whats_new_slumber_studios_redeem_now_button,
        )
    }

    sealed class NavigationState(
        open val shouldCloseOnConfirm: Boolean = true,
    ) {
        data object HeadphoneControlsSettings : NavigationState()
        data object FullScreenPlayerScreen : NavigationState()
        data class StartUpsellFlow(
            val source: OnboardingUpgradeSource,
            override val shouldCloseOnConfirm: Boolean = true,
        ) : NavigationState()
        data object SlumberStudiosRedeemPromoCode : NavigationState(shouldCloseOnConfirm = false)
        data object SlumberStudiosClose : NavigationState()
    }
}

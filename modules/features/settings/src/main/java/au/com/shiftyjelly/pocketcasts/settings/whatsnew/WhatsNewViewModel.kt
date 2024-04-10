package au.com.shiftyjelly.pocketcasts.settings.whatsnew

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.models.type.Subscription.SubscriptionTier
import au.com.shiftyjelly.pocketcasts.models.type.SubscriptionFrequency
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingUpgradeSource
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
    private val settings: Settings,
) : ViewModel() {
    private val _state: MutableStateFlow<UiState> = MutableStateFlow(UiState.Loading)
    val state = _state.asStateFlow()

    private val _navigationState: MutableSharedFlow<NavigationState> = MutableSharedFlow()
    val navigationState = _navigationState.asSharedFlow()

    init {
        _state.value = UiState.Loaded(
            feature = WhatsNewFeature.NewWidgets,
            fullModel = true,
            tier = UserTier.Free,
        )
        viewModelScope.launch {
            settings.cachedSubscriptionStatus.flow.stateIn(viewModelScope).collect { subscriptionStatus ->
                if (FeatureFlag.isEnabled(Feature.DESELECT_CHAPTERS)) {
                    updateStateForDeselectChapters()
                } else if (FeatureFlag.isEnabled(Feature.SLUMBER_STUDIOS_YEARLY_PROMO)) {
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

    private fun updateStateForDeselectChapters() {
        val userTier = settings.userTier
        val isUserEntitled = Feature.isUserEntitled(Feature.DESELECT_CHAPTERS, userTier)
        if (isUserEntitled) {
            _state.value = UiState.Loaded(
                feature = deselectChaptersFeature(
                    messageRes = when (userTier) {
                        UserTier.Patron -> LR.string.whats_new_deselect_chapters_patron_message
                        UserTier.Plus, UserTier.Free -> LR.string.whats_new_deselect_chapters_plus_message
                    },
                    isUserEntitled = true,
                ),
                fullModel = true,
                tier = userTier,
            )
        } else {
            _state.value = UiState.Loaded(
                feature = deselectChaptersFeature(
                    messageRes = if (SubscriptionTier.fromFeatureTier(Feature.DESELECT_CHAPTERS) == SubscriptionTier.PATRON) {
                        LR.string.whats_new_deselect_chapters_subscribe_to_patron_message
                    } else {
                        LR.string.whats_new_deselect_chapters_subscribe_to_plus_message
                    },
                    isUserEntitled = false,
                ),
                fullModel = true,
                tier = userTier,
            )
        }
    }

    private fun deselectChaptersFeature(
        @StringRes messageRes: Int,
        isUserEntitled: Boolean,
    ) = WhatsNewFeature.DeselectChapters(
        message = messageRes,
        confirmButtonTitle = if (isUserEntitled) {
            LR.string.whats_new_got_it_button
        } else {
            if (SubscriptionTier.fromFeatureTier(Feature.DESELECT_CHAPTERS) == SubscriptionTier.PLUS) {
                LR.string.upgrade_to_plus
            } else {
                LR.string.upgrade_to_patron
            }
        },
        isUserEntitled = isUserEntitled,
    )

    fun onConfirm() {
        viewModelScope.launch {
            val currentState = state.value as? UiState.Loaded ?: return@launch
            val target = when (currentState.feature) {
                is WhatsNewFeature.SlumberStudiosPromo -> if (currentState.feature.isUserEntitled) {
                    NavigationState.SlumberStudiosRedeemPromoCode
                } else {
                    NavigationState.StartUpsellFlow(
                        source = OnboardingUpgradeSource.SLUMBER_STUDIOS,
                        shouldCloseOnConfirm = false,
                    )
                }

                is WhatsNewFeature.DeselectChapters -> if (currentState.feature.isUserEntitled) {
                    NavigationState.DeselectChapterClose
                } else {
                    NavigationState.StartUpsellFlow(OnboardingUpgradeSource.WHATS_NEW_SKIP_CHAPTERS)
                }

                is WhatsNewFeature.NewWidgets -> NavigationState.NewWidgetsClose
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

    sealed interface WhatsNewFeature {
        @get:StringRes val title: Int

        @get:StringRes val message: Int

        @get:StringRes val confirmButtonTitle: Int

        @get:StringRes val closeButtonTitle: Int? get() = null
        val hasOffer: Boolean
        val isUserEntitled: Boolean
        val subscriptionTier: SubscriptionTier? // To show subscription when user is not entitled to the feature

        data class SlumberStudiosPromo(
            val promoCode: String = "",
            @StringRes override val message: Int,
            override val hasOffer: Boolean = false,
            override val isUserEntitled: Boolean = true,
            override val subscriptionTier: SubscriptionTier? = null,
        ) : WhatsNewFeature {
            override val title = LR.string.whats_new_slumber_studios_title
            override val confirmButtonTitle = LR.string.whats_new_slumber_studios_redeem_now_button
        }

        data class DeselectChapters(
            @StringRes override val message: Int,
            @StringRes override val confirmButtonTitle: Int,
            override val hasOffer: Boolean = false,
            override val isUserEntitled: Boolean = true,
            override val subscriptionTier: SubscriptionTier? = null,
        ) : WhatsNewFeature {
            override val title = LR.string.skip_chapters
        }

        data object NewWidgets : WhatsNewFeature {
            override val title = LR.string.whats_new_new_widgets_title
            override val message = LR.string.whats_new_new_widgets_message
            override val confirmButtonTitle = LR.string.whats_new_got_it_button
            override val hasOffer = false
            override val isUserEntitled = true
            override val subscriptionTier = null
        }
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
        data object DeselectChapterClose : NavigationState()
        data object NewWidgetsClose : NavigationState()
    }
}

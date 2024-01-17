package au.com.shiftyjelly.pocketcasts.settings.whatsnew

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.models.type.Subscription
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.subscription.SubscriptionManager
import au.com.shiftyjelly.pocketcasts.utils.earlyaccess.EarlyAccessStrings
import au.com.shiftyjelly.pocketcasts.utils.featureflag.BookmarkFeatureControl
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
        updateStateForBookmarks()
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
            hasFreeTrial = trialExists,
            isUserEntitled = isUserEntitled,
            subscriptionTier = subscriptionTier,
        )
    }
    fun onConfirm() {
        viewModelScope.launch {
            val currentState = state.value as? UiState.Loaded ?: return@launch
            val currentEpisode = playbackManager.getCurrentEpisode()
            val target = when (currentState.feature) {
                is WhatsNewFeature.AutoPlay -> NavigationState.PlaybackSettings
                is WhatsNewFeature.Bookmarks -> if (currentState.feature.isUserEntitled) {
                    if (currentEpisode == null) {
                        NavigationState.HeadphoneControlsSettings
                    } else {
                        NavigationState.FullScreenPlayerScreen
                    }
                } else {
                    NavigationState.StartUpsellFlow
                }
            }
            _navigationState.emit(target)
        }
    }

    sealed class UiState {
        data object Loading : UiState()
        data class Loaded(
            val feature: WhatsNewFeature,
            val tier: UserTier,
        ) : UiState()
    }

    sealed class WhatsNewFeature(
        @StringRes open val title: Int,
        @StringRes open val message: Int,
        @StringRes open val confirmButtonTitle: Int,
        @StringRes val closeButtonTitle: Int? = null,
    ) {
        data object AutoPlay : WhatsNewFeature(
            title = LR.string.whats_new_autoplay_title,
            message = LR.string.whats_new_autoplay_body,
            confirmButtonTitle = LR.string.whats_new_autoplay_enable_button,
            closeButtonTitle = LR.string.whats_new_autoplay_maybe_later_button,
        )

        data class Bookmarks(
            @StringRes override val title: Int,
            @StringRes override val message: Int,
            @StringRes override val confirmButtonTitle: Int,
            val hasFreeTrial: Boolean,
            val isUserEntitled: Boolean,
            val subscriptionTier: Subscription.SubscriptionTier? = null, // To show subscription when user is not entitled to the feature
        ) : WhatsNewFeature(
            title = title,
            message = message,
            confirmButtonTitle = confirmButtonTitle,
        )
    }

    sealed class NavigationState {
        data object PlaybackSettings : NavigationState()
        data object HeadphoneControlsSettings : NavigationState()
        data object FullScreenPlayerScreen : NavigationState()
        data object StartUpsellFlow : NavigationState()
    }
}

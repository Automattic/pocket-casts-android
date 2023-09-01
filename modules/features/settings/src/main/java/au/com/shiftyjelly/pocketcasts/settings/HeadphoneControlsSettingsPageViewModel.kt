package au.com.shiftyjelly.pocketcasts.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.featureflag.UserTier
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.HeadphoneAction
import au.com.shiftyjelly.pocketcasts.repositories.user.UserManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.reactive.asFlow
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HeadphoneControlsSettingsPageViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val userManager: UserManager,
    private val settings: Settings,
) : ViewModel() {

    private val _state: MutableStateFlow<UiState> = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    init {
        viewModelScope.launch {
            userManager.getSignInState().asFlow()
                .stateIn(viewModelScope)
                .collect {
                    val userTier = (settings.cachedSubscriptionStatus.value as? SubscriptionStatus.Paid)?.tier?.toUserTier() ?: UserTier.Free
                    val isAddBookmarkEnabled = FeatureFlag.isEnabled(Feature.BOOKMARKS_ENABLED) &&
                        Feature.isAvailable(Feature.BOOKMARKS_ENABLED, userTier)

                    _state.update { state -> state.copy(isAddBookmarkEnabled = isAddBookmarkEnabled) }

                    _state.value.startUpsellFromSource?.let { upsellFrom ->
                        onUpsellComplete(upsellFrom)
                    }
                }
        }
    }

    private fun onUpsellComplete(
        upsellSourceAction: UpsellSourceAction,
    ) {
        if (state.value.isAddBookmarkEnabled) {
            when (upsellSourceAction) {
                UpsellSourceAction.PREVIOUS -> onPreviousActionSave(HeadphoneAction.ADD_BOOKMARK)
                UpsellSourceAction.NEXT -> onNextActionSave(HeadphoneAction.ADD_BOOKMARK)
            }
            resetUpsellSourceAction()
        }
    }

    fun onShown() {
        analyticsTracker.track(AnalyticsEvent.SETTINGS_HEADPHONE_CONTROLS_SHOWN)
    }

    fun onConfirmationSoundChanged(playConfirmationSound: Boolean) {
        analyticsTracker.track(
            AnalyticsEvent.SETTINGS_HEADPHONE_CONTROLS_BOOKMARK_CONFIRMATION_SOUND,
            mapOf("value" to playConfirmationSound)
        )
    }

    fun onNextActionSave(action: HeadphoneAction) {
        if (action.canSave()) {
            settings.headphoneControlsNextAction.set(action)
            trackHeadphoneAction(action, AnalyticsEvent.SETTINGS_HEADPHONE_CONTROLS_NEXT_CHANGED)
        } else {
            _state.update { it.copy(startUpsellFromSource = UpsellSourceAction.NEXT) }
        }
    }

    fun onPreviousActionSave(action: HeadphoneAction) {
        if (action.canSave()) {
            settings.headphoneControlsPreviousAction.set(action)
            trackHeadphoneAction(action, AnalyticsEvent.SETTINGS_HEADPHONE_CONTROLS_PREVIOUS_CHANGED)
        } else {
            _state.update { it.copy(startUpsellFromSource = UpsellSourceAction.PREVIOUS) }
        }
    }

    fun onOptionsDialogShown() {
        /* Upsell source action is reset here so that upsell can be re-triggered
           from the options dialog if the previous upsell flow was not complete. */
        resetUpsellSourceAction()
    }

    private fun resetUpsellSourceAction() {
        _state.update { it.copy(startUpsellFromSource = null) }
    }

    private fun trackHeadphoneAction(action: HeadphoneAction, event: AnalyticsEvent) {
        analyticsTracker.track(event, mapOf("value" to action.analyticsValue))
    }

    private fun HeadphoneAction.canSave() = when (this) {
        HeadphoneAction.SKIP_BACK,
        HeadphoneAction.SKIP_FORWARD, -> true
        HeadphoneAction.ADD_BOOKMARK -> state.value.isAddBookmarkEnabled
        HeadphoneAction.NEXT_CHAPTER,
        HeadphoneAction.PREVIOUS_CHAPTER -> {
            Timber.e("Headphone action not supported")
            false
        }
    }

    data class UiState(
        val isAddBookmarkEnabled: Boolean = false,
        val startUpsellFromSource: UpsellSourceAction? = null,
    )

    enum class UpsellSourceAction {
        PREVIOUS,
        NEXT,
    }
}

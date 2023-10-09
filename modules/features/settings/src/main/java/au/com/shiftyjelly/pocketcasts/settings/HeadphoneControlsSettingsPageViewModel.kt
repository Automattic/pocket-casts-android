package au.com.shiftyjelly.pocketcasts.settings

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import au.com.shiftyjelly.pocketcasts.compose.images.SubscriptionTierColor
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.models.to.SubscriptionStatus
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.HeadphoneAction
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureTier
import au.com.shiftyjelly.pocketcasts.utils.featureflag.UserTier
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class HeadphoneControlsSettingsPageViewModel @Inject constructor(
    private val analyticsTracker: AnalyticsTrackerWrapper,
    private val settings: Settings,
) : ViewModel() {

    private val _state: MutableStateFlow<UiState> = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    init {
        viewModelScope.launch {
            settings.cachedSubscriptionStatus.flow
                .stateIn(viewModelScope)
                .collect {
                    val userTier = (it as? SubscriptionStatus.Paid)?.tier?.toUserTier() ?: UserTier.Free
                    val isAddBookmarkEnabled = FeatureFlag.isEnabled(Feature.BOOKMARKS_ENABLED) &&
                        Feature.isAvailable(Feature.BOOKMARKS_ENABLED, userTier)

                    _state.update { state ->
                        state.copy(
                            isAddBookmarkEnabled = isAddBookmarkEnabled,
                            addBookmarkIconId = addBookmarkIconId
                                .takeIf { !isAddBookmarkEnabled },
                            addBookmarkIconColor = addBookmarkIconColor
                                .takeIf { !isAddBookmarkEnabled } ?: SubscriptionTierColor.plusGold,
                        )
                    }

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
        val addBookmarkIconId: Int? = null,
        val addBookmarkIconColor: Color = SubscriptionTierColor.plusGold,
    )

    private val addBookmarkIconId
        get() = when {
            Feature.BOOKMARKS_ENABLED.tier is FeatureTier.Patron ||
                Feature.BOOKMARKS_ENABLED.isCurrentlyExclusiveToPatron() -> R.drawable.ic_patron
            Feature.BOOKMARKS_ENABLED.tier is FeatureTier.Plus -> R.drawable.ic_plus
            Feature.BOOKMARKS_ENABLED.tier is FeatureTier.Free -> null
            else -> null
        }

    private val addBookmarkIconColor
        get() = when {
            Feature.BOOKMARKS_ENABLED.tier is FeatureTier.Patron ||
                Feature.BOOKMARKS_ENABLED.isCurrentlyExclusiveToPatron() -> SubscriptionTierColor.patronPurple
            Feature.BOOKMARKS_ENABLED.tier is FeatureTier.Plus -> SubscriptionTierColor.plusGold
            Feature.BOOKMARKS_ENABLED.tier is FeatureTier.Free -> null
            else -> null
        }

    enum class UpsellSourceAction {
        PREVIOUS,
        NEXT,
    }
}

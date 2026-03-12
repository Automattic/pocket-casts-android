package au.com.shiftyjelly.pocketcasts.settings

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.compose.plusGold
import au.com.shiftyjelly.pocketcasts.images.R
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.HeadphoneAction
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.SettingsHeadphoneControlsBookmarkSoundToggledEvent
import com.automattic.eventhorizon.SettingsHeadphoneControlsNextChangedEvent
import com.automattic.eventhorizon.SettingsHeadphoneControlsPreviousChangedEvent
import com.automattic.eventhorizon.SettingsHeadphoneControlsShownEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@HiltViewModel
class HeadphoneControlsSettingsPageViewModel @Inject constructor(
    private val eventHorizon: EventHorizon,
    private val settings: Settings,
) : ViewModel() {

    private val _state: MutableStateFlow<UiState> = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state

    init {
        viewModelScope.launch {
            settings.cachedSubscription.flow
                .stateIn(viewModelScope)
                .collect { subscription ->
                    val isPaidUser = subscription != null

                    _state.update { state ->
                        state.copy(
                            isAddBookmarkEnabled = isPaidUser,
                            addBookmarkIconId = R.drawable.ic_plus.takeIf { !isPaidUser },
                            addBookmarkIconColor = Color.plusGold,
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
        eventHorizon.track(SettingsHeadphoneControlsShownEvent)
    }

    fun onConfirmationSoundChanged(playConfirmationSound: Boolean) {
        eventHorizon.track(
            SettingsHeadphoneControlsBookmarkSoundToggledEvent(
                enabled = playConfirmationSound,
            ),
        )
    }

    fun onNextActionSave(action: HeadphoneAction) {
        if (action.canSave()) {
            settings.headphoneControlsNextAction.set(action, updateModifiedAt = true)
            eventHorizon.track(
                SettingsHeadphoneControlsNextChangedEvent(
                    value = action.analyticsValue,
                ),
            )
        } else {
            _state.update { it.copy(startUpsellFromSource = UpsellSourceAction.NEXT) }
        }
    }

    fun onPreviousActionSave(action: HeadphoneAction) {
        if (action.canSave()) {
            settings.headphoneControlsPreviousAction.set(action, updateModifiedAt = true)
            eventHorizon.track(
                SettingsHeadphoneControlsPreviousChangedEvent(
                    value = action.analyticsValue,
                ),
            )
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

    private fun HeadphoneAction.canSave() = when (this) {
        HeadphoneAction.SKIP_BACK,
        HeadphoneAction.SKIP_FORWARD,
        -> true

        HeadphoneAction.ADD_BOOKMARK -> state.value.isAddBookmarkEnabled

        HeadphoneAction.NEXT_CHAPTER,
        HeadphoneAction.PREVIOUS_CHAPTER,
        -> {
            Timber.e("Headphone action not supported")
            false
        }
    }

    data class UiState(
        val isAddBookmarkEnabled: Boolean = false,
        val startUpsellFromSource: UpsellSourceAction? = null,
        val addBookmarkIconId: Int? = null,
        val addBookmarkIconColor: Color = Color.plusGold,
    )

    enum class UpsellSourceAction {
        PREVIOUS,
        NEXT,
    }
}

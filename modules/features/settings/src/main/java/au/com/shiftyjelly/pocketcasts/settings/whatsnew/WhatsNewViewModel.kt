package au.com.shiftyjelly.pocketcasts.settings.whatsnew

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class WhatsNewViewModel @Inject constructor() : ViewModel() {
    private val _state: MutableStateFlow<UiState> = MutableStateFlow(UiState.Loading)
    val state = _state.asStateFlow()

    private val _navigationState: MutableSharedFlow<NavigationState> = MutableSharedFlow()
    val navigationState = _navigationState.asSharedFlow()

    init {
        _state.value = UiState.Loaded(
            feature = WhatsNewFeature.AutoPlay,
        )
    }

    fun onConfirm() {
        viewModelScope.launch {
            _navigationState.emit(NavigationState.PlaybackSettings)
        }
    }

    sealed class UiState {
        object Loading : UiState()
        data class Loaded(
            val feature: WhatsNewFeature,
        ) : UiState()
    }

    sealed class WhatsNewFeature(
        @StringRes val title: Int,
        @StringRes val message: Int,
        @StringRes val confirmButtonTitle: Int,
        @StringRes val closeButtonTitle: Int? = null,
    ) {
        object AutoPlay : WhatsNewFeature(
            title = LR.string.whats_new_autoplay_title,
            message = LR.string.whats_new_autoplay_body,
            confirmButtonTitle = LR.string.whats_new_autoplay_enable_button,
            closeButtonTitle = LR.string.whats_new_autoplay_maybe_later_button,
        )
    }

    sealed class NavigationState {
        object PlaybackSettings : NavigationState()
    }
}

package au.com.shiftyjelly.pocketcasts.account.viewmodel

import android.app.Application
import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.lifecycle.AndroidViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class OnboardingPlusFeaturesViewModel @Inject constructor(
    app: Application
) : AndroidViewModel(app) {

    private val _state: MutableStateFlow<OnboardingPlusFeaturesState>
    val state: StateFlow<OnboardingPlusFeaturesState>

    init {
        val accessibiltyManager = getApplication<Application>().getSystemService(Context.ACCESSIBILITY_SERVICE)
            as? AccessibilityManager

        val isTouchExplorationEnabled = accessibiltyManager?.isTouchExplorationEnabled ?: false
        _state = MutableStateFlow(OnboardingPlusFeaturesState(isTouchExplorationEnabled))
        state = _state

        accessibiltyManager?.addTouchExplorationStateChangeListener { enabled ->
            _state.value = OnboardingPlusFeaturesState(enabled)
        }
    }
}

data class OnboardingPlusFeaturesState(private val isTouchExplorationEnabled: Boolean) {
    val scrollAutomatically = !isTouchExplorationEnabled
}

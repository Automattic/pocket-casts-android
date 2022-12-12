package au.com.shiftyjelly.pocketcasts.account.viewmodel

import android.app.Application
import android.content.Context
import android.view.accessibility.AccessibilityManager
import androidx.lifecycle.AndroidViewModel
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTrackerWrapper
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class OnboardingPlusFeaturesViewModel @Inject constructor(
    app: Application,
    private val analyticsTracker: AnalyticsTrackerWrapper,
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

    fun onShown(flow: String) {
        analyticsTracker.track(
            AnalyticsEvent.PLUS_PROMOTION_SHOWN,
            mapOf(flowKey to flow)
        )
    }

    fun onBackPressed(flow: String) {
        analyticsTracker.track(
            AnalyticsEvent.PLUS_PROMOTION_DISMISSED,
            mapOf(flowKey to flow)
        )
    }

    fun onUpgradePressed(flow: String) {
        analyticsTracker.track(
            AnalyticsEvent.PLUS_PROMOTION_UPGRADE_BUTTON_TAPPED,
            mapOf(flowKey to flow)
        )
    }

    fun onNotNowPressed(flow: String) {
        analyticsTracker.track(
            AnalyticsEvent.PLUS_PROMOTION_NOT_NOW_BUTTON_TAPPED,
            mapOf(flowKey to flow)
        )
    }

    companion object {
        private const val flowKey = "flow"
    }
}

data class OnboardingPlusFeaturesState(private val isTouchExplorationEnabled: Boolean) {
    val scrollAutomatically = !isTouchExplorationEnabled
}

package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.featureflag.FeatureFlagManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class BetaFeaturesViewModel @Inject constructor() : ViewModel() {

    data class State(
        val featureFlags: List<FeatureFlagWrapper>,
    )

    private val _state = MutableStateFlow(
        State(featureFlags = featureFlags)
    )
    val state: StateFlow<State> = _state

    private val featureFlags: List<FeatureFlagWrapper>
        get() = Feature.values().map {
            FeatureFlagWrapper(
                featureFlag = it,
                isEnabled = FeatureFlagManager.isFeatureEnabled(it)
            )
        }

    fun setFeatureEnabled(feature: Feature, enabled: Boolean) {
        FeatureFlagManager.setFeatureEnabled(feature, enabled)
        _state.value = _state.value.copy(
            featureFlags = featureFlags
        )
    }

    data class FeatureFlagWrapper(
        val featureFlag: Feature,
        val isEnabled: Boolean,
    )
}

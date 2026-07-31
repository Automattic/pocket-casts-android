package au.com.shiftyjelly.pocketcasts.settings.viewmodel

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

@HiltViewModel
class BetaFeaturesViewModel @Inject constructor() : ViewModel() {

    enum class SortOrder(@StringRes val labelId: Int) {
        Name(R.string.settings_beta_features_sort_name),
        ReleaseDate(R.string.settings_beta_features_sort_release_date),
    }

    data class State(
        val featureFlags: List<FeatureFlagWrapper>,
        val sortOrder: SortOrder = SortOrder.Name,
    )

    private val _state = MutableStateFlow(
        State(featureFlags = featureFlags(SortOrder.Name)),
    )
    val state: StateFlow<State> = _state

    private fun featureFlags(sortOrder: SortOrder): List<FeatureFlagWrapper> {
        val comparator = when (sortOrder) {
            SortOrder.Name -> compareBy { it.title }
            SortOrder.ReleaseDate -> compareByDescending<Feature> { it.addedOn }.thenBy { it.title }
        }
        return Feature.entries
            .filter { it.hasDevToggle }
            .sortedWith(comparator)
            .map {
                FeatureFlagWrapper(
                    featureFlag = it,
                    isEnabled = FeatureFlag.isEnabled(it),
                )
            }
    }

    fun setFeatureEnabled(feature: Feature, enabled: Boolean) {
        FeatureFlag.setEnabled(feature, enabled)
        _state.value = _state.value.copy(
            featureFlags = featureFlags(_state.value.sortOrder),
        )
    }

    fun setSortOrder(sortOrder: SortOrder) {
        _state.value = _state.value.copy(
            sortOrder = sortOrder,
            featureFlags = featureFlags(sortOrder),
        )
    }

    data class FeatureFlagWrapper(
        val featureFlag: Feature,
        val isEnabled: Boolean,
    )
}

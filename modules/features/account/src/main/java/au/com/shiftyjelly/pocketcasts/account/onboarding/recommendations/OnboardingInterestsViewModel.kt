package au.com.shiftyjelly.pocketcasts.account.onboarding.recommendations

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class OnboardingInterestsViewModel @Inject constructor() : ViewModel() {

    private val _state = MutableStateFlow(State(availableCategories = listOf("True Crime", "Comedy", "Society & Culture", "Fiction"), isShowingAllCategories = false))
    val state = _state.asStateFlow()

    fun skipSelection() {
    }

    fun updateSelectedCategory(category: String, isSelected: Boolean) {
        _state.update {
            it.copy(
                selectedCategories = if (isSelected) {
                    it.selectedCategories + category
                } else {
                    it.selectedCategories - category
                },
            )
        }
    }

    fun showMore() {
        _state.update {
            it.copy(
                isShowingAllCategories = true,
                availableCategories = it.availableCategories + listOf("Arts", "Education", "Sports", "TV & Film"),
            )
        }
    }

    fun saveInterests() {
    }

    data class State(
        val selectedCategories: Set<String> = emptySet(),
        val availableCategories: List<String>,
        val isShowingAllCategories: Boolean,
    ) {
        val isCtaEnabled: Boolean = selectedCategories.size >= 3
        val ctaLabelResId: Int = if (isCtaEnabled) {
            LR.string.navigation_continue
        } else {
            LR.string.onboarding_interests_select_at_least_label
        }
    }
}

package au.com.shiftyjelly.pocketcasts.account.onboarding.recommendations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.repositories.categories.CategoriesManager
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class OnboardingInterestsViewModel @Inject constructor(
    private val categoriesManager: CategoriesManager,
) : ViewModel() {

    private val _state = MutableStateFlow(
        State(
            allCategories = emptyList(),
            displayedCategories = emptyList(),
        ),
    )
    val state = _state.asStateFlow()

    init {
        fetchCategories()
    }

    fun skipSelection(doWhenFinished: () -> Unit) {
        viewModelScope.launch {
            categoriesManager.setInterestCategories(emptySet())
            doWhenFinished()
        }
    }

    fun updateSelectedCategory(category: DiscoverCategory, isSelected: Boolean) {
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
                displayedCategories = it.allCategories,
            )
        }
    }

    fun saveInterests(doWhenFinished: () -> Unit) {
        viewModelScope.launch {
            categoriesManager.setInterestCategories(_state.value.selectedCategories)
            doWhenFinished()
        }
    }

    private fun fetchCategories() {
        viewModelScope.launch {
            categoriesManager.state.collect { categoryState ->
                _state.update {
                    it.copy(
                        // TODO sort categories by their popularity, as soon as we've introduced the new field to backend.
                        allCategories = categoryState.allCategories,
                        displayedCategories = categoryState.allCategories.take(10),
                    )
                }
            }
        }
    }

    data class State(
        val selectedCategories: Set<DiscoverCategory> = emptySet(),
        val allCategories: List<DiscoverCategory>,
        val displayedCategories: List<DiscoverCategory>,
    ) {
        val isCtaEnabled: Boolean = selectedCategories.size >= 3
        val ctaLabelResId: Int = if (isCtaEnabled) {
            LR.string.navigation_continue
        } else {
            LR.string.onboarding_interests_select_at_least_label
        }
        val isShowingAllCategories: Boolean = displayedCategories.size == allCategories.size
    }
}

package au.com.shiftyjelly.pocketcasts.account.onboarding.recommendations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.repositories.categories.CategoriesManager
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
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
    private val analyticsTracker: AnalyticsTracker,
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

    fun onShow(flow: OnboardingFlow) {
        analyticsTracker.trackInterestsShown(flow = flow.analyticsValue)
    }

    fun skipSelection(flow: OnboardingFlow) {
        analyticsTracker.trackInterestsNotNowTapped(flow = flow.analyticsValue)
        categoriesManager.setInterestCategories(emptySet())
    }

    fun updateSelectedCategory(category: DiscoverCategory, isSelected: Boolean, flow: OnboardingFlow) {
        analyticsTracker.trackInterestsCategorySelected(
            flow = flow.analyticsValue,
            categoryId = category.id,
            categoryName = category.name,
            isSelected = isSelected,
        )
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

    fun showMore(flow: OnboardingFlow) {
        analyticsTracker.trackInterestsShowMoreTapped(flow = flow.analyticsValue)
        _state.update {
            it.copy(
                displayedCategories = it.allCategories,
            )
        }
    }

    fun saveInterests(flow: OnboardingFlow) {
        analyticsTracker.trackInterestsContinueTapped(
            flow = flow.analyticsValue,
            categories = _state.value.selectedCategories.map { it.name },
        )
        categoriesManager.setInterestCategories(_state.value.selectedCategories)
    }

    private fun fetchCategories() {
        viewModelScope.launch {
            categoriesManager.availableInterestCategories.collect { categories ->
                _state.update {
                    it.copy(
                        allCategories = categories,
                        displayedCategories = categories.take(12),
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

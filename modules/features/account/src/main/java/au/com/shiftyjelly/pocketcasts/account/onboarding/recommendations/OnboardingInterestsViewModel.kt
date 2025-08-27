package au.com.shiftyjelly.pocketcasts.account.onboarding.recommendations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
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

    fun onShow() {
        analyticsTracker.track(AnalyticsEvent.INTERESTS_SHOWN)
    }

    fun skipSelection() {
        analyticsTracker.track(AnalyticsEvent.INTERESTS_NOT_NOW_TAPPED)
        categoriesManager.setInterestCategories(emptySet())
    }

    fun updateSelectedCategory(category: DiscoverCategory, isSelected: Boolean) {
        analyticsTracker.track(
            AnalyticsEvent.INTERESTS_CATEGORY_SELECTED,
            mapOf(
                "categoryId" to category.id,
                "name" to category.name,
                "isSelected" to isSelected,
            ),
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

    fun showMore() {
        analyticsTracker.track(AnalyticsEvent.INTERESTS_SHOW_MORE_TAPPED)
        _state.update {
            it.copy(
                displayedCategories = it.allCategories,
            )
        }
    }

    fun saveInterests() {
        analyticsTracker.track(AnalyticsEvent.INTERESTS_CONTINUE_TAPPED)
        categoriesManager.setInterestCategories(_state.value.selectedCategories)
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

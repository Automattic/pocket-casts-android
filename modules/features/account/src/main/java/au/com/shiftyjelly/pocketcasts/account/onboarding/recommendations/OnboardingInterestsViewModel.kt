package au.com.shiftyjelly.pocketcasts.account.onboarding.recommendations

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import au.com.shiftyjelly.pocketcasts.repositories.categories.CategoriesManager
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.settings.onboarding.OnboardingFlow
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.OnboardingInterestsCategorySelectedEvent
import com.automattic.eventhorizon.OnboardingInterestsContinueTappedEvent
import com.automattic.eventhorizon.OnboardingInterestsNotNowTappedEvent
import com.automattic.eventhorizon.OnboardingInterestsShowMoreTappedEvent
import com.automattic.eventhorizon.OnboardingInterestsShownEvent
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
    private val eventHorizon: EventHorizon,
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
        eventHorizon.track(
            OnboardingInterestsShownEvent(
                flow = flow.eventHorizonValue,
            ),
        )
    }

    fun skipSelection(flow: OnboardingFlow) {
        eventHorizon.track(
            OnboardingInterestsNotNowTappedEvent(
                flow = flow.eventHorizonValue,
            ),
        )
        categoriesManager.setInterestCategories(emptySet())
    }

    fun updateSelectedCategory(category: DiscoverCategory, isSelected: Boolean, flow: OnboardingFlow) {
        eventHorizon.track(
            OnboardingInterestsCategorySelectedEvent(
                flow = flow.eventHorizonValue,
                categoryId = category.id.toLong(),
                name = category.name,
                isSelected = isSelected,
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

    fun showMore(flow: OnboardingFlow) {
        eventHorizon.track(
            OnboardingInterestsShowMoreTappedEvent(
                flow = flow.eventHorizonValue,
            ),
        )
        _state.update {
            it.copy(
                displayedCategories = it.allCategories,
            )
        }
    }

    fun saveInterests(flow: OnboardingFlow) {
        eventHorizon.track(
            OnboardingInterestsContinueTappedEvent(
                flow = flow.eventHorizonValue,
                categories = _state.value.selectedCategories.joinToString(separator = ", ") { it.name },
            ),
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

package au.com.shiftyjelly.pocketcasts.account.onboarding.recommendations

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.repositories.categories.CategoriesManager
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class OnboardingInterestsViewModelTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val categoriesManager = mock<CategoriesManager>()
    val stateFlow = MutableStateFlow(CategoriesManager.State.Idle(featuredCategories = emptyList(), allCategories = emptyList(), areAllCategoriesShown = true))

    @Before
    fun setup() {
        whenever(categoriesManager.state).thenReturn(stateFlow)
    }

    @Test
    fun `should fetch categories on init`() = runTest {
        stateFlow.value = CategoriesManager.State.Idle(allCategories = demoCategories, featuredCategories = emptyList(), areAllCategoriesShown = true)
        val viewModel = OnboardingInterestsViewModel(categoriesManager)

        viewModel.state.test {
            val item = awaitItem()
            assert(item.displayedCategories.size == 10)
            assert(item.allCategories.size == demoCategories.size)
            assert(item.selectedCategories.isEmpty())
        }
    }

    @Test
    fun `should update categories on more selected`() = runTest {
        stateFlow.value = CategoriesManager.State.Idle(allCategories = demoCategories, featuredCategories = emptyList(), areAllCategoriesShown = true)
        val viewModel = OnboardingInterestsViewModel(categoriesManager)

        viewModel.state.test {
            val item = awaitItem()
            assert(!item.isShowingAllCategories)

            viewModel.showMore()

            val nextItem = awaitItem()
            assert(nextItem.isShowingAllCategories)
        }
    }

    @Test
    fun `should update selected categories on selecting some`() = runTest {
        stateFlow.value = CategoriesManager.State.Idle(allCategories = demoCategories, featuredCategories = emptyList(), areAllCategoriesShown = true)
        val viewModel = OnboardingInterestsViewModel(categoriesManager)

        viewModel.state.test {
            val item = awaitItem()
            assert(item.selectedCategories.isEmpty())

            viewModel.updateSelectedCategory(demoCategories[0], true)
            val nextItem = awaitItem()
            assert(nextItem.selectedCategories.isNotEmpty())
        }
    }

    private companion object {
        val demoCategories = List(20) {
            DiscoverCategory(
                id = it,
                name = "Category $it",
                icon = "",
                source = "",
            )
        }
    }
}

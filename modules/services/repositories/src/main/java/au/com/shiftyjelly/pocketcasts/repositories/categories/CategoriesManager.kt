package au.com.shiftyjelly.pocketcasts.repositories.categories

import au.com.shiftyjelly.pocketcasts.models.db.dao.UserCategoryVisitsDao
import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.repositories.lists.ListRepository
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.time.Duration
import kotlin.time.Duration.Companion.days
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import timber.log.Timber

@Singleton
class CategoriesManager @Inject constructor(
    private val listRepository: ListRepository,
    private val userCategoryVisitsDao: UserCategoryVisitsDao,
    @ApplicationScope private val scope: CoroutineScope,
) {
    private val discoverCategories = MutableStateFlow<List<DiscoverCategory>>(emptyList())
    private val discoverRowInfo = MutableStateFlow(DiscoverRowInfo())
    private val selectedId = MutableStateFlow<Int?>(null)
    private val areAllCategoriesShown = MutableStateFlow(false)

    private var lastUpdate: TimeSource.Monotonic.ValueTimeMark? = null

    val state = combine(
        discoverCategories,
        discoverRowInfo,
        selectedId,
        areAllCategoriesShown,
    ) { categories, rowInfo, selectedId, areAllShown ->
        val selectedCategory = categories.find { it.id == selectedId }
        if (selectedCategory != null) {
            State.Selected(selectedCategory, categories, areAllShown)
        } else {
            val categoriesWithVisits = categories
                .filter { it.totalVisits > 0 }
                .sortedByDescending { it.totalVisits }
            val groupedBySponsoredWithVisits = categoriesWithVisits.groupBy { rowInfo.sponsoredCategoryIds.contains(it.id) }
            val featuredCategories = buildList {
                addAll(groupedBySponsoredWithVisits[true] ?: emptyList())
                addAll(groupedBySponsoredWithVisits[false] ?: emptyList())

                if (size < FEATURED_CATEGORY_COUNT) {
                    val popularFillers = categories
                        .filter { !contains(it) }
                        .filter { it.id in rowInfo.popularCategoryIds }
                        .sortedBy { rowInfo.popularCategoryIds.indexOf(it.id) }
                        .take(FEATURED_CATEGORY_COUNT - size)
                    addAll(popularFillers)
                }
            }.ifEmpty {
                categories
            }.take(FEATURED_CATEGORY_COUNT)
            State.Idle(
                featuredCategories = featuredCategories,
                allCategories = categories,
                areAllCategoriesShown = areAllShown,
            )
        }
    }.stateIn(scope, SharingStarted.Eagerly, State.Empty)

    val selectedCategory = state.map { state ->
        when (state) {
            is State.Selected -> state.selectedCategory
            is State.Idle -> null
        }
    }.distinctUntilChanged()

    fun loadCategories(url: String) {
        if (discoverCategories.value.isEmpty() || areCategoriesStale()) {
            scope.launch {
                try {
                    val userVisits = userCategoryVisitsDao.getCategoryVisitsOrdered()
                    discoverCategories.value = listRepository.getCategoriesList(url).map { category ->
                        category.copy(
                            totalVisits = userVisits.find { it.categoryId == category.id }?.totalVisits ?: 0,
                        )
                    }
                    lastUpdate = TimeSource.Monotonic.markNow()
                } catch (e: Throwable) {
                    Timber.e(e, "Failed to fetch categories under $url")
                }
            }
        }
    }

    fun setRowInfo(popularCategoryIds: List<Int>, sponsoredCategoryIds: List<Int>) {
        discoverRowInfo.value = DiscoverRowInfo(
            popularCategoryIds = popularCategoryIds,
            sponsoredCategoryIds = sponsoredCategoryIds,
        )
    }

    fun selectCategory(id: Int) {
        selectedId.value = id
        scope.launch {
            userCategoryVisitsDao.incrementVisits(id)
        }
    }

    fun dismissSelectedCategory() {
        selectedId.value = null
    }

    fun setAllCategoriesShown(areShown: Boolean) {
        areAllCategoriesShown.value = areShown
    }

    private fun areCategoriesStale() = (lastUpdate?.elapsedNow() ?: Duration.INFINITE) > 1.days

    private data class DiscoverRowInfo(
        val popularCategoryIds: List<Int> = emptyList(),
        val sponsoredCategoryIds: List<Int> = emptyList(),
    )

    sealed interface State {
        val allCategories: List<DiscoverCategory>
        val areAllCategoriesShown: Boolean

        data class Idle(
            val featuredCategories: List<DiscoverCategory>,
            override val allCategories: List<DiscoverCategory>,
            override val areAllCategoriesShown: Boolean,
        ) : State

        data class Selected(
            val selectedCategory: DiscoverCategory,
            override val allCategories: List<DiscoverCategory>,
            override val areAllCategoriesShown: Boolean,
        ) : State

        companion object {
            val Empty: State = Idle(
                allCategories = emptyList(),
                featuredCategories = emptyList(),
                areAllCategoriesShown = false,
            )
        }
    }

    private companion object {
        const val FEATURED_CATEGORY_COUNT = 6
    }
}

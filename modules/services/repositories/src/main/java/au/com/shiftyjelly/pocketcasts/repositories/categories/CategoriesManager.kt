package au.com.shiftyjelly.pocketcasts.repositories.categories

import android.content.Context
import au.com.shiftyjelly.pocketcasts.repositories.di.ApplicationScope
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.servers.server.ListRepository
import dagger.hilt.android.qualifiers.ApplicationContext
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
import kotlinx.coroutines.rx2.await
import timber.log.Timber

@Singleton
class CategoriesManager @Inject constructor(
    private val listRepository: ListRepository,
    @ApplicationScope private val scope: CoroutineScope,
    @ApplicationContext private val context: Context,
) {
    private val discoverCategories = MutableStateFlow<List<DiscoverCategory>>(emptyList())
    private val mostPopularIds = MutableStateFlow<List<Int>>(emptyList())
    private val selectedId = MutableStateFlow<Int?>(null)
    private val areAllCategoriesShown = MutableStateFlow(false)

    private var lastUpdate: TimeSource.Monotonic.ValueTimeMark? = null

    val state = combine(
        discoverCategories,
        mostPopularIds,
        selectedId,
        areAllCategoriesShown,
    ) { categories, popularIds, selectedId, areAllShown ->
        val selectedCategory = categories.find { it.id == selectedId }
        if (selectedCategory != null) {
            State.Selected(selectedCategory, categories, areAllShown)
        } else {
            State.Idle(
                mostPopularCategories = categories
                    .filter { it.id in popularIds }
                    .sortedBy { popularIds.indexOf(it.id) }
                    .ifEmpty { categories },
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
                    discoverCategories.value = listRepository.getCategoriesList(url).await()
                    lastUpdate = TimeSource.Monotonic.markNow()
                } catch (e: Throwable) {
                    Timber.e(e, "Failed to fetch categories under $url")
                }
            }
        }
    }

    fun setMostPopularCategories(ids: List<Int>) {
        mostPopularIds.value = ids
    }

    fun selectCategory(id: Int) {
        selectedId.value = id
    }

    fun dismissSelectedCategory() {
        selectedId.value = null
    }

    fun setAllCategoriesShown(areShown: Boolean) {
        areAllCategoriesShown.value = areShown
    }

    private fun areCategoriesStale() = (lastUpdate?.elapsedNow() ?: Duration.INFINITE) > 1.days

    sealed interface State {
        val allCategories: List<DiscoverCategory>
        val areAllCategoriesShown: Boolean

        data class Idle(
            val mostPopularCategories: List<DiscoverCategory>,
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
                mostPopularCategories = emptyList(),
                areAllCategoriesShown = false,
            )
        }
    }
}

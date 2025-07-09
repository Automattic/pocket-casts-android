package au.com.shiftyjelly.pocketcasts.repositories.categories

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.db.dao.UserCategoryVisitsDao
import au.com.shiftyjelly.pocketcasts.models.entity.UserCategoryVisits
import au.com.shiftyjelly.pocketcasts.repositories.lists.ListRepository
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
internal class CategoriesManagerTest {
    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    private val listRepository = mock<ListRepository>()
    private val userCategoryVisitsDao = mock<UserCategoryVisitsDao>()
    private val coroutineScope = CoroutineScope(UnconfinedTestDispatcher())

    companion object {
        val testCategories = List(10) { DiscoverCategory(id = it, name = "name$it", icon = "", source = "") }
    }

    @After
    fun tearDown() {
        coroutineScope.cancel()
    }

    @Test
    fun `GIVEN no visits WHEN categories evaluated THEN popular categories are returned`() = runBlocking {
        FeatureFlag.setEnabled(Feature.SMART_CATEGORIES, true)
        whenever(listRepository.getCategoriesList(any())).thenReturn(testCategories)
        whenever(userCategoryVisitsDao.getCategoryVisitsOrdered()).thenReturn(emptyList())

        val categoriesManager = CategoriesManager(listRepository, userCategoryVisitsDao, coroutineScope)
        categoriesManager.loadCategories("whatever")
        val popularCategoryIds = (0 until 6).toList()
        categoriesManager.setRowInfo(popularCategoryIds = popularCategoryIds, sponsoredCategoryIds = listOf(4, 5, 6, 7))

        categoriesManager.state.test {
            val state = awaitItem()
            assert(state is CategoriesManager.State.Idle)
            assertEquals(testCategories.filter { popularCategoryIds.contains(it.id) }.map { it.id }, (state as CategoriesManager.State.Idle).featuredCategories.map { it.id })
        }
    }

    @Test
    fun `GIVEN less than 6 visited categories WHEN categories evaluated THEN visited categories are ordered as expected`() = runBlocking {
        FeatureFlag.setEnabled(Feature.SMART_CATEGORIES, true)
        whenever(listRepository.getCategoriesList(any())).thenReturn(testCategories)
        whenever(userCategoryVisitsDao.getCategoryVisitsOrdered()).thenReturn(
            listOf(
                UserCategoryVisits(categoryId = 4, totalVisits = 1),
                UserCategoryVisits(categoryId = 5, totalVisits = 3),
                UserCategoryVisits(categoryId = 8, totalVisits = 4),
                UserCategoryVisits(categoryId = 9, totalVisits = 11),
            ),
        )

        val categoriesManager = CategoriesManager(listRepository, userCategoryVisitsDao, coroutineScope)
        categoriesManager.loadCategories("whatever")
        val popularCategoryIds = (0 until 6).toList()
        categoriesManager.setRowInfo(popularCategoryIds = popularCategoryIds, sponsoredCategoryIds = listOf(4, 5, 6, 7))

        categoriesManager.state.test {
            val state = awaitItem()
            assert(state is CategoriesManager.State.Idle)
            assertEquals(listOf(5, 4, 9, 8, 0, 1), (state as CategoriesManager.State.Idle).featuredCategories.map { it.id })
        }
    }

    @Test
    fun `GIVEN more than 6 visited categories WHEN categories evaluated THEN visited categories are ordered as expected`() = runBlocking {
        FeatureFlag.setEnabled(Feature.SMART_CATEGORIES, true)
        whenever(listRepository.getCategoriesList(any())).thenReturn(testCategories)
        whenever(userCategoryVisitsDao.getCategoryVisitsOrdered()).thenReturn(
            listOf(
                UserCategoryVisits(categoryId = 1, totalVisits = 8),
                UserCategoryVisits(categoryId = 2, totalVisits = 3),
                UserCategoryVisits(categoryId = 4, totalVisits = 1),
                UserCategoryVisits(categoryId = 5, totalVisits = 3),
                UserCategoryVisits(categoryId = 8, totalVisits = 4),
                UserCategoryVisits(categoryId = 9, totalVisits = 11),
            ),
        )

        val categoriesManager = CategoriesManager(listRepository, userCategoryVisitsDao, coroutineScope)
        categoriesManager.loadCategories("whatever")
        val popularCategoryIds = (0 until 6).toList()
        categoriesManager.setRowInfo(popularCategoryIds = popularCategoryIds, sponsoredCategoryIds = listOf(4, 5, 6, 7))

        categoriesManager.state.test {
            val state = awaitItem()
            assert(state is CategoriesManager.State.Idle)
            assertEquals(listOf(5, 4, 9, 1, 8, 2), (state as CategoriesManager.State.Idle).featuredCategories.map { it.id })
        }
    }

    @Test
    fun `GIVEN no visited categories and no populars WHEN categories evaluated THEN categories are ordered as expected`() = runBlocking {
        FeatureFlag.setEnabled(Feature.SMART_CATEGORIES, true)
        whenever(listRepository.getCategoriesList(any())).thenReturn(testCategories)
        whenever(userCategoryVisitsDao.getCategoryVisitsOrdered()).thenReturn(
            emptyList(),
        )

        val categoriesManager = CategoriesManager(listRepository, userCategoryVisitsDao, coroutineScope)
        categoriesManager.loadCategories("whatever")
        categoriesManager.setRowInfo(popularCategoryIds = emptyList(), sponsoredCategoryIds = listOf(4, 5, 6, 7))

        categoriesManager.state.test {
            val state = awaitItem()
            assert(state is CategoriesManager.State.Idle)
            assertEquals((0..5).toList(), (state as CategoriesManager.State.Idle).featuredCategories.map { it.id })
        }
    }

    @Test
    fun `GIVEN FF disabled WHEN categories evaluated THEN popular categories are returned`() = runTest {
        FeatureFlag.setEnabled(Feature.SMART_CATEGORIES, false)
        whenever(listRepository.getCategoriesList(any())).thenReturn(testCategories)
        whenever(userCategoryVisitsDao.getCategoryVisitsOrdered()).thenReturn(
            emptyList(),
        )

        val categoriesManager = CategoriesManager(listRepository, userCategoryVisitsDao, coroutineScope)
        categoriesManager.loadCategories("whatever")
        categoriesManager.setRowInfo(popularCategoryIds = (5..9).toList(), sponsoredCategoryIds = listOf(4, 5, 6, 7))

        categoriesManager.state.test {
            val state = awaitItem()
            assert(state is CategoriesManager.State.Idle)
            assertEquals((5..9).toList(), (state as CategoriesManager.State.Idle).featuredCategories.map { it.id })
        }
    }
}

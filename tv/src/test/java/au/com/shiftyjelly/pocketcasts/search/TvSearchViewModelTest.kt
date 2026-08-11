package au.com.shiftyjelly.pocketcasts.search

import au.com.shiftyjelly.pocketcasts.repositories.lists.ListRepository
import au.com.shiftyjelly.pocketcasts.servers.model.DiscoverCategory
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class TvSearchViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val listRepository = mock<ListRepository>()

    @Test
    fun `exposes the loaded browse categories`() = runTest {
        whenever(listRepository.getCategoriesList(any())).thenReturn(
            listOf(category(1, "Comedy"), category(2, "True Crime")),
        )

        val viewModel = TvSearchViewModel(listRepository)

        assertEquals(listOf("Comedy", "True Crime"), viewModel.categories.value.map { it.name })
    }

    @Test
    fun `categories are empty when loading fails`() = runTest {
        whenever(listRepository.getCategoriesList(any())).thenThrow(RuntimeException("Network error"))

        val viewModel = TvSearchViewModel(listRepository)

        assertTrue(viewModel.categories.value.isEmpty())
    }

    private fun category(id: Int, name: String) = DiscoverCategory(id = id, name = name, icon = "", source = "")
}

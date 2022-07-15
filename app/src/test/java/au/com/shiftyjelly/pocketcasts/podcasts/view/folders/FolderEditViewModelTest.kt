package au.com.shiftyjelly.pocketcasts.podcasts.view.folders

import com.nhaarman.mockitokotlin2.mock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class FolderEditViewModelTest {

    lateinit var viewModel: FolderEditViewModel

    @OptIn(ExperimentalCoroutinesApi::class)
    @Before
    fun setUp() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
        viewModel = FolderEditViewModel(mock(), mock(), mock())
    }

    @Test
    fun test_nameLength_100() {
        val name = "a".repeat(100)
        viewModel.changeFolderName(name)
        assertEquals(viewModel.folderName.value, name)
    }

    @Test
    fun test_nameLength_101() {
        val expectedName = "a".repeat(100)
        viewModel.changeFolderName(expectedName + "all these characters should be ignored")
        // Drops the characters above 100
        assertEquals(viewModel.folderName.value, expectedName)
    }
}

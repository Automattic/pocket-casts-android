
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.entity.UpNextHistoryEntry
import au.com.shiftyjelly.pocketcasts.repositories.history.upnext.UpNextHistoryManager
import au.com.shiftyjelly.pocketcasts.settings.history.upnext.UpNextHistoryViewModel
import au.com.shiftyjelly.pocketcasts.settings.history.upnext.UpNextHistoryViewModel.NavigationState
import au.com.shiftyjelly.pocketcasts.settings.history.upnext.UpNextHistoryViewModel.UiState
import java.util.Date
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
@ExperimentalCoroutinesApi
class UpNextHistoryViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @Mock
    private lateinit var upNextHistoryManager: UpNextHistoryManager
    private lateinit var viewModel: UpNextHistoryViewModel

    @Test
    fun `load history entries successfully updates state to loaded`() = runTest {
        val entries = listOf(UpNextHistoryEntry(Date(), 1))
        whenever(upNextHistoryManager.findAllHistoryEntries()).thenReturn(entries)

        initViewModel()

        viewModel.uiState.test {
            assertEquals(UiState.Loaded(entries), awaitItem())
        }
    }

    @Test
    fun `load history entries with empty list updates state to loaded with empty list`() = runTest {
        val entries = emptyList<UpNextHistoryEntry>()
        whenever(upNextHistoryManager.findAllHistoryEntries()).thenReturn(entries)

        initViewModel()

        viewModel.uiState.test {
            assertEquals(UiState.Loaded(entries), awaitItem())
        }
    }

    @Test
    fun `load history entries failure updates state to error`() = runTest {
        whenever(upNextHistoryManager.findAllHistoryEntries()).thenThrow(RuntimeException::class.java)

        initViewModel()

        viewModel.uiState.test {
            assertEquals(UiState.Error, awaitItem())
        }
    }

    @Test
    fun `on history entry click emits show history details navigation state`() = runTest {
        val entry = UpNextHistoryEntry(Date(), 1)
        initViewModel()

        viewModel.navigationState.test {
            viewModel.onHistoryEntryClick(entry)
            assertEquals(NavigationState.ShowHistoryDetails(entry.date), awaitItem())
        }
    }

    private fun initViewModel() {
        viewModel = UpNextHistoryViewModel(
            upNextHistoryManager = upNextHistoryManager,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }
}

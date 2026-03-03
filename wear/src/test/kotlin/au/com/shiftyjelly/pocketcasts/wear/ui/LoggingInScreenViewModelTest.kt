package au.com.shiftyjelly.pocketcasts.wear.ui

import au.com.shiftyjelly.pocketcasts.models.to.RefreshState
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class LoggingInScreenViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Mock
    private lateinit var settings: Settings

    @Mock
    private lateinit var syncManager: SyncManager

    private lateinit var testSubject: LoggingInScreenViewModel

    private val refreshStateFlow = MutableStateFlow<RefreshState>(RefreshState.Never)

    @Before
    fun setup() {
        MockitoAnnotations.openMocks(this)
        whenever(settings.refreshStateFlow).thenReturn(refreshStateFlow)
        whenever(settings.getRefreshState()).thenReturn(RefreshState.Never)
        testSubject = LoggingInScreenViewModel(
            settings = settings,
            syncManager = syncManager,
        )
    }

    @After
    fun tearDown() = runTest {
        testScheduler.advanceUntilIdle()
    }

    @Test
    fun `shouldClose immediately when state is None`() = runBlocking {
        val result = testSubject.shouldClose(withMinimumDelay = true)

        assertEquals(true, result)
    }

    @Test
    fun `shouldClose immediately when state is RefreshComplete`() = runBlocking {
        val result = testSubject.shouldClose(withMinimumDelay = true)

        assertEquals(true, result)
    }

    @Test
    fun `shouldClose with minimum delay when state is CompleteButDelaying`() = runBlocking {
        LoggingInScreenViewModel.State.CompleteButDelaying("test@email.com")

        val result = testSubject.shouldClose(withMinimumDelay = true)

        assertEquals(true, result)
    }

    @Test
    fun `shouldClose without delay when state is CompleteButDelaying and withMinimumDelay is false`() = runBlocking {
        LoggingInScreenViewModel.State.CompleteButDelaying("test@email.com")

        val result = testSubject.shouldClose(withMinimumDelay = false)

        assertEquals(true, result)
    }
}

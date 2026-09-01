package au.com.shiftyjelly.pocketcasts.account.deviceapprove

import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import retrofit2.HttpException
import retrofit2.Response

@OptIn(ExperimentalCoroutinesApi::class)
class DeviceApproveViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val syncManager = mock<SyncManager>()

    @Test
    fun `connect approves the device`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(true)

        val viewModel = createViewModel(userCode = "ABCD12")
        viewModel.connect()
        advanceUntilIdle()

        assertEquals(DeviceApproveStatus.Approved, viewModel.uiState.value.status)
    }

    @Test
    fun `connect maps expired code to expired error`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(true)
        whenever(syncManager.deviceApprove("ABCD12", true)).thenThrow(httpException(400))

        val viewModel = createViewModel(userCode = "ABCD12")
        viewModel.connect()
        advanceUntilIdle()

        assertEquals(DeviceApproveStatus.ExpiredError, viewModel.uiState.value.status)
    }

    @Test
    fun `connect maps other failures to generic error`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(true)
        whenever(syncManager.deviceApprove("ABCD12", true)).thenThrow(RuntimeException("boom"))

        val viewModel = createViewModel(userCode = "ABCD12")
        viewModel.connect()
        advanceUntilIdle()

        assertEquals(DeviceApproveStatus.GenericError, viewModel.uiState.value.status)
    }

    @Test
    fun `connect is ignored without a user code`() = runTest {
        val viewModel = createViewModel(userCode = "")
        viewModel.connect()
        advanceUntilIdle()

        assertEquals(DeviceApproveStatus.Idle, viewModel.uiState.value.status)
    }

    @Test
    fun `upsell is prompted only when the user signed in during pairing`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(false)
        val signedOut = createViewModel(userCode = "ABCD12")
        assertTrue(signedOut.shouldPromptUpsellAfterApproval)

        whenever(syncManager.isLoggedIn()).thenReturn(true)
        val signedIn = createViewModel(userCode = "ABCD12")
        assertFalse(signedIn.shouldPromptUpsellAfterApproval)
    }

    @Test
    fun `refreshAccountState reflects the current login`() = runTest {
        whenever(syncManager.isLoggedIn()).thenReturn(false)
        whenever(syncManager.getEmail()).thenReturn(null)

        val viewModel = createViewModel(userCode = "ABCD12")
        assertFalse(viewModel.uiState.value.isLoggedIn)

        whenever(syncManager.isLoggedIn()).thenReturn(true)
        whenever(syncManager.getEmail()).thenReturn("user@example.com")
        viewModel.refreshAccountState()

        assertTrue(viewModel.uiState.value.isLoggedIn)
        assertEquals("user@example.com", viewModel.uiState.value.email)
    }

    private fun createViewModel(userCode: String) = DeviceApproveViewModel(syncManager).apply {
        setUserCode(userCode)
    }

    private fun httpException(code: Int) = HttpException(
        Response.error<Any>(
            "".toResponseBody(),
            okhttp3.Response.Builder()
                .code(code)
                .message("error")
                .protocol(Protocol.HTTP_1_1)
                .request(Request.Builder().url("http://localhost/").build())
                .build(),
        ),
    )
}

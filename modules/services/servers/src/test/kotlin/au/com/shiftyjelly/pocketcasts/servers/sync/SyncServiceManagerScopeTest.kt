package au.com.shiftyjelly.pocketcasts.servers.sync

import au.com.shiftyjelly.pocketcasts.servers.sync.login.DeviceAuthorizeRequest
import au.com.shiftyjelly.pocketcasts.servers.sync.login.LoginPocketCastsRequest
import au.com.shiftyjelly.pocketcasts.utils.AppPlatform
import dagger.Lazy
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

class SyncServiceManagerScopeTest {

    private val service = mock<SyncService>()

    private fun createManager(appPlatform: AppPlatform) = SyncServiceManager(
        service = service,
        settings = mock(),
        cache = Lazy { mock() },
        appPlatform = appPlatform,
    )

    @Test
    fun `tv app signs in with the tv scope`() = runTest {
        createManager(AppPlatform.Tv).login(email = "test@pocketcasts.com", password = "password")

        assertEquals("tv", captureLoginScope())
    }

    @Test
    fun `other apps sign in with the mobile scope`() = runTest {
        listOf(AppPlatform.Phone, AppPlatform.WearOs, AppPlatform.Automotive).forEach { appPlatform ->
            createManager(appPlatform).login(email = "test@pocketcasts.com", password = "password")
        }

        val captor = argumentCaptor<LoginPocketCastsRequest>()
        verify(service, times(3)).loginPocketCasts(captor.capture())
        assertEquals(listOf("mobile", "mobile", "mobile"), captor.allValues.map { it.scope })
    }

    @Test
    fun `tv app authorizes a device with the tv scope`() = runTest {
        createManager(AppPlatform.Tv).deviceAuthorize()

        val captor = argumentCaptor<DeviceAuthorizeRequest>()
        verify(service).deviceAuthorize(captor.capture())
        assertEquals("tv", captor.firstValue.scope)
    }

    private suspend fun captureLoginScope(): String {
        val captor = argumentCaptor<LoginPocketCastsRequest>()
        verify(service).loginPocketCasts(captor.capture())
        return captor.firstValue.scope
    }
}

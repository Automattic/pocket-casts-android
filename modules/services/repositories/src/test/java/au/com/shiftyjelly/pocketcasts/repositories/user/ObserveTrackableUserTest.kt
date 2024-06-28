package au.com.shiftyjelly.pocketcasts.repositories.user

import au.com.shiftyjelly.pocketcasts.crashlogging.User
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class ObserveTrackableUserTest {

    private lateinit var sut: ObserveTrackableUser

    @Mock
    private lateinit var settings: Settings

    @Mock
    private lateinit var syncManager: SyncManager

    @Before
    fun setUp() {
        sut = ObserveTrackableUser(settings, syncManager)
    }

    @Test
    fun `given user allowed linking crash reports to user, when requesting user details then provide them`() =
        runTest {
            val mockedUserSettings: UserSetting<Boolean> = mock {
                on { flow } doReturn MutableStateFlow(true)
            }
            whenever(settings.linkCrashReportsToUser).thenReturn(mockedUserSettings)
            whenever(syncManager.getEmail()).thenReturn("test@example.com")

            val result = sut.invoke().first()

            assertEquals(User("test@example.com"), result)
        }

    @Test
    fun `given user disallowed linking crash reports to user, when requesting user details then return null`() =
        runTest {
            val mockedUserSettings: UserSetting<Boolean> = mock {
                on { flow } doReturn MutableStateFlow(false)
            }
            whenever(settings.linkCrashReportsToUser).thenReturn(mockedUserSettings)

            val result = sut.invoke().first()

            assertNull(result)
        }

    @Test
    fun `given user allowed linking crash reports to user but email is not available, when requesting user details then return null`() =
        runTest {
            val mockedUserSettings: UserSetting<Boolean> = mock {
                on { flow } doReturn MutableStateFlow(true)
            }
            whenever(settings.linkCrashReportsToUser).thenReturn(mockedUserSettings)
            whenever(syncManager.getEmail()).thenReturn(null)

            val result = sut.invoke().first()

            assertNull(result)
        }
}

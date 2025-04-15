package au.com.shiftyjelly.pocketcasts.repositories.sync

import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationManager
import au.com.shiftyjelly.pocketcasts.repositories.notification.OnboardingNotificationType
import au.com.shiftyjelly.pocketcasts.servers.sync.SyncServiceManager
import au.com.shiftyjelly.pocketcasts.servers.sync.login.LoginTokenResponse
import com.squareup.moshi.Moshi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class SyncManagerImplTest {

    @Mock
    private lateinit var analyticsTracker: AnalyticsTracker

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var settings: Settings

    @Mock
    private lateinit var syncAccountManager: SyncAccountManager

    @Mock
    private lateinit var syncServiceManager: SyncServiceManager

    @Mock
    private lateinit var moshi: Moshi

    @Mock
    private lateinit var notificationManager: NotificationManager

    private lateinit var syncManager: SyncManagerImpl

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)

        syncManager = SyncManagerImpl(
            analyticsTracker = analyticsTracker,
            context = context,
            settings = settings,
            syncAccountManager = syncAccountManager,
            syncServiceManager = syncServiceManager,
            moshi = moshi,
            notificationManager = notificationManager,
        )
    }

    @Test
    fun `should update user interaction with sync feature on user registration with password`() = runTest {
        val response = createMockLoginResponse(isNew = true)
        whenever(syncServiceManager.register(any(), any())).thenReturn(response)
        syncManager.createUserWithEmailAndPassword("test@example.com", "password123")
        verifyNotificationCalled()
    }

    @Test
    fun `should update user interaction with sync feature on user registration with google account`() = runTest {
        val response = createMockLoginResponse(isNew = true)
        whenever(syncServiceManager.loginGoogle(any())).thenReturn(response)
        syncManager.loginWithGoogle("google_token", SignInSource.UserInitiated.Onboarding)
        verifyNotificationCalled()
    }

    @Test
    fun `should not update user interaction when is not user registration`() = runTest {
        val response = createMockLoginResponse(isNew = false)
        whenever(syncServiceManager.loginGoogle(any())).thenReturn(response)
        syncManager.loginWithGoogle("google_token", SignInSource.UserInitiated.Onboarding)
        verifyNotificationNotCalled()
    }

    private fun createMockLoginResponse(isNew: Boolean): LoginTokenResponse {
        return mock<LoginTokenResponse>().apply {
            whenever(email).thenReturn("test@example.com")
            whenever(uuid).thenReturn("uuid")
            whenever(refreshToken).thenReturn(mock())
            whenever(accessToken).thenReturn(mock())
            whenever(this.isNew).thenReturn(isNew)
        }
    }

    private suspend fun verifyNotificationCalled() {
        verify(notificationManager).updateUserFeatureInteraction(OnboardingNotificationType.Sync)
    }

    private suspend fun verifyNotificationNotCalled() {
        verify(notificationManager, never()).updateUserFeatureInteraction(OnboardingNotificationType.Sync)
    }
}

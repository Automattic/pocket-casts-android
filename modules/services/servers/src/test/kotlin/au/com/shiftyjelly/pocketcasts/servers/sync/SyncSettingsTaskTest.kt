package au.com.shiftyjelly.pocketcasts.servers.sync

import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import java.time.Instant
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

class SyncSettingsTaskTest {
    private val lastSyncTime: Instant = Instant.EPOCH

    private val showGeneratedChapters = mock<UserSetting<Boolean>>()
    private val settings = mock<Settings>()

    private class FakeNamedSettingsCaller(
        private val response: NamedSettingsResponse = emptyMap(),
    ) : NamedSettingsCaller {
        var request: NamedSettingsRequest? = null

        override suspend fun namedSettings(request: NamedSettingsRequest): NamedSettingsResponse {
            this.request = request
            return response
        }
    }

    // Unstubbed getSyncValue returns null, which is how a setting says "nothing to sync".
    private fun <T> unsyncedSetting(): UserSetting<T> = mock()

    @Before
    fun setup() {
        whenever(settings.skipForwardInSecs).thenReturn(unsyncedSetting())
        whenever(settings.skipBackInSecs).thenReturn(unsyncedSetting())
        whenever(settings.marketingOptIn).thenReturn(unsyncedSetting())
        whenever(settings.freeGiftAcknowledged).thenReturn(unsyncedSetting())
        whenever(settings.podcastsSortType).thenReturn(unsyncedSetting())
        whenever(settings.collectListeningStats).thenReturn(unsyncedSetting())
        whenever(settings.audioOnly).thenReturn(unsyncedSetting())
        whenever(settings.showGeneratedChapters).thenReturn(showGeneratedChapters)
    }

    @Test
    fun `opting out of generated chapters pushes disableAiChapters true`() = runTest {
        whenever(showGeneratedChapters.getSyncValue(lastSyncTime)).thenReturn(false)
        val caller = FakeNamedSettingsCaller()

        SyncSettingsTask.run(settings, lastSyncTime, caller)

        assertEquals(true, caller.request?.settings?.disableAiChapters)
    }

    @Test
    fun `showing generated chapters pushes disableAiChapters false`() = runTest {
        whenever(showGeneratedChapters.getSyncValue(lastSyncTime)).thenReturn(true)
        val caller = FakeNamedSettingsCaller()

        SyncSettingsTask.run(settings, lastSyncTime, caller)

        assertEquals(false, caller.request?.settings?.disableAiChapters)
    }

    @Test
    fun `an unchanged setting is not pushed`() = runTest {
        whenever(showGeneratedChapters.getSyncValue(lastSyncTime)).thenReturn(null)
        val caller = FakeNamedSettingsCaller()

        SyncSettingsTask.run(settings, lastSyncTime, caller)

        assertNotNull(caller.request)
        assertNull(caller.request?.settings?.disableAiChapters)
    }

    @Test
    fun `a disableAiChapters response of true hides generated chapters`() = runTest {
        val caller = FakeNamedSettingsCaller(
            mapOf("disableAiChapters" to SettingResponse(value = true, changed = true)),
        )

        SyncSettingsTask.run(settings, lastSyncTime, caller)

        verify(showGeneratedChapters).set(eq(false), eq(false), any(), any())
    }

    @Test
    fun `a disableAiChapters response of false shows generated chapters`() = runTest {
        val caller = FakeNamedSettingsCaller(
            mapOf("disableAiChapters" to SettingResponse(value = false, changed = true)),
        )

        SyncSettingsTask.run(settings, lastSyncTime, caller)

        verify(showGeneratedChapters).set(eq(true), eq(false), any(), any())
    }

    @Test
    fun `an unchanged disableAiChapters response is not applied`() = runTest {
        val caller = FakeNamedSettingsCaller(
            mapOf("disableAiChapters" to SettingResponse(value = true, changed = false)),
        )

        SyncSettingsTask.run(settings, lastSyncTime, caller)

        verify(showGeneratedChapters, never()).set(any(), any(), any(), any())
    }
}

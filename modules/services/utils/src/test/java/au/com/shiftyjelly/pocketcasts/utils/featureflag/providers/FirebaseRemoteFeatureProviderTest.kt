package au.com.shiftyjelly.pocketcasts.utils.featureflag.providers

import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.remoteconfig.ConfigUpdateListenerRegistration
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class FirebaseRemoteFeatureProviderTest {
    private val remoteConfig = mock<FirebaseRemoteConfig>()
    private val settings = FakeRemoteConfigFetchSettings(versionCode = VERSION_CODE)
    private val testScope = TestScope(UnconfinedTestDispatcher())

    @Before
    fun setUp() {
        val registration = mock<ConfigUpdateListenerRegistration>()
        val activateTask = completedTask(true)
        val fetchTask = completedTask<Void?>(null)
        whenever(remoteConfig.addOnConfigUpdateListener(any())).thenReturn(registration)
        whenever(remoteConfig.activate()).thenReturn(activateTask)
        whenever(remoteConfig.fetch()).thenReturn(fetchTask)
        whenever(remoteConfig.fetch(0L)).thenReturn(fetchTask)
    }

    @After
    fun tearDown() {
        testScope.cancel()
    }

    @Test
    fun `fetch skips the throttle when config has never been fetched`() {
        settings.fetchedVersionCode = 0

        createProvider()

        verify(remoteConfig).fetch(0L)
        verify(remoteConfig, never()).fetch()
    }

    @Test
    fun `fetch skips the throttle when the version code changed since the last fetch`() {
        settings.fetchedVersionCode = VERSION_CODE - 1

        createProvider()

        verify(remoteConfig).fetch(0L)
        verify(remoteConfig, never()).fetch()
    }

    @Test
    fun `fetch uses the throttle when config was already fetched for this version`() {
        settings.fetchedVersionCode = VERSION_CODE

        createProvider()

        verify(remoteConfig).fetch()
        verify(remoteConfig, never()).fetch(0L)
    }

    @Test
    fun `successful fetch stores the version code`() {
        settings.fetchedVersionCode = VERSION_CODE - 1

        createProvider()

        assertEquals(VERSION_CODE, settings.fetchedVersionCode)
    }

    @Test
    fun `failed fetch does not store the version code`() {
        settings.fetchedVersionCode = VERSION_CODE - 1
        val fetchTask = failedTask<Void?>(RuntimeException("Network error"))
        whenever(remoteConfig.fetch(0L)).thenReturn(fetchTask)

        createProvider()

        assertEquals(VERSION_CODE - 1, settings.fetchedVersionCode)
    }

    @Test
    fun `failed activate does not store the version code`() {
        settings.fetchedVersionCode = VERSION_CODE - 1
        val activateTask = failedTask<Boolean>(RuntimeException("Activate error"))
        whenever(remoteConfig.activate()).thenReturn(activateTask)

        createProvider()

        assertEquals(VERSION_CODE - 1, settings.fetchedVersionCode)
    }

    private fun createProvider() = FirebaseRemoteFeatureProvider(settings, remoteConfig, testScope)

    private fun <T> completedTask(result: T) = mockTask<T>(exception = null).also { task ->
        whenever(task.result).thenReturn(result)
    }

    private fun <T> failedTask(exception: Exception) = mockTask<T>(exception)

    private fun <T> mockTask(exception: Exception?): Task<T> {
        val task = mock<Task<T>>()
        whenever(task.exception).thenReturn(exception)
        whenever(task.addOnCompleteListener(any<OnCompleteListener<T>>())).thenAnswer { invocation ->
            invocation.getArgument<OnCompleteListener<T>>(0).onComplete(task)
            task
        }
        return task
    }

    private class FakeRemoteConfigFetchSettings(
        private val versionCode: Int,
        var fetchedVersionCode: Int = 0,
    ) : RemoteConfigFetchSettings {
        override fun getVersionCode() = versionCode
        override fun getRemoteConfigFetchedVersionCode() = fetchedVersionCode
        override fun setRemoteConfigFetchedVersionCode(versionCode: Int) {
            fetchedVersionCode = versionCode
        }
    }

    private companion object {
        const val VERSION_CODE = 9471
    }
}

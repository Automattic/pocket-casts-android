package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.os.Bundle
import au.com.shiftyjelly.pocketcasts.models.to.RefreshState
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.sync.SyncManager
import java.util.Date
import kotlin.time.Duration.Companion.milliseconds
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.yield
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.atLeastOnce
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner

@Suppress("DEPRECATION") // Network.isConnected reads the deprecated activeNetworkInfo, so the stubs must too.
@RunWith(RobolectricTestRunner::class)
class AutomotiveStartupSyncTest {

    private lateinit var settings: Settings
    private lateinit var syncManager: SyncManager
    private lateinit var connectivityManager: ConnectivityManager

    @Before
    fun setUp() {
        settings = mock()
        syncManager = mock()
        connectivityManager = mock()
    }

    @Test
    fun `is never pending off automotive`() = runTest {
        val sync = createSync(automotive = false)

        assertFalse(sync.isPending)
        sync.awaitSynced()
        verify(syncManager, never()).isLoggedIn()
    }

    @Test
    fun `is pending on automotive until awaited`() = runTest {
        val sync = createSync(automotive = true)
        whenever(syncManager.isLoggedIn()).thenReturn(false)

        assertTrue(sync.isPending)
        sync.awaitSynced()
        assertFalse(sync.isPending)
    }

    @Test
    fun `skips the sync when signed out`() = runTest {
        val sync = createSync(automotive = true)
        whenever(syncManager.isLoggedIn()).thenReturn(false)

        sync.awaitSynced()

        assertFalse(sync.isPending)
        verify(connectivityManager, never()).activeNetworkInfo
    }

    @Test
    fun `skips the sync when a refresh already succeeded since the process started`() = runTest {
        val sync = createSync(automotive = true)
        whenever(syncManager.isLoggedIn()).thenReturn(true)
        whenever(settings.getLastRefreshTime()).thenReturn(System.currentTimeMillis() + 1_000)
        whenever(settings.getRefreshState()).thenReturn(RefreshState.Success(Date()))

        sync.awaitSynced()

        assertFalse(sync.isPending)
        verify(connectivityManager, never()).activeNetworkInfo
    }

    @Test
    fun `gives up when the network never comes up`() = runTest {
        val sync = createOfflineSync()

        sync.awaitSynced()

        assertFalse(sync.isPending)
        verify(connectivityManager, atLeastOnce()).activeNetworkInfo
    }

    @Test
    fun `runs the sync once however many plays wait for it`() = runTest {
        val sync = createOfflineSync()

        val first = async { sync.awaitSynced() }
        val second = async { sync.awaitSynced() }
        first.await()
        second.await()

        assertFalse(sync.isPending)
        verify(syncManager, times(1)).isLoggedIn()
    }

    @Test
    fun `cancelling a waiting play does not cancel the sync`() = runTest {
        val sync = createOfflineSync()

        val cancelled = launch { sync.awaitSynced() }
        yield()
        cancelled.cancel()
        sync.awaitSynced()

        assertFalse(sync.isPending)
        verify(syncManager, times(1)).isLoggedIn()
        verify(connectivityManager, atLeastOnce()).activeNetworkInfo
    }

    @Test
    fun `reset makes the next car start pending again`() = runTest {
        val sync = createSync(automotive = true)
        whenever(syncManager.isLoggedIn()).thenReturn(false)
        sync.awaitSynced()
        assertFalse(sync.isPending)

        sync.reset()

        assertTrue(sync.isPending)
        sync.awaitSynced()
        verify(syncManager, times(2)).isLoggedIn()
    }

    private fun createOfflineSync(): AutomotiveStartupSync {
        val sync = createSync(automotive = true).apply { networkTimeout = 100.milliseconds }
        whenever(syncManager.isLoggedIn()).thenReturn(true)
        whenever(settings.getLastRefreshTime()).thenReturn(0L)
        whenever(settings.getRefreshState()).thenReturn(RefreshState.Failed("Not refreshing as internet not connected"))
        whenever(connectivityManager.activeNetworkInfo).thenReturn(null)
        return sync
    }

    private fun createSync(automotive: Boolean): AutomotiveStartupSync {
        val context: Context = mock()
        val packageManager: PackageManager = mock()
        val metaData = Bundle().apply { putBoolean("pocketcasts_automotive", automotive) }
        val appInfo = ApplicationInfo().apply { this.metaData = metaData }
        whenever(packageManager.getApplicationInfo(any<String>(), any<Int>())).thenReturn(appInfo)
        whenever(context.packageManager).thenReturn(packageManager)
        whenever(context.packageName).thenReturn("au.com.shiftyjelly.pocketcasts.debug")
        whenever(context.getSystemService(Context.CONNECTIVITY_SERVICE)).thenReturn(connectivityManager)
        return AutomotiveStartupSync(
            context = context,
            settings = settings,
            syncManager = syncManager,
            applicationScope = CoroutineScope(Dispatchers.Unconfined),
        )
    }
}

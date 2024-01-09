@file:OptIn(ExperimentalCoroutinesApi::class)

package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.net.NetworkCapabilities
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestCoroutineScheduler
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class PlaybackManagerNetworkWatcherTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Mock private lateinit var networkConnectionWatcher: NetworkConnectionWatcher
    private lateinit var playbackManagerNetworkWatcher: PlaybackManagerNetworkWatcher

    @Test
    fun `handles switch to metered connection`() = runTest {
        val isMeteredCapabilities = listOf(false, true)
        val timesCalled = getSwitchToMeteredCalls(backgroundScope, testScheduler, isMeteredCapabilities)
        assertEquals(1, timesCalled)
    }

    @Test
    fun `does not record switch if always on metered`() = runTest {
        val isMeteredCapabilities = listOf(true, true, true)
        val timesCalled = getSwitchToMeteredCalls(backgroundScope, testScheduler, isMeteredCapabilities)
        assertEquals(0, timesCalled)
    }

    @Test
    fun `records multiple switches to metered connection`() = runTest {
        val isMeteredCapabilities = listOf(
            false, true, // first switch to metered
            true, true, true, false, false, true, // second switch to metered
            true, // not a switch to metered because was already metered
        )
        val timesCalled = getSwitchToMeteredCalls(backgroundScope, testScheduler, isMeteredCapabilities)
        assertEquals(2, timesCalled)
    }

    private fun getSwitchToMeteredCalls(
        backgroundScope: CoroutineScope,
        testScheduler: TestCoroutineScheduler,
        list: List<Boolean>,
    ): Int {
        // initialize NetworkConnectionWatcher mock
        val mutableNetworkCapabilitiesFlow = MutableStateFlow<NetworkCapabilities?>(null)
        whenever(networkConnectionWatcher.networkCapabilities)
            .thenReturn(mutableNetworkCapabilitiesFlow)

        playbackManagerNetworkWatcher = PlaybackManagerNetworkWatcher(
            applicationScope = CoroutineScope(UnconfinedTestDispatcher(testScheduler)),
            networkConnectionWatcher = networkConnectionWatcher,
        )

        var numTimesCalledOnSwitchToMeteredConnection = 0
        playbackManagerNetworkWatcher.initialize(
            onSwitchToMeteredConnection = { numTimesCalledOnSwitchToMeteredConnection++ },
        )

        list.forEach { isMetered ->
            backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
                mutableNetworkCapabilitiesFlow.value = networkCapabilities(isMetered)
            }
        }

        return numTimesCalledOnSwitchToMeteredConnection
    }

    private fun networkCapabilities(isMetered: Boolean) = mock<NetworkCapabilities> {
        on { hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) } doReturn !isMetered
    }
}

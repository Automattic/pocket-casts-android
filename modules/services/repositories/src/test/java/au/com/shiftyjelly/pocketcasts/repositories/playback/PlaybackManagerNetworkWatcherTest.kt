@file:OptIn(ExperimentalCoroutinesApi::class)

package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.net.NetworkCapabilities
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

@ExperimentalCoroutinesApi
class PlaybackManagerNetworkWatcherTest {
    private val inputNetworkCapabilities = MutableStateFlow<NetworkCapabilities?>(null)
    private var numTimesCalledOnSwitchToMeteredConnection = 0

    private val networkConnectionWatcher = object : NetworkConnectionWatcher {
        override val networkCapabilities = inputNetworkCapabilities
    }
    private val playbackManagerNetworkWatcher = PlaybackManagerNetworkWatcher(
        networkConnectionWatcher = networkConnectionWatcher,
        onSwitchToMeteredConnection = { numTimesCalledOnSwitchToMeteredConnection++ },
    )

    @Test
    fun `handles switch to metered connection`() = runTest {
        val isMeteredCapabilities = listOf(false, true)
        runNetworkMetering(isMeteredCapabilities)
        assertEquals(1, numTimesCalledOnSwitchToMeteredConnection)
    }

    @Test
    fun `does not record switch if always on metered`() = runTest {
        val isMeteredCapabilities = listOf(true, true, true)
        runNetworkMetering(isMeteredCapabilities)
        assertEquals(0, numTimesCalledOnSwitchToMeteredConnection)
    }

    @Test
    fun `records multiple switches to metered connection`() = runTest {
        val isMeteredCapabilities = listOf(
            false, true, // first switch to metered
            true, true, true, false, false, true, // second switch to metered
            true, // not a switch to metered because was already metered
        )
        runNetworkMetering(isMeteredCapabilities)
        assertEquals(2, numTimesCalledOnSwitchToMeteredConnection)
    }

    private suspend fun TestScope.runNetworkMetering(meteredChanges: List<Boolean>) {
        backgroundScope.launch(Dispatchers.Unconfined) {
            playbackManagerNetworkWatcher.observeConnection()
        }

        inputNetworkCapabilities.emitAll(meteredChanges.map(::networkCapabilities).asFlow())
    }

    private fun networkCapabilities(isMetered: Boolean) = mock<NetworkCapabilities> {
        on { hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) } doReturn !isMetered
    }
}

package au.com.shiftyjelly.pocketcasts.shared

import android.content.Context
import android.net.NetworkCapabilities
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.never
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify

@RunWith(MockitoJUnitRunner::class)
@OptIn(ExperimentalCoroutinesApi::class)
class NetworkConnectionWatcherTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Mock @ApplicationContext private lateinit var context: Context
    @Mock private lateinit var playbackManager: PlaybackManager

    lateinit var networkConnectionWatcher: NetworkConnectionWatcher

    @Before
    fun setUp() {
        networkConnectionWatcher = NetworkConnectionWatcher(
            applicationScope = CoroutineScope(Dispatchers.Default),
            context = context,
            playbackManager = playbackManager,
        )
    }

    @Test
    fun `handles change to metered connection`() = runTest {
        listOf(false, true).forEach { isMetered ->
            networkConnectionWatcher.onCapabilitiesChanged(networkCapabilities(isMetered))
        }

        verify(playbackManager).onSwitchedToMeteredConnection()
    }

    @Test
    fun `does not record switch if always on metered`() = runTest {
        listOf(true, true, true).forEach { isMetered ->
            networkConnectionWatcher.onCapabilitiesChanged(networkCapabilities(isMetered))
        }

        verify(playbackManager, never()).onSwitchedToMeteredConnection()
    }

    @Test
    fun `records multiple switches to metered connection`() = runTest {
        listOf(
            false, true, // first switch to metered
            true, true, true, false, false, true, // second switch to metered
            true // not a switch to metered because was already metered
        ).forEach { isMetered ->
            networkConnectionWatcher.onCapabilitiesChanged(networkCapabilities(isMetered))
        }

        verify(playbackManager, times(2)).onSwitchedToMeteredConnection()
    }

    // Returns a mock NetworkCapabilities reflecting a metered or unmetered connection based on
    // the parameter value
    private fun networkCapabilities(isMetered: Boolean) = mock<NetworkCapabilities> {
        on { hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) } doReturn !isMetered
    }
}

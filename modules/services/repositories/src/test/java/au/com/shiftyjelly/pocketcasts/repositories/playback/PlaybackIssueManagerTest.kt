package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import android.net.NetworkCapabilities
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@OptIn(ExperimentalCoroutinesApi::class)
class PlaybackIssueManagerTest {

    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    private val playbackStateFlow = MutableSharedFlow<PlaybackState>()
    private val networkCapabilitiesFlow = MutableStateFlow<NetworkCapabilities?>(null)

    private val playbackManager = mock<PlaybackManager> {
        on { this.playbackStateFlow } doReturn this@PlaybackIssueManagerTest.playbackStateFlow
    }
    private val networkConnectionWatcher = object : NetworkConnectionWatcher {
        override val networkCapabilities = networkCapabilitiesFlow
    }
    private val context = mock<Context> {
        on { getString(LR.string.error_playback_offline) } doReturn "You're offline"
        on { getString(LR.string.error_check_your_internet_connection) } doReturn "Check your internet connection."
    }

    private val manager = PlaybackIssueManager(
        playbackManager = playbackManager,
        networkConnectionWatcher = networkConnectionWatcher,
        context = context,
    )

    @Test
    fun `error state while offline shows offline message`() = runTest {
        FeatureFlag.setEnabled(Feature.PLAYBACK_ERROR_INFO_BAR, true)
        networkCapabilitiesFlow.value = null

        manager.playbackIssue.test {
            playbackStateFlow.emit(PlaybackState(state = PlaybackState.State.ERROR))

            assertEquals("You're offline", awaitItem()?.message)
        }
    }

    @Test
    fun `error state while online with lastErrorMessage shows that message`() = runTest {
        FeatureFlag.setEnabled(Feature.PLAYBACK_ERROR_INFO_BAR, true)
        networkCapabilitiesFlow.value = onlineCapabilities()

        manager.playbackIssue.test {
            playbackStateFlow.emit(
                PlaybackState(
                    state = PlaybackState.State.ERROR,
                    lastErrorMessage = "Custom error",
                ),
            )

            assertEquals("Custom error", awaitItem()?.message)
        }
    }

    @Test
    fun `error state while online without lastErrorMessage shows generic internet message`() = runTest {
        FeatureFlag.setEnabled(Feature.PLAYBACK_ERROR_INFO_BAR, true)
        networkCapabilitiesFlow.value = onlineCapabilities()

        manager.playbackIssue.test {
            playbackStateFlow.emit(PlaybackState(state = PlaybackState.State.ERROR))

            assertEquals("Check your internet connection.", awaitItem()?.message)
        }
    }

    @Test
    fun `non-error state returns null`() = runTest {
        FeatureFlag.setEnabled(Feature.PLAYBACK_ERROR_INFO_BAR, true)
        networkCapabilitiesFlow.value = onlineCapabilities()

        manager.playbackIssue.test {
            playbackStateFlow.emit(PlaybackState(state = PlaybackState.State.PLAYING))

            assertNull(awaitItem())
        }
    }

    @Test
    fun `disabled feature flag returns null`() = runTest {
        FeatureFlag.setEnabled(Feature.PLAYBACK_ERROR_INFO_BAR, false)
        networkCapabilitiesFlow.value = null

        manager.playbackIssue.test {
            playbackStateFlow.emit(PlaybackState(state = PlaybackState.State.ERROR))

            assertNull(awaitItem())
        }
    }

    private fun onlineCapabilities() = mock<NetworkCapabilities> {
        on { hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } doReturn true
    }
}

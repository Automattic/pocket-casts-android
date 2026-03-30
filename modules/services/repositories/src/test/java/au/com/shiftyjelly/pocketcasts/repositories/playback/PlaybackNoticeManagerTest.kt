@file:OptIn(ExperimentalCoroutinesApi::class)

package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import android.net.NetworkCapabilities
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import junit.framework.TestCase.assertEquals
import junit.framework.TestCase.assertNull
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock

class PlaybackNoticeManagerTest {

    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    private val networkCapabilities = MutableStateFlow<NetworkCapabilities?>(null)
    private val playbackStateFlow = MutableStateFlow(PlaybackState())
    private val isInForeground = MutableStateFlow(true)

    private val offlineMessage = "No connection"
    private val connectedMessage = "You're connected"
    private val episodeNotAvailableMessage = "Episode not available"

    private val context: Context = mock {
        on { getString(au.com.shiftyjelly.pocketcasts.localization.R.string.error_playback_offline) } doReturn offlineMessage
        on { getString(au.com.shiftyjelly.pocketcasts.localization.R.string.error_playback_connected) } doReturn connectedMessage
        on { getString(au.com.shiftyjelly.pocketcasts.localization.R.string.error_episode_not_available) } doReturn episodeNotAvailableMessage
    }

    private val networkConnectionWatcher = object : NetworkConnectionWatcher {
        override val networkCapabilities: StateFlow<NetworkCapabilities?> = this@PlaybackNoticeManagerTest.networkCapabilities
    }

    private val playbackManager: PlaybackManager = mock {
        on { this.playbackStateFlow } doReturn this@PlaybackNoticeManagerTest.playbackStateFlow as Flow<PlaybackState>
    }

    private val appLifecycleProvider = object : AppLifecycleProvider {
        override val isInForeground: StateFlow<Boolean> = this@PlaybackNoticeManagerTest.isInForeground
    }

    private fun createManager(scope: kotlinx.coroutines.CoroutineScope) = PlaybackNoticeManager(
        playbackManager = playbackManager,
        networkConnectionWatcher = networkConnectionWatcher,
        appLifecycleProvider = appLifecycleProvider,
        applicationScope = scope,
        context = context,
    )

    @Test
    fun `connection lost shown when offline without playback`() = runTest {
        FeatureFlag.setEnabled(Feature.PLAYBACK_ERROR_INFO_BAR, true)
        val manager = createManager(backgroundScope)
        runCurrent()

        manager.playbackNotice.test {
            val notice = awaitItem()
            assertEquals(PlaybackNoticeType.CONNECTION_LOST, notice?.type)
            assertEquals(offlineMessage, notice?.message)

            advanceTimeBy(PlaybackNoticeManager.AUTO_DISMISS_DURATION)
            runCurrent()
            assertNull(awaitItem())
        }
    }

    @Test
    fun `connection lost shown when offline during playback`() = runTest {
        FeatureFlag.setEnabled(Feature.PLAYBACK_ERROR_INFO_BAR, true)
        networkCapabilities.value = onlineCapabilities()
        playbackStateFlow.value = PlaybackState(state = PlaybackState.State.PLAYING)
        val manager = createManager(backgroundScope)
        runCurrent()

        manager.playbackNotice.test {
            assertNull(awaitItem())

            networkCapabilities.value = null
            val notice = awaitItem()
            assertEquals(PlaybackNoticeType.CONNECTION_LOST, notice?.type)
            assertEquals(offlineMessage, notice?.message)

            advanceTimeBy(PlaybackNoticeManager.AUTO_DISMISS_DURATION)
            runCurrent()
            assertNull(awaitItem())
        }
    }

    @Test
    fun `recovery message on offline to online transition`() = runTest {
        FeatureFlag.setEnabled(Feature.PLAYBACK_ERROR_INFO_BAR, true)
        networkCapabilities.value = onlineCapabilities()
        val manager = createManager(backgroundScope)
        runCurrent()

        manager.playbackNotice.test {
            assertNull(awaitItem())

            networkCapabilities.value = null
            assertEquals(PlaybackNoticeType.CONNECTION_LOST, awaitItem()?.type)

            networkCapabilities.value = onlineCapabilities()
            val recovery = awaitItem()
            assertEquals(PlaybackNoticeType.RECOVERY, recovery?.type)
            assertEquals(connectedMessage, recovery?.message)
        }
    }

    @Test
    fun `connection lost auto-dismisses after 5 seconds`() = runTest {
        FeatureFlag.setEnabled(Feature.PLAYBACK_ERROR_INFO_BAR, true)
        networkCapabilities.value = onlineCapabilities()
        val manager = createManager(backgroundScope)
        runCurrent()

        manager.playbackNotice.test {
            assertNull(awaitItem())

            networkCapabilities.value = null
            assertEquals(PlaybackNoticeType.CONNECTION_LOST, awaitItem()?.type)

            advanceTimeBy(PlaybackNoticeManager.AUTO_DISMISS_DURATION)
            runCurrent()
            assertNull(awaitItem())
        }
    }

    @Test
    fun `recovery auto-dismisses after 5 seconds`() = runTest {
        FeatureFlag.setEnabled(Feature.PLAYBACK_ERROR_INFO_BAR, true)
        networkCapabilities.value = onlineCapabilities()
        val manager = createManager(backgroundScope)
        runCurrent()

        manager.playbackNotice.test {
            assertNull(awaitItem())

            networkCapabilities.value = null
            awaitItem()

            networkCapabilities.value = onlineCapabilities()
            awaitItem()

            advanceTimeBy(PlaybackNoticeManager.AUTO_DISMISS_DURATION)
            runCurrent()
            assertNull(awaitItem())
        }
    }

    @Test
    fun `no recovery on initial app start`() = runTest {
        FeatureFlag.setEnabled(Feature.PLAYBACK_ERROR_INFO_BAR, true)
        val manager = createManager(backgroundScope)
        runCurrent()

        manager.playbackNotice.test {
            assertEquals(PlaybackNoticeType.CONNECTION_LOST, awaitItem()?.type)

            networkCapabilities.value = onlineCapabilities()
            assertNull(awaitItem())
        }
    }

    @Test
    fun `rapid offline-online toggling resets recovery timer`() = runTest {
        FeatureFlag.setEnabled(Feature.PLAYBACK_ERROR_INFO_BAR, true)
        networkCapabilities.value = onlineCapabilities()
        val manager = createManager(backgroundScope)
        runCurrent()

        manager.playbackNotice.test {
            assertNull(awaitItem())

            networkCapabilities.value = null
            awaitItem()

            networkCapabilities.value = onlineCapabilities()
            awaitItem()

            advanceTimeBy(3_000)
            runCurrent()

            networkCapabilities.value = null
            assertEquals(PlaybackNoticeType.CONNECTION_LOST, awaitItem()?.type)

            networkCapabilities.value = onlineCapabilities()
            assertEquals(PlaybackNoticeType.RECOVERY, awaitItem()?.type)

            advanceTimeBy(PlaybackNoticeManager.AUTO_DISMISS_DURATION)
            runCurrent()
            assertNull(awaitItem())
        }
    }

    @Test
    fun `playback error shown and auto-dismissed after 5 seconds`() = runTest {
        FeatureFlag.setEnabled(Feature.PLAYBACK_ERROR_INFO_BAR, true)
        networkCapabilities.value = onlineCapabilities()
        val manager = createManager(backgroundScope)
        runCurrent()

        manager.playbackNotice.test {
            assertNull(awaitItem())

            playbackStateFlow.value = PlaybackState(state = PlaybackState.State.ERROR)
            val notice = awaitItem()
            assertEquals(PlaybackNoticeType.PLAYBACK, notice?.type)
            assertEquals(episodeNotAvailableMessage, notice?.message)

            advanceTimeBy(PlaybackNoticeManager.AUTO_DISMISS_DURATION)
            runCurrent()
            assertNull(awaitItem())
        }
    }

    @Test
    fun `playback error deferred when in background`() = runTest {
        FeatureFlag.setEnabled(Feature.PLAYBACK_ERROR_INFO_BAR, true)
        networkCapabilities.value = onlineCapabilities()
        isInForeground.value = false
        val manager = createManager(backgroundScope)
        runCurrent()

        manager.playbackNotice.test {
            assertNull(awaitItem())

            playbackStateFlow.value = PlaybackState(state = PlaybackState.State.ERROR)
            runCurrent()
            expectNoEvents()
        }
    }

    @Test
    fun `playback error shown on return to foreground`() = runTest {
        FeatureFlag.setEnabled(Feature.PLAYBACK_ERROR_INFO_BAR, true)
        networkCapabilities.value = onlineCapabilities()
        isInForeground.value = false
        val manager = createManager(backgroundScope)
        runCurrent()

        manager.playbackNotice.test {
            assertNull(awaitItem())

            playbackStateFlow.value = PlaybackState(state = PlaybackState.State.ERROR)
            runCurrent()
            expectNoEvents()

            isInForeground.value = true
            assertEquals(PlaybackNoticeType.PLAYBACK, awaitItem()?.type)
        }
    }

    @Test
    fun `playback error shown on return to foreground auto-dismisses after 5 seconds`() = runTest {
        FeatureFlag.setEnabled(Feature.PLAYBACK_ERROR_INFO_BAR, true)
        networkCapabilities.value = onlineCapabilities()
        isInForeground.value = false
        val manager = createManager(backgroundScope)
        runCurrent()

        manager.playbackNotice.test {
            assertNull(awaitItem())

            playbackStateFlow.value = PlaybackState(state = PlaybackState.State.ERROR)
            runCurrent()
            expectNoEvents()

            isInForeground.value = true
            assertEquals(PlaybackNoticeType.PLAYBACK, awaitItem()?.type)

            advanceTimeBy(PlaybackNoticeManager.AUTO_DISMISS_DURATION)
            runCurrent()
            assertNull(awaitItem())
        }
    }

    @Test
    fun `no playback error on foreground return if playback resumed in background`() = runTest {
        FeatureFlag.setEnabled(Feature.PLAYBACK_ERROR_INFO_BAR, true)
        networkCapabilities.value = onlineCapabilities()
        isInForeground.value = false
        val manager = createManager(backgroundScope)
        runCurrent()

        manager.playbackNotice.test {
            assertNull(awaitItem())

            playbackStateFlow.value = PlaybackState(state = PlaybackState.State.ERROR)
            runCurrent()
            expectNoEvents()

            // Playback resumes while in background
            playbackStateFlow.value = PlaybackState(state = PlaybackState.State.PLAYING)
            runCurrent()
            expectNoEvents()

            // User returns to foreground - no error should be shown
            isInForeground.value = true
            runCurrent()
            expectNoEvents()
        }
    }

    @Test
    fun `connection lost takes priority over playback error`() = runTest {
        FeatureFlag.setEnabled(Feature.PLAYBACK_ERROR_INFO_BAR, true)
        networkCapabilities.value = onlineCapabilities()
        playbackStateFlow.value = PlaybackState(state = PlaybackState.State.ERROR)
        val manager = createManager(backgroundScope)
        runCurrent()

        manager.playbackNotice.test {
            assertEquals(PlaybackNoticeType.PLAYBACK, awaitItem()?.type)

            networkCapabilities.value = null
            assertEquals(PlaybackNoticeType.CONNECTION_LOST, awaitItem()?.type)
        }
    }

    @Test
    fun `recovery takes priority over playback error`() = runTest {
        FeatureFlag.setEnabled(Feature.PLAYBACK_ERROR_INFO_BAR, true)
        networkCapabilities.value = onlineCapabilities()
        playbackStateFlow.value = PlaybackState(state = PlaybackState.State.ERROR)
        val manager = createManager(backgroundScope)
        runCurrent()

        manager.playbackNotice.test {
            assertEquals(PlaybackNoticeType.PLAYBACK, awaitItem()?.type)

            networkCapabilities.value = null
            awaitItem()

            networkCapabilities.value = onlineCapabilities()
            assertEquals(PlaybackNoticeType.RECOVERY, awaitItem()?.type)
        }
    }

    @Test
    fun `playback error dismissed while masked by connection lost`() = runTest {
        FeatureFlag.setEnabled(Feature.PLAYBACK_ERROR_INFO_BAR, true)
        networkCapabilities.value = onlineCapabilities()
        playbackStateFlow.value = PlaybackState(state = PlaybackState.State.ERROR)
        val manager = createManager(backgroundScope)
        runCurrent()

        manager.playbackNotice.test {
            // Playback error is shown
            assertEquals(PlaybackNoticeType.PLAYBACK, awaitItem()?.type)

            // Connection lost masks the playback error (auto-dismiss timer continues)
            networkCapabilities.value = null
            assertEquals(PlaybackNoticeType.CONNECTION_LOST, awaitItem()?.type)

            // Wait longer than auto-dismiss duration while masked
            // Both CONNECTION_LOST and PLAYBACK auto-dismiss timers fire
            advanceTimeBy(PlaybackNoticeManager.AUTO_DISMISS_DURATION + 1.seconds)
            runCurrent()
            assertNull(awaitItem())

            // Connection recovers — playback error was already auto-dismissed while masked
            networkCapabilities.value = onlineCapabilities()
            val notice = awaitItem()
            // Recovery is shown (not the stale playback error)
            assertEquals(PlaybackNoticeType.RECOVERY, notice?.type)

            // After recovery auto-dismisses, notice clears completely
            advanceTimeBy(PlaybackNoticeManager.AUTO_DISMISS_DURATION)
            runCurrent()
            assertNull(awaitItem())
        }
    }

    @Test
    fun `feature flag disabled returns null`() = runTest {
        FeatureFlag.setEnabled(Feature.PLAYBACK_ERROR_INFO_BAR, false)
        val manager = createManager(backgroundScope)
        runCurrent()

        manager.playbackNotice.test {
            assertNull(awaitItem())

            networkCapabilities.value = null
            playbackStateFlow.value = PlaybackState(state = PlaybackState.State.ERROR)
            runCurrent()

            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun onlineCapabilities() = mock<NetworkCapabilities> {
        on { hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } doReturn true
    }
}

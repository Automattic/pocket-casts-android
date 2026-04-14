@file:OptIn(ExperimentalCoroutinesApi::class)

package au.com.shiftyjelly.pocketcasts.repositories.playback

import android.content.Context
import android.net.NetworkCapabilities
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.preferences.Settings
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
    private val unableToPlayMessage = "Unable to play"
    private val accessDeniedMessage = "This episode can't be played."
    private val accessDeniedAction = "Learn more"

    private val context: Context = mock {
        on { getString(au.com.shiftyjelly.pocketcasts.localization.R.string.error_playback_offline) } doReturn offlineMessage
        on { getString(au.com.shiftyjelly.pocketcasts.localization.R.string.error_playback_connected) } doReturn connectedMessage
        on { getString(au.com.shiftyjelly.pocketcasts.localization.R.string.error_episode_not_available) } doReturn episodeNotAvailableMessage
        on { getString(au.com.shiftyjelly.pocketcasts.localization.R.string.error_unable_to_play) } doReturn unableToPlayMessage
        on { getString(au.com.shiftyjelly.pocketcasts.localization.R.string.error_streaming_access_denied) } doReturn accessDeniedMessage
        on { getString(au.com.shiftyjelly.pocketcasts.localization.R.string.settings_battery_learn_more) } doReturn accessDeniedAction
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

    private val errorClassifier = PlaybackErrorClassifier()

    private fun createManager(scope: kotlinx.coroutines.CoroutineScope) = PlaybackNoticeManager(
        playbackManager = playbackManager,
        networkConnectionWatcher = networkConnectionWatcher,
        appLifecycleProvider = appLifecycleProvider,
        errorClassifier = errorClassifier,
        applicationScope = scope,
        context = context,
    )

    @Test
    fun `connection lost shown when offline without playback`() = runTest {
        FeatureFlag.setEnabled(Feature.PLAYBACK_ERROR_INFO_BAR, true)
        networkCapabilities.value = onlineCapabilities()
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
    fun `no notice on initial app start with unknown network`() = runTest {
        FeatureFlag.setEnabled(Feature.PLAYBACK_ERROR_INFO_BAR, true)
        val manager = createManager(backgroundScope)
        runCurrent()

        manager.playbackNotice.test {
            // Initial null capabilities treated as unknown — no CONNECTION_LOST shown
            assertNull(awaitItem())

            // Going online from unknown state should not show RECOVERY
            networkCapabilities.value = onlineCapabilities()
            runCurrent()
            expectNoEvents()
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
    fun `connection playback error shown as connection lost`() = runTest {
        FeatureFlag.setEnabled(Feature.PLAYBACK_ERROR_INFO_BAR, true)
        networkCapabilities.value = onlineCapabilities()
        val manager = createManager(backgroundScope)
        runCurrent()

        manager.playbackNotice.test {
            assertNull(awaitItem())

            playbackStateFlow.value = PlaybackState(
                state = PlaybackState.State.ERROR,
                playbackIssue = PlaybackIssue.ConnectionError,
            )
            val notice = awaitItem()
            assertEquals(PlaybackNoticeType.CONNECTION_LOST, notice?.type)
            assertEquals(offlineMessage, notice?.message)

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
            assertEquals(accessDeniedMessage, notice?.message)
            assertEquals(accessDeniedAction, notice?.linkText)

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

    @Test
    fun `playback error with http 401 has access issues support url`() = runTest {
        FeatureFlag.setEnabled(Feature.PLAYBACK_ERROR_INFO_BAR, true)
        networkCapabilities.value = onlineCapabilities()
        val manager = createManager(backgroundScope)
        runCurrent()

        manager.playbackNotice.test {
            assertNull(awaitItem())

            playbackStateFlow.value = PlaybackState(
                state = PlaybackState.State.ERROR,
                playbackIssue = PlaybackIssue.HttpError(401),
            )
            val notice = awaitItem()
            assertEquals(PlaybackNoticeType.PLAYBACK, notice?.type)
            assertEquals(Settings.INFO_EPISODE_ACCESS_ISSUES_URL, notice?.supportUrl)
        }
    }

    @Test
    fun `playback error with http 404 has not found support url`() = runTest {
        FeatureFlag.setEnabled(Feature.PLAYBACK_ERROR_INFO_BAR, true)
        networkCapabilities.value = onlineCapabilities()
        val manager = createManager(backgroundScope)
        runCurrent()

        manager.playbackNotice.test {
            assertNull(awaitItem())

            playbackStateFlow.value = PlaybackState(
                state = PlaybackState.State.ERROR,
                playbackIssue = PlaybackIssue.HttpError(404),
            )
            val notice = awaitItem()
            assertEquals(PlaybackNoticeType.PLAYBACK, notice?.type)
            assertEquals(Settings.INFO_EPISODE_NOT_FOUND_URL, notice?.supportUrl)
        }
    }

    @Test
    fun `playback error with http 500 has server problem support url`() = runTest {
        FeatureFlag.setEnabled(Feature.PLAYBACK_ERROR_INFO_BAR, true)
        networkCapabilities.value = onlineCapabilities()
        val manager = createManager(backgroundScope)
        runCurrent()

        manager.playbackNotice.test {
            assertNull(awaitItem())

            playbackStateFlow.value = PlaybackState(
                state = PlaybackState.State.ERROR,
                playbackIssue = PlaybackIssue.HttpError(500),
            )
            val notice = awaitItem()
            assertEquals(PlaybackNoticeType.PLAYBACK, notice?.type)
            assertEquals(Settings.INFO_EPISODE_SERVER_PROBLEM_URL, notice?.supportUrl)
        }
    }

    @Test
    fun `playback error without http code has default support url`() = runTest {
        FeatureFlag.setEnabled(Feature.PLAYBACK_ERROR_INFO_BAR, true)
        networkCapabilities.value = onlineCapabilities()
        val manager = createManager(backgroundScope)
        runCurrent()

        manager.playbackNotice.test {
            assertNull(awaitItem())

            playbackStateFlow.value = PlaybackState(state = PlaybackState.State.ERROR)
            val notice = awaitItem()
            assertEquals(PlaybackNoticeType.PLAYBACK, notice?.type)
            assertEquals(Settings.INFO_DOWNLOAD_AND_PLAYBACK_URL, notice?.supportUrl)
        }
    }

    @Test
    fun `connection error notice has no support url`() = runTest {
        FeatureFlag.setEnabled(Feature.PLAYBACK_ERROR_INFO_BAR, true)
        networkCapabilities.value = onlineCapabilities()
        val manager = createManager(backgroundScope)
        runCurrent()

        manager.playbackNotice.test {
            assertNull(awaitItem())

            playbackStateFlow.value = PlaybackState(
                state = PlaybackState.State.ERROR,
                playbackIssue = PlaybackIssue.ConnectionError,
            )
            val notice = awaitItem()
            assertEquals(PlaybackNoticeType.CONNECTION_LOST, notice?.type)
            assertNull(notice?.supportUrl)
        }
    }

    @Test
    fun `connection lost network notice has no support url`() = runTest {
        FeatureFlag.setEnabled(Feature.PLAYBACK_ERROR_INFO_BAR, true)
        networkCapabilities.value = onlineCapabilities()
        val manager = createManager(backgroundScope)
        runCurrent()

        manager.playbackNotice.test {
            assertNull(awaitItem())

            networkCapabilities.value = null
            val notice = awaitItem()
            assertEquals(PlaybackNoticeType.CONNECTION_LOST, notice?.type)
            assertNull(notice?.supportUrl)
        }
    }

    @Test
    fun `recovery notice has no support url`() = runTest {
        FeatureFlag.setEnabled(Feature.PLAYBACK_ERROR_INFO_BAR, true)
        networkCapabilities.value = onlineCapabilities()
        val manager = createManager(backgroundScope)
        runCurrent()

        manager.playbackNotice.test {
            assertNull(awaitItem())

            networkCapabilities.value = null
            awaitItem()

            networkCapabilities.value = onlineCapabilities()
            val recovery = awaitItem()
            assertEquals(PlaybackNoticeType.RECOVERY, recovery?.type)
            assertNull(recovery?.supportUrl)
        }
    }

    @Test
    fun `stuck player error shown with support url`() = runTest {
        FeatureFlag.setEnabled(Feature.PLAYBACK_ERROR_INFO_BAR, true)
        networkCapabilities.value = onlineCapabilities()
        val manager = createManager(backgroundScope)
        runCurrent()

        manager.playbackNotice.test {
            assertNull(awaitItem())

            playbackStateFlow.value = PlaybackState(
                state = PlaybackState.State.ERROR,
                playbackIssue = PlaybackIssue.StuckPlayer(au.com.shiftyjelly.pocketcasts.localization.R.string.error_unable_to_play),
            )
            val notice = awaitItem()
            assertEquals(PlaybackNoticeType.PLAYBACK, notice?.type)
            assertEquals(accessDeniedMessage, notice?.message)
            assertEquals(accessDeniedAction, notice?.linkText)
            assertEquals(Settings.INFO_DOWNLOAD_AND_PLAYBACK_URL, notice?.supportUrl)
        }
    }

    @Test
    fun `transient error state during player switch does not trigger notice`() = runTest {
        FeatureFlag.setEnabled(Feature.PLAYBACK_ERROR_INFO_BAR, true)
        networkCapabilities.value = onlineCapabilities()
        val manager = createManager(backgroundScope)
        runCurrent()

        manager.playbackNotice.test {
            assertNull(awaitItem())

            // Simulate pause(transientLoss=true) re-emitting an ERROR state during player switch
            playbackStateFlow.value = PlaybackState(
                state = PlaybackState.State.ERROR,
                playbackIssue = PlaybackIssue.HttpError(401),
                transientLoss = true,
            )
            runCurrent()
            expectNoEvents()

            // loadCurrentEpisode emits PLAYING state
            playbackStateFlow.value = PlaybackState(state = PlaybackState.State.PLAYING)
            runCurrent()
            expectNoEvents()

            // Real error arrives with transientLoss=false
            playbackStateFlow.value = PlaybackState(
                state = PlaybackState.State.ERROR,
                playbackIssue = PlaybackIssue.HttpError(401),
            )
            val notice = awaitItem()
            assertEquals(PlaybackNoticeType.PLAYBACK, notice?.type)
        }
    }

    private fun onlineCapabilities() = mock<NetworkCapabilities> {
        on { hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } doReturn true
    }
}

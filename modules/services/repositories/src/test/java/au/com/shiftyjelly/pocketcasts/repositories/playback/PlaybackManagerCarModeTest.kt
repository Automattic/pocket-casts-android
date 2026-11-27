package au.com.shiftyjelly.pocketcasts.repositories.playback

import androidx.work.testing.WorkManagerTestInitHelper
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.model.PlayOverNotificationSetting
import au.com.shiftyjelly.pocketcasts.repositories.notification.NotificationManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import java.util.Date
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PlaybackManagerCarModeTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Test
    fun `pauseSuspend retains audio focus when car is connected`() = runTest {
        val playbackManager = createPlaybackManager(backgroundScope)
        playbackManager.isAndroidAutoConnected = true
        assertTrue(playbackManager.focusManager.tryToGetAudioFocus())

        playbackManager.pauseSuspend(sourceView = SourceView.ANDROID_AUTO)

        assertTrue(playbackManager.focusManager.isFocused)
    }

    @Test
    fun `pauseSuspend gives up audio focus when car is not connected`() = runTest {
        val playbackManager = createPlaybackManager(backgroundScope)
        playbackManager.isAndroidAutoConnected = false
        assertTrue(playbackManager.focusManager.tryToGetAudioFocus())

        playbackManager.pauseSuspend(sourceView = SourceView.PLAYER)

        assertFalse(playbackManager.focusManager.isFocused)
    }

    @Test
    fun `disconnecting from Android Auto while paused releases audio focus`() = runTest {
        val playbackManager = createPlaybackManager(backgroundScope)
        playbackManager.onCarConnectionChanged(true)
        assertTrue(playbackManager.focusManager.tryToGetAudioFocus())

        playbackManager.pauseSuspend(sourceView = SourceView.ANDROID_AUTO)
        assertTrue(playbackManager.focusManager.isFocused)

        playbackManager.onCarConnectionChanged(false)

        assertFalse(playbackManager.focusManager.isFocused)
    }

    @Test
    fun `initial disconnected car state does not release audio focus`() = runTest {
        val playbackManager = createPlaybackManager(backgroundScope)
        assertTrue(playbackManager.focusManager.tryToGetAudioFocus())

        playbackManager.onCarConnectionChanged(false)

        assertTrue(playbackManager.focusManager.isFocused)
    }

    @Test
    fun `stop gives up audio focus even when car is connected`() = runTest {
        WorkManagerTestInitHelper.initializeTestWorkManager(RuntimeEnvironment.getApplication())
        val playbackManager = createPlaybackManager(backgroundScope)
        playbackManager.isAndroidAutoConnected = true
        assertTrue(playbackManager.focusManager.tryToGetAudioFocus())

        playbackManager.stop()

        assertFalse(playbackManager.focusManager.isFocused)
    }

    @Test
    fun `onFocusLoss preserves focusWasPlaying on subsequent transient loss while paused`() = runTest {
        val playbackManager = createPlaybackManager(backgroundScope)
        playbackManager.player = mock { on { isRemote } doReturn false }
        playbackManager.playbackStateRelay.accept(PlaybackState(state = PlaybackState.State.PLAYING))

        playbackManager.onFocusLoss(PlayOverNotificationSetting.NEVER, transientLoss = true)
        assertNotNull(playbackManager.focusWasPlaying)

        playbackManager.playbackStateRelay.accept(PlaybackState(state = PlaybackState.State.PAUSED))
        playbackManager.onFocusLoss(PlayOverNotificationSetting.NEVER, transientLoss = true)

        assertNotNull(playbackManager.focusWasPlaying)
    }

    @Test
    fun `onFocusLoss clears focusWasPlaying on permanent loss while already paused`() = runTest {
        val playbackManager = createPlaybackManager(backgroundScope)
        playbackManager.player = mock { on { isRemote } doReturn false }
        playbackManager.focusWasPlaying = Date()
        playbackManager.playbackStateRelay.accept(PlaybackState(state = PlaybackState.State.PAUSED))

        playbackManager.onFocusLoss(PlayOverNotificationSetting.NEVER, transientLoss = false)

        assertNull(playbackManager.focusWasPlaying)
    }

    @Test
    fun `manual pause clears focusWasPlaying while auto pause preserves it`() = runTest {
        val playbackManager = createPlaybackManager(backgroundScope)

        playbackManager.focusWasPlaying = Date()
        playbackManager.pauseSuspend(transientLoss = false, sourceView = SourceView.PLAYER)
        assertNull(playbackManager.focusWasPlaying)

        playbackManager.focusWasPlaying = Date()
        playbackManager.pauseSuspend(transientLoss = false, sourceView = SourceView.AUTO_PAUSE)
        assertNotNull(playbackManager.focusWasPlaying)
    }

    @Test
    fun `handleMediaSessionPause resumes when paused for Automotive source even if isCarConnected is false`() = runTest {
        val playbackManager = mock<PlaybackManager>()
        whenever(playbackManager.isPlaying()).thenReturn(false)
        whenever(playbackManager.isCarConnected()).thenReturn(false)

        playbackManager.handleMediaSessionPause(SourceView.AUTOMOTIVE)

        verify(playbackManager).playQueueSuspend(
            sourceView = eq(SourceView.AUTOMOTIVE),
            showedStreamWarning = any(),
        )
    }

    private fun createPlaybackManager(scope: CoroutineScope): PlaybackManager {
        val settings = mock<Settings>()
        val notificationManager = mock<NotificationManager>()
        return PlaybackManager(
            settings = settings,
            podcastManager = mock(),
            episodeManager = mock(),
            statsManager = mock(),
            playerManager = mock(),
            castManager = mock(),
            application = RuntimeEnvironment.getApplication(),
            playlistManager = mock(),
            downloadQueue = mock(),
            upNextQueue = mock(),
            notificationHelper = mock(),
            userEpisodeManager = mock(),
            eventHorizon = mock(),
            syncManager = mock(),
            bookmarkManager = mock(),
            showNotesManager = mock(),
            chapterManager = mock(),
            sleepTimer = mock(),
            playbackManagerNetworkWatcherFactory = mock(),
            applicationScope = scope,
            crashLogging = mock(),
            upNextHistoryManager = mock(),
            notificationManager = notificationManager,
            autoPlaySelector = mock(),
            browseTreeProvider = mock(),
            alternateEnclosureManager = mock(),
            generatedChapterSeeker = mock(),
            playbackServiceErrorReporter = mock(),
        )
    }
}

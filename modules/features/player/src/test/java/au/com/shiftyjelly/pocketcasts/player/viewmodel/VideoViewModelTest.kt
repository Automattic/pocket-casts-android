package au.com.shiftyjelly.pocketcasts.player.viewmodel

import android.os.Looper
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import com.jakewharton.rxrelay2.BehaviorRelay
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.argumentCaptor
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [35])
@LooperMode(LooperMode.Mode.PAUSED)
class VideoViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    private lateinit var playbackManager: PlaybackManager
    private lateinit var viewModel: VideoViewModel

    @Before
    fun setUp() {
        playbackManager = mock()
        whenever(playbackManager.playbackStateRelay).thenReturn(
            BehaviorRelay.createDefault(PlaybackState()).toSerialized(),
        )
        whenever(playbackManager.isPlaying()).thenReturn(true)
        viewModel = VideoViewModel(playbackManager)
    }

    @Test
    fun `only latest seek completion restarts hide timer while every UI completion runs`() {
        var firstCompletionCount = 0
        var secondCompletionCount = 0

        viewModel.seekStarted()
        viewModel.seekToMs(EPISODE_UUID, 10_000) { firstCompletionCount++ }
        viewModel.seekStarted()
        viewModel.seekToMs(EPISODE_UUID, 20_000) { secondCompletionCount++ }

        val callbacks = captureSeekCallbacks(10_000, 20_000)
        callbacks[0]()
        idleMainLooperFor(HIDE_CONTROLS_DELAY_MS)

        assertEquals(1, firstCompletionCount)
        assertEquals(0, secondCompletionCount)
        assertTrue(viewModel.controlsVisible.value == true)

        callbacks[1]()
        idleMainLooperFor(HIDE_CONTROLS_DELAY_MS - 1)
        assertTrue(viewModel.controlsVisible.value == true)

        idleMainLooperFor(1)
        assertEquals(1, secondCompletionCount)
        assertFalse(viewModel.controlsVisible.value == true)
    }

    @Test
    fun `stale completion after cancellation does not restart the existing hide timer`() {
        var completionCount = 0

        viewModel.seekStarted()
        viewModel.seekToMs(EPISODE_UUID, 30_000) { completionCount++ }
        val callback = captureSeekCallbacks(30_000).single()

        viewModel.seekCancelled()
        idleMainLooperFor(2_000)
        callback()
        idleMainLooperFor(999)

        assertEquals(1, completionCount)
        assertTrue(viewModel.controlsVisible.value == true)

        idleMainLooperFor(1)
        assertFalse(viewModel.controlsVisible.value == true)
    }

    private fun captureSeekCallbacks(vararg expectedPositions: Int): List<() -> Unit> {
        val positionCaptor = argumentCaptor<Int>()
        val callbackCaptor = argumentCaptor<() -> Unit>()
        verify(playbackManager, times(expectedPositions.size)).seekIfPlayingToTimeMs(
            eq(EPISODE_UUID),
            positionCaptor.capture(),
            eq(SourceView.FULL_SCREEN_VIDEO),
            callbackCaptor.capture(),
        )
        assertEquals(expectedPositions.toList(), positionCaptor.allValues)
        return callbackCaptor.allValues
    }

    private fun idleMainLooperFor(durationMs: Long) {
        shadowOf(Looper.getMainLooper()).idleFor(durationMs, TimeUnit.MILLISECONDS)
    }

    private companion object {
        const val EPISODE_UUID = "episode-uuid"
        const val HIDE_CONTROLS_DELAY_MS = 3_000L
    }
}

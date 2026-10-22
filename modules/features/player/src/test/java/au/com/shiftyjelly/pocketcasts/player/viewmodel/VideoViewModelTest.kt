package au.com.shiftyjelly.pocketcasts.player.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class VideoViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    // Replays its latest value without de-duplicating, like the BehaviorRelay behind playbackStateFlow
    private val playbackStateFlow = MutableSharedFlow<PlaybackState>(replay = 1).apply {
        tryEmit(PlaybackState(title = "First"))
    }
    private val playbackManager = mock<PlaybackManager>()
    private lateinit var viewModel: VideoViewModel

    @Before
    fun setUp() {
        whenever(playbackManager.playbackStateFlow).thenReturn(playbackStateFlow)
        viewModel = VideoViewModel(playbackManager)
    }

    @Test
    fun `controls hide three seconds after being shown while playing`() = runTest {
        whenever(playbackManager.isPlaying()).thenReturn(true)

        viewModel.showControls()
        advanceTimeBy(2_999)
        runCurrent()
        assertEquals(true, viewModel.controlsVisible.value)

        advanceTimeBy(1)
        runCurrent()
        assertEquals(false, viewModel.controlsVisible.value)
    }

    @Test
    fun `controls stay visible when paused`() = runTest {
        whenever(playbackManager.isPlaying()).thenReturn(false)

        viewModel.showControls()
        advanceTimeBy(5_000)
        runCurrent()

        assertEquals(true, viewModel.controlsVisible.value)
    }

    @Test
    fun `starting a seek cancels the hide timer`() = runTest {
        whenever(playbackManager.isPlaying()).thenReturn(true)

        viewModel.showControls()
        viewModel.seekStarted()
        advanceTimeBy(5_000)
        runCurrent()

        assertEquals(true, viewModel.controlsVisible.value)
    }

    @Test
    fun `restarting the timer pushes back when controls hide`() = runTest {
        whenever(playbackManager.isPlaying()).thenReturn(true)

        viewModel.showControls()
        advanceTimeBy(2_000)
        viewModel.skipForward()
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(true, viewModel.controlsVisible.value)

        advanceTimeBy(1_000)
        runCurrent()
        assertEquals(false, viewModel.controlsVisible.value)
    }

    @Test
    fun `playback state forwards every emission including repeats`() = runTest {
        val observed = mutableListOf<String>()
        viewModel.playbackState.observeForever { observed += it.title }

        playbackStateFlow.emit(PlaybackState(title = "Second"))
        playbackStateFlow.emit(PlaybackState(title = "Second"))

        assertEquals(listOf("First", "Second", "Second"), observed)
    }
}

package au.com.shiftyjelly.pocketcasts.nowplaying

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffects
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.playback.Player
import au.com.shiftyjelly.pocketcasts.repositories.playback.StreamVideoState
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import io.reactivex.subjects.BehaviorSubject
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.argThat
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyNoInteractions
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class TvNowPlayingViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val playbackStates = MutableStateFlow(PlaybackState())
    private val queueChanges = BehaviorSubject.create<UpNextQueue.State>()
    private val players = MutableStateFlow<Player?>(null)
    private val streamVideoStates = MutableStateFlow(StreamVideoState.NotVideo)
    private val videoRenderingEnabledStates = MutableStateFlow(true)

    private val queue = mock<UpNextQueue> {
        on { changesObservable } doReturn queueChanges
    }
    private val playbackManager = mock<PlaybackManager> {
        on { playbackStateFlow } doReturn playbackStates
        on { upNextQueue } doReturn queue
        on { playerFlow } doReturn players
        on { streamVideoState } doReturn streamVideoStates
        on { videoRenderingEnabled } doReturn videoRenderingEnabledStates
    }

    private val podcastManager = mock<PodcastManager>()

    private val skipForwardSetting = mock<UserSetting<Int>> { on { value } doReturn 30 }
    private val skipBackSetting = mock<UserSetting<Int>> { on { value } doReturn 10 }
    private val globalPlaybackEffectsSetting = mock<UserSetting<PlaybackEffects>>()
    private val settings = mock<Settings> {
        on { skipForwardInSecs } doReturn skipForwardSetting
        on { skipBackInSecs } doReturn skipBackSetting
        on { globalPlaybackEffects } doReturn globalPlaybackEffectsSetting
    }

    private val audioEpisode = PodcastEpisode(uuid = "audio", publishedDate = Date(0))
    private val videoEpisode = PodcastEpisode(uuid = "video", publishedDate = Date(0), fileType = "video/mp4")

    private val viewModel by lazy {
        TvNowPlayingViewModel(playbackManager, podcastManager, settings, coroutineRule.testDispatcher)
    }

    @Test
    fun `an empty queue maps to the empty state`() = runTest {
        viewModel.uiState.test {
            assertEquals(TvNowPlayingUiState.Empty, awaitItem())

            queueChanges.onNext(UpNextQueue.State.Empty)
            expectNoEvents()
        }
    }

    @Test
    fun `a loaded queue maps the playback state`() = runTest {
        val podcast = Podcast(uuid = "podcast", title = "Podcast")
        queueChanges.onNext(UpNextQueue.State.Loaded(audioEpisode, podcast, emptyList()))
        playbackStates.value = PlaybackState(
            state = PlaybackState.State.PLAYING,
            episodeUuid = audioEpisode.uuid,
            isBuffering = true,
            positionMs = 1_000,
            durationMs = 60_000,
            bufferedMs = 5_000,
            playbackSpeed = 1.5,
            trimMode = TrimMode.MEDIUM,
            isVolumeBoosted = true,
        )

        viewModel.uiState.test {
            val state = awaitItem() as TvNowPlayingUiState.Loaded
            assertEquals(audioEpisode, state.episode)
            assertEquals("Podcast", state.podcastTitle)
            assertEquals(true, state.isPlaying)
            assertEquals(true, state.isBuffering)
            assertEquals(null, state.errorMessage)
            assertEquals(1_000, state.positionMs)
            assertEquals(60_000, state.durationMs)
            assertEquals(5_000, state.bufferedMs)
            assertEquals(false, state.isVideo)
            assertEquals(1.5, state.playbackSpeed, 0.0)
            assertEquals(TrimMode.MEDIUM, state.trimMode)
            assertEquals(true, state.isVolumeBoosted)
        }
    }

    @Test
    fun `a default playback state maps default effects`() = runTest {
        queueChanges.onNext(UpNextQueue.State.Loaded(audioEpisode, null, emptyList()))

        viewModel.uiState.test {
            val state = awaitItem() as TvNowPlayingUiState.Loaded
            assertEquals(1.0, state.playbackSpeed, 0.0)
            assertEquals(TrimMode.OFF, state.trimMode)
            assertEquals(false, state.isVolumeBoosted)
        }
    }

    @Test
    fun `playback progress of a different episode falls back to the entity`() = runTest {
        val episode = PodcastEpisode(uuid = "next", publishedDate = Date(0), playedUpTo = 12.0, duration = 60.0)
        queueChanges.onNext(UpNextQueue.State.Loaded(episode, null, emptyList()))
        playbackStates.value = PlaybackState(
            state = PlaybackState.State.PLAYING,
            episodeUuid = "previous",
            positionMs = 55_000,
            durationMs = 55_000,
            bufferedMs = 55_000,
        )

        viewModel.uiState.test {
            val state = awaitItem() as TvNowPlayingUiState.Loaded
            assertEquals(12_000, state.positionMs)
            assertEquals(60_000, state.durationMs)
            assertEquals(0, state.bufferedMs)
        }
    }

    @Test
    fun `an error playback state is exposed`() = runTest {
        queueChanges.onNext(UpNextQueue.State.Loaded(audioEpisode, null, emptyList()))
        playbackStates.value = PlaybackState(
            state = PlaybackState.State.ERROR,
            lastErrorMessage = "Something went wrong",
        )

        viewModel.uiState.test {
            val state = awaitItem() as TvNowPlayingUiState.Loaded
            assertEquals("Something went wrong", state.errorMessage)
        }
    }

    @Test
    fun `a stream with video is video`() = runTest {
        queueChanges.onNext(UpNextQueue.State.Loaded(audioEpisode, null, emptyList()))
        streamVideoStates.value = StreamVideoState.HasVideo

        viewModel.uiState.test {
            assertEquals(true, (awaitItem() as TvNowPlayingUiState.Loaded).isVideo)
        }
    }

    @Test
    fun `an unknown stream is not video`() = runTest {
        queueChanges.onNext(UpNextQueue.State.Loaded(videoEpisode, null, emptyList()))
        streamVideoStates.value = StreamVideoState.Unknown

        viewModel.uiState.test {
            assertEquals(false, (awaitItem() as TvNowPlayingUiState.Loaded).isVideo)
        }
    }

    @Test
    fun `an audio only stream is not video`() = runTest {
        queueChanges.onNext(UpNextQueue.State.Loaded(videoEpisode, null, emptyList()))
        streamVideoStates.value = StreamVideoState.AudioOnly

        viewModel.uiState.test {
            assertEquals(false, (awaitItem() as TvNowPlayingUiState.Loaded).isVideo)
        }
    }

    @Test
    fun `a video episode without stream video info is video`() = runTest {
        queueChanges.onNext(UpNextQueue.State.Loaded(videoEpisode, null, emptyList()))

        viewModel.uiState.test {
            assertEquals(true, (awaitItem() as TvNowPlayingUiState.Loaded).isVideo)
        }
    }

    @Test
    fun `video is disabled when rendering is off`() = runTest {
        queueChanges.onNext(UpNextQueue.State.Loaded(videoEpisode, null, emptyList()))
        videoRenderingEnabledStates.value = false

        viewModel.uiState.test {
            assertEquals(false, (awaitItem() as TvNowPlayingUiState.Loaded).isVideo)
        }
    }

    @Test
    fun `playPause delegates to the playback manager`() = runTest {
        viewModel.playPause()

        verify(playbackManager).playPause(sourceView = SourceView.PLAYER)
    }

    @Test
    fun `skipForward delegates to the playback manager`() = runTest {
        viewModel.skipForward()

        verify(playbackManager).skipForward(sourceView = SourceView.PLAYER, jumpAmountSeconds = 30)
    }

    @Test
    fun `skipBackward delegates to the playback manager`() = runTest {
        viewModel.skipBackward()

        verify(playbackManager).skipBackward(sourceView = SourceView.PLAYER, jumpAmountSeconds = 10)
    }

    @Test
    fun `a speed change without a podcast override saves global effects`() = runTest {
        playbackStates.value = PlaybackState(episodeUuid = audioEpisode.uuid, trimMode = TrimMode.LOW, isVolumeBoosted = true)
        whenever(playbackManager.getCurrentEpisode()).thenReturn(audioEpisode)
        whenever(podcastManager.findPodcastByUuid(audioEpisode.podcastUuid)).thenReturn(Podcast(uuid = "podcast"))

        viewModel.setPlaybackSpeed(1.5)
        runCurrent()

        verify(globalPlaybackEffectsSetting).set(effectsWith(1.5, TrimMode.LOW, true), eq(true), any(), any())
        verify(playbackManager).updatePlayerEffects(effectsWith(1.5, TrimMode.LOW, true))
        verify(podcastManager, never()).updateEffectsBlocking(any(), any())
    }

    @Test
    fun `a speed change with a podcast override saves the podcast effects`() = runTest {
        val podcast = Podcast(uuid = "podcast", overrideGlobalEffects = true)
        playbackStates.value = PlaybackState(episodeUuid = audioEpisode.uuid)
        whenever(playbackManager.getCurrentEpisode()).thenReturn(audioEpisode)
        whenever(podcastManager.findPodcastByUuid(audioEpisode.podcastUuid)).thenReturn(podcast)

        viewModel.setPlaybackSpeed(2.0)
        runCurrent()

        verify(podcastManager).updateEffectsBlocking(eq(podcast), effectsWith(2.0, TrimMode.OFF, false))
        verify(playbackManager).updatePlayerEffects(effectsWith(2.0, TrimMode.OFF, false))
        verify(globalPlaybackEffectsSetting, never()).set(any(), any(), any(), any())
    }

    @Test
    fun `a user episode saves global effects without a podcast lookup`() = runTest {
        val userEpisode = UserEpisode(uuid = "user", publishedDate = Date(0))
        playbackStates.value = PlaybackState(episodeUuid = userEpisode.uuid)
        whenever(playbackManager.getCurrentEpisode()).thenReturn(userEpisode)

        viewModel.setTrimMode(TrimMode.HIGH)
        runCurrent()

        verify(globalPlaybackEffectsSetting).set(effectsWith(1.0, TrimMode.HIGH, false), eq(true), any(), any())
        verify(playbackManager).updatePlayerEffects(effectsWith(1.0, TrimMode.HIGH, false))
        verifyNoInteractions(podcastManager)
    }

    @Test
    fun `setting volume boost keeps the other effects`() = runTest {
        playbackStates.value = PlaybackState(episodeUuid = audioEpisode.uuid, playbackSpeed = 1.5, trimMode = TrimMode.LOW)
        whenever(playbackManager.getCurrentEpisode()).thenReturn(audioEpisode)

        viewModel.setVolumeBoost(true)
        runCurrent()

        verify(playbackManager).updatePlayerEffects(effectsWith(1.5, TrimMode.LOW, true))
    }

    @Test
    fun `rapid effect changes build on the pending value`() = runTest {
        playbackStates.value = PlaybackState(episodeUuid = audioEpisode.uuid)
        whenever(playbackManager.getCurrentEpisode()).thenReturn(audioEpisode)

        viewModel.setPlaybackSpeed(1.5)
        viewModel.setVolumeBoost(true)
        runCurrent()

        verify(playbackManager).updatePlayerEffects(effectsWith(1.5, TrimMode.OFF, true))
    }

    @Test
    fun `pending effects are dropped once the playback state reflects them`() = runTest {
        playbackStates.value = PlaybackState(episodeUuid = audioEpisode.uuid)
        whenever(playbackManager.getCurrentEpisode()).thenReturn(audioEpisode)

        viewModel.setPlaybackSpeed(1.5)
        runCurrent()
        playbackStates.value = PlaybackState(episodeUuid = audioEpisode.uuid, playbackSpeed = 1.5)
        playbackStates.value = PlaybackState(episodeUuid = audioEpisode.uuid, playbackSpeed = 2.0)

        viewModel.setVolumeBoost(true)
        runCurrent()

        verify(playbackManager).updatePlayerEffects(effectsWith(2.0, TrimMode.OFF, true))
    }

    @Test
    fun `an effect change is ignored while the playback state lags behind an episode switch`() = runTest {
        playbackStates.value = PlaybackState(episodeUuid = "previous")
        whenever(playbackManager.getCurrentEpisode()).thenReturn(audioEpisode)

        viewModel.setPlaybackSpeed(2.0)
        runCurrent()

        verify(playbackManager, never()).updatePlayerEffects(any())
        verifyNoInteractions(podcastManager)
        verify(globalPlaybackEffectsSetting, never()).set(any(), any(), any(), any())
    }

    private fun effectsWith(speed: Double, trimMode: TrimMode, isVolumeBoosted: Boolean) = argThat<PlaybackEffects> {
        playbackSpeed == speed && this.trimMode == trimMode && this.isVolumeBoosted == isVolumeBoosted
    }
}

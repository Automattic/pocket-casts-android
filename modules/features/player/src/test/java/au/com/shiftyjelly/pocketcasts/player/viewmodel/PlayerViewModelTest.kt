package au.com.shiftyjelly.pocketcasts.player.viewmodel

import android.content.Context
import android.content.res.Resources
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.asFlow
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.EpisodeAnalytics
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffects
import au.com.shiftyjelly.pocketcasts.player.viewmodel.PlayerViewModel.PlaybackEffectsSettingsTab
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration
import au.com.shiftyjelly.pocketcasts.preferences.model.ShelfItem
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.download.DownloadManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.playback.SleepTimer
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue.State
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.jakewharton.rxrelay2.BehaviorRelay
import io.reactivex.Flowable
import io.reactivex.Observable
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.Mockito.mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class PlayerViewModelTest {

    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @Mock
    private lateinit var episodeManager: EpisodeManager

    @Mock
    private lateinit var settings: Settings

    @Mock
    private lateinit var playbackManager: PlaybackManager

    @Mock
    private lateinit var bookmarkManager: BookmarkManager

    @Mock
    private lateinit var downloadManager: DownloadManager

    @Mock
    private lateinit var podcastManager: PodcastManager

    @Mock
    private lateinit var userEpisodeManager: UserEpisodeManager

    @Mock
    private lateinit var theme: Theme

    @Mock
    private lateinit var sleepTimer: SleepTimer

    @Mock
    private lateinit var analyticsTracker: AnalyticsTracker

    @Mock
    private lateinit var episodeAnalytics: EpisodeAnalytics

    @Mock
    private lateinit var context: Context

    @Mock
    private lateinit var applicationScope: CoroutineScope

    @Mock
    private lateinit var upNextQueue: UpNextQueue

    @Mock
    private lateinit var globalEffects: PlaybackEffects

    @Mock
    private lateinit var podcastPlaybackEffects: PlaybackEffects

    @Mock
    private lateinit var podcastEpisode: PodcastEpisode

    @Mock
    private lateinit var userEpisode: UserEpisode

    @Mock
    private lateinit var podcast: Podcast

    @Mock
    private lateinit var userSettingsGlobalEffects: UserSetting<PlaybackEffects>

    private val podcastUuid = "podcastUuid"
    private lateinit var viewModel: PlayerViewModel

    @Test
    fun `does not override global effects when all podcasts effects settings segmented tab selected`() {
        initViewModel()

        viewModel.onEffectsSettingsSegmentedTabSelected(podcast, PlaybackEffectsSettingsTab.AllPodcasts)

        verify(podcastManager).updateOverrideGlobalEffects(podcast, false)
    }

    @Test
    fun `override global effects when this podcasts effects settings segmented tab selected`() {
        initViewModel()

        viewModel.onEffectsSettingsSegmentedTabSelected(podcast, PlaybackEffectsSettingsTab.ThisPodcast)

        verify(podcastManager).updateOverrideGlobalEffects(podcast, true)
    }

    @Test
    fun `effects settings segmented tab bar shown for podcast episode`() = runTest {
        initViewModel()

        playbackManager.playbackStateRelay.accept(PlaybackState(podcast = podcast))

        viewModel.effectsLive.asFlow().test {
            assertTrue(awaitItem().showCustomEffectsSettings)
        }
    }

    @Test
    fun `effects settings segmented tab bar hidden for user episode`() = runTest {
        initViewModel(currentEpisode = userEpisode)

        playbackManager.playbackStateRelay.accept(PlaybackState(podcast = Podcast.userPodcast))

        viewModel.effectsLive.asFlow().test {
            assertFalse(awaitItem().showCustomEffectsSettings)
        }
    }

    private fun initViewModel(
        currentEpisode: BaseEpisode = podcastEpisode,
    ) {
        whenever(playbackManager.playbackStateRelay).thenReturn(BehaviorRelay.create<PlaybackState>().toSerialized())
        whenever(playbackManager.upNextQueue).thenReturn(upNextQueue)
        whenever(upNextQueue.getChangesObservableWithLiveCurrentEpisode(episodeManager, podcastManager)).thenReturn(Observable.just(State.Empty))
        val userSettingsIntMock = mock<UserSetting<Int>>()
        whenever(userSettingsIntMock.flow).thenReturn(MutableStateFlow(0))
        val userSettingsArtworkConfigurationMock = mock<UserSetting<ArtworkConfiguration>>()
        whenever(userSettingsArtworkConfigurationMock.flow).thenReturn(MutableStateFlow(ArtworkConfiguration(false, emptySet())))
        whenever(settings.skipBackInSecs).thenReturn(userSettingsIntMock)
        whenever(settings.skipForwardInSecs).thenReturn(userSettingsIntMock)
        whenever(settings.artworkConfiguration).thenReturn(userSettingsArtworkConfigurationMock)
        val userSettingsShelfItemsMock = mock<UserSetting<List<ShelfItem>>>()
        whenever(userSettingsShelfItemsMock.flow).thenReturn(MutableStateFlow(emptyList()))
        whenever(settings.shelfItems).thenReturn(userSettingsShelfItemsMock)
        val resourcesMock = mock<Resources>()
        whenever(resourcesMock.getString(anyOrNull(), anyOrNull())).thenReturn("")
        whenever(context.resources).thenReturn(resourcesMock)
        whenever(podcast.uuid).thenReturn(podcastUuid)
        whenever(userSettingsGlobalEffects.value).thenReturn(globalEffects)
        whenever(settings.globalPlaybackEffects).thenReturn(userSettingsGlobalEffects)
        whenever(podcastEpisode.podcastOrSubstituteUuid).thenReturn(podcastUuid)
        whenever(playbackManager.getCurrentEpisode()).thenReturn(currentEpisode)
        whenever(episodeManager.observeEpisodeByUuidRx(anyOrNull())).thenReturn(Flowable.just(currentEpisode))
        whenever(podcast.playbackEffects).thenReturn(podcastPlaybackEffects)
        whenever(podcastManager.observePodcastByUuid(anyOrNull())).thenReturn(Flowable.just(podcast))
        val useRealTimeForPlaybackRemainingTimeMock = mock<UserSetting<Boolean>>()
        whenever(useRealTimeForPlaybackRemainingTimeMock.flow).thenReturn(MutableStateFlow(false))
        whenever(settings.useRealTimeForPlaybackRemaingTime).thenReturn(useRealTimeForPlaybackRemainingTimeMock)

        viewModel = PlayerViewModel(
            playbackManager = playbackManager,
            episodeManager = episodeManager,
            userEpisodeManager = userEpisodeManager,
            podcastManager = podcastManager,
            bookmarkManager = bookmarkManager,
            downloadManager = downloadManager,
            sleepTimer = sleepTimer,
            settings = settings,
            theme = theme,
            analyticsTracker = analyticsTracker,
            episodeAnalytics = episodeAnalytics,
            context = context,
            applicationScope = applicationScope,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }
}

package au.com.shiftyjelly.pocketcasts.podcasts.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.lifecycle.asFlow
import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.PlaybackEffects
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.UpNextQueue
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import io.reactivex.Flowable
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.eq
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class PodcastEffectsViewModelTest {
    @get:Rule
    val instantTaskExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @get:Rule
    val inMemoryFeatureFlagRule = InMemoryFeatureFlagRule()

    @Mock private lateinit var podcastManager: PodcastManager

    @Mock private lateinit var playbackManager: PlaybackManager

    @Mock private lateinit var settings: Settings

    @Mock private lateinit var episode: PodcastEpisode

    @Mock private lateinit var podcast: Podcast

    @Mock private lateinit var podcastPlaybackEffects: PlaybackEffects

    @Mock private lateinit var userSettingsGlobalPlaybackEffects: UserSetting<PlaybackEffects>

    @Mock private lateinit var globalPlaybackEffects: PlaybackEffects

    @Mock private lateinit var upNextQueue: UpNextQueue

    private lateinit var viewModel: PodcastEffectsViewModel
    private val podcastUuid = "podcastUuid"

    @Before
    fun setUp() {
        FeatureFlag.setEnabled(Feature.CUSTOM_PLAYBACK_SETTINGS, true)
    }

    @Test
    fun `given ff on and custom effects settings not used before, when custom settings selected, then global effects applied to podcast and playback`() = runTest {
        FeatureFlag.setEnabled(Feature.CUSTOM_PLAYBACK_SETTINGS, true)
        initViewModel(usedCustomEffectsBefore = false)
        viewModel.loadPodcast(podcastUuid)

        viewModel.podcast.asFlow().test {
            viewModel.updateOverrideGlobalEffects(override = true)
            awaitItem()
            verify(podcastManager).updateEffects(podcast, globalPlaybackEffects)
            verify(playbackManager).updatePlayerEffects(globalPlaybackEffects)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given ff on and custom effects settings used before, when custom settings selected, then podcast effects applied to podcast and playback`() = runTest {
        FeatureFlag.setEnabled(Feature.CUSTOM_PLAYBACK_SETTINGS, true)
        initViewModel(usedCustomEffectsBefore = true)
        viewModel.loadPodcast(podcastUuid)

        viewModel.podcast.asFlow().test {
            viewModel.updateOverrideGlobalEffects(override = true)
            awaitItem()
            verify(podcastManager).updateEffects(podcast, podcastPlaybackEffects)
            verify(playbackManager).updatePlayerEffects(podcastPlaybackEffects)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given ff off and custom effects settings not used before, when custom settings selected, then podcast effects applied to only playback`() = runTest {
        FeatureFlag.setEnabled(Feature.CUSTOM_PLAYBACK_SETTINGS, false)
        initViewModel(usedCustomEffectsBefore = false)
        viewModel.loadPodcast(podcastUuid)

        viewModel.podcast.asFlow().test {
            viewModel.updateOverrideGlobalEffects(override = true)
            awaitItem()
            verify(podcastManager, times(0)).updateEffects(eq(podcast), anyOrNull())
            verify(playbackManager).updatePlayerEffects(podcastPlaybackEffects)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given ff off and custom effects settings used before, when custom settings selected, then podcast effects applied to only playback`() = runTest {
        FeatureFlag.setEnabled(Feature.CUSTOM_PLAYBACK_SETTINGS, false)
        initViewModel(usedCustomEffectsBefore = true)
        viewModel.loadPodcast(podcastUuid)

        viewModel.podcast.asFlow().test {
            viewModel.updateOverrideGlobalEffects(override = true)
            awaitItem()
            verify(podcastManager, times(0)).updateEffects(eq(podcast), anyOrNull())
            verify(playbackManager).updatePlayerEffects(podcastPlaybackEffects)
            cancelAndIgnoreRemainingEvents()
        }
    }

    private fun initViewModel(
        usedCustomEffectsBefore: Boolean = false,
    ) {
        podcast.uuid = podcastUuid
        episode.podcastUuid = podcastUuid
        whenever(podcast.usedCustomEffectsBefore).thenReturn(usedCustomEffectsBefore)
        whenever(podcast.playbackEffects).thenReturn(podcastPlaybackEffects)
        whenever(podcastManager.observePodcastByUuid(podcastUuid)).thenReturn(Flowable.just(podcast))
        whenever(settings.globalPlaybackEffects).thenReturn(userSettingsGlobalPlaybackEffects)
        whenever(userSettingsGlobalPlaybackEffects.value).thenReturn(globalPlaybackEffects)
        whenever(playbackManager.upNextQueue).thenReturn(upNextQueue)
        whenever(upNextQueue.currentEpisode).thenReturn(episode)

        viewModel = PodcastEffectsViewModel(
            podcastManager = podcastManager,
            playbackManager = playbackManager,
            settings = settings,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }
}

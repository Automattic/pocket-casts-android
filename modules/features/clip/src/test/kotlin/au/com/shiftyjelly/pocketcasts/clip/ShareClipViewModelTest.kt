package au.com.shiftyjelly.pocketcasts.clip

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.clip.FakeClipPlayer.PlaybackState
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import java.util.Date
import junit.framework.TestCase.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class ShareClipViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val clipPlayer = FakeClipPlayer()
    private val tracker = FakeTracker()
    private val episodeManager = mock<EpisodeManager>()
    private val podcastManager = mock<PodcastManager>()
    private val settings = mock<Settings>()

    private val episode = PodcastEpisode(uuid = "episode-id", podcastUuid = "podcast-id", publishedDate = Date())
    private val podcast = Podcast(uuid = "podcast-id", title = "Podcast Title")
    private val clipRange = Clip.Range(15.seconds, 30.seconds)

    private lateinit var viewModel: ShareClipViewModel

    @Before
    fun setUp() {
        whenever(episodeManager.observeByUuid("episode-id")).thenReturn(flowOf(episode))
        whenever(podcastManager.observePodcastByEpisodeUuid("episode-id")).thenReturn(flowOf(podcast))
        whenever(podcastManager.observeEpisodeCountByEpisodeUuid("episode-id")).thenReturn(flowOf(10))
        val artworkSetting = mock<UserSetting<ArtworkConfiguration>>()
        whenever(artworkSetting.flow).thenReturn(MutableStateFlow(ArtworkConfiguration(useEpisodeArtwork = true)))
        whenever(settings.artworkConfiguration).thenReturn(artworkSetting)

        viewModel = ShareClipViewModel(
            episode.uuid,
            clipRange,
            clipPlayer,
            ClipAnalytics(
                episodeId = episode.uuid,
                podcastId = podcast.uuid,
                clipId = "clip-id",
                source = SourceView.PLAYER,
                initialClipRange = clipRange,
                analyticsTracker = AnalyticsTracker.test(tracker, isEnabled = true),
            ),
            episodeManager,
            podcastManager,
            settings,
        )
    }

    @Test
    fun `play clip`() = runTest {
        viewModel.uiState.test {
            viewModel.playClip()

            assertEquals(Clip(episode, Clip.Range(15.seconds, 30.seconds)), clipPlayer.clips.awaitItem())

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `update play state`() = runTest {
        viewModel.uiState.test {
            assertFalse(awaitItem().isPlaying)

            viewModel.playClip()
            assertTrue(awaitItem().isPlaying)
            assertEquals(PlaybackState.Playing, clipPlayer.playbackStates.awaitItem())

            viewModel.pauseClip()
            assertFalse(awaitItem().isPlaying)
            assertEquals(PlaybackState.Paused, clipPlayer.playbackStates.awaitItem())
        }
    }

    @Test
    fun `update clip start`() = runTest {
        viewModel.uiState.test {
            assertEquals(Clip.Range(15.seconds, 30.seconds), awaitItem().clip?.range)

            viewModel.updateClipStart(5.seconds)
            assertEquals(Clip.Range(5.seconds, 30.seconds), awaitItem().clip?.range)
        }
    }

    @Test
    fun `update clip end`() = runTest {
        viewModel.uiState.test {
            assertEquals(Clip.Range(15.seconds, 30.seconds), awaitItem().clip?.range)

            viewModel.updateClipEnd(20.seconds)
            assertEquals(Clip.Range(15.seconds, 20.seconds), awaitItem().clip?.range)
        }
    }

    @Test
    fun `updating clip start stops playback`() = runTest {
        viewModel.uiState.test {
            viewModel.playClip()
            skipItems(2)
            assertEquals(PlaybackState.Playing, clipPlayer.playbackStates.awaitItem())

            viewModel.updateClipStart(10.seconds)
            skipItems(2)
            assertEquals(PlaybackState.Stopped, clipPlayer.playbackStates.awaitItem())
        }
    }

    @Test
    fun `update clip end stops playback`() = runTest {
        viewModel.uiState.test {
            viewModel.playClip()
            skipItems(2)
            assertEquals(PlaybackState.Playing, clipPlayer.playbackStates.awaitItem())

            viewModel.updateClipEnd(80.seconds)
            skipItems(2)
            assertEquals(PlaybackState.Stopped, clipPlayer.playbackStates.awaitItem())
        }
    }

    @Test
    fun `update polling period`() = runTest {
        viewModel.uiState.test {
            skipItems(1)

            viewModel.onTimelineResolutionUpdate(scale = 1f, tickResolution = 1)
            assertEquals(200.milliseconds, clipPlayer.pollingPeriods.awaitItem())

            viewModel.onTimelineResolutionUpdate(scale = 5f, tickResolution = 1)
            assertEquals(40.milliseconds, clipPlayer.pollingPeriods.awaitItem())

            viewModel.onTimelineResolutionUpdate(scale = 1f, tickResolution = 5)
            assertEquals(1.seconds, clipPlayer.pollingPeriods.awaitItem())
        }
    }

    @Test
    fun `playing clip tracks analytics event`() = runTest {
        viewModel.uiState.test {
            viewModel.playClip()

            val event = tracker.events.last()

            assertEquals(
                TrackEvent(
                    AnalyticsEvent.CLIP_SCREEN_PLAY_TAPPED,
                    mapOf(
                        "episode_uuid" to "episode-id",
                        "podcast_uuid" to "podcast-id",
                        "clip_uuid" to "clip-id",
                        "source" to "player",
                    ),
                ),
                event,
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `pausing clip tracks analytics event`() = runTest {
        viewModel.uiState.test {
            viewModel.playClip()
            viewModel.pauseClip()

            val event = tracker.events.last()

            assertEquals(
                TrackEvent(
                    AnalyticsEvent.CLIP_SCREEN_PAUSE_TAPPED,
                    mapOf(
                        "episode_uuid" to "episode-id",
                        "podcast_uuid" to "podcast-id",
                        "clip_uuid" to "clip-id",
                        "source" to "player",
                    ),
                ),
                event,
            )

            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `track screen show event`() = runTest {
        viewModel.onClipScreenShown()

        val event = tracker.events.last()

        assertEquals(
            TrackEvent(
                AnalyticsEvent.CLIP_SCREEN_SHOWN,
                mapOf(
                    "episode_uuid" to "episode-id",
                    "podcast_uuid" to "podcast-id",
                    "clip_uuid" to "clip-id",
                    "source" to "player",
                ),
            ),
            event,
        )
    }

    @Test
    fun `sharing clip link tracks no changes to the clip`() = runTest {
        viewModel.onClipLinkShared(Clip(episode, clipRange))

        val event = tracker.events.last()

        assertEquals(
            TrackEvent(
                AnalyticsEvent.CLIP_SCREEN_LINK_SHARED,
                mapOf(
                    "episode_uuid" to "episode-id",
                    "podcast_uuid" to "podcast-id",
                    "clip_uuid" to "clip-id",
                    "source" to "player",
                    "start" to 15,
                    "end" to 30,
                    "start_modified" to false,
                    "end_modified" to false,
                ),
            ),
            event,
        )
    }

    @Test
    fun `sharing clip link tracks changes to the clip start`() = runTest {
        viewModel.onClipLinkShared(Clip(episode, clipRange.copy(start = 7.seconds)))

        val event = tracker.events.last()

        assertEquals(
            TrackEvent(
                AnalyticsEvent.CLIP_SCREEN_LINK_SHARED,
                mapOf(
                    "episode_uuid" to "episode-id",
                    "podcast_uuid" to "podcast-id",
                    "clip_uuid" to "clip-id",
                    "source" to "player",
                    "start" to 7,
                    "end" to 30,
                    "start_modified" to true,
                    "end_modified" to false,
                ),
            ),
            event,
        )
    }

    @Test
    fun `sharing clip link tracks changes to the clip end`() = runTest {
        viewModel.onClipLinkShared(Clip(episode, clipRange.copy(end = 20.seconds)))

        val event = tracker.events.last()

        assertEquals(
            TrackEvent(
                AnalyticsEvent.CLIP_SCREEN_LINK_SHARED,
                mapOf(
                    "episode_uuid" to "episode-id",
                    "podcast_uuid" to "podcast-id",
                    "clip_uuid" to "clip-id",
                    "source" to "player",
                    "start" to 15,
                    "end" to 20,
                    "start_modified" to false,
                    "end_modified" to true,
                ),
            ),
            event,
        )
    }

    @Test
    fun `sharing clip link tracks changes to the whole clip`() = runTest {
        viewModel.onClipLinkShared(Clip(episode, Clip.Range(17.seconds, 34.seconds)))

        val event = tracker.events.last()

        assertEquals(
            TrackEvent(
                AnalyticsEvent.CLIP_SCREEN_LINK_SHARED,
                mapOf(
                    "episode_uuid" to "episode-id",
                    "podcast_uuid" to "podcast-id",
                    "clip_uuid" to "clip-id",
                    "source" to "player",
                    "start" to 17,
                    "end" to 34,
                    "start_modified" to true,
                    "end_modified" to true,
                ),
            ),
            event,
        )
    }
}

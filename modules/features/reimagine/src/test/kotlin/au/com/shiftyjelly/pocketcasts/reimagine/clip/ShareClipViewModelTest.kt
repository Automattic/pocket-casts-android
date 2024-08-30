package au.com.shiftyjelly.pocketcasts.reimagine.clip

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsEvent
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsTracker
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration
import au.com.shiftyjelly.pocketcasts.reimagine.FakeTracker
import au.com.shiftyjelly.pocketcasts.reimagine.TrackEvent
import au.com.shiftyjelly.pocketcasts.reimagine.clip.FakeClipPlayer.PlaybackState
import au.com.shiftyjelly.pocketcasts.reimagine.clip.SharingState.Step
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.sharing.CardType
import au.com.shiftyjelly.pocketcasts.sharing.Clip
import au.com.shiftyjelly.pocketcasts.sharing.SharingRequest
import au.com.shiftyjelly.pocketcasts.sharing.SocialPlatform
import java.io.IOException
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class ShareClipViewModelTest {
    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @get:Rule
    val tempDir = TemporaryFolder()

    private val clipPlayer = FakeClipPlayer()
    private val sharingClient = FakeClipSharingClient()
    private val tracker = FakeTracker()
    private val episodeManager = mock<EpisodeManager>()
    private val podcastManager = mock<PodcastManager>()
    private val settings = mock<Settings>()

    private val episode = PodcastEpisode(uuid = "episode-id", podcastUuid = "podcast-id", publishedDate = Date(), duration = 60.0)
    private val podcast = Podcast(uuid = "podcast-id", title = "Podcast Title")
    private val clipRange = Clip.Range(15.seconds, 30.seconds)

    private lateinit var viewModel: ShareClipViewModel

    @Before
    fun setUp() {
        whenever(episodeManager.observeByUuid("episode-id")).thenReturn(flowOf(episode))
        whenever(podcastManager.observePodcastByEpisodeUuid("episode-id")).thenReturn(flowOf(podcast))
        val artworkSetting = mock<UserSetting<ArtworkConfiguration>>()
        whenever(artworkSetting.flow).thenReturn(MutableStateFlow(ArtworkConfiguration(useEpisodeArtwork = true)))
        whenever(settings.artworkConfiguration).thenReturn(artworkSetting)

        viewModel = ShareClipViewModel(
            episode.uuid,
            clipRange,
            clipPlayer,
            sharingClient,
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

            assertEquals(Clip.fromEpisode(episode, Clip.Range(15.seconds, 30.seconds)), clipPlayer.clips.awaitItem())

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

            viewModel.updateProgressPollingPeriod(scale = 1f, tickResolution = 1)
            assertEquals(200.milliseconds, clipPlayer.pollingPeriods.awaitItem())

            viewModel.updateProgressPollingPeriod(scale = 5f, tickResolution = 1)
            assertEquals(40.milliseconds, clipPlayer.pollingPeriods.awaitItem())

            viewModel.updateProgressPollingPeriod(scale = 1f, tickResolution = 5)
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
                    AnalyticsEvent.SHARE_SCREEN_PLAY_TAPPED,
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
                    AnalyticsEvent.SHARE_SCREEN_PAUSE_TAPPED,
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
        viewModel.onScreenShown()

        val event = tracker.events.last()

        assertEquals(
            TrackEvent(
                AnalyticsEvent.SHARE_SCREEN_SHOWN,
                mapOf(
                    "type" to "clip",
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
    fun `track sharing clip link`() = runTest {
        viewModel.shareClip(
            podcast,
            episode,
            clipRange,
            SocialPlatform.PocketCasts,
            CardType.Vertical,
            SourceView.PLAYER,
            createBackgroundAsset = { error("Unexpected operation") },
        )

        val event = tracker.events.last()

        assertEquals(
            TrackEvent(
                AnalyticsEvent.SHARE_SCREEN_CLIP_SHARED,
                mapOf(
                    "episode_uuid" to "episode-id",
                    "podcast_uuid" to "podcast-id",
                    "clip_uuid" to "clip-id",
                    "source" to "player",
                    "start" to 15,
                    "end" to 30,
                    "start_modified" to false,
                    "end_modified" to false,
                    "type" to "link",
                    "card_type" to "vertical",
                ),
            ),
            event,
        )
    }

    @Test
    fun `track sharing audio clip`() = runTest {
        viewModel.shareClip(
            podcast,
            episode,
            clipRange,
            SocialPlatform.PocketCasts,
            CardType.Audio,
            SourceView.PLAYER,
            createBackgroundAsset = { error("Unexpected operation") },
        )

        val event = tracker.events.last()

        assertEquals(
            TrackEvent(
                AnalyticsEvent.SHARE_SCREEN_CLIP_SHARED,
                mapOf(
                    "episode_uuid" to "episode-id",
                    "podcast_uuid" to "podcast-id",
                    "clip_uuid" to "clip-id",
                    "source" to "player",
                    "start" to 15,
                    "end" to 30,
                    "start_modified" to false,
                    "end_modified" to false,
                    "type" to "audio",
                    "card_type" to "audio",
                ),
            ),
            event,
        )
    }

    @Test
    fun `track sharing video clip`() = runTest {
        viewModel.shareClip(
            podcast,
            episode,
            clipRange,
            SocialPlatform.Instagram,
            CardType.Square,
            SourceView.PLAYER,
            createBackgroundAsset = { Result.success(tempDir.newFile()) },
        )

        val event = tracker.events.last()

        assertEquals(
            TrackEvent(
                AnalyticsEvent.SHARE_SCREEN_CLIP_SHARED,
                mapOf(
                    "episode_uuid" to "episode-id",
                    "podcast_uuid" to "podcast-id",
                    "clip_uuid" to "clip-id",
                    "source" to "player",
                    "start" to 15,
                    "end" to 30,
                    "start_modified" to false,
                    "end_modified" to false,
                    "type" to "video",
                    "card_type" to "square",
                ),
            ),
            event,
        )
    }

    @Test
    fun `track sharink clip with different start timestamp`() = runTest {
        viewModel.shareClip(
            podcast,
            episode,
            clipRange.copy(start = 7.seconds),
            SocialPlatform.PocketCasts,
            CardType.Vertical,
            SourceView.PLAYER,
            createBackgroundAsset = { error("Unexpected operation") },
        )

        val event = tracker.events.last()

        assertEquals(
            TrackEvent(
                AnalyticsEvent.SHARE_SCREEN_CLIP_SHARED,
                mapOf(
                    "episode_uuid" to "episode-id",
                    "podcast_uuid" to "podcast-id",
                    "clip_uuid" to "clip-id",
                    "source" to "player",
                    "start" to 7,
                    "end" to 30,
                    "start_modified" to true,
                    "end_modified" to false,
                    "type" to "link",
                    "card_type" to "vertical",
                ),
            ),
            event,
        )
    }

    @Test
    fun `track sharing clip with different end timestamp`() = runTest {
        viewModel.shareClip(
            podcast,
            episode,
            clipRange.copy(end = 20.seconds),
            SocialPlatform.PocketCasts,
            CardType.Vertical,
            SourceView.PLAYER,
            createBackgroundAsset = { error("Unexpected operation") },
        )

        val event = tracker.events.last()

        assertEquals(
            TrackEvent(
                AnalyticsEvent.SHARE_SCREEN_CLIP_SHARED,
                mapOf(
                    "episode_uuid" to "episode-id",
                    "podcast_uuid" to "podcast-id",
                    "clip_uuid" to "clip-id",
                    "source" to "player",
                    "start" to 15,
                    "end" to 20,
                    "start_modified" to false,
                    "end_modified" to true,
                    "type" to "link",
                    "card_type" to "vertical",
                ),
            ),
            event,
        )
    }

    @Test
    fun `track sharing clip with different timestamps`() = runTest {
        viewModel.shareClip(
            podcast,
            episode,
            Clip.Range(17.seconds, 34.seconds),
            SocialPlatform.PocketCasts,
            CardType.Vertical,
            SourceView.PLAYER,
            createBackgroundAsset = { error("Unexpected operation") },
        )

        val event = tracker.events.last()

        assertEquals(
            TrackEvent(
                AnalyticsEvent.SHARE_SCREEN_CLIP_SHARED,
                mapOf(
                    "episode_uuid" to "episode-id",
                    "podcast_uuid" to "podcast-id",
                    "clip_uuid" to "clip-id",
                    "source" to "player",
                    "start" to 17,
                    "end" to 34,
                    "start_modified" to true,
                    "end_modified" to true,
                    "type" to "link",
                    "card_type" to "vertical",
                ),
            ),
            event,
        )
    }

    @Test
    fun `too short clip is not shared`() = runTest {
        viewModel.shareClip(
            podcast,
            episode,
            Clip.Range(start = 10.seconds, end = 10.seconds),
            SocialPlatform.PocketCasts,
            CardType.Vertical,
            SourceView.PLAYER,
            createBackgroundAsset = { error("Unexpected operation") },
        )

        assertTrue(tracker.events.isEmpty())
    }

    @Test
    fun `clip with end timestamp after episode end is not shared`() = runTest {
        viewModel.shareClip(
            podcast,
            episode.copy(duration = 10.0),
            Clip.Range(start = 0.seconds, end = 11.seconds),
            SocialPlatform.PocketCasts,
            CardType.Vertical,
            SourceView.PLAYER,
            createBackgroundAsset = { error("Unexpected operation") },
        )

        assertTrue(tracker.events.isEmpty())
    }

    @Test
    fun `sharing clip changes sharing status`() = runTest {
        viewModel.uiState.test {
            assertEquals(false, awaitItem().sharingState.iSharing)

            viewModel.shareClip(
                podcast,
                episode,
                clipRange,
                SocialPlatform.PocketCasts,
                CardType.Vertical,
                SourceView.PLAYER,
                createBackgroundAsset = { error("Unexpected operation") },
            )

            assertEquals(true, awaitItem().sharingState.iSharing)
            assertEquals(false, awaitItem().sharingState.iSharing)
        }
    }

    @Test
    fun `share clip link`() = runTest {
        viewModel.shareClip(
            podcast,
            episode,
            clipRange,
            SocialPlatform.PocketCasts,
            CardType.Vertical,
            SourceView.PLAYER,
            createBackgroundAsset = { error("Unexpected operation") },
        )
        val request = sharingClient.request!!

        assertTrue(request.data is SharingRequest.Data.ClipLink)
    }

    @Test
    fun `share audio clip`() = runTest {
        viewModel.shareClip(
            podcast,
            episode,
            clipRange,
            SocialPlatform.PocketCasts,
            CardType.Audio,
            SourceView.PLAYER,
            createBackgroundAsset = { error("Unexpected operation") },
        )
        val request = sharingClient.request!!

        assertTrue(request.data is SharingRequest.Data.ClipAudio)
    }

    @Test
    fun `share video clip`() = runTest {
        viewModel.shareClip(
            podcast,
            episode,
            clipRange,
            SocialPlatform.Instagram,
            CardType.Vertical,
            SourceView.PLAYER,
            createBackgroundAsset = { Result.success(tempDir.newFile()) },
        )
        val request = sharingClient.request!!

        assertTrue(request.data is SharingRequest.Data.ClipVideo)
    }

    @Test
    fun `fail to share video clip when there is no background asset`() = runTest {
        viewModel.shareClip(
            podcast,
            episode,
            clipRange,
            SocialPlatform.Instagram,
            CardType.Vertical,
            SourceView.PLAYER,
            createBackgroundAsset = { Result.failure(IOException("Whoops!")) },
        )

        assertNull(sharingClient.request)
    }

    @Test
    fun `show platform selection`() = runTest {
        viewModel.uiState.test {
            assertEquals(Step.ClipSelection, awaitItem().sharingState.step)

            viewModel.showPlatformSelection()

            assertEquals(Step.PlatformSelection, awaitItem().sharingState.step)
        }
    }

    @Test
    fun `do not show platform selection if is sharing`() = runTest {
        sharingClient.isSuspended = true

        viewModel.uiState.test {
            viewModel.shareClip(
                podcast,
                episode,
                clipRange,
                SocialPlatform.PocketCasts,
                CardType.Vertical,
                SourceView.PLAYER,
                createBackgroundAsset = { error("Unexpected operation") },
            )
            skipItems(2)

            viewModel.showPlatformSelection()
            expectNoEvents()

            sharingClient.isSuspended = false
            assertEquals(Step.ClipSelection, awaitItem().sharingState.step)
        }
    }

    @Test
    fun `show clip selection`() = runTest {
        viewModel.uiState.test {
            viewModel.showPlatformSelection()
            skipItems(2)

            viewModel.showClipSelection()

            assertEquals(Step.ClipSelection, awaitItem().sharingState.step)
        }
    }

    @Test
    fun `do not show clip selection if is sharing`() = runTest {
        sharingClient.isSuspended = true

        viewModel.uiState.test {
            viewModel.showPlatformSelection()
            viewModel.shareClip(
                podcast,
                episode,
                clipRange,
                SocialPlatform.PocketCasts,
                CardType.Vertical,
                SourceView.PLAYER,
                createBackgroundAsset = { error("Unexpected operation") },
            )
            skipItems(3)

            viewModel.showClipSelection()
            expectNoEvents()

            sharingClient.isSuspended = false
            assertEquals(Step.PlatformSelection, awaitItem().sharingState.step)
        }
    }
}

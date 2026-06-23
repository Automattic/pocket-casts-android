package au.com.shiftyjelly.pocketcasts.player.viewmodel

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.entity.AlternateEnclosureSource
import au.com.shiftyjelly.pocketcasts.models.entity.BaseEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeAlternateEnclosure
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.playback.SelectedStream
import au.com.shiftyjelly.pocketcasts.repositories.podcast.AlternateEnclosureManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
class StreamSelectorViewModelTest {
    private val testDispatcher = UnconfinedTestDispatcher()

    @get:Rule
    val coroutineRule = MainCoroutineRule(testDispatcher)

    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    private val playbackManager = mock<PlaybackManager>()
    private val episodeManager = mock<EpisodeManager>()
    private val alternateEnclosureManager = mock<AlternateEnclosureManager>()

    private val episode = PodcastEpisode(
        uuid = "id",
        publishedDate = Date(),
        downloadUrl = "https://example.com/episode.mp3",
        hlsUrl = "https://example.com/master.m3u8",
        fileType = "audio/mpeg",
    )
    private val enclosures = listOf(
        EpisodeAlternateEnclosure(
            episodeUuid = "id",
            position = 0,
            type = "application/x-mpegURL",
            sources = listOf(AlternateEnclosureSource(uri = "https://example.com/master.m3u8")),
        ),
        EpisodeAlternateEnclosure(
            episodeUuid = "id",
            position = 1,
            type = "video/mp4",
            height = 1080,
            sources = listOf(AlternateEnclosureSource(uri = "https://example.com/video-1080.mp4")),
        ),
    )

    private val playbackStateFlow = MutableStateFlow(PlaybackState(episodeUuid = "id"))
    private val episodeFlow = MutableStateFlow<BaseEpisode>(episode)
    private val enclosuresFlow = MutableStateFlow(enclosures)
    private val selectedStreamsFlow = MutableStateFlow<Map<String, SelectedStream>>(emptyMap())

    private lateinit var viewModel: StreamSelectorViewModel

    @Before
    fun setup() {
        FeatureFlag.setEnabled(Feature.HLS_STREAMING, true)
        whenever(playbackManager.playbackStateFlow).thenReturn(playbackStateFlow)
        whenever(playbackManager.selectedStreams).thenReturn(selectedStreamsFlow)
        whenever(episodeManager.findEpisodeByUuidFlow("id")).thenReturn(episodeFlow)
        whenever(alternateEnclosureManager.observeForEpisode("id")).thenReturn(enclosuresFlow)
        viewModel = StreamSelectorViewModel(playbackManager, episodeManager, alternateEnclosureManager)
    }

    @Test
    fun `builds primary plus alternate options de-duped`() = runTest {
        viewModel.uiState.test {
            val state = awaitItem()
            assertEquals("id", state.episodeUuid)
            assertEquals(
                listOf(
                    "https://example.com/episode.mp3",
                    "https://example.com/master.m3u8",
                    "https://example.com/video-1080.mp4",
                ),
                state.options.map { it.uri },
            )
            assertEquals(
                listOf(
                    StreamSelectorViewModel.StreamKind.Audio,
                    StreamSelectorViewModel.StreamKind.Hls,
                    StreamSelectorViewModel.StreamKind.Video,
                ),
                state.options.map { it.kind },
            )
            assertEquals(1080, state.options.last().height)
        }
    }

    @Test
    fun `marks the current stream url as selected`() = runTest {
        viewModel.uiState.test {
            val options = awaitItem().options
            // With HLS preferred and no override, the current stream is the HLS master playlist.
            assertEquals(
                "https://example.com/master.m3u8",
                options.single { it.isSelected }.uri,
            )
        }
    }

    @Test
    fun `selecting an option switches the playback stream`() = runTest {
        viewModel.uiState.test {
            val videoOption = awaitItem().options.single { it.kind == StreamSelectorViewModel.StreamKind.Video }
            viewModel.selectStream(videoOption)
            verify(playbackManager).selectStream("id", SelectedStream("https://example.com/video-1080.mp4", "video/mp4"))
            assertTrue(videoOption.height == 1080)
        }
    }
}

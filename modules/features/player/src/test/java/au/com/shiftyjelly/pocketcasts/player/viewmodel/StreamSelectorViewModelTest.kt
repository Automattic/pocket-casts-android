package au.com.shiftyjelly.pocketcasts.player.viewmodel

import app.cash.turbine.test
import au.com.shiftyjelly.pocketcasts.models.converter.SafeDate
import au.com.shiftyjelly.pocketcasts.models.entity.AlternateEnclosureSource
import au.com.shiftyjelly.pocketcasts.models.entity.EpisodeAlternateEnclosure
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.player.viewmodel.StreamSelectorViewModel.StreamKind
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackState
import au.com.shiftyjelly.pocketcasts.repositories.playback.SelectedStream
import au.com.shiftyjelly.pocketcasts.repositories.podcast.AlternateEnclosureManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@ExperimentalCoroutinesApi
@RunWith(MockitoJUnitRunner::class)
class StreamSelectorViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    @Mock
    private lateinit var playbackManager: PlaybackManager

    @Mock
    private lateinit var episodeManager: EpisodeManager

    @Mock
    private lateinit var alternateEnclosureManager: AlternateEnclosureManager

    private val episode = PodcastEpisode(
        uuid = "uuid",
        publishedDate = SafeDate(),
        downloadUrl = "https://example.com/episode.mp3",
        fileType = "audio/mpeg",
    )

    private fun initViewModel(
        enclosures: List<EpisodeAlternateEnclosure>,
        selected: Map<String, SelectedStream> = emptyMap(),
    ): StreamSelectorViewModel {
        whenever(playbackManager.playbackStateFlow).thenReturn(flowOf(PlaybackState(episodeUuid = "uuid")))
        whenever(episodeManager.findEpisodeByUuidFlow("uuid")).thenReturn(flowOf(episode))
        whenever(alternateEnclosureManager.observeForEpisode("uuid")).thenReturn(flowOf(enclosures))
        whenever(playbackManager.selectedStreams).thenReturn(MutableStateFlow(selected))
        return StreamSelectorViewModel(playbackManager, episodeManager, alternateEnclosureManager)
    }

    @Test
    fun `builds an option per source with the right kinds`() = runTest {
        val viewModel = initViewModel(
            enclosures = listOf(
                enclosure("application/x-mpegurl", "https://example.com/master.m3u8"),
                enclosure("video/mp4", "https://example.com/file-1080.mp4", height = 1080),
                enclosure("audio/mpeg", "https://example.com/audio.mp3"),
            ),
        )

        viewModel.uiState.test {
            val options = awaitNonEmpty().options
            assertEquals(4, options.size)
            // The progressive download is first, then the enclosures in feed order.
            assertEquals("https://example.com/episode.mp3", options[0].uri)
            assertEquals(StreamKind.Audio, options[0].kind)
            assertEquals(StreamKind.Hls, options[1].kind)
            assertEquals(StreamKind.Video, options[2].kind)
            assertEquals(1080, options[2].height)
            assertEquals(StreamKind.Audio, options[3].kind)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `defaults to the first hls stream when hls streaming is enabled`() = runTest {
        FeatureFlag.setEnabled(Feature.HLS_STREAMING, true)
        val viewModel = initViewModel(
            enclosures = listOf(
                enclosure("application/x-mpegurl", "https://example.com/master.m3u8"),
                enclosure("video/mp4", "https://example.com/file-1080.mp4", height = 1080),
            ),
        )

        viewModel.uiState.test {
            val options = awaitNonEmpty().options
            assertTrue(options.single { it.kind == StreamKind.Hls }.isSelected)
            assertTrue(options.none { it.uri == "https://example.com/episode.mp3" && it.isSelected })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `marks the currently playing stream as selected`() = runTest {
        val viewModel = initViewModel(
            enclosures = listOf(enclosure("application/x-mpegurl", "https://example.com/master.m3u8")),
            selected = mapOf("uuid" to SelectedStream("https://example.com/master.m3u8", "application/x-mpegurl")),
        )

        viewModel.uiState.test {
            val options = awaitNonEmpty().options
            assertTrue(options.single { it.uri == "https://example.com/master.m3u8" }.isSelected)
            assertTrue(options.none { it.uri == "https://example.com/episode.mp3" && it.isSelected })
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `selecting a stream delegates to the playback manager`() = runTest {
        val viewModel = initViewModel(
            enclosures = listOf(enclosure("application/x-mpegurl", "https://example.com/master.m3u8")),
        )

        viewModel.uiState.test {
            val option = awaitNonEmpty().options.single { it.kind == StreamKind.Hls }
            viewModel.selectStream(option)
            verify(playbackManager).selectStream(
                "uuid",
                SelectedStream("https://example.com/master.m3u8", "application/x-mpegurl"),
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    private suspend fun app.cash.turbine.ReceiveTurbine<StreamSelectorViewModel.UiState>.awaitNonEmpty(): StreamSelectorViewModel.UiState {
        var state = awaitItem()
        while (state.options.isEmpty()) {
            state = awaitItem()
        }
        return state
    }

    private fun enclosure(
        type: String,
        uri: String,
        height: Int? = null,
    ) = EpisodeAlternateEnclosure(
        episodeUuid = "uuid",
        position = 0,
        type = type,
        height = height,
        sources = listOf(AlternateEnclosureSource(uri = uri)),
    )
}

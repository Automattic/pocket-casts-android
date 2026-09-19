package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.entity.Podcast
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.to.Transcript
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptEntry
import au.com.shiftyjelly.pocketcasts.models.to.TranscriptType
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.preferences.model.ArtworkConfiguration
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.shownotes.ShowNotesManager
import au.com.shiftyjelly.pocketcasts.repositories.transcript.TranscriptManager
import au.com.shiftyjelly.pocketcasts.servers.podcast.PodcastCacheServiceManager
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import io.reactivex.Single
import java.util.Date
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.doReturn
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.times
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class BookmarkDetailViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    private val bookmarkManager = mock<BookmarkManager>()
    private val episodeManager = mock<EpisodeManager>()
    private val podcastCacheServiceManager = mock<PodcastCacheServiceManager>()
    private val transcriptManager = mock<TranscriptManager>()
    private val showNotesManager = mock<ShowNotesManager>()
    private val settings = mock<Settings>()
    private val viewModel = BookmarkDetailViewModel(bookmarkManager, episodeManager, podcastCacheServiceManager, transcriptManager, showNotesManager, settings)

    private val bookmarkUuid = "bookmark-id"
    private val episodeUuid = "episode-id"
    private val podcastUuid = "podcast-id"
    private val firstSentence = "that's the thing about selective admissions."
    private val secondSentence = "The difference between the kid who gets in and the kid who doesn't is often basically noise."
    private val transcript = Transcript.Text(
        entries = listOf(TranscriptEntry.Text(firstSentence), TranscriptEntry.Text(secondSentence)),
        type = TranscriptType.Vtt,
        url = "https://example.com/transcript.vtt",
        isGenerated = true,
        episodeUuid = episodeUuid,
        podcastUuid = podcastUuid,
    )

    @Before
    fun setUp() {
        FeatureFlag.setEnabled(Feature.SMART_BOOKMARKS, true)
        val artworkConfiguration = mock<UserSetting<ArtworkConfiguration>> {
            on { value } doReturn ArtworkConfiguration(useEpisodeArtwork = false)
        }
        whenever(settings.artworkConfiguration).thenReturn(artworkConfiguration)
    }

    @Test
    fun `loads and relocates the passage`() = runTest {
        whenever(transcriptManager.loadGeneratedTranscript(episodeUuid)).thenReturn(transcript)

        load(passage = firstSentence, passageLocation = 0)

        val state = viewModel.uiState.value.transcriptState
        assertTrue(state is BookmarkDetailViewModel.TranscriptState.Loaded)
        state as BookmarkDetailViewModel.TranscriptState.Loaded
        assertEquals(firstSentence, state.transcript.displaySubstring(state.passage!!))
    }

    @Test
    fun `is unavailable when the transcript is missing`() = runTest {
        whenever(transcriptManager.loadGeneratedTranscript(episodeUuid)).thenReturn(null)

        load(passage = firstSentence, passageLocation = 0)

        assertEquals(BookmarkDetailViewModel.TranscriptState.Unavailable, viewModel.uiState.value.transcriptState)
    }

    @Test
    fun `is none when there is no passage`() = runTest {
        load(passage = null, passageLocation = null)

        assertEquals(BookmarkDetailViewModel.TranscriptState.None, viewModel.uiState.value.transcriptState)
    }

    @Test
    fun `is none when the flag is off`() = runTest {
        FeatureFlag.setEnabled(Feature.SMART_BOOKMARKS, false)

        load(passage = firstSentence, passageLocation = 0)

        assertEquals(BookmarkDetailViewModel.TranscriptState.None, viewModel.uiState.value.transcriptState)
    }

    @Test
    fun `loads show notes before resolving the transcript`() = runTest {
        whenever(transcriptManager.loadGeneratedTranscript(episodeUuid)).thenReturn(transcript)

        load(passage = firstSentence, passageLocation = 0)

        verifyBlocking(showNotesManager) { loadShowNotes(podcastUuid, episodeUuid) }
    }

    @Test
    fun `is unavailable when the passage cannot be relocated`() = runTest {
        whenever(transcriptManager.loadGeneratedTranscript(episodeUuid)).thenReturn(transcript)

        load(passage = "not in the transcript", passageLocation = null)

        assertEquals(BookmarkDetailViewModel.TranscriptState.Unavailable, viewModel.uiState.value.transcriptState)
    }

    @Test
    fun `does not reload after the first load`() = runTest {
        whenever(transcriptManager.loadGeneratedTranscript(episodeUuid)).thenReturn(transcript)

        load(passage = firstSentence, passageLocation = 0)
        load(passage = firstSentence, passageLocation = 0)

        verifyBlocking(transcriptManager, times(1)) { loadGeneratedTranscript(episodeUuid) }
    }

    @Test
    fun `refresh re-reads the bookmark play times`() = runTest {
        whenever(transcriptManager.loadGeneratedTranscript(episodeUuid)).thenReturn(transcript)
        load(passage = firstSentence, passageLocation = 0, timeSecs = 10, referenceTime = 12)
        whenever(bookmarkManager.findBookmark(bookmarkUuid)).thenReturn(
            Bookmark(
                uuid = bookmarkUuid,
                podcastUuid = podcastUuid,
                episodeUuid = episodeUuid,
                title = "Renamed",
                passage = secondSentence,
                passageLocation = firstSentence.length + 1,
                timeSecs = 42,
                referenceTime = 44,
            ),
        )

        viewModel.refresh()

        val state = viewModel.uiState.value
        assertEquals(42, state.timeSecs)
        assertEquals(44, state.referenceTime)
    }

    @Test
    fun `refresh re-reads the bookmark and relocates the passage`() = runTest {
        whenever(transcriptManager.loadGeneratedTranscript(episodeUuid)).thenReturn(transcript)
        load(passage = firstSentence, passageLocation = 0)
        whenever(bookmarkManager.findBookmark(bookmarkUuid)).thenReturn(
            Bookmark(
                uuid = bookmarkUuid,
                podcastUuid = podcastUuid,
                episodeUuid = episodeUuid,
                title = "Renamed",
                passage = secondSentence,
                passageLocation = firstSentence.length + 1,
            ),
        )

        viewModel.refresh()

        val state = viewModel.uiState.value
        assertEquals("Renamed", state.title)
        val transcriptState = state.transcriptState
        assertTrue(transcriptState is BookmarkDetailViewModel.TranscriptState.Loaded)
        transcriptState as BookmarkDetailViewModel.TranscriptState.Loaded
        assertEquals(secondSentence, transcriptState.transcript.displaySubstring(transcriptState.passage!!))
    }

    @Test
    fun `loads the bookmark episode into the state`() = runTest {
        val episode = PodcastEpisode(uuid = episodeUuid, publishedDate = Date())
        whenever(episodeManager.findEpisodeByUuid(episodeUuid)).thenReturn(episode)

        load(passage = null, passageLocation = null)

        assertEquals(episode, viewModel.uiState.value.episode)
    }

    @Test
    fun `reads use episode artwork from the artwork configuration`() = runTest {
        val artworkConfiguration = mock<UserSetting<ArtworkConfiguration>> {
            on { value } doReturn ArtworkConfiguration(useEpisodeArtwork = true)
        }
        whenever(settings.artworkConfiguration).thenReturn(artworkConfiguration)

        load(passage = null, passageLocation = null)

        assertTrue(viewModel.uiState.value.useEpisodeArtwork)
    }

    @Test
    fun `fetches the podcast title when it is missing`() = runTest {
        whenever(podcastCacheServiceManager.getPodcast(podcastUuid))
            .thenReturn(Single.just(Podcast(uuid = podcastUuid, title = "Fetched")))

        load(passage = null, passageLocation = null, podcastTitle = "")

        val state = viewModel.uiState.value
        assertEquals("Fetched", state.podcastTitle)
        assertFalse(state.isPodcastTitleLoading)
    }

    @Test
    fun `does not fetch the podcast title for uploaded files`() = runTest {
        viewModel.load(
            bookmarkUuid = bookmarkUuid,
            title = "Title",
            episodeUuid = episodeUuid,
            podcastUuid = Podcast.userPodcast.uuid,
            podcastTitle = "",
            passage = null,
            passageLocation = null,
            timeSecs = 0,
            referenceTime = null,
        )

        val state = viewModel.uiState.value
        assertEquals("", state.podcastTitle)
        assertFalse(state.isPodcastTitleLoading)
        verify(podcastCacheServiceManager, never()).getPodcast(any())
    }

    @Test
    fun `computes the reference offset at the bookmark time`() = runTest {
        val timed = Transcript.Text(
            entries = listOf(
                TranscriptEntry.Text(firstSentence, startTimeMs = 0),
                TranscriptEntry.Text(secondSentence, startTimeMs = 10_000),
            ),
            type = TranscriptType.Vtt,
            url = "https://example.com/transcript.vtt",
            isGenerated = true,
            episodeUuid = episodeUuid,
            podcastUuid = podcastUuid,
        )
        whenever(transcriptManager.loadGeneratedTranscript(episodeUuid)).thenReturn(timed)

        load(passage = secondSentence, passageLocation = firstSentence.length + 1, referenceTime = 10)

        val state = viewModel.uiState.value.transcriptState
        assertTrue(state is BookmarkDetailViewModel.TranscriptState.Loaded)
        state as BookmarkDetailViewModel.TranscriptState.Loaded
        assertEquals(state.transcript.displayText.indexOf(secondSentence), state.referenceOffset)
    }

    @Test
    fun `clamps the reference offset to the passage when the reference time precedes it`() = runTest {
        val timed = Transcript.Text(
            entries = listOf(
                TranscriptEntry.Text(firstSentence, startTimeMs = 0),
                TranscriptEntry.Text(secondSentence, startTimeMs = 10_000),
            ),
            type = TranscriptType.Vtt,
            url = "https://example.com/transcript.vtt",
            isGenerated = true,
            episodeUuid = episodeUuid,
            podcastUuid = podcastUuid,
        )
        whenever(transcriptManager.loadGeneratedTranscript(episodeUuid)).thenReturn(timed)

        load(passage = secondSentence, passageLocation = firstSentence.length + 1, referenceTime = 0)

        val state = viewModel.uiState.value.transcriptState
        assertTrue(state is BookmarkDetailViewModel.TranscriptState.Loaded)
        state as BookmarkDetailViewModel.TranscriptState.Loaded
        assertEquals(state.passage!!.start, state.referenceOffset)
    }

    @Test
    fun `falls back to the passage start when there is no reference time`() = runTest {
        val timed = Transcript.Text(
            entries = listOf(
                TranscriptEntry.Text(firstSentence, startTimeMs = 0),
                TranscriptEntry.Text(secondSentence, startTimeMs = 10_000),
            ),
            type = TranscriptType.Vtt,
            url = "https://example.com/transcript.vtt",
            isGenerated = true,
            episodeUuid = episodeUuid,
            podcastUuid = podcastUuid,
        )
        whenever(transcriptManager.loadGeneratedTranscript(episodeUuid)).thenReturn(timed)

        load(passage = secondSentence, passageLocation = firstSentence.length + 1, referenceTime = null)

        val state = viewModel.uiState.value.transcriptState
        assertTrue(state is BookmarkDetailViewModel.TranscriptState.Loaded)
        state as BookmarkDetailViewModel.TranscriptState.Loaded
        assertEquals(state.passage!!.start, state.referenceOffset)
    }

    private fun load(
        passage: String?,
        passageLocation: Int?,
        podcastTitle: String = "Podcast",
        timeSecs: Int = 0,
        referenceTime: Int? = null,
    ) {
        viewModel.load(
            bookmarkUuid = bookmarkUuid,
            title = "Title",
            episodeUuid = episodeUuid,
            podcastUuid = podcastUuid,
            podcastTitle = podcastTitle,
            passage = passage,
            passageLocation = passageLocation,
            timeSecs = timeSecs,
            referenceTime = referenceTime,
        )
    }
}

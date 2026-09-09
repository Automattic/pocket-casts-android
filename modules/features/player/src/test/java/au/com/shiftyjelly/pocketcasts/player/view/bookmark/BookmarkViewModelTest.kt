package au.com.shiftyjelly.pocketcasts.player.view.bookmark

import androidx.compose.ui.text.input.TextFieldValue
import au.com.shiftyjelly.pocketcasts.analytics.testing.TestEventSink
import au.com.shiftyjelly.pocketcasts.compose.PodcastColors
import au.com.shiftyjelly.pocketcasts.models.entity.Bookmark
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkManager
import au.com.shiftyjelly.pocketcasts.repositories.bookmark.BookmarkSuggestion
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.UserEpisodeManager
import au.com.shiftyjelly.pocketcasts.sharedtest.InMemoryFeatureFlagRule
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import au.com.shiftyjelly.pocketcasts.utils.featureflag.Feature
import au.com.shiftyjelly.pocketcasts.utils.featureflag.FeatureFlag
import com.automattic.eventhorizon.EventHorizon
import java.util.Date
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.anyOrNull
import org.mockito.kotlin.doSuspendableAnswer
import org.mockito.kotlin.eq
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@OptIn(ExperimentalCoroutinesApi::class)
class BookmarkViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    @get:Rule
    val featureFlagRule = InMemoryFeatureFlagRule()

    private val episodeManager = mock<EpisodeManager>()
    private val userEpisodeManager = mock<UserEpisodeManager>()
    private val bookmarkManager = mock<BookmarkManager>()

    private val viewModel = BookmarkViewModel(episodeManager, userEpisodeManager, bookmarkManager, EventHorizon(TestEventSink()))

    private val episodeUuid = "episode-id"
    private val timeSecs = 120
    private val arguments = BookmarkArguments(
        bookmarkUuid = null,
        episodeUuid = episodeUuid,
        timeSecs = timeSecs,
        podcastColors = PodcastColors.ForUserEpisode,
    )
    private val suggestion = BookmarkSuggestion(passage = "the passage", passageLocation = 5, referenceTimeSecs = 118, title = "A great moment")

    @Before
    fun setUp() {
        FeatureFlag.setEnabled(Feature.SMART_BOOKMARKS, true)
    }

    @Test
    fun `auto-applies the suggested title when the title is untouched`() = runTest {
        stubNewBookmark()
        whenever(bookmarkManager.suggestBookmark(episodeUuid, timeSecs)).thenReturn(suggestion)

        viewModel.load(arguments)

        val state = viewModel.uiState.value
        assertEquals("A great moment", state.title.text)
        assertEquals(BookmarkViewModel.TitleSuggestion.None, state.titleSuggestion)
    }

    @Test
    fun `offers the suggestion and cancels generating when the title is edited`() = runTest {
        stubNewBookmark()
        val gate = CompletableDeferred<BookmarkSuggestion?>()
        doSuspendableAnswer { gate.await() }.whenever(bookmarkManager).suggestBookmark(episodeUuid, timeSecs)

        viewModel.load(arguments)
        assertEquals(BookmarkViewModel.TitleSuggestion.Generating, viewModel.uiState.value.titleSuggestion)

        viewModel.changeTitle(TextFieldValue("My own title"))
        assertEquals(BookmarkViewModel.TitleSuggestion.None, viewModel.uiState.value.titleSuggestion)

        gate.complete(suggestion)

        val state = viewModel.uiState.value
        assertEquals(BookmarkViewModel.TitleSuggestion.Available("A great moment"), state.titleSuggestion)
        assertEquals("My own title", state.title.text)
    }

    @Test
    fun `saves the captured passage with the bookmark`() = runTest {
        stubNewBookmark()
        whenever(bookmarkManager.suggestBookmark(episodeUuid, timeSecs)).thenReturn(suggestion)
        whenever(episodeManager.findByUuid(episodeUuid)).thenReturn(PodcastEpisode(uuid = episodeUuid, publishedDate = Date()))
        whenever(bookmarkManager.add(any(), any(), any(), any(), any(), anyOrNull(), anyOrNull(), anyOrNull())).thenReturn(Bookmark(uuid = "new-id"))

        viewModel.load(arguments)
        val saved = CompletableDeferred<Unit>()
        viewModel.saveBookmark { _, _ -> saved.complete(Unit) }
        saved.await()

        verify(bookmarkManager).add(
            episode = any(),
            timeSecs = eq(timeSecs),
            title = eq("A great moment"),
            creationSource = any(),
            addedAt = any(),
            passage = eq("the passage"),
            passageLocation = eq(5),
            referenceTime = eq(118),
        )
    }

    private suspend fun stubNewBookmark() {
        whenever(episodeManager.findEpisodeByUuid(episodeUuid)).thenReturn(PodcastEpisode(uuid = episodeUuid, publishedDate = Date()))
        whenever(bookmarkManager.findByEpisodeTime(any(), eq(timeSecs))).thenReturn(null)
    }
}

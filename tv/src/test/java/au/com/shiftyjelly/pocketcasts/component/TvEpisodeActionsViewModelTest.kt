package au.com.shiftyjelly.pocketcasts.component

import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import java.util.Date
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Rule
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.verifyBlocking

@OptIn(ExperimentalCoroutinesApi::class)
class TvEpisodeActionsViewModelTest {

    @get:Rule
    val coroutineRule = MainCoroutineRule()

    private val episodeManager = mock<EpisodeManager>()
    private val playbackManager = mock<PlaybackManager>()
    private val podcastManager = mock<PodcastManager>()

    private val episode = PodcastEpisode(
        uuid = "episode-uuid",
        title = "Episode",
        podcastUuid = "podcast-uuid",
        publishedDate = Date(0),
    )

    private fun viewModel() = TvEpisodeActionsViewModel(
        episodeManager = episodeManager,
        playbackManager = playbackManager,
        podcastManager = podcastManager,
        applicationScope = CoroutineScope(coroutineRule.testDispatcher),
        ioDispatcher = coroutineRule.testDispatcher,
    )

    @Test
    fun `play starts playback of the episode`() = runTest {
        viewModel().play(episode, SourceView.UP_NEXT)

        verifyBlocking(playbackManager) { playNowSuspend(episode = episode, sourceView = SourceView.UP_NEXT) }
    }

    @Test
    fun `playNext adds episode to the top of the queue`() = runTest {
        viewModel().playNext(episode, SourceView.UP_NEXT)

        verifyBlocking(playbackManager) { playNext(episode, SourceView.UP_NEXT) }
    }

    @Test
    fun `playLast adds episode to the bottom of the queue`() = runTest {
        viewModel().playLast(episode, SourceView.UP_NEXT)

        verifyBlocking(playbackManager) { playLast(episode, SourceView.UP_NEXT) }
    }

    @Test
    fun `markAsPlayed marks the episode as played`() = runTest {
        viewModel().markAsPlayed(episode)

        verify(episodeManager).markAsPlayedBlocking(episode, playbackManager, podcastManager)
    }

    @Test
    fun `markAsUnplayed marks the episode as not played`() = runTest {
        viewModel().markAsUnplayed(episode)

        verify(episodeManager).markAsNotPlayedBlocking(episode)
    }

    @Test
    fun `archive archives the episode`() = runTest {
        viewModel().archive(episode)

        verify(episodeManager).archiveBlocking(episode, playbackManager)
    }

    @Test
    fun `unarchive unarchives the episode`() = runTest {
        viewModel().unarchive(episode)

        verify(episodeManager).unarchiveBlocking(episode)
    }

    @Test
    fun `removeFromUpNext removes the episode from the queue`() = runTest {
        viewModel().removeFromUpNext(episode, SourceView.UP_NEXT)

        verify(playbackManager).removeEpisode(episodeToRemove = episode, source = SourceView.UP_NEXT)
    }
}

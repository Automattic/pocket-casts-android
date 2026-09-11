package au.com.shiftyjelly.pocketcasts.component

import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.sharedtest.MainCoroutineRule
import com.automattic.eventhorizon.EpisodeActionsShownEvent
import com.automattic.eventhorizon.EpisodeArchivedEvent
import com.automattic.eventhorizon.EpisodeMarkedAsPlayedEvent
import com.automattic.eventhorizon.EpisodeMarkedAsUnplayedEvent
import com.automattic.eventhorizon.EpisodeUnarchivedEvent
import com.automattic.eventhorizon.EpisodeViewSourceType
import com.automattic.eventhorizon.EventHorizon
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
    private val eventHorizon = mock<EventHorizon>()

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
        eventHorizon = eventHorizon,
        applicationScope = CoroutineScope(coroutineRule.testDispatcher),
        ioDispatcher = coroutineRule.testDispatcher,
    )

    @Test
    fun `tracking actions shown records the event with the source`() = runTest {
        viewModel().trackActionsShown(EpisodeViewSourceType.Search)

        verify(eventHorizon).track(EpisodeActionsShownEvent(source = EpisodeViewSourceType.Search))
    }

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
    fun `markAsPlayed marks the episode as played and tracks the event with the source`() = runTest {
        viewModel().markAsPlayed(episode, SourceView.PODCAST_SCREEN)

        verify(episodeManager).markAsPlayedBlocking(episode, playbackManager, podcastManager)
        verify(eventHorizon).track(EpisodeMarkedAsPlayedEvent(source = SourceView.PODCAST_SCREEN.analyticsValue, episodeUuid = "episode-uuid"))
    }

    @Test
    fun `markAsUnplayed marks the episode as not played and tracks the event with the source`() = runTest {
        viewModel().markAsUnplayed(episode, SourceView.LISTENING_HISTORY)

        verify(episodeManager).markAsNotPlayedBlocking(episode)
        verify(eventHorizon).track(EpisodeMarkedAsUnplayedEvent(source = SourceView.LISTENING_HISTORY.analyticsValue, episodeUuid = "episode-uuid"))
    }

    @Test
    fun `archive archives the episode and tracks the event with the source`() = runTest {
        viewModel().archive(episode, SourceView.UP_NEXT)

        verify(episodeManager).archiveBlocking(episode, playbackManager)
        verify(eventHorizon).track(EpisodeArchivedEvent(source = SourceView.UP_NEXT.analyticsValue, episodeUuid = "episode-uuid"))
    }

    @Test
    fun `unarchive unarchives the episode and tracks the event with the source`() = runTest {
        viewModel().unarchive(episode, SourceView.STARRED)

        verify(episodeManager).unarchiveBlocking(episode)
        verify(eventHorizon).track(EpisodeUnarchivedEvent(source = SourceView.STARRED.analyticsValue, episodeUuid = "episode-uuid"))
    }

    @Test
    fun `removeFromUpNext removes the episode from the queue`() = runTest {
        viewModel().removeFromUpNext(episode, SourceView.UP_NEXT)

        verify(playbackManager).removeEpisode(episodeToRemove = episode, source = SourceView.UP_NEXT)
    }
}

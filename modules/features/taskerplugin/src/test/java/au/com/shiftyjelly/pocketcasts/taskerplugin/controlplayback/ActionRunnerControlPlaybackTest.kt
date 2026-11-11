package au.com.shiftyjelly.pocketcasts.taskerplugin.controlplayback

import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.models.entity.UserEpisode
import au.com.shiftyjelly.pocketcasts.preferences.UserSetting
import au.com.shiftyjelly.pocketcasts.taskerplugin.TaskerShimApplication
import au.com.shiftyjelly.pocketcasts.taskerplugin.controlplayback.InputControlPlayback.PlaybackCommand
import com.automattic.eventhorizon.EpisodeArchivedEvent
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess
import java.util.Date
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.kotlin.any
import org.mockito.kotlin.doAnswer
import org.mockito.kotlin.mock
import org.mockito.kotlin.never
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = TaskerShimApplication::class)
class ActionRunnerControlPlaybackTest {
    private val application = RuntimeEnvironment.getApplication() as TaskerShimApplication
    private val playbackManager = application.playbackManager
    private val episodeManager = application.episodeManager

    private var upNextShuffle = false

    @Before
    fun setUp() {
        val upNextShuffleSetting = mock<UserSetting<Boolean>> { on { value } doAnswer { upNextShuffle } }
        whenever(application.settings.upNextShuffle).thenReturn(upNextShuffleSetting)
    }

    @Test
    fun `archive current episode archives the playing podcast episode`() {
        val episode = PodcastEpisode(uuid = "episode-uuid", publishedDate = Date())
        whenever(playbackManager.getCurrentEpisode()).thenReturn(episode)

        val result = run(PlaybackCommand.ArchiveCurrentEpisode)

        assertTrue(result is TaskerPluginResultSucess)
        verify(episodeManager).archiveBlocking(episode, playbackManager, sync = true, shouldShuffleUpNext = false)
        val event = application.trackedEvents.single() as EpisodeArchivedEvent
        assertEquals("episode-uuid", event.episodeUuid)
        assertEquals(SourceView.TASKER.analyticsValue, event.source)
    }

    @Test
    fun `archive current episode respects the Up Next shuffle setting`() {
        upNextShuffle = true
        val episode = PodcastEpisode(uuid = "episode-uuid", publishedDate = Date())
        whenever(playbackManager.getCurrentEpisode()).thenReturn(episode)

        run(PlaybackCommand.ArchiveCurrentEpisode)

        verify(episodeManager).archiveBlocking(episode, playbackManager, sync = true, shouldShuffleUpNext = true)
    }

    @Test
    fun `archive current episode fails when nothing is loaded`() {
        whenever(playbackManager.getCurrentEpisode()).thenReturn(null)

        val result = run(PlaybackCommand.ArchiveCurrentEpisode)

        assertArchiveError(result)
    }

    @Test
    fun `archive current episode fails for a user file`() {
        whenever(playbackManager.getCurrentEpisode()).thenReturn(UserEpisode(uuid = "file-uuid", publishedDate = Date()))

        val result = run(PlaybackCommand.ArchiveCurrentEpisode)

        assertArchiveError(result)
    }

    private fun assertArchiveError(result: TaskerPluginResult<Unit>) {
        assertTrue(result is TaskerPluginResultError)
        assertEquals("No podcast episode is loaded to archive", (result as TaskerPluginResultError).message)
        verify(episodeManager, never()).archiveBlocking(any(), any(), any(), any())
        assertTrue(application.trackedEvents.isEmpty())
    }

    private fun run(command: PlaybackCommand): TaskerPluginResult<Unit> {
        return ActionRunnerControlPlayback().run(application, TaskerInput(InputControlPlayback(command = command.name)))
    }
}

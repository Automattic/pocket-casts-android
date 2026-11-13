package au.com.shiftyjelly.pocketcasts.taskerplugin

import android.app.Application
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playback.SleepTimer
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.hilt.TaskerEntryPoint
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.automattic.eventhorizon.EventHorizon
import dagger.hilt.internal.GeneratedComponent
import dagger.hilt.internal.GeneratedComponentManager
import org.mockito.kotlin.mock

/**
 * Lets the Tasker action runners resolve their dependencies through [TaskerEntryPoint] without a full Hilt graph.
 */
class TaskerShimApplication :
    Application(),
    GeneratedComponentManager<Any> {
    val sleepTimer = SleepTimer(settings = mock<Settings>(), eventHorizon = EventHorizon {})

    private val component = object : GeneratedComponent, TaskerEntryPoint {
        override fun getTheme() = mock<Theme>()
        override fun getSettings() = mock<Settings>()
        override fun getPlaybackManager() = mock<PlaybackManager>()
        override fun getPodcastManager() = mock<PodcastManager>()
        override fun getEpisodeManager() = mock<EpisodeManager>()
        override fun getPlaylistManager() = mock<PlaylistManager>()
        override fun getSleepTimer() = sleepTimer
    }

    override fun generatedComponent(): Any = component
}

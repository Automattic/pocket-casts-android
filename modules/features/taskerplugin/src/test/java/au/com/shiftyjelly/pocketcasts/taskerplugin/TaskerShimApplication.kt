package au.com.shiftyjelly.pocketcasts.taskerplugin

import android.app.Application
import au.com.shiftyjelly.pocketcasts.preferences.Settings
import au.com.shiftyjelly.pocketcasts.repositories.playback.PlaybackManager
import au.com.shiftyjelly.pocketcasts.repositories.playlist.PlaylistManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.EpisodeManager
import au.com.shiftyjelly.pocketcasts.repositories.podcast.PodcastManager
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.hilt.TaskerEntryPoint
import au.com.shiftyjelly.pocketcasts.ui.theme.Theme
import com.automattic.eventhorizon.EventHorizon
import com.automattic.eventhorizon.Trackable
import dagger.hilt.internal.GeneratedComponent
import dagger.hilt.internal.GeneratedComponentManager
import org.mockito.kotlin.mock

/**
 * Lets the Tasker action runners resolve their dependencies through [TaskerEntryPoint] without a full Hilt graph.
 */
class TaskerShimApplication :
    Application(),
    GeneratedComponentManager<Any> {
    val settings = mock<Settings>()
    val playbackManager = mock<PlaybackManager>()
    val episodeManager = mock<EpisodeManager>()
    val trackedEvents = mutableListOf<Trackable>()

    private val component = object : GeneratedComponent, TaskerEntryPoint {
        override fun getTheme() = mock<Theme>()
        override fun getSettings() = settings
        override fun getPlaybackManager() = playbackManager
        override fun getPodcastManager() = mock<PodcastManager>()
        override fun getEpisodeManager() = episodeManager
        override fun getPlaylistManager() = mock<PlaylistManager>()
        override fun getEventHorizon() = EventHorizon { trackedEvents += it }
    }

    override fun generatedComponent(): Any = component
}

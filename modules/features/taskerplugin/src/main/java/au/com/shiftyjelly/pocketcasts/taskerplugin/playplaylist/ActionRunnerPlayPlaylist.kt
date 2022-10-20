package au.com.shiftyjelly.pocketcasts.taskerplugin.playplaylist

import android.content.Context
import au.com.shiftyjelly.pocketcasts.taskerplugin.hilt.episodeManager
import au.com.shiftyjelly.pocketcasts.taskerplugin.hilt.playbackManager
import au.com.shiftyjelly.pocketcasts.taskerplugin.hilt.playlistManager
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerActionNoOutput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess

class ActionRunnerPlayPlaylist : TaskerPluginRunnerActionNoOutput<InputPlayPlaylist>() {

    override fun run(context: Context, input: TaskerInput<InputPlayPlaylist>): TaskerPluginResult<Unit> {
        val title = input.regular.title ?: return TaskerPluginResultError(1, "Must provide title")
        val playbackManager = context.playbackManager
        val playlistManager = context.playlistManager
        val episodeManager = context.episodeManager

        playbackManager.upNextQueue.removeAll()

        val playlist = playlistManager.findFirstByTitle(title) ?: return TaskerPluginResultError(2, "Playlist $title not found")

        val episodes = playlistManager.findEpisodes(playlist, episodeManager, playbackManager).sortedByDescending { it.publishedDate }
        if (episodes.isEmpty()) return TaskerPluginResultError(3, "No episodes in playlist $title")

        playbackManager.playEpisodes(episodes)
        return TaskerPluginResultSucess()
    }
}

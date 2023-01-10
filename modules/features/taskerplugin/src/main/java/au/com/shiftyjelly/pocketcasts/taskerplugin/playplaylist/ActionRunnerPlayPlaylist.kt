package au.com.shiftyjelly.pocketcasts.taskerplugin.playplaylist

import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.AnalyticsSource
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.hilt.episodeManager
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.hilt.playbackManager
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.hilt.playlistManager
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerActionNoOutput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess

private const val ERROR_NO_TITLE_PROVIDED = 1
private const val ERROR_PLAYLIST_NOT_FOUND = 2
private const val ERROR_PLAYLIST_NO_EPISODES = 3

class ActionRunnerPlayPlaylist : TaskerPluginRunnerActionNoOutput<InputPlayPlaylist>() {

    override fun run(context: Context, input: TaskerInput<InputPlayPlaylist>): TaskerPluginResult<Unit> {
        val title = input.regular.title ?: return TaskerPluginResultError(ERROR_NO_TITLE_PROVIDED, context.getString(R.string.must_provide_filter_name))
        val playbackManager = context.playbackManager
        val playlistManager = context.playlistManager
        val episodeManager = context.episodeManager

        playbackManager.upNextQueue.removeAll()

        val playlist = playlistManager.findFirstByTitle(title) ?: return TaskerPluginResultError(ERROR_PLAYLIST_NOT_FOUND, context.getString(R.string.filter_x_not_found, title))

        val episodes = playlistManager.findEpisodes(playlist, episodeManager, playbackManager)
        if (episodes.isEmpty()) return TaskerPluginResultError(ERROR_PLAYLIST_NO_EPISODES, context.getString(R.string.no_episodes_in_filter_x, title))

        playbackManager.playEpisodes(episodes, AnalyticsSource.TASKER)
        return TaskerPluginResultSucess()
    }
}

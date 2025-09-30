package au.com.shiftyjelly.pocketcasts.taskerplugin.playplaylist

import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.models.to.PlaylistEpisode
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.findPlaylist
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.hilt.playbackManager
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.hilt.playlistManager
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerActionNoOutput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

private const val ERROR_NO_TITLE_PROVIDED = 1
private const val ERROR_PLAYLIST_NOT_FOUND = 2
private const val ERROR_PLAYLIST_NO_EPISODES = 3

class ActionRunnerPlayPlaylist : TaskerPluginRunnerActionNoOutput<InputPlayPlaylist>() {

    override fun run(context: Context, input: TaskerInput<InputPlayPlaylist>): TaskerPluginResult<Unit> {
        val title = input.regular.title ?: return TaskerPluginResultError(ERROR_NO_TITLE_PROVIDED, context.getString(R.string.must_provide_filter_name))
        val playbackManager = context.playbackManager
        val playlistManager = context.playlistManager

        val playlistPreviews = runBlocking { playlistManager.playlistPreviewsFlow().first() }
        val playlistPreview = playlistPreviews.find { it.title.equals(title, ignoreCase = true) }
        if (playlistPreview == null) {
            return TaskerPluginResultError(ERROR_PLAYLIST_NOT_FOUND, context.getString(R.string.filter_x_not_found, title))
        }
        val playlist = runBlocking { playlistManager.findPlaylist(playlistPreview.uuid) }
        if (playlist == null) {
            return TaskerPluginResultError(ERROR_PLAYLIST_NOT_FOUND, context.getString(R.string.filter_x_not_found, title))
        }

        val episodes = playlist.episodes.mapNotNull(PlaylistEpisode::toPodcastEpisode)
        if (episodes.isEmpty()) {
            return TaskerPluginResultError(ERROR_PLAYLIST_NO_EPISODES, context.getString(R.string.no_episodes_in_filter_x, title))
        }

        playbackManager.upNextQueue.removeAll()
        playbackManager.playEpisodes(episodes, SourceView.TASKER)
        return TaskerPluginResultSucess()
    }
}

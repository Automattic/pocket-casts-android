package au.com.shiftyjelly.pocketcasts.taskerplugin.queryfilterepisodes

import android.content.Context
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.OutputQueryEpisodes
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.hilt.episodeManager
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.hilt.playbackManager
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.hilt.playlistManager
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.nullIfEmpty
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerAction
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess

class ActionRunnerQueryFilterEpisodes : TaskerPluginRunnerAction<InputQueryFilterEpisodes, Array<OutputQueryEpisodes>>() {

    override fun run(context: Context, input: TaskerInput<InputQueryFilterEpisodes>): TaskerPluginResult<Array<OutputQueryEpisodes>> {
        val playlistManager = context.playlistManager
        val titleOrId = input.regular.titleOrId.nullIfEmpty ?: return TaskerPluginResultSucess()

        val playlist = playlistManager.findAll().firstOrNull { it.title.lowercase().contains(titleOrId.trim().lowercase()) || it.uuid == titleOrId } ?: return TaskerPluginResultSucess(arrayOf())
        val episodes = playlistManager.findEpisodes(playlist, context.episodeManager, context.playbackManager).take(50)
        val output = episodes.map { OutputQueryEpisodes(it) }.toTypedArray()
        return TaskerPluginResultSucess(output)
    }
}

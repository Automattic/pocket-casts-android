package au.com.shiftyjelly.pocketcasts.taskerplugin.queryfilters

import android.content.Context
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.hilt.playlistManager
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerAction
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class ActionRunnerQueryFilters : TaskerPluginRunnerAction<InputQueryFilters, Array<OutputQueryFilters>>() {

    override fun run(context: Context, input: TaskerInput<InputQueryFilters>): TaskerPluginResult<Array<OutputQueryFilters>> {
        val playlistManager = context.playlistManager
        val playlists = runBlocking { playlistManager.playlistPreviewsFlow().first() }
        val output = playlists.map { OutputQueryFilters(it.uuid, it.title) }.toTypedArray()
        return TaskerPluginResultSucess(output)
    }
}

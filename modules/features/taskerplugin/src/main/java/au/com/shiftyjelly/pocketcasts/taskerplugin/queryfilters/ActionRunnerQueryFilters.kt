package au.com.shiftyjelly.pocketcasts.taskerplugin.queryfilters

import android.content.Context
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.hilt.playlistManager
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerAction
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess

class ActionRunnerQueryFilters : TaskerPluginRunnerAction<InputQueryFilters, Array<OutputQueryFilters>>() {

    override fun run(context: Context, input: TaskerInput<InputQueryFilters>): TaskerPluginResult<Array<OutputQueryFilters>> {
        val playlistManager = context.playlistManager
        val output = playlistManager.findAll().map {
            OutputQueryFilters(it.uuid, it.title, it.episodeCount)
        }.toTypedArray()
        return TaskerPluginResultSucess(output)
    }
}

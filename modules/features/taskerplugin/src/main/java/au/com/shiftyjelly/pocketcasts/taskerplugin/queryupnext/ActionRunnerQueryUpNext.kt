package au.com.shiftyjelly.pocketcasts.taskerplugin.queryUpNext

import android.content.Context
import au.com.shiftyjelly.pocketcasts.models.entity.PodcastEpisode
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.OutputQueryEpisodes
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.hilt.playbackManager
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerAction
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess

class ActionRunnerQueryUpNext : TaskerPluginRunnerAction<InputQueryUpNext, Array<OutputQueryEpisodes>>() {

    override fun run(context: Context, input: TaskerInput<InputQueryUpNext>): TaskerPluginResult<Array<OutputQueryEpisodes>> {
        val upNextQueue = context.playbackManager.upNextQueue
        val currentEpisodes = upNextQueue.allEpisodes
        val output = currentEpisodes.filterIsInstance<PodcastEpisode>().map { OutputQueryEpisodes(it) }.toTypedArray()
        return TaskerPluginResultSucess(output)
    }
}

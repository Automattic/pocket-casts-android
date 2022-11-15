package au.com.shiftyjelly.pocketcasts.taskerplugin.addtoupnext

import android.content.Context
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.hilt.episodeManager
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.hilt.playbackManager
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerActionNoOutput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess

private const val ERROR_NO_EPISODE_IDS_PROVIDED = 1
private const val ERROR_NO_EPISODES_FOUND = 2
class ActionRunnerAddToUpNext : TaskerPluginRunnerActionNoOutput<InputAddToUpNext>() {

    override fun run(context: Context, input: TaskerInput<InputAddToUpNext>): TaskerPluginResult<Unit> {
        val regularInput = input.regular
        val episodeIds = regularInput.episodeIds
        val episodeIdsSplit = episodeIds?.split(",") ?: return TaskerPluginResultError(ERROR_NO_EPISODE_IDS_PROVIDED, context.getString(R.string.no_episode_ids_provided))

        val episodes = episodeIdsSplit.mapNotNull { context.episodeManager.findByUuid(it) }
        if (episodes.isEmpty()) return TaskerPluginResultError(ERROR_NO_EPISODES_FOUND, context.getString(R.string.no_episodes_found_for_episode_ids_x, episodeIds))

        val playbackManager = context.playbackManager
        when (regularInput.clearModeEnum) {
            InputAddToUpNext.ClearMode.ClearUpNext -> playbackManager.upNextQueue.clearUpNext()
            InputAddToUpNext.ClearMode.ClearAll -> playbackManager.upNextQueue.removeAll()
            else -> {}
        }
        when (regularInput.addModeEnum) {
            InputAddToUpNext.AddMode.Next -> playbackManager.playEpisodesNext(episodes)
            else -> playbackManager.playEpisodesLast(episodes)
        }

        return TaskerPluginResultSucess()
    }
}

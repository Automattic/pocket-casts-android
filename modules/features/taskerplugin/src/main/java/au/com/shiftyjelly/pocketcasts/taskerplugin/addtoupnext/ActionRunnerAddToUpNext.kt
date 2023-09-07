package au.com.shiftyjelly.pocketcasts.taskerplugin.addtoupnext

import android.content.Context
import au.com.shiftyjelly.pocketcasts.analytics.SourceView
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.hilt.episodeManager
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.hilt.playbackManager
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerActionNoOutput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultError
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess
import kotlinx.coroutines.runBlocking

private const val ERROR_NO_EPISODE_IDS_PROVIDED = 1
private const val ERROR_NO_EPISODES_FOUND = 2

class ActionRunnerAddToUpNext : TaskerPluginRunnerActionNoOutput<InputAddToUpNext>() {

    override fun run(context: Context, input: TaskerInput<InputAddToUpNext>): TaskerPluginResult<Unit> {
        val regularInput = input.regular
        val episodeIds = regularInput.episodeIds
        val episodeIdsSplit = episodeIds?.split(",") ?: return TaskerPluginResultError(ERROR_NO_EPISODE_IDS_PROVIDED, context.getString(R.string.no_episode_ids_provided))

        val episodesFromInput = episodeIdsSplit.mapNotNull {
            runBlocking {
                context.episodeManager.findByUuid(it)
            }
        }
        if (episodesFromInput.isEmpty()) return TaskerPluginResultError(ERROR_NO_EPISODES_FOUND, context.getString(R.string.no_episodes_found_for_episode_ids_x, episodeIds))

        val playbackManager = context.playbackManager

        val upNextQueue = playbackManager.upNextQueue
        val currentEpisodes = upNextQueue.allEpisodes
        val episodesToPlay = when (regularInput.clearModeEnum) {
            InputAddToUpNext.ClearMode.ClearUpNext -> currentEpisodes.take(1)
            InputAddToUpNext.ClearMode.ClearAll -> listOf()
            else -> currentEpisodes
        }.toMutableList()
        when (regularInput.addModeEnum) {
            InputAddToUpNext.AddMode.Current -> episodesToPlay.addAll(0, episodesFromInput)
            InputAddToUpNext.AddMode.Next -> if (episodesToPlay.isEmpty()) episodesToPlay.addAll(episodesFromInput) else episodesToPlay.addAll(1, episodesFromInput)
            null, InputAddToUpNext.AddMode.Last -> episodesToPlay.addAll(episodesFromInput)
        }

        if (regularInput.startPlaying?.toBooleanStrictOrNull() == true) {
            playbackManager.playEpisodes(episodesToPlay, SourceView.TASKER)
        } else {
            upNextQueue.changeList(episodesToPlay)
        }
        return TaskerPluginResultSucess()
    }
}

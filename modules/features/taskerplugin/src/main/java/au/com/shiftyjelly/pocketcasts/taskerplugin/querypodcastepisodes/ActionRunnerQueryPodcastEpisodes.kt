package au.com.shiftyjelly.pocketcasts.taskerplugin.querypodcastepisodes

import android.content.Context
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.OutputQueryEpisodes
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.hilt.episodeManager
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.hilt.podcastManager
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.nullIfEmpty
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerAction
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess

class ActionRunnerQueryPodcastEpisodes : TaskerPluginRunnerAction<InputQueryPodcastEpisodes, Array<OutputQueryEpisodes>>() {

    override fun run(context: Context, input: TaskerInput<InputQueryPodcastEpisodes>): TaskerPluginResult<Array<OutputQueryEpisodes>> {
        val podcastManager = context.podcastManager
        val titleOrId = input.regular.titleOrId.nullIfEmpty ?: return TaskerPluginResultSucess()

        val podcast = podcastManager.findSubscribed().firstOrNull { it.title.lowercase().contains(titleOrId.trim().lowercase()) || it.uuid == titleOrId } ?: return TaskerPluginResultSucess(arrayOf())
        val episodes = context.episodeManager.findEpisodesByPodcastOrdered(podcast).take(50)
        val output = episodes.map { OutputQueryEpisodes(it) }.toTypedArray()
        return TaskerPluginResultSucess(output)
    }
}

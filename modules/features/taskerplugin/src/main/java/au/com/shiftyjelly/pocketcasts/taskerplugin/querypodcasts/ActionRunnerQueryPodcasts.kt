package au.com.shiftyjelly.pocketcasts.taskerplugin.querypodcasts

import android.content.Context
import au.com.shiftyjelly.pocketcasts.repositories.images.PodcastImage
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.formattedForTasker
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.hilt.podcastManager
import com.joaomgcd.taskerpluginlibrary.action.TaskerPluginRunnerAction
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResult
import com.joaomgcd.taskerpluginlibrary.runner.TaskerPluginResultSucess

class ActionRunnerQueryPodcasts : TaskerPluginRunnerAction<InputQueryPodcasts, Array<OutputQueryPodcasts>>() {

    override fun run(context: Context, input: TaskerInput<InputQueryPodcasts>): TaskerPluginResult<Array<OutputQueryPodcasts>> {
        val podcastManager = context.podcastManager
        val output = podcastManager.findSubscribed().filter { it.isSubscribed }.map {
            OutputQueryPodcasts(it.uuid, it.title.formattedForTasker, it.author.formattedForTasker, it.podcastUrl, it.thumbnailUrl ?: PodcastImage.getArtworkUrl(480, it.uuid), it.podcastCategory.formattedForTasker, it.addedDate?.formattedForTasker, it.latestEpisodeDate?.formattedForTasker)
        }.toTypedArray()
        return TaskerPluginResultSucess(output)
    }
}

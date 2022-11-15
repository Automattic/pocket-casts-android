package au.com.shiftyjelly.pocketcasts.taskerplugin.querypodcastepisodes

import au.com.shiftyjelly.pocketcasts.taskerplugin.base.OutputQueryEpisodes
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper

class ActionHelperQueryPodcastEpisodes(config: TaskerPluginConfig<InputQueryPodcastEpisodes>) : TaskerPluginConfigHelper<InputQueryPodcastEpisodes, Array<OutputQueryEpisodes>, ActionRunnerQueryPodcastEpisodes>(config) {
    override val runnerClass: Class<ActionRunnerQueryPodcastEpisodes> get() = ActionRunnerQueryPodcastEpisodes::class.java
    override val inputClass get() = InputQueryPodcastEpisodes::class.java
    override val outputClass get() = Array<OutputQueryEpisodes>::class.java
}

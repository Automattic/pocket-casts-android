package au.com.shiftyjelly.pocketcasts.taskerplugin.queryfilterepisodes

import au.com.shiftyjelly.pocketcasts.taskerplugin.base.OutputQueryEpisodes
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper

class ActionHelperQueryFilterEpisodes(config: TaskerPluginConfig<InputQueryFilterEpisodes>) : TaskerPluginConfigHelper<InputQueryFilterEpisodes, Array<OutputQueryEpisodes>, ActionRunnerQueryFilterEpisodes>(config) {
    override val runnerClass: Class<ActionRunnerQueryFilterEpisodes> get() = ActionRunnerQueryFilterEpisodes::class.java
    override val inputClass get() = InputQueryFilterEpisodes::class.java
    override val outputClass get() = Array<OutputQueryEpisodes>::class.java
}

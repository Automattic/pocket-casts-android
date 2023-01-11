package au.com.shiftyjelly.pocketcasts.taskerplugin.queryUpNext

import au.com.shiftyjelly.pocketcasts.taskerplugin.base.OutputQueryEpisodes
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper

class ActionHelperQueryUpNext(config: TaskerPluginConfig<InputQueryUpNext>) : TaskerPluginConfigHelper<InputQueryUpNext, Array<OutputQueryEpisodes>, ActionRunnerQueryUpNext>(config) {
    override val runnerClass: Class<ActionRunnerQueryUpNext> get() = ActionRunnerQueryUpNext::class.java
    override val inputClass get() = InputQueryUpNext::class.java
    override val outputClass get() = Array<OutputQueryEpisodes>::class.java
}

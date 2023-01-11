package au.com.shiftyjelly.pocketcasts.taskerplugin.addtoupnext

import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelperNoOutput

class ActionHelperAddToUpNext(config: TaskerPluginConfig<InputAddToUpNext>) : TaskerPluginConfigHelperNoOutput<InputAddToUpNext, ActionRunnerAddToUpNext>(config) {
    override val runnerClass: Class<ActionRunnerAddToUpNext> get() = ActionRunnerAddToUpNext::class.java
    override val addDefaultStringBlurb get() = true
    override val inputClass get() = InputAddToUpNext::class.java
}

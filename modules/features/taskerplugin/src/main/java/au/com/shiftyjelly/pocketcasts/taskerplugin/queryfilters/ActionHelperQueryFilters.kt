package au.com.shiftyjelly.pocketcasts.taskerplugin.queryfilters

import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper

class ActionHelperQueryFilters(config: TaskerPluginConfig<InputQueryFilters>) : TaskerPluginConfigHelper<InputQueryFilters, Array<OutputQueryFilters>, ActionRunnerQueryFilters>(config) {
    override val runnerClass: Class<ActionRunnerQueryFilters> get() = ActionRunnerQueryFilters::class.java
    override val inputClass get() = InputQueryFilters::class.java
    override val outputClass get() = Array<OutputQueryFilters>::class.java
}

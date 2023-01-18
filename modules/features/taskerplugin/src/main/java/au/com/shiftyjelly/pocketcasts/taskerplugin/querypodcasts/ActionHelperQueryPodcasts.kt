package au.com.shiftyjelly.pocketcasts.taskerplugin.querypodcasts

import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelper

class ActionHelperQueryPodcasts(config: TaskerPluginConfig<InputQueryPodcasts>) : TaskerPluginConfigHelper<InputQueryPodcasts, Array<OutputQueryPodcasts>, ActionRunnerQueryPodcasts>(config) {
    override val runnerClass: Class<ActionRunnerQueryPodcasts> get() = ActionRunnerQueryPodcasts::class.java
    override val inputClass get() = InputQueryPodcasts::class.java
    override val outputClass get() = Array<OutputQueryPodcasts>::class.java
}

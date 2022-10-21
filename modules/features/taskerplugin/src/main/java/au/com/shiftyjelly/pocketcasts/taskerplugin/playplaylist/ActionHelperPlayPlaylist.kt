package au.com.shiftyjelly.pocketcasts.taskerplugin.playplaylist

import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelperNoOutput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput

class ActionHelperPlayPlaylist(config: TaskerPluginConfig<InputPlayPlaylist>) : TaskerPluginConfigHelperNoOutput<InputPlayPlaylist, ActionRunnerPlayPlaylist>(config) {
    override val runnerClass: Class<ActionRunnerPlayPlaylist> get() = ActionRunnerPlayPlaylist::class.java
    override fun addToStringBlurb(input: TaskerInput<InputPlayPlaylist>, blurbBuilder: StringBuilder) {
        blurbBuilder.append("${context.getString(au.com.shiftyjelly.pocketcasts.localization.R.string.filters_filter_name)}: ${input.regular.title}")
    }

    override val addDefaultStringBlurb: Boolean
        get() = false
    override val inputClass: Class<InputPlayPlaylist>
        get() = InputPlayPlaylist::class.java
}

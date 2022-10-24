package au.com.shiftyjelly.pocketcasts.taskerplugin.controlplayback

import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfigHelperNoOutput
import com.joaomgcd.taskerpluginlibrary.input.TaskerInput

class ActionHelperControlPlayback(config: TaskerPluginConfig<InputControlPlayback>) : TaskerPluginConfigHelperNoOutput<InputControlPlayback, ActionRunnerControlPlayback>(config) {
    override val runnerClass: Class<ActionRunnerControlPlayback> get() = ActionRunnerControlPlayback::class.java
    override fun addToStringBlurb(input: TaskerInput<InputControlPlayback>, blurbBuilder: StringBuilder) {
        val inputControlPlayback = input.regular
        val commandEnum = inputControlPlayback.commandEnum
        blurbBuilder.append("${context.getString(au.com.shiftyjelly.pocketcasts.localization.R.string.playback_command)}: ${commandEnum?.getDescription(context)}")
        when (commandEnum) {
            InputControlPlayback.PlaybackCommand.SkipToChapter -> blurbBuilder.append("\n${context.getString(au.com.shiftyjelly.pocketcasts.localization.R.string.chapter_to_skip_to)}: ${inputControlPlayback.chapterToSkipTo}")
            InputControlPlayback.PlaybackCommand.SkipToTime -> blurbBuilder.append("\n${context.getString(au.com.shiftyjelly.pocketcasts.localization.R.string.time_to_skip_to_seconds)}: ${inputControlPlayback.timeToSkipToSeconds}")
            else -> {}
        }
    }

    override val addDefaultStringBlurb: Boolean
        get() = false
    override val inputClass: Class<InputControlPlayback>
        get() = InputControlPlayback::class.java
}

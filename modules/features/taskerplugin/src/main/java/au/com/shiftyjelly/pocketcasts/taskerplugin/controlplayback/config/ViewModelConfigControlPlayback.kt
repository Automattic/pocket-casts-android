package au.com.shiftyjelly.pocketcasts.taskerplugin.controlplayback.config

import android.app.Application
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.ViewModelBase
import au.com.shiftyjelly.pocketcasts.taskerplugin.controlplayback.ActionHelperControlPlayback
import au.com.shiftyjelly.pocketcasts.taskerplugin.controlplayback.InputControlPlayback
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject

@HiltViewModel
class ViewModelConfigControlPlayback @Inject constructor(
    application: Application
) : ViewModelBase<InputControlPlayback, ActionHelperControlPlayback>(application), TaskerPluginConfig<InputControlPlayback> {
    override val helperClass get() = ActionHelperControlPlayback::class.java

    /**
     * A field that only appears depending on the type of the playback command. For example, the field "Time to Skip To" will only appear if the command is "Skip To Time"
     * @param showForCommands the types of playback commands that makes this field appear
     * @param valueGetter how to get the value of this input field from the Tasker input
     * @param valueSetter how to set a newly assigned value of this field to the Tasker input
     */
    inner class OptionalField constructor(val showForCommands: List<InputControlPlayback.PlaybackCommand>, valueGetter: (InputControlPlayback?) -> String?, val valueSetter: (String?) -> Unit) {
        constructor(showForCommand: InputControlPlayback.PlaybackCommand, valueGetter: (InputControlPlayback?) -> String?, valueSetter: (String?) -> Unit) : this(listOf(showForCommand), valueGetter, valueSetter)

        private val askFor get() = showForCommands.contains(input?.commandEnum)
        val shouldAskForState by lazy { MutableStateFlow(askFor) }
        fun updateAskForState() {
            shouldAskForState.tryEmit(askFor)
        }

        val valueState by lazy { MutableStateFlow(valueGetter(input)) }
        var value: String? = null
            set(value) {
                valueSetter(value)
                valueState.tryEmit(value)
            }
    }

    /**
     * When adding new optional fields here make sure to add them to the [optionalFields] list below so that everything is correctly updated
     */
    private val optionalFieldChapterToSkipTo by lazy { OptionalField(InputControlPlayback.PlaybackCommand.SkipToChapter, { input?.chapterToSkipTo }, { input?.chapterToSkipTo = it }) }
    private val optionalFieldTimeToSkipTo by lazy { OptionalField(InputControlPlayback.PlaybackCommand.SkipToTime, { input?.skipToSeconds }, { input?.skipToSeconds = it }) }
    private val optionalFieldTimeToSkip by lazy { OptionalField(listOf(InputControlPlayback.PlaybackCommand.SkipForward, InputControlPlayback.PlaybackCommand.SkipBack), { input?.skipSeconds }, { input?.skipSeconds = it }) }
    private val optionalFields = listOf(
        optionalFieldChapterToSkipTo,
        optionalFieldTimeToSkipTo,
        optionalFieldTimeToSkip
    )

    val commandState by lazy {
        MutableStateFlow(
            input?.command
        )
    }
    var command: String?
        get() = input?.command
        set(value) {
            input?.command = value
            commandState.tryEmit(value)
            optionalFields.forEach { it.updateAskForState() }
        }

    val availableCommands get() = InputControlPlayback.PlaybackCommand.values()

    /*Chapter To skip to*/
    val shouldAskForChapter get() = optionalFieldChapterToSkipTo.shouldAskForState
    val chapterToSkipTo get() = optionalFieldChapterToSkipTo.valueState
    fun setChapterToSkipTo(value: String) {
        optionalFieldChapterToSkipTo.value = value
    }
    /*End chapter To skip to*/

    /*Time To skip to*/
    val showAskForTimeToSkipTo get() = optionalFieldTimeToSkipTo.shouldAskForState
    val timeToSkipTo get() = optionalFieldTimeToSkipTo.valueState
    fun setTimeToSkipTo(value: String) {
        optionalFieldTimeToSkipTo.value = value
    }
    /*End time To skip to*/

    /*Time To skip */
    val showAskForTimeToSkip get() = optionalFieldTimeToSkip.shouldAskForState
    val timeToSkip get() = optionalFieldTimeToSkip.valueState
    fun setTimeToSkip(value: String) {
        optionalFieldTimeToSkip.value = value
    }
    /*End time To skip to*/
}

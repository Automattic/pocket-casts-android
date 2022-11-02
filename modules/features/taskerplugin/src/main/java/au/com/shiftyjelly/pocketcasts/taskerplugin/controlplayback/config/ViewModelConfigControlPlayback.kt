package au.com.shiftyjelly.pocketcasts.taskerplugin.controlplayback.config

import android.app.Application
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.models.type.TrimMode
import au.com.shiftyjelly.pocketcasts.taskerplugin.R
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.ViewModelBase
import au.com.shiftyjelly.pocketcasts.taskerplugin.controlplayback.ActionHelperControlPlayback
import au.com.shiftyjelly.pocketcasts.taskerplugin.controlplayback.InputControlPlayback
import com.joaomgcd.taskerpluginlibrary.config.TaskerPluginConfig
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import au.com.shiftyjelly.pocketcasts.images.R as IR
import au.com.shiftyjelly.pocketcasts.localization.R as LR

@HiltViewModel
class ViewModelConfigControlPlayback @Inject constructor(
    application: Application
) : ViewModelBase<InputControlPlayback, ActionHelperControlPlayback>(application), TaskerPluginConfig<InputControlPlayback> {
    override val helperClass get() = ActionHelperControlPlayback::class.java

    /**
     * A field that only appears depending on the type of the playback command. For example, the field "Time to Skip To" will only appear if the command is "Skip To Time". A field can also always appear if it doesn't depend on a command type
     * @param showForCommands the types of playback commands that makes this field appear. If empty, will always show field
     * @param valueGetter how to get the value of this input field from the Tasker input
     * @param valueSetter how to set a newly assigned value of this field to the Tasker input
     */
    private open inner class InputField<T : Any> constructor(@StringRes labelResId: Int, @DrawableRes iconResId: Int, private val showForCommands: List<InputControlPlayback.PlaybackCommand>, valueGetter: InputControlPlayback.() -> String?, valueSetter: InputControlPlayback.(String?) -> Unit) : InputFieldBase<T>(labelResId, iconResId, valueGetter, valueSetter) {
        constructor(@StringRes labelResId: Int, @DrawableRes iconResId: Int, showForCommand: InputControlPlayback.PlaybackCommand, valueGetter: InputControlPlayback.() -> String?, valueSetter: InputControlPlayback.(String?) -> Unit) : this(labelResId, iconResId, listOf(showForCommand), valueGetter, valueSetter)
        constructor(@StringRes labelResId: Int, @DrawableRes iconResId: Int, valueGetter: InputControlPlayback.() -> String?, valueSetter: InputControlPlayback.(String?) -> Unit) : this(labelResId, iconResId, listOf(), valueGetter, valueSetter)

        override val askFor get() = showForCommands.isEmpty() || showForCommands.contains(input?.commandEnum)
    }

    private val inputFieldCommand = object : InputField<InputControlPlayback.PlaybackCommand>(LR.string.playback_command, IR.drawable.filter_play, { command }, { setCommand(it) }) {
        override fun getPossibleValues() = MutableStateFlow(InputControlPlayback.PlaybackCommand.values().toList())
        override fun getValueDescriptionSpecific(possibleValue: InputControlPlayback.PlaybackCommand?) = possibleValue?.getDescription(context)
    }
    override val inputFields: List<InputFieldBase<*>> = listOf(
        inputFieldCommand,
        InputField(LR.string.chapter_to_skip_to, IR.drawable.filter_bullet, InputControlPlayback.PlaybackCommand.SkipToChapter, { chapterToSkipTo }, { chapterToSkipTo = it }),
        InputField(LR.string.time_to_skip_to_seconds, IR.drawable.filter_time, InputControlPlayback.PlaybackCommand.SkipToTime, { skipToSeconds }, { skipToSeconds = it }),
        InputField(LR.string.time_to_skip_seconds, IR.drawable.filter_time, listOf(InputControlPlayback.PlaybackCommand.SkipForward, InputControlPlayback.PlaybackCommand.SkipBack), { skipSeconds }, { skipSeconds = it }),
        object : InputField<Double>(LR.string.playback_speed_between_values, R.drawable.speedometer, InputControlPlayback.PlaybackCommand.SetPlaybackSpeed, { playbackSpeed }, { playbackSpeed = it }) {
            override fun getPossibleValues() = MutableStateFlow((5..30).map { it / 10.0 })
        },
        object : InputField<TrimMode>(LR.string.trim_silence_mode, R.drawable.content_cut, InputControlPlayback.PlaybackCommand.SetTrimSilenceMode, { trimSilenceMode }, { trimSilenceMode = it }) {
            override fun getPossibleValues() = MutableStateFlow(TrimMode.values().toList())
            override fun getValueDescriptionSpecific(possibleValue: TrimMode?): String? {
                if (possibleValue == null) return null

                return context.getString(
                    when (possibleValue) {
                        TrimMode.OFF -> LR.string.off
                        TrimMode.LOW -> LR.string.player_effects_trim_mild
                        TrimMode.MEDIUM -> LR.string.player_effects_trim_medium
                        TrimMode.HIGH -> LR.string.player_effects_trim_mad_max
                    }
                )
            }
        },
        object : InputField<Boolean>(LR.string.set_volume_boost, IR.drawable.filter_volume, InputControlPlayback.PlaybackCommand.SetVolumeBoost, { volumeBoostEnabled }, { volumeBoostEnabled = it }) {
            override fun getPossibleValues() = MutableStateFlow(listOf(true, false))
        }
    )

    fun setCommand(value: String?) {
        input?.command = value
        inputFieldCommand.valueState.tryEmit(value)
        inputFields.forEach { it.updateAskForState() }
    }
}

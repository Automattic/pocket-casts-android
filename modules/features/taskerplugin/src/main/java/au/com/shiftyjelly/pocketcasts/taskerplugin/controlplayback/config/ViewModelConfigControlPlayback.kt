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
            updateAskForChapterToSkipToState()
        }

    /*Chapter To skip to*/
    private val askForChapterToSkipTo get() = input?.commandEnum == InputControlPlayback.PlaybackCommand.SkipToChapter
    val askForChapterToSkipToState by lazy { MutableStateFlow(askForChapterToSkipTo) }
    private fun updateAskForChapterToSkipToState() {
        askForChapterToSkipToState.tryEmit(askForChapterToSkipTo)
    }

    val chapterToSkipToState by lazy { MutableStateFlow(input?.chapterToSkipTo) }
    var chapterToSkipTo: String? = null
        set(value) {
            input?.chapterToSkipTo = value
            chapterToSkipToState.tryEmit(value)
        }

    /*End chapter To skip to*/

    val availableCommands get() = InputControlPlayback.PlaybackCommand.values()
}

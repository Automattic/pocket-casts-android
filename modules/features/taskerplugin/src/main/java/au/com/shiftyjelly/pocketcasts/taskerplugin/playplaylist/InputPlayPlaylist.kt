package au.com.shiftyjelly.pocketcasts.taskerplugin.playplaylist

import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot

@TaskerInputRoot
class InputPlayPlaylist @JvmOverloads constructor(
    @field:TaskerInputField("title") var title: String? = null
)

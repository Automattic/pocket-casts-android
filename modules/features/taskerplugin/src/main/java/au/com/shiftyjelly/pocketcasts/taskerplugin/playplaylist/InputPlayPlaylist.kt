package au.com.shiftyjelly.pocketcasts.taskerplugin.playplaylist

import android.annotation.SuppressLint
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot

@TaskerInputRoot
class InputPlayPlaylist @JvmOverloads constructor(
    @SuppressLint("NonConstantResourceId") @field:TaskerInputField("title") var title: String? = null,
)

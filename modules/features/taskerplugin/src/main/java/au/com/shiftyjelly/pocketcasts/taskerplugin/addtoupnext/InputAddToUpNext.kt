package au.com.shiftyjelly.pocketcasts.taskerplugin.addtoupnext

import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.tryOrNull
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot

@TaskerInputRoot
class InputAddToUpNext @JvmOverloads constructor(
    @field:TaskerInputField("episodeIds", labelResIdName = "episode_ids") var episodeIds: String? = null,
    @field:TaskerInputField("clear_mode", labelResIdName = "clear_before_adding") var clearMode: String? = null,
    @field:TaskerInputField("add_mode", labelResIdName = "add_mode") var addMode: String? = null
) {
    enum class AddMode(@StringRes val descriptionResId: Int) { Next(R.string.next), Last(R.string.last) }
    enum class ClearMode(@StringRes val descriptionResId: Int) { DontClear(R.string.dont_clear), ClearUpNext(R.string.clear_up_next), ClearAll(R.string.clear_all) }

    val addModeEnum get() = tryOrNull { addMode?.let { AddMode.valueOf(it) } }
    val clearModeEnum get() = tryOrNull { clearMode?.let { ClearMode.valueOf(it) } }
}

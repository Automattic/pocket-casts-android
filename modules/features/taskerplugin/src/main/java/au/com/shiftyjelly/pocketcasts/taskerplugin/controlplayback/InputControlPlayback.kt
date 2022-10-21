package au.com.shiftyjelly.pocketcasts.taskerplugin.controlplayback

import android.content.Context
import androidx.annotation.StringRes
import au.com.shiftyjelly.pocketcasts.localization.R
import au.com.shiftyjelly.pocketcasts.taskerplugin.base.tryOrNull
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputField
import com.joaomgcd.taskerpluginlibrary.input.TaskerInputRoot

@TaskerInputRoot
class InputControlPlayback @JvmOverloads constructor(
    @field:TaskerInputField("command") var command: String? = null,
    @field:TaskerInputField("skipToChapter") var chapterToSkipTo: String? = null
) {

    val commandEnum get() = tryOrNull { command?.let { PlaybackCommand.valueOf(it) } }

    enum class PlaybackCommand(@StringRes val descriptionResId: Int) {
        SkipToNextChapter(R.string.skip_to_next_chapter), SkipToPrevious(R.string.skip_to_previous_chapter), SkipToChapter(R.string.skip_to_chapter);

        fun getDescription(context: Context) = context.getString(descriptionResId)
    }
}

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
    @field:TaskerInputField("skipToChapter") var chapterToSkipTo: String? = null,
    @field:TaskerInputField("timeToSkipToSeconds") var timeToSkipToSeconds: String? = null
) {

    val commandEnum get() = tryOrNull { command?.let { PlaybackCommand.valueOf(it) } }

    enum class PlaybackCommand(@StringRes val descriptionResId: Int) {
        PlayNextInQueue(R.string.play_next_in_queue),
        SkipToNextChapter(R.string.skip_to_next_chapter),
        SkipToPrevious(R.string.skip_to_previous_chapter),
        SkipToChapter(R.string.skip_to_chapter),
        SkipToTime(R.string.skip_to_time);

        fun getDescription(context: Context) = context.getString(descriptionResId)
    }
}
